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

import com.google.common.cache.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.*;
import com.netflix.ice.reader.*;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Tag;
import com.netflix.ice.tag.TagType;
import org.joda.time.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * This class reads data from s3 bucket and feeds the data to UI
 */
public class BasicDataManager extends Poller implements DataManager {

    protected ReaderConfig config = ReaderConfig.getInstance();
    protected String dbName;
    protected ConsolidateType consolidateType;
    protected Product product;

    protected Map<DateTime, File> fileCache = Maps.newConcurrentMap();
    protected LoadingCache<DateTime, ReadOnlyData> data = CacheBuilder.newBuilder()
       .maximumSize(config.monthlyCacheSize)
       .removalListener(new RemovalListener<DateTime, ReadOnlyData>() {
           public void onRemoval(RemovalNotification<DateTime, ReadOnlyData> objectRemovalNotification) {
               logger.info(dbName + " removing from file cache " + objectRemovalNotification.getKey());
               fileCache.remove(objectRemovalNotification.getKey());
           }
       })
       .build(
               new CacheLoader<DateTime, ReadOnlyData>() {
                   public ReadOnlyData load(DateTime monthDate) throws Exception {
                       return loadData(monthDate);
                   }
               });

    public BasicDataManager(Product product, ConsolidateType consolidateType, boolean isCost) {
        this.product = product;
        this.consolidateType = consolidateType;
        this.dbName = (isCost ? "cost_" : "usage_") + consolidateType + "_" + (product == null ? "all" : product.s3Name);

        start(300);
    }

    /**
     * We check if new data is available periodically
     * @throws Exception
     */
    @Override
    protected void poll() throws Exception {
        logger.info(dbName + " start polling...");
        for (DateTime key: Sets.newHashSet(fileCache.keySet())) {
            File file = fileCache.get(key);
            try {
                logger.info("trying to download " + file);
                boolean downloaded = downloadFile(file);
                if (downloaded) {
                    ReadOnlyData newData = loadDataFromFile(file);
                    data.put(key, newData);
                    fileCache.put(key, file);
                }
            }
            catch (Exception e) {
                logger.error("failed to download " + file, e);
            }
        }
    }

    @Override
    protected String getThreadName() {
        return this.dbName;
    }

    private ReadOnlyData loadData(DateTime monthDate) throws InterruptedException {
        while (true) {
            File file = getDownloadFile(monthDate);
            try {
                ReadOnlyData result = loadDataFromFile(file);
                fileCache.put(monthDate, file);
                return result;
            }
            catch (FileNotFoundException e) {
                logger.error("error in loading data for " + monthDate + " " + this.dbName, e);
                fileCache.put(monthDate, file);
                return new ReadOnlyData(new double[][]{}, Lists.<TagGroup>newArrayList());
            }
            catch (Exception e) {
                logger.error("error in loading data for " + monthDate + " " + this.dbName, e);
                if (file.delete())
                    logger.info("deleted corrupted file " + file);
                else
                    logger.error("not able to delete corrupted file " + file);
                Thread.sleep(2000L);
            }
        }
    }

    private synchronized File getDownloadFile(DateTime monthDate) {
        File file = getFile(monthDate);
        downloadFile(file);
        return file;
    }

    private File getFile(DateTime monthDate) {
        File file = new File(config.localDir, this.dbName);
        if (consolidateType == ConsolidateType.hourly)
            file = new File(config.localDir, this.dbName + "_" + AwsUtils.monthDateFormat.print(monthDate));
        else if (consolidateType == ConsolidateType.daily)
            file = new File(config.localDir, this.dbName + "_" + monthDate.getYear());

        return file;
    }

    private synchronized boolean downloadFile(File file) {
        try {
            return AwsUtils.downloadFileIfChanged(config.workS3BucketName, config.workS3BucketPrefix, file, 0);
        }
        catch (Exception e) {
            logger.error("error downloading " + file, e);
            return false;
        }
    }

