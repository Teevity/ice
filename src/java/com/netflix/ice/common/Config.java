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
package com.netflix.ice.common;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Properties;

public abstract class Config {

    public final String workS3BucketName;
    public final String workS3BucketPrefix;
    public final String localDir;
    public final AccountService accountService;
    public final ProductService productService;
    public final ResourceService resourceService;
    public final DateTime startDate;
    public final AWSCredentialsProvider credentialsProvider;

    /**
     *
     * @param properties (required)
     * @param credentialsProvider (required)
     * @param accountService (required)
     * @param productService (required)
     * @param resourceService (optional)
     */
    public Config(
            Properties properties,
            AWSCredentialsProvider credentialsProvider,
            AccountService accountService,
            ProductService productService,
            ResourceService resourceService) {
        if (properties.getProperty(IceOptions.START_MILLIS) == null) throw new IllegalArgumentException("IceOptions.START_MILLIS must be specified");
        if (properties == null) throw new IllegalArgumentException("properties must be specified");
        if (credentialsProvider == null) throw new IllegalArgumentException("credentialsProvider must be specified");
        if (accountService == null) throw new IllegalArgumentException("accountService must be specified");
        if (productService == null) throw new IllegalArgumentException("productService must be specified");

        DateTime startDate = new DateTime(Long.parseLong(properties.getProperty(IceOptions.START_MILLIS)), DateTimeZone.UTC);
        workS3BucketName = properties.getProperty(IceOptions.WORK_S3_BUCKET_NAME);
        workS3BucketPrefix = properties.getProperty(IceOptions.WORK_S3_BUCKET_PREFIX, "ice/");
        localDir = properties.getProperty(IceOptions.LOCAL_DIR, "/mnt/ice");

        if (workS3BucketName == null) throw new IllegalArgumentException("IceOptions.WORK_S3_BUCKET_NAME must be specified");

        this.credentialsProvider = credentialsProvider;
        this.startDate = startDate;
        this.accountService = accountService;
        this.productService = productService;
        this.resourceService = resourceService;

        AwsUtils.init(credentialsProvider);
    }
}
