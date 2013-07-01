/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.processor;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import com.csvreader.CsvReader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.*;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Operation;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Zone;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Months;
import org.joda.time.Weeks;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Class to process billing files and produce tag, usage, cost output files for reader/UI.
 */
public class BillingFileProcessor extends Poller {

    private static Map<String, Double> ondemandRate = Maps.newHashMap();
    private ProcessorConfig config = ProcessorConfig.getInstance();
    private Long startMilli;
    private Long endMilli;
    private boolean processingMonitor;
    private Map<Product, ReadWriteData> usageDataByProduct;
    private Map<Product, ReadWriteData> costDataByProduct;
    private Double ondemandThreshold;
    private String fromEmail;
    private String alertEmails;
    private String urlPrefix;

    public BillingFileProcessor(String urlPrefix, Double ondemandThreshold, String fromEmail, String alertEmails) {
        this.ondemandThreshold = ondemandThreshold;
        this.fromEmail = fromEmail;
        this.alertEmails = alertEmails;
        this.urlPrefix = urlPrefix;
    }

    @Override
    protected void poll() throws Exception {

        // list the tar.gz file in billing file folder
        List<S3ObjectSummary> objectSummaries = AwsUtils.listAllObjects(config.billingS3BucketName, config.billingS3BucketPrefix);
        logger.info("found " + objectSummaries.size() + " in billing bucket...");
        TreeMap<DateTime, S3ObjectSummary> filesToProcess = Maps.newTreeMap();
        Map<DateTime, S3ObjectSummary> monitorFilesToProcess = Maps.newTreeMap();

        // for each file, download&process if not needed
        DateTime currentTime = new DateTime(DateTimeZone.UTC);
        for (S3ObjectSummary objectSummary : objectSummaries) {

            String fileKey = objectSummary.getKey();
            DateTime dataTime = config.resourceService == null ? null : AwsUtils.getDateTimeFromFileNameWithTags(fileKey);
            if (dataTime == null) {
                dataTime = AwsUtils.getDateTimeFromFileName(fileKey);
            }

            if (dataTime != null && !dataTime.isBefore(config.startDate)) {
                filesToProcess.put(dataTime, objectSummary);
            }
            else {
                logger.info("ignoring file " + objectSummary.getKey());
            }
        }

        for (S3ObjectSummary objectSummary : objectSummaries) {
            String fileKey = objectSummary.getKey();
            DateTime dataTime = AwsUtils.getDateTimeFromFileNameWithMonitoring(fileKey);

            if (dataTime != null && !dataTime.isBefore(config.startDate)) {
                monitorFilesToProcess.put(dataTime, objectSummary);
            }
        }

        for (DateTime dataTime: filesToProcess.keySet()) {
            S3ObjectSummary objectSummary = filesToProcess.get(dataTime);
            startMilli = endMilli = dataTime.getMillis();
            init();

            long lastProcessed = AwsUtils.getLastModified(config.workS3BucketName, config.workS3BucketPrefix + "usage_hourly_all_" + AwsUtils.monthDateFormat.print(dataTime)) - 3*3600000L;
            if (objectSummary.getLastModified().getTime() < lastProcessed) {
                logger.info("data has been processed. ignoring " + objectSummary.getKey() + "...");
                continue;
            }

            String fileKey = objectSummary.getKey();
            File file = new File(config.localDir, fileKey.substring(fileKey.lastIndexOf("/") + 1));
            logger.info("trying to download " + fileKey + "...");
            boolean downloaded = AwsUtils.downloadFileIfChangedSince(config.billingS3BucketName, config.billingS3BucketPrefix, file, lastProcessed);
            if (downloaded)
                logger.info("downloaded " + fileKey);
            else {
                logger.info("file already downloaded " + fileKey + "...");
            }

            logger.info("processing " + fileKey + "...");
            boolean withTags = fileKey.contains("with-resources-and-tags");
            processingMonitor = false;
            processBillingZipFile(file, withTags);
            logger.info("done processing " + fileKey);

            S3ObjectSummary monitorObjectSummary = monitorFilesToProcess.get(dataTime);
            if (monitorObjectSummary != null) {
                String monitorFileKey = monitorObjectSummary.getKey();
                logger.info("processing " + monitorFileKey + "...");
                File monitorFile = new File(config.localDir, monitorFileKey.substring(monitorFileKey.lastIndexOf("/") + 1));
                logger.info("trying to download " + monitorFileKey + "...");
                downloaded = AwsUtils.downloadFileIfChangedSince(config.billingS3BucketName, config.billingS3BucketPrefix, monitorFile, lastProcessed);
                if (downloaded)
                    logger.info("downloaded " + monitorFile);
                else
                    logger.warn(monitorFile + "already downloaded...");
                FileInputStream in = new FileInputStream(monitorFile);
                try {
                    processBillingFile(monitorFile.getName(), in, withTags);
                }
                catch (Exception e) {
                    logger.error("Error processing " + monitorFile, e);
                }
                finally {
                    in.close();
                }
            }

            if (Months.monthsBetween(dataTime, currentTime).getMonths() == 0) {
                int hours = (int) ((endMilli - startMilli)/3600000L);
                logger.info("cut hours to " + hours);
                cutData(hours);
            }

            // now get reservation capacity to calculate upfront and un-used cost
            processReservations();

            if (withTags)
                config.resourceService.commit();

            logger.info("archiving results for " + dataTime + "...");
            archive();
            logger.info("done archiving " + dataTime);

            if (dataTime.equals(filesToProcess.lastKey()))
                sendOndemandCostAlert();
        }

        logger.info("AWS usage processed.");
    }