    private ReadOnlyData loadDataFromFile(File file) throws Exception {
        logger.info("trying to load data from " + file);
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            ReadOnlyData result = ReadOnlyData.Serializer.deserialize(in);
            logger.info("done loading data from " + file);
            return result;
        }
        finally {
            in.close();
        }
    }

    private ReadOnlyData getReadOnlyData(DateTime key) throws ExecutionException {

        ReadOnlyData result = this.data.get(key);

        if (fileCache.get(key) == null) {
            logger.warn(dbName + " cannot find file in fileCache " + key);
            fileCache.put(key, getFile(key));
        }
        return result;
    }

    private double[] getData(Interval interval, TagLists tagLists) throws ExecutionException {
        DateTime start = config.startDate;
        DateTime end = config.startDate;

        if (consolidateType == ConsolidateType.hourly) {
            start = interval.getStart().withDayOfMonth(1).withMillisOfDay(0);
            end = interval.getEnd();
        }
        else if (consolidateType == ConsolidateType.daily) {
            start = interval.getStart().withDayOfYear(1).withMillisOfDay(0);
            end = interval.getEnd();
        }

        int num = 0;
        if (consolidateType == ConsolidateType.hourly) {
            num = interval.toPeriod(PeriodType.hours()).getHours();
            if (interval.getStart().plusHours(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.daily) {
            num = interval.toPeriod(PeriodType.days()).getDays();
            if (interval.getStart().plusDays(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.weekly) {
            num = interval.toPeriod(PeriodType.weeks()).getWeeks();
            if (interval.getStart().plusWeeks(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.monthly) {
            num = interval.toPeriod(PeriodType.months()).getMonths();
            if (interval.getStart().plusMonths(num).isBefore(interval.getEnd()))
                num++;
        }

        double[] result = new double[num];

        do {
            ReadOnlyData data = getReadOnlyData(start);

            int resultIndex = 0;
            int fromIndex = 0;

            if (interval.getStart().isBefore(start)) {
                if (consolidateType == ConsolidateType.hourly) {
                    resultIndex = Hours.hoursBetween(interval.getStart(), start).getHours();
                }
                else if (consolidateType == ConsolidateType.daily) {
                    resultIndex = Days.daysBetween(interval.getStart(), start).getDays();
                }
                else if (consolidateType == ConsolidateType.weekly) {
                    resultIndex = Weeks.weeksBetween(interval.getStart(), start).getWeeks();
                }
                else if (consolidateType == ConsolidateType.monthly) {
                    resultIndex = Months.monthsBetween(interval.getStart(), start).getMonths();
                }
            }
            else {
                if (consolidateType == ConsolidateType.hourly) {
                    fromIndex = Hours.hoursBetween(start, interval.getStart()).getHours();
                }
                else if (consolidateType == ConsolidateType.daily) {
                    fromIndex = Days.daysBetween(start, interval.getStart()).getDays();
                }
                else if (consolidateType == ConsolidateType.weekly) {
                    fromIndex = Weeks.weeksBetween(start, interval.getStart()).getWeeks();
                    if (start.getDayOfWeek() != interval.getStart().getDayOfWeek())
                        fromIndex++;
                }
                else if (consolidateType == ConsolidateType.monthly) {
                    fromIndex = Months.monthsBetween(start, interval.getStart()).getMonths();
                }
            }

            List<Integer> columeIndexs = Lists.newArrayList();
            int columeIndex = 0;
            for (TagGroup tagGroup: data.getTagGroups()) {
                if (tagLists.contains(tagGroup))
                    columeIndexs.add(columeIndex);
                columeIndex++;
            }
            while (resultIndex < num && fromIndex < data.getNum()) {
                double[] fromData = data.getData(fromIndex++);
                for (Integer cIndex: columeIndexs)
                    result[resultIndex] += fromData[cIndex];
                resultIndex++;
            }

            if (consolidateType  == ConsolidateType.hourly)
                start = start.plusMonths(1);
            else if (consolidateType  == ConsolidateType.daily)
                start = start.plusYears(1);
            else
                break;
        }
        while (start.isBefore(end));

        return result;
    }

    private void addData(double[] from, double[] to) {
        for (int i = 0; i < from.length; i++)
            to[i] += from[i];
    }

    public Map<Tag, double[]> getData(Interval interval, TagLists tagLists, TagType groupBy, AggregateType aggregate, boolean forReservation) {

        Map<Tag, TagLists> tagListsMap;

        if (groupBy == null) {
            tagListsMap = Maps.newHashMap();
            tagListsMap.put(Tag.aggregated, tagLists);
        }
        else
            tagListsMap = config.managers.getTagGroupManager(product).getTagListsMap(interval, tagLists, groupBy, forReservation);

        Map<Tag, double[]> result = Maps.newTreeMap();
        double[] aggregated = null;

        for (Tag tag: tagListsMap.keySet()) {
            try {
                double[] data = getData(interval, tagListsMap.get(tag));
                result.put(tag, data);
                if (aggregate != AggregateType.none && tagListsMap.size() > 1) {
                    if (aggregated == null)
                        aggregated = new double[data.length];
                    addData(data, aggregated);
                }
            }
            catch (ExecutionException e) {
                logger.error("error in getData for " + tag + " " + interval, e);
            }
        }
        if (aggregated != null)
            result.put(Tag.aggregated, aggregated);
        return result;
    }

    public int getDataLength(DateTime start) {
        try {
            ReadOnlyData data = getReadOnlyData(start);
            return data.getNum();
        }
        catch (ExecutionException e) {
            logger.error("error in getDataLength for " + start, e);
            return 0;
        }
    }
}
