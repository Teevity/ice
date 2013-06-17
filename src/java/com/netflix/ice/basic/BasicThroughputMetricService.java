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
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.ConsolidateType;
import com.netflix.ice.common.Poller;
import com.netflix.ice.reader.ReaderConfig;
import com.netflix.ice.reader.ThroughputMetricService;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.PeriodType;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

public class BasicThroughputMetricService extends Poller implements ThroughputMetricService {

    private String metricName;
    private String metricUnitName;
    private String factoredCostCurrencySign;
    private double factoredCostMultiply;
    private String filePrefix;
    private ReaderConfig config;

    private Map<DateTime, File> fileCache;
    private LoadingCache<DateTime, double[]> data;

    public BasicThroughputMetricService(String metricName, String metricUnitName, String factoredCostCurrencySign, double factoredCostMultiply, String filePrefix) {
        this.metricName = metricName;
        this.metricUnitName = metricUnitName;
        this.factoredCostCurrencySign = factoredCostCurrencySign;
        this.factoredCostMultiply = factoredCostMultiply;
        this.filePrefix = filePrefix;
        fileCache = Maps.newConcurrentMap();
    }

    public void init() {
        config = ReaderConfig.getInstance();
        data = CacheBuilder.newBuilder()
           .maximumSize(config.monthlyCacheSize)
           .removalListener(new RemovalListener<DateTime, double[]>() {
               public void onRemoval(RemovalNotification<DateTime, double[]> objectRemovalNotification) {
                   fileCache.remove(objectRemovalNotification.getKey());
               }
           })
           .build(
                   new CacheLoader<DateTime, double[]>() {
                       public double[] load(DateTime monthDate) throws Exception {
                           return loadData(monthDate);
                       }
                   });
        this.start();
    }

    @Override
    protected void poll() throws Exception {
        for (DateTime key: fileCache.keySet()) {
            File file = fileCache.get(key);
            boolean downloaded = AwsUtils.downloadFileIfChanged(config.workS3BucketName, config.workS3BucketPrefix, file, 0);
            if (downloaded) {
                logger.info("trying to re-read data for " + file);
                FileInputStream in = new FileInputStream(file);
                try {
                    String[] strs = IOUtils.toString(in).split(",");
                    double[] values = new double[strs.length];
                    for (int i = 0; i < strs.length; i++)
                        values[i] = Double.parseDouble(strs[i]);

                    data.put(key, values);
                    logger.info("done re-read data for " + file);
                }
                finally {
                    in.close();
                }
            }
        }
    }

    private double[] loadData(DateTime monthDate) throws InterruptedException {
        while (true) {
            try {
                File file = new File(config.localDir, filePrefix + AwsUtils.monthDateFormat.print(monthDate));
                AwsUtils.downloadFileIfChanged(config.workS3BucketName, config.workS3BucketPrefix, file, 0);

                FileInputStream in = new FileInputStream(file);
                try {
                    String[] strs = IOUtils.toString(in).split(",");
                    double[] values = new double[strs.length];
                    for (int i = 0; i < strs.length; i++)
                        values[i] = Double.parseDouble(strs[i]);

                    return values;
                }
                finally {
                    in.close();
                }
            }
            catch (Exception e) {
                logger.error("error in loading data for " + monthDate, e);
                Thread.sleep(1000*20L);
            }
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public String getMetricUnitName() {
        return metricUnitName;
    }

    public String getFactoredCostCurrencySign() {
        return factoredCostCurrencySign;
    }

    public double getFactoredCostMultiply() {
        return factoredCostMultiply;
    }

    public double[] getData(Interval interval, ConsolidateType consolidateType) throws Exception {
        DateTime start = interval.getStart().withDayOfMonth(1).withMillisOfDay(0);
        DateTime end = interval.getEnd();

        int num = interval.toPeriod(PeriodType.hours()).getHours();
        if (interval.getStart().plusHours(num).isBefore(interval.getEnd()))
            num++;

        double[] hourly = new double[num];
        List<Double> monthly = Lists.newArrayList();
        do {
            double total = 0;
            int resultIndex = interval.getStart().isBefore(start) ? Hours.hoursBetween(interval.getStart(), start).getHours() : 0;
            int fromIndex = interval.getStart().isBefore(start) ? 0 : Hours.hoursBetween(start, interval.getStart()).getHours();

            double[] data = this.data.get(start);
            while (resultIndex < num && fromIndex < data.length) {
                total += data[fromIndex];
                hourly[resultIndex++] = data[fromIndex++];
            }

            start = start.plusMonths(1);
            monthly.add(total);
        }
        while (start.isBefore(end));

        int hoursInPeriod = (int) (consolidateType.millis / AwsUtils.hourMillis);
        num = consolidateType == ConsolidateType.monthly ? monthly.size() : (int) Math.ceil(1.0 * num / hoursInPeriod);
        double[] result = new double[num];

        if (consolidateType == ConsolidateType.monthly) {
            for (int i = 0; i < num; i++)
                result[i] = monthly.get(i);
        }
        else {
            for (int i = 0; i < hourly.length; i++)
                result[i/hoursInPeriod] += hourly[i];
        }
        return result;
    }
}