    private double getSum(List<TagGroup> tagGroups, Map<TagGroup, Double> map) {
        double sum = 0;
        for (TagGroup tagGroup: tagGroups) {
            Double v = map.get(tagGroup);
            if (v != null)
                sum += v;
        }
        return sum;
    }

    private double getValue(Map<TagGroup, Double> map, TagGroup tagGroup) {
        Double v = map.get(tagGroup);
        return v == null ? 0 : v;
    }

    private void adjustUsage(
            ReservationService.ReservationInfo reservation,
            Map<TagGroup, Double> usage,
            Map<TagGroup, Double> cost,
            TagGroup ondemandTagGroup,
            TagGroup reservedTagGroup,
            TagGroup unusedTagGroup,
            TagGroup lentTagGroup,
            List<TagGroup> borrowedTagGroups,
            List<TagGroup> ondemandTagGroups) {

        double ondemandUsage = getSum(ondemandTagGroups, usage);
        double ondemandCost = getSum(ondemandTagGroups, cost);
        double borrowedUsage = getSum(borrowedTagGroups, usage);
        double borrowedCost = getSum(borrowedTagGroups, cost);

        double unusedUsage = reservation.capacity - borrowedUsage;
        double unusedCost = reservation.capacity * reservation.reservationHourlyCost - borrowedCost;

        if (unusedUsage < 0 || unusedUsage > 0 && ondemandUsage > 0) {

            double toRemoveUsage = 0;
            double toRemoveRate = 0;
            double toAddRate = 0;
            List<TagGroup> fromTagGroups = null;
            List<TagGroup> toTagGroups = null;
            if (unusedUsage < 0) {
                toRemoveUsage = -unusedUsage;
                toRemoveRate = reservation.reservationHourlyCost;
                if (ondemandUsage == 0) {
                    String key = ondemandTagGroup.operation + "|" + ondemandTagGroup.region + "|" + ondemandTagGroup.usageType;
                    if (ondemandRate.get(key) == null) {
                        int iii = 0;
                        toAddRate = toRemoveRate;
                    }
                    else
                        toAddRate = ondemandRate.get(key);
                }
                else
                    toAddRate = ondemandCost / ondemandUsage;
                fromTagGroups = borrowedTagGroups;
                toTagGroups = ondemandTagGroups;
            }
            else if (unusedUsage > 0 && ondemandUsage > 0) {
                toRemoveUsage = Math.min(unusedUsage, ondemandUsage);
                toRemoveRate = ondemandCost / ondemandUsage;
                toAddRate = reservation.reservationHourlyCost;
                fromTagGroups = ondemandTagGroups;
                toTagGroups = borrowedTagGroups;
            }

            for (int i = 0; i < fromTagGroups.size(); i++) {
                TagGroup from = fromTagGroups.get(i);
                TagGroup to = toTagGroups.get(i);

                Double u = usage.get(from);
                Double c = cost.get(from);
                if (u == null)
                    continue;

                double deltaUsage = Math.min(toRemoveUsage, u);
                double deltaCost = deltaUsage * toRemoveRate;

                u -= deltaUsage;
                c -= deltaCost;

                usage.put(from, u);
                cost.put(from, c);

                u = usage.get(to);
                c = cost.get(to);

                deltaCost =  deltaUsage * toAddRate;
                u = u == null ? deltaUsage : u + deltaUsage;
                c = c == null ? deltaCost : c + deltaCost;

                usage.put(to, u);
                cost.put(to, c);

                toRemoveUsage -= deltaUsage;
                if (toRemoveUsage <= 0)
                    break;
            }

            if (toRemoveUsage > 0) {
                int iii = 0;
            }
        }

        ondemandUsage = getSum(ondemandTagGroups, usage);
        ondemandCost = getSum(ondemandTagGroups, cost);
        borrowedUsage = getSum(borrowedTagGroups, usage);
        borrowedCost = getSum(borrowedTagGroups, cost);

        unusedUsage = reservation.capacity - borrowedUsage;
        unusedCost = reservation.capacity * reservation.reservationHourlyCost - borrowedCost;

        if (unusedUsage < 0 || unusedCost < -0.0001 || unusedUsage > 0 && ondemandUsage > 0 || unusedCost > 0.0001 && ondemandCost > 0.0001) {
            int iii = 0;

            for (TagGroup tg: borrowedTagGroups) {
                Double u = usage.get(tg);
                Double c = cost.get(tg);

                if (u != null) {
                    double rate = c / u;
                    rate = c / u;
                }
            }
        }
        usage.put(lentTagGroup, borrowedUsage - getValue(usage, reservedTagGroup));
        cost.put(lentTagGroup, borrowedCost - getValue(cost, reservedTagGroup));
        usage.put(unusedTagGroup, unusedUsage);
        cost.put(unusedTagGroup, unusedCost);
    }

