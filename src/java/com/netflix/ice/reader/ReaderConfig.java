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
package com.netflix.ice.reader;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.netflix.ice.basic.BasicWeeklyCostEmailService;
import com.netflix.ice.common.*;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.TagType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Properties;

/**
 * COnfiguration class for reader/UI.
 */
public class ReaderConfig extends Config {
    private static ReaderConfig instance;
    private static final Logger logger = LoggerFactory.getLogger(ReaderConfig.class);

    public final String companyName;
    public final String currencySign;
    public final double currencyRate;
    public final String highstockUrl;
    public final ApplicationGroupService applicationGroupService;
    public final ThroughputMetricService throughputMetricService;
    public final BasicWeeklyCostEmailService costEmailService;
    public final Managers managers;
    public final int monthlyCacheSize;

    /**
     *
     * @param properties (required)
     * @param managers (required)
     * @param accountService (required)
     * @param productService (required)
     * @param resourceService (optional)
     * @param applicationGroupService (optional)
     * @param throughputMetricService (optional)
     */
    public ReaderConfig(
            Properties properties,
            AWSCredentialsProvider credentialsProvider,
            Managers managers,
            AccountService accountService,
            ProductService productService,
            ResourceService resourceService,
            ApplicationGroupService applicationGroupService,
            ThroughputMetricService throughputMetricService,
            BasicWeeklyCostEmailService costEmailService) {
        super(properties, credentialsProvider, accountService, productService, resourceService);

        companyName = properties.getProperty(IceOptions.COMPANY_NAME, "");
        currencySign = properties.getProperty(IceOptions.CURRENCY_SIGN, "$");
        currencyRate = Double.parseDouble(properties.getProperty(IceOptions.CURRENCY_RATE, "1"));
        highstockUrl = properties.getProperty(IceOptions.HIGHSTOCK_URL, "https://code.highcharts.com/stock/4.2.1/highstock.js");

        this.managers = managers;
        this.applicationGroupService = applicationGroupService;
        this.throughputMetricService = throughputMetricService;
        this.costEmailService = costEmailService;
        this.monthlyCacheSize = Integer.parseInt(properties.getProperty(IceOptions.MONTHLY_CACHE_SIZE, "12"));

        ReaderConfig.instance = this;

//        AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();
//        logger.info("Deleting all files...");
//        List<S3ObjectSummary> objectSummariesToDelete = AwsUtils.listAllObjects(instance.workS3BucketName, instance.workS3BucketPrefix);
//        for (S3ObjectSummary objectSummary : objectSummariesToDelete) {
//
//            String fileKey = objectSummary.getKey();
//
//            String name = fileKey.substring(fileKey.lastIndexOf("/") + 1);
//            if (name.startsWith("cost_") || name.startsWith("usage_") || name.startsWith("tagdb_")) {
//                s3Client.deleteObject(instance.workS3BucketName, fileKey);
//                continue;
//            }
//        }

        if (throughputMetricService != null)
            throughputMetricService.init();
        managers.init();
        applicationGroupService.init();
    }

    /**
     *
     * @return singlton instance
     */
    public static ReaderConfig getInstance() {
        return instance;
    }

    public void start() {

        Managers managers = ReaderConfig.getInstance().managers;
        Collection<Product> products = managers.getProducts();
        for (Product product: products) {
            TagGroupManager tagGroupManager = managers.getTagGroupManager(product);
            Interval interval = tagGroupManager.getOverlapInterval(new Interval(new DateTime(DateTimeZone.UTC).minusMonths(monthlyCacheSize), new DateTime(DateTimeZone.UTC)));
            if (interval == null)
                continue;
            for (ConsolidateType consolidateType: ConsolidateType.values()) {
                readData(product, managers.getCostManager(product, consolidateType), interval, consolidateType);
                readData(product, managers.getUsageManager(product, consolidateType), interval, consolidateType);
            }
        }

        if (costEmailService != null)
            costEmailService.start();
    }

    public void shutdown() {
        logger.info("Shutting down...");

        instance.managers.shutdown();
        if (instance.costEmailService != null)
            instance.costEmailService.shutdown();
    }

    private void readData(Product product, DataManager dataManager, Interval interval, ConsolidateType consolidateType) {
        if (consolidateType == ConsolidateType.hourly) {
            DateTime start = interval.getStart().withDayOfMonth(1).withMillisOfDay(0);
            do {
                int hours = dataManager.getDataLength(start);
                logger.info("found " + hours + " hours data for " + product + " "  + interval);
                start = start.plusMonths(1);
            }
            while (start.isBefore(interval.getEnd()));
        }
        else if (consolidateType == ConsolidateType.daily) {
            DateTime start = interval.getStart().withDayOfYear(1).withMillisOfDay(0);
            do {
                dataManager.getDataLength(start);
                start = start.plusYears(1);
            }
            while (start.isBefore(interval.getEnd()));
        }
        else {
            dataManager.getData(interval, new TagLists(), TagType.Account, AggregateType.both, false);
        }
    }
}
