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
package com.netflix.ice.basic;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.Poller;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.processor.Ec2InstanceReservationPrice;
import com.netflix.ice.processor.Ec2InstanceReservationPrice.*;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.processor.ReservationService;
import com.netflix.ice.tag.*;
import com.netflix.ice.tag.Region;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateMidnight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class BasicReservationService extends Poller implements ReservationService {
    protected Logger logger = LoggerFactory.getLogger(BasicReservationService.class);
    protected ProcessorConfig config;
    protected Map<ReservationUtilization, Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice>> ec2InstanceReservationPrices;
    protected Map<ReservationUtilization, Map<TagGroup, List<Reservation>>> reservations;
    protected ReservationPeriod term;
    protected ReservationUtilization defaultUtilization;
    protected Map<ReservationUtilization, File> files;
    protected Long futureMillis = new DateMidnight().withYearOfCentury(99).getMillis();

    protected static Map<String, String> instanceTypes = Maps.newHashMap();
    protected static Map<String, String> instanceSizes = Maps.newHashMap();
    static {
        instanceTypes.put("stdResI", "m1");
        instanceTypes.put("secgenstdResI", "m3");
        instanceTypes.put("uResI", "t1");
        instanceTypes.put("hiMemResI", "m2");
        instanceTypes.put("hiCPUResI", "c1");
        instanceTypes.put("clusterCompResI", "cc1");
        instanceTypes.put("clusterHiMemResI", "cr1");
        instanceTypes.put("clusterGPUResI", "cg1");
        instanceTypes.put("hiIoResI", "hi1");
        instanceTypes.put("hiStoreResI", "hs1");

        instanceSizes.put("xxxxxxxxl", "8xlarge");
        instanceSizes.put("xxxxl", "4xlarge");
        instanceSizes.put("xxl", "2xlarge");
        instanceSizes.put("xl", "xlarge");
        instanceSizes.put("sm", "small");
        instanceSizes.put("med", "medium");
        instanceSizes.put("lg", "large");
        instanceSizes.put("u", "micro");
    }

    public BasicReservationService(ReservationPeriod term, ReservationUtilization defaultUtilization) {
        this.term = term;
        this.defaultUtilization = defaultUtilization;

        ec2InstanceReservationPrices = Maps.newHashMap();
        for (ReservationUtilization utilization: ReservationUtilization.values()) {
            ec2InstanceReservationPrices.put(utilization, new ConcurrentSkipListMap<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice>());
        }

        reservations = Maps.newHashMap();
        for (ReservationUtilization utilization: ReservationUtilization.values()) {
            reservations.put(utilization, Maps.<TagGroup, List<Reservation>>newHashMap());
        }
    }

    public void init() {
        this.config = ProcessorConfig.getInstance();
        files = Maps.newHashMap();
        for (ReservationUtilization utilization: ReservationUtilization.values()) {
            files.put(utilization,  new File(config.localDir, "reservation_prices." + term.name() + "." + utilization.name()));
        }

        boolean fileExisted = false;
        for (ReservationUtilization utilization: ReservationUtilization.values()) {
            File file = files.get(utilization);
            AwsUtils.downloadFileIfNotExist(config.workS3BucketName, config.workS3BucketPrefix, file);
            fileExisted = file.exists();
        }
        if (!fileExisted) {
            try {
                pollAPI();
            }
            catch (Exception e) {
                logger.error("failed to poll reservation prices", e);
                throw new RuntimeException("failed to poll reservation prices for " + e.getMessage());
            }
        }
        else {
            for (ReservationUtilization utilization: ReservationUtilization.values()) {
                try {
                    File file = files.get(utilization);
                    if (file.exists()) {
                        DataInputStream in = new DataInputStream(new FileInputStream(file));
                        ec2InstanceReservationPrices.put(utilization, Serializer.deserialize(in));
                        in.close();
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException("failed to load reservation prices " + e.getMessage());
                }
            }
        }

        start(3600, 3600*24, true);
    }

    @Override
    protected void poll() throws Exception {
        logger.info("start polling reservation prices. it might take a while...");
        pollAPI();
    }

    private void pollAPI() throws Exception {
        long currentTime = new DateMidnight().getMillis();

        DescribeReservedInstancesOfferingsRequest req =  new DescribeReservedInstancesOfferingsRequest()
                .withFilters(new com.amazonaws.services.ec2.model.Filter().withName("marketplace").withValues("false"));
        String token = null;
        boolean hasNewPrice = false;
        AmazonEC2Client ec2Client = new AmazonEC2Client(AwsUtils.awsCredentialsProvider, AwsUtils.clientConfig);

        for (Region region: Region.getAllRegions()) {
            // GovCloud uses different credentials than standard AWS, so you would need two separate
            // sets of credentials if you wanted to poll for RIs in both environments. For now, we
            // just ignore GovCloud here in order to prevent AuthFailure errors.
            if (region == Region.US_GOV_WEST_1) {
                continue;
            }
            ec2Client.setEndpoint("ec2." + region.name + ".amazonaws.com");
            do {
                if (!StringUtils.isEmpty(token))
                    req.setNextToken(token);
                DescribeReservedInstancesOfferingsResult offers = ec2Client.describeReservedInstancesOfferings(req);
                token = offers.getNextToken();

                for (ReservedInstancesOffering offer: offers.getReservedInstancesOfferings()) {
                    if (offer.getProductDescription().indexOf("Amazon VPC") >= 0)
                        continue;

                    // Ignore Region-Wide RIs
                    if (offer.getAvailabilityZone() == null)
                        continue;

                    ReservationUtilization utilization = ReservationUtilization.get(offer.getOfferingType());
                    Ec2InstanceReservationPrice.ReservationPeriod term = offer.getDuration() / 24 / 3600 > 366 ?
                            Ec2InstanceReservationPrice.ReservationPeriod.threeyear : Ec2InstanceReservationPrice.ReservationPeriod.oneyear;
                    if (term != this.term)
                        continue;

                    double hourly = offer.getUsagePrice();
                    if (hourly <= 0) {
                        for (RecurringCharge recurringCharge: offer.getRecurringCharges()) {
                            if (recurringCharge.getFrequency().equals("Hourly")) {
                                hourly = recurringCharge.getAmount();
                                break;
                            }
                        }
                    }
                    UsageType usageType = getUsageType(offer.getInstanceType(), offer.getProductDescription());
                    // Unknown Zone
                    if (Zone.getZone(offer.getAvailabilityZone()) == null) {
                        logger.error("No Zone for " + offer.getAvailabilityZone());
                    } else {
                        hasNewPrice = setPrice(utilization, currentTime, Zone.getZone(offer.getAvailabilityZone()).region, usageType,
                                offer.getFixedPrice(), hourly) || hasNewPrice;

                        logger.info("Setting RI price for " + Zone.getZone(offer.getAvailabilityZone()).region + " " + utilization + " " + usageType + " " + offer.getFixedPrice() + " " + hourly);
                    }
                }
            } while (!StringUtils.isEmpty(token));
        }

        ec2Client.shutdown();
        if (hasNewPrice) {
            for (ReservationUtilization utilization: files.keySet()) {
                File file = files.get(utilization);
                DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
                try {
                    Serializer.serialize(out, this.ec2InstanceReservationPrices.get(utilization));
                    AwsUtils.upload(config.workS3BucketName, config.workS3BucketPrefix, file);
                }
                finally {
                    out.close();
                }
            }
        }
    }

    private UsageType getUsageType(String type, String productDescription) {
        return UsageType.getUsageType(type + InstanceOs.withDescription(productDescription).usageType, Operation.reservedInstancesHeavy, "");
    }

//    private UsageType getUsageType(String type, String size, boolean isWindows) {
//        type = instanceTypes.get(type);
//        size = instanceSizes.get(size);
//
//        if (type.equals("cc1") && size.equals("8xlarge"))
//            type = "cc2";
//        return UsageType.getUsageType(type + "." + size + (isWindows ? "." + InstanceOs.windows : ""), Operation.reservedInstances, "");
//    }

    public static class Serializer {
        public static void serialize(DataOutput out,
                                     Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> reservationPrices)
                throws IOException {

            out.writeInt(reservationPrices.size());
            for (Ec2InstanceReservationPrice.Key key: reservationPrices.keySet()) {
                Ec2InstanceReservationPrice.Key.Serializer.serialize(out, key);
                Ec2InstanceReservationPrice.Serializer.serialize(out, reservationPrices.get(key));
            }
        }

        public static Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> deserialize(DataInput in)
                throws IOException {

            int size = in.readInt();
            Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> result =
                    new ConcurrentSkipListMap<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice>();
            for (int i = 0; i < size; i++) {
                Ec2InstanceReservationPrice.Key key = Ec2InstanceReservationPrice.Key.Serializer.deserialize(in);
                Ec2InstanceReservationPrice price = Ec2InstanceReservationPrice.Serializer.deserialize(in);
                result.put(key, price);
            }

            return result;
        }
    }

    private boolean setPrice(ReservationUtilization utilization, long currentTime, Region region, UsageType usageType, double upfront, double hourly) {

        Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(region, usageType);
        Ec2InstanceReservationPrice reservationPrice = ec2InstanceReservationPrices.get(utilization).get(key);

        if (reservationPrice == null)  {
            reservationPrice = new Ec2InstanceReservationPrice();
            ec2InstanceReservationPrices.get(utilization).put(key, reservationPrice);
        }

        Ec2InstanceReservationPrice.Price latestHourly = reservationPrice.hourlyPrice.getCreatePrice(futureMillis);
        Ec2InstanceReservationPrice.Price latestUpfront = reservationPrice.upfrontPrice.getCreatePrice(futureMillis);

        if (latestHourly.getListPrice() == null) {
            latestHourly.setListPrice(hourly);
            latestUpfront.setListPrice(upfront);

            //logger.info("setting reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return true;
        }
        else if (latestHourly.getListPrice() != hourly || latestUpfront.getListPrice() != upfront) {
            Ec2InstanceReservationPrice.Price oldHourly = reservationPrice.hourlyPrice.getCreatePrice(currentTime);
            Ec2InstanceReservationPrice.Price oldUpfront = reservationPrice.upfrontPrice.getCreatePrice(currentTime);
            oldHourly.setListPrice(latestHourly.getListPrice());
            oldUpfront.setListPrice(latestUpfront.getListPrice());

            latestHourly.setListPrice(hourly);
            latestUpfront.setListPrice(upfront);

            //logger.info("changing reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return true;
        }
        else {
            //logger.info("exisitng reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return false;
        }
    }

    public static class Reservation {
        final int count;
        final long start;
        final long end;
        final ReservationUtilization utilization;
        final float fixedPrice;
        final float usagePrice;

        public Reservation(
                int count,
                long start,
                long end,
                ReservationUtilization utilization,
                float fixedPrice,
                float usagePrice) {
            this.count = count;
            this.start = start;
            this.end = end;
            this.utilization = utilization;
            this.fixedPrice = fixedPrice;
            this.usagePrice = usagePrice;
        }
    }

    protected double getEc2Tier(long time) {
        return 0;
    }

    public Collection<TagGroup> getTagGroups(ReservationUtilization utilization) {
        return reservations.get(utilization).keySet();
    }

    public ReservationUtilization getDefaultReservationUtilization(long time) {
        return defaultUtilization;
    }

    public double getLatestHourlyTotalPrice(
            long time,
            Region region,
            UsageType usageType,
            ReservationUtilization utilization) {
        Ec2InstanceReservationPrice ec2Price =
            ec2InstanceReservationPrices.get(utilization).get(new Ec2InstanceReservationPrice.Key(region, usageType));

        double tier = getEc2Tier(time);
        return ec2Price.hourlyPrice.getPrice(null).getPrice(tier) +
               ec2Price.upfrontPrice.getPrice(null).getUpfrontAmortized(time, term, tier);
    }

    public ReservationInfo getReservation(
            long time,
            TagGroup tagGroup,
            ReservationUtilization utilization) {

        if (utilization == ReservationUtilization.FIXED)
            return getFixedReservation(time, tagGroup);

        double tier = getEc2Tier(time);

        double upfrontAmortized = 0;
        double houlyCost = 0;

        int count = 0;
        if (this.reservations.get(utilization).containsKey(tagGroup)) {
            for (Reservation reservation : this.reservations.get(utilization).get(tagGroup)) {
                if (time >= reservation.start && time < reservation.end) {
                    count += reservation.count;
                    Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(tagGroup.region, tagGroup.usageType);
                    Ec2InstanceReservationPrice ec2Price = ec2InstanceReservationPrices.get(utilization).get(key);
                    if (ec2Price != null) { // remove this...
                        upfrontAmortized += reservation.count * ec2Price.upfrontPrice.getPrice(reservation.start).getUpfrontAmortized(reservation.start, term, tier);
                        houlyCost += reservation.count * ec2Price.hourlyPrice.getPrice(reservation.start).getPrice(tier);
                    }
                    else {
                        logger.error("Not able to find reservation price for " + key);
                    }
                }
            }
        }

        if (count == 0) {
            Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(tagGroup.region, tagGroup.usageType);
            Ec2InstanceReservationPrice ec2Price = ec2InstanceReservationPrices.get(utilization).get(key);
            if (ec2Price != null) { // remove this...
                upfrontAmortized = ec2Price.upfrontPrice.getPrice(null).getUpfrontAmortized(time, term, tier);
                houlyCost = ec2Price.hourlyPrice.getPrice(null).getPrice(tier);
            }
        }
        else {
            upfrontAmortized = upfrontAmortized / count;
            houlyCost = houlyCost / count;
        }

        return new ReservationInfo(count, upfrontAmortized, houlyCost);
    }

    private ReservationInfo getFixedReservation(
            long time,
            TagGroup tagGroup) {

        double upfrontAmortized = 0;
        double houlyCost = 0;

        int count = 0;
        if (this.reservations.get(ReservationUtilization.FIXED).containsKey(tagGroup)) {
            for (Reservation reservation : this.reservations.get(ReservationUtilization.FIXED).get(tagGroup)) {
                if (time >= reservation.start && time < reservation.end) {
                    count += reservation.count;
                    upfrontAmortized += reservation.count * reservation.fixedPrice / ((reservation.end - reservation.start) / AwsUtils.hourMillis);
                    houlyCost += reservation.count * reservation.usagePrice;
                }
            }
        }

        if (count > 0) {
            upfrontAmortized = upfrontAmortized / count;
            houlyCost = houlyCost / count;
        }

        return new ReservationInfo(count, upfrontAmortized, houlyCost);
    }

    public void updateEc2Reservations(Map<String, ReservedInstances> reservationsFromApi) {
        Map<ReservationUtilization, Map<TagGroup, List<Reservation>>> reservationMap = Maps.newTreeMap();
        for (ReservationUtilization utilization: ReservationUtilization.values()) {
            reservationMap.put(utilization, Maps.<TagGroup, List<Reservation>>newHashMap());
        }

        for (String key: reservationsFromApi.keySet()) {
            ReservedInstances reservedInstances = reservationsFromApi.get(key);
            if (reservedInstances.getInstanceCount() <= 0)
                continue;

            String accountId = key.substring(0, key.indexOf(","));
            Account account = config.accountService.getAccountById(accountId);
            Zone zone = Zone.getZone(reservedInstances.getAvailabilityZone());
            if (zone == null)
                logger.error("Not able to find zone for reserved instances " + reservedInstances.getAvailabilityZone());

            ReservationUtilization utilization = ReservationUtilization.get(reservedInstances.getOfferingType());
            long endTime = Math.min(reservedInstances.getEnd().getTime(), reservedInstances.getStart().getTime() + reservedInstances.getDuration() * 1000);
            if (endTime <= config.startDate.getMillis())
                continue;
            Reservation reservation = new Reservation(reservedInstances.getInstanceCount(), reservedInstances.getStart().getTime(), endTime, utilization, reservedInstances.getFixedPrice(), reservedInstances.getUsagePrice());

            String osStr = reservedInstances.getProductDescription();
            InstanceOs os = InstanceOs.withDescription(osStr);

            UsageType usageType = UsageType.getUsageType(reservedInstances.getInstanceType() + os.usageType, "hours");

            TagGroup reservationKey = new TagGroup(account, zone.region, zone, Product.ec2_instance, Operation.getReservedInstances(utilization), usageType, null);

            List<Reservation> reservations = reservationMap.get(utilization).get(reservationKey);
            if (reservations == null) {
                reservationMap.get(utilization).put(reservationKey, Lists.<Reservation>newArrayList(reservation));
            }
            else {
                reservations.add(reservation);
            }
        }

        this.reservations = reservationMap;
    }
}