    private void processReservations() {

        ReadWriteData usageData = usageDataByProduct.get(null);
        ReadWriteData costData = costDataByProduct.get(null);

        Map<Account, List<Account>> reservationAccounts = config.accountService.getReservationAccounts();
        Set<Account> reservationOwners = reservationAccounts.keySet();
        Map<Account, Account> reservationBorrowers = Maps.newHashMap();
        for (Account account: reservationAccounts.keySet()) {
            List<Account> list = reservationAccounts.get(account);
            for (Account borrowingAccount: list)
                reservationBorrowers.put(borrowingAccount, account);
        }

        // first mark borrowed instances
        Set<TagGroup> toMarkBorrowing = Sets.newTreeSet();
        Set<TagGroup> toMarkUnused = Sets.newTreeSet();
        for (TagGroup tagGroup: usageData.getTagGroups()) {
            if (tagGroup.resourceGroup == null &&
                tagGroup.product == Product.ec2_instance &&
                (tagGroup.operation == Operation.reservedInstances)) {

                toMarkBorrowing.add(tagGroup);
            }
        }
        for (TagGroup tagGroup: toMarkBorrowing) {
            TagGroup borrowedTagGroup = new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, Operation.borrowedInstances, tagGroup.usageType, null);
            for (int i = 0; i < usageData.getNum(); i++) {

                Map<TagGroup, Double> usageMap = usageData.getData(i);
                Map<TagGroup, Double> costMap = costData.getData(i);

                if (reservationOwners.contains(tagGroup.account) && reservationBorrowers.containsKey(tagGroup.account)) {
                    ReservationService.ReservationInfo reservation = config.reservationService.getReservation(startMilli + i * AwsUtils.hourMillis, tagGroup);
                    Double value = usageMap.get(tagGroup);

                    if (value != null && value > reservation.capacity) {

                        Zone mappedZone = config.accountService.getAccountMappedZone(reservationBorrowers.get(tagGroup.account), tagGroup.account, tagGroup.zone);
                        if (mappedZone == null) {
                            mappedZone = tagGroup.zone;
                            logger.info("failed to find mapped zone for " + reservationBorrowers.get(tagGroup.account) + " " + tagGroup.account + " " + tagGroup.zone);
                        }
                        TagGroup fromTagGroup = new TagGroup(reservationBorrowers.get(tagGroup.account), tagGroup.region, mappedZone, tagGroup.product, Operation.reservedInstances, tagGroup.usageType, null);
                        TagGroup lentTagGroup = new TagGroup(reservationBorrowers.get(tagGroup.account), tagGroup.region, mappedZone, tagGroup.product, Operation.lentInstances, tagGroup.usageType, null);
                        ReservationService.ReservationInfo reservationBorrowed = config.reservationService.getReservation(startMilli + i * AwsUtils.hourMillis, fromTagGroup);
                        if (reservationBorrowed.capacity == 0) {
                            int iii = 0;
                        }
                        usageMap.put(tagGroup, new Double(reservation.capacity));
                        usageMap.put(borrowedTagGroup, value - reservation.capacity);
                        usageMap.put(lentTagGroup, value - reservation.capacity);

                        costMap.put(tagGroup, reservation.capacity * reservation.reservationHourlyCost);
                        costMap.put(borrowedTagGroup, (value - reservation.capacity) * reservationBorrowed.reservationHourlyCost);
                        costMap.put(lentTagGroup, (value - reservation.capacity) * reservationBorrowed.reservationHourlyCost);

                        toMarkUnused.add(fromTagGroup);
                    }
                    else if (value != null) {
                        costMap.put(tagGroup, value * reservation.reservationHourlyCost);
                    }
                }
                else if (reservationOwners.contains(tagGroup.account)) {
                    Double value = usageMap.get(tagGroup);
                    if (value != null) {
                        ReservationService.ReservationInfo reservation = config.reservationService.getReservation(startMilli + i * AwsUtils.hourMillis, tagGroup);
                        if (reservation.capacity == 0) {
                            int iii = 0;
                        }
                        costMap.put(tagGroup, value * reservation.reservationHourlyCost);
                    }
                }
                else if (reservationBorrowers.get(tagGroup.account) != null) {
                    Double value = usageMap.remove(tagGroup);
                    if (value != null) {
                        Zone mappedZone = config.accountService.getAccountMappedZone(reservationBorrowers.get(tagGroup.account), tagGroup.account, tagGroup.zone);
                        if (mappedZone == null) {
                            mappedZone = tagGroup.zone;
                            logger.info("failed to find mapped zone for " + reservationBorrowers.get(tagGroup.account) + " " + tagGroup.account + " " + tagGroup.zone);
                        }
                        TagGroup fromTagGroup = new TagGroup(reservationBorrowers.get(tagGroup.account), tagGroup.region, mappedZone, tagGroup.product, Operation.reservedInstances, tagGroup.usageType, null);
                        TagGroup lentTagGroup = new TagGroup(reservationBorrowers.get(tagGroup.account), tagGroup.region, mappedZone, tagGroup.product, Operation.lentInstances, tagGroup.usageType, null);
                        ReservationService.ReservationInfo reservationBorrowed = config.reservationService.getReservation(startMilli + i * AwsUtils.hourMillis, fromTagGroup);
                        if (reservationBorrowed.capacity == 0) {
                            int iii = 0;
                        }
                        usageMap.put(borrowedTagGroup, value);
                        usageMap.put(lentTagGroup, value);
                        costMap.remove(tagGroup);
                        costMap.put(borrowedTagGroup, value * reservationBorrowed.reservationHourlyCost);
                        costMap.put(lentTagGroup, value * reservationBorrowed.reservationHourlyCost);
                        toMarkUnused.add(fromTagGroup);
                    }
                }

                if (costMap.size() != usageMap.size()) {
                    int iii = 0;
                }
            }
        }

        // now mark unused and lent instancs
        toMarkUnused.addAll(config.reservationService.getTaGroups());
        for (TagGroup tagGroup: toMarkUnused) {

            TagGroup upfrontTagGroup = new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, Operation.upfrontAmortized, tagGroup.usageType, null);
            TagGroup ondemandTagGroup = new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, Operation.ondemandInstances, tagGroup.usageType, null);
            TagGroup unusedTagGroup = new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, Operation.unusedInstances, tagGroup.usageType, null);
            TagGroup lentTagGroup = new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, Operation.lentInstances, tagGroup.usageType, null);
            TagGroup reservedTagGroup = tagGroup;

            List<TagGroup> borrowedTagGroups = Lists.newArrayList();
            for (Account borrowingAccount: reservationAccounts.get(tagGroup.account)) {
                Zone mappedZone = config.accountService.getAccountMappedZone(borrowingAccount, tagGroup.account, tagGroup.zone);
                if (mappedZone == null) {
                    mappedZone = tagGroup.zone;
                    logger.info("failed to find mapped zone for " + borrowingAccount + " " + tagGroup.account + " " + tagGroup.zone);
                }
                borrowedTagGroups.add(new TagGroup(borrowingAccount, tagGroup.region, mappedZone, tagGroup.product, Operation.borrowedInstances, tagGroup.usageType, null));
            }
            borrowedTagGroups.add(reservedTagGroup);

            List<TagGroup> ondemandTagGroups = Lists.newArrayList();
            for (Account borrowingAccount: reservationAccounts.get(tagGroup.account)) {
                Zone mappedZone = config.accountService.getAccountMappedZone(borrowingAccount, tagGroup.account, tagGroup.zone);
                if (mappedZone == null) {
                    mappedZone = tagGroup.zone;
                    logger.info("failed to find mapped zone for " + borrowingAccount + " " + tagGroup.account + " " + tagGroup.zone);
                }
                ondemandTagGroups.add(new TagGroup(borrowingAccount, tagGroup.region, mappedZone, tagGroup.product, Operation.ondemandInstances, tagGroup.usageType, null));
            }
            ondemandTagGroups.add(ondemandTagGroup);

            for (int i = 0; i < usageData.getNum(); i++) {
                ReservationService.ReservationInfo reservation = config.reservationService.getReservation(startMilli + i * AwsUtils.hourMillis, reservedTagGroup);

                Map<TagGroup, Double> usageMap = usageData.getData(i);
                Map<TagGroup, Double> costMap = costData.getData(i);

                if (reservation.capacity > 0) {
                    costMap.put(upfrontTagGroup, reservation.capacity * reservation.upfrontAmortized);
                }

                adjustUsage(
                    reservation,
                    usageMap,
                    costMap,
                    ondemandTagGroup,
                    reservedTagGroup,
                    unusedTagGroup,
                    lentTagGroup,
                    borrowedTagGroups,
                    ondemandTagGroups);
            }
        }
    }

    private void cutData(int hours) {
        for (ReadWriteData data: usageDataByProduct.values()) {
            data.cutData(hours);
        }
        for (ReadWriteData data: costDataByProduct.values()) {
            data.cutData(hours);
        }
    }

    private void archive() throws Exception {

        logger.info("archiving tag data...");

        for (Product product: costDataByProduct.keySet()) {
            TagGroupWriter writer = new TagGroupWriter(product == null ? "all" : product.name);
            writer.archive(startMilli, costDataByProduct.get(product).getTagGroups());
        }

        logger.info("archiving summary data...");

        archiveSummary(usageDataByProduct, "usage_");
        archiveSummary(costDataByProduct, "cost_");

        logger.info("archiving hourly data...");

        archiveHourly(usageDataByProduct, "usage_");
        archiveHourly(costDataByProduct, "cost_");

        logger.info("archiving data done.");
    }

    private void archiveHourly(Map<Product, ReadWriteData> dataMap, String prefix) throws Exception {
        DateTime monthDateTime = new DateTime(startMilli, DateTimeZone.UTC);
        for (Product product: dataMap.keySet()) {
            String prodName = product == null ? "all" : product.name;
            DataWriter writer = new DataWriter(prefix + "hourly_" + prodName + "_" + AwsUtils.monthDateFormat.print(monthDateTime), false);
            writer.archive(dataMap.get(product));
        }
    }

    private void addValue(List<Map<TagGroup, Double>> list, int index, TagGroup tagGroup, double v) {
        Map<TagGroup, Double> map = ReadWriteData.getCreateData(list, index);
        Double existedV = map.get(tagGroup);
        map.put(tagGroup, existedV == null ? v : existedV + v);
    }


    private void archiveSummary(Map<Product, ReadWriteData> dataMap, String prefix) throws Exception {

        DateTime monthDateTime = new DateTime(startMilli, DateTimeZone.UTC);

        for (Product product: dataMap.keySet()) {

            String prodName = product == null ? "all" : product.name;
            ReadWriteData data = dataMap.get(product);
            Collection<TagGroup> tagGroups = data.getTagGroups();

            // init daily, weekly and monthly
            List<Map<TagGroup, Double>> daily = Lists.newArrayList();
            List<Map<TagGroup, Double>> weekly = Lists.newArrayList();
            List<Map<TagGroup, Double>> monthly = Lists.newArrayList();

            // get last month data
            ReadWriteData lastMonthData = new DataWriter(prefix + "hourly_" + prodName + "_" + AwsUtils.monthDateFormat.print(monthDateTime.minusMonths(1)), true).getData();

            // aggregate to daily, weekly and monthly
            int dayOfWeek = monthDateTime.getDayOfWeek();
            int daysFromLastMonth = dayOfWeek - 1;
            int lastMonthNumHours = monthDateTime.minusMonths(1).dayOfMonth().getMaximumValue() * 24;
            for (int hour = 0 - daysFromLastMonth * 24; hour < data.getNum(); hour++) {
                if (hour < 0) {
                    // handle data from last month, add to weekly
                    Map<TagGroup, Double> prevData = lastMonthData.getData(lastMonthNumHours + hour);
                    for (TagGroup tagGroup: tagGroups) {
                        Double v = prevData.get(tagGroup);
                        if (v != null && v != 0) {
                            addValue(weekly, 0, tagGroup, v);
                        }
                    }
                }
                else {
                    // this month, add to weekly, monthly and daily
                    Map<TagGroup, Double> map = data.getData(hour);

                    for (TagGroup tagGroup: tagGroups) {
                        Double v = map.get(tagGroup);
                        if (v != null && v != 0) {
                            addValue(monthly, 0, tagGroup, v);
                            addValue(daily, hour/24, tagGroup, v);
                            addValue(weekly, (hour + daysFromLastMonth*24) / 24/7, tagGroup, v);
                        }
                    }
                }
            }

            // archive daily
            int year = monthDateTime.getYear();
            DataWriter writer = new DataWriter(prefix + "daily_" + prodName + "_" + year, true);
            ReadWriteData dailyData = writer.getData();
            dailyData.setData(daily, monthDateTime.getDayOfYear() -1, false);
            writer.archive();

            // archive monthly
            writer = new DataWriter(prefix + "monthly_" + prodName, true);
            ReadWriteData monthlyData = writer.getData();
            monthlyData.setData(monthly, Months.monthsBetween(config.startDate, monthDateTime).getMonths(), false);
            writer.archive();

            // archive weekly
            writer = new DataWriter(prefix + "weekly_" + prodName, true);
            ReadWriteData weeklyData = writer.getData();
            DateTime weekStart = monthDateTime.withDayOfWeek(1);
            int index;
            if (!weekStart.isAfter(config.startDate))
                index = 0;
            else
                index = Weeks.weeksBetween(config.startDate, weekStart).getWeeks() + (config.startDate.dayOfWeek() == weekStart.dayOfWeek() ? 0 : 1);
            weeklyData.setData(weekly, index, true);
            writer.archive();
        }
    }

    private void init() {
        usageDataByProduct = new HashMap<Product, ReadWriteData>();
        costDataByProduct = new HashMap<Product, ReadWriteData>();
        usageDataByProduct.put(null, new ReadWriteData());
        costDataByProduct.put(null, new ReadWriteData());
    }

    private void processBillingZipFile(File file, boolean withTags) throws IOException {

        InputStream input = new FileInputStream(file);
        ZipInputStream zipInput;

        zipInput = new ZipInputStream(input);

        try {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;

                processBillingFile(entry.getName(), zipInput, withTags);
            }
        }
        catch (IOException e) {
            if (e.getMessage().equals("Stream closed"))
                logger.info("reached end of file.");
            else
                logger.error("Error processing " + file, e);
        }
        finally {
            try {
                zipInput.close();
            } catch (IOException e) {
                logger.error("Error closing " + file, e);
            }
            try {
                input.close();
            }
            catch (IOException e1) {
                logger.error("Cannot close input for " + file, e1);
            }
        }
    }

    private void processBillingFile(String fileName, InputStream tempIn, boolean withTags) {

        CsvReader reader = new CsvReader(new InputStreamReader(tempIn), ',');

        long lineNumber = 0;
        List<String[]> delayedItems = Lists.newArrayList();
        try {
            reader.readRecord();
            String[] headers = reader.getValues();

            config.lineItemProcessor.initIndexes(withTags, headers);

            while (reader.readRecord()) {
                String[] items = reader.getValues();
                try {
                    processOneLine(delayedItems, items);
                }
                catch (Exception e) {
                    logger.error(StringUtils.join(items, ","), e);
                }
                lineNumber++;

                if (lineNumber % 500000 == 0) {
                    logger.info("processed " + lineNumber + " lines...");
                }
//                if (lineNumber == 40000000) {//100000000      //
//                    break;
//                }
            }
        }
        catch (IOException e ) {
            logger.error("Error processing " + fileName + " at line " + lineNumber, e);
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
                logger.error("Cannot close BufferedReader...", e);
            }
        }

        for (String[] items: delayedItems) {
            processOneLine(null, items);
        }
    }

    private void processOneLine(List<String[]> delayedItems, String[] items) {

        LineItemProcessor.Result result = config.lineItemProcessor.process(startMilli, delayedItems == null, config, items, usageDataByProduct, costDataByProduct, ondemandRate);

        if (result == LineItemProcessor.Result.delay) {
            delayedItems.add(items);
        }
        else if (result == LineItemProcessor.Result.hourly && !processingMonitor) {
            endMilli = Math.max(endMilli, config.lineItemProcessor.getEndMillis(items));
        }
    }

    private Map<Long, Map<Ec2InstanceReservationPrice.Key, Double>> getOndemandCosts(long fromMillis) {
        Map<Long, Map<Ec2InstanceReservationPrice.Key, Double>> ondemandCostsByHour = Maps.newHashMap();
        ReadWriteData costs = costDataByProduct.get(null);

        Collection<TagGroup> tagGroups = costs.getTagGroups();
        for (int i = 0; i < costs.getNum(); i++) {
            Long millis = startMilli + i * AwsUtils.hourMillis;
            if (millis <= fromMillis)
                continue;

            Map<Ec2InstanceReservationPrice.Key, Double> ondemandCosts = Maps.newHashMap();
            ondemandCostsByHour.put(millis, ondemandCosts);

            Map<TagGroup, Double> data = costs.getData(i);
            for (TagGroup tagGroup : tagGroups) {
                if (tagGroup.product == Product.ec2_instance && tagGroup.operation == Operation.ondemandInstances &&
                    data.get(tagGroup) != null) {
                    Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(tagGroup.region, tagGroup.usageType);
                    if (ondemandCosts.get(key) != null)
                        ondemandCosts.put(key, data.get(tagGroup) + ondemandCosts.get(key));
                    else
                        ondemandCosts.put(key, data.get(tagGroup));
                }
            }
        }

        return ondemandCostsByHour;
    }

    private Long lastAlertMillis() {
        AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();
        InputStream in = null;
        try {
            in = s3Client.getObject(config.workS3BucketName, config.workS3BucketPrefix + "ondemandAlertMillis").getObjectContent();
            return Long.parseLong(IOUtils.toString(in));
        }
        catch (Exception e) {
            logger.error("Error reading from ondemandAlertMillis file", e);
            return 0L;
        }
        finally {
            if (in != null)
                try {in.close();} catch (Exception e){}
        }
    }

    private void updateLastAlertMillis(Long millis) {
        AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();
        s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + "ondemandAlertMillis", IOUtils.toInputStream(millis.toString()), new ObjectMetadata());
    }

    private void sendOndemandCostAlert() {

        if (ondemandThreshold == null || StringUtils.isEmpty(fromEmail) || StringUtils.isEmpty(alertEmails) ||
            new Date().getTime() < lastAlertMillis() + AwsUtils.hourMillis * 24)
            return;

        Map<Long, Map<Ec2InstanceReservationPrice.Key, Double>> ondemandCosts = getOndemandCosts(lastAlertMillis() + AwsUtils.hourMillis * 24);
        Long maxHour = null;
        double maxTotal = ondemandThreshold;

        for (Long hour: ondemandCosts.keySet()) {
            double total = 0;
            for (Double value: ondemandCosts.get(hour).values())
                total += value;

            if (total > ondemandThreshold) {
                maxHour = hour;
                maxTotal = total;
            }
        }

        if (maxHour != null) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
            String subject = String.format("Alert: Ondemand cost per hour reached $%s at %s",
                    numberFormat.format(maxTotal), AwsUtils.dateFormatter.print(maxHour));
            StringBuilder body = new StringBuilder();
            body.append(String.format("Total ondemand cost $%s at %s:<br><br>",
                    numberFormat.format(maxTotal), AwsUtils.dateFormatter.print(maxHour)));
            TreeMap<Double, String> costs = Maps.newTreeMap();
            for (Map.Entry<Ec2InstanceReservationPrice.Key, Double> entry: ondemandCosts.get(maxHour).entrySet()) {
                costs.put(entry.getValue(), entry.getKey().region + " " + entry.getKey().usageType + ": ");
            }
            for (Double cost: costs.descendingKeySet()) {
                if (cost > 0)
                    body.append(costs.get(cost)).append("$" + numberFormat.format(cost)).append("<br>");
            }
            body.append("<br>Please go to <a href=\"" + urlPrefix + "dashboard/reservation#usage_cost=cost&groupBy=UsageType&product=ec2_instance&operation=OndemandInstances\">Ice</a> for details.");
            SendEmailRequest request = new SendEmailRequest();
            request.withSource(fromEmail);
            List<String> emails = Lists.newArrayList(alertEmails.split(","));
            request.withDestination(new Destination(emails));
            request.withMessage(new Message(new Content(subject), new Body().withHtml(new Content(body.toString()))));

            AmazonSimpleEmailServiceClient emailService = AwsUtils.getAmazonSimpleEmailServiceClient();
            try {
                emailService.sendEmail(request);
                updateLastAlertMillis(maxHour);
                logger.info("updateLastAlertMillis " + maxHour);
            }
            catch (Exception e) {
                logger.error("Error in sending alert emails", e);
            }
        }
    }
}

