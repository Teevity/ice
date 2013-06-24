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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Region;
import org.apache.commons.lang.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

public class MapDb {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DB db;
    private Map<String, String> items;
    private long numItemsToCommit = 0;
    private ProcessorConfig config;
    private String dbName;

    MapDb(String name) {
        this.config = ProcessorConfig.getInstance();

        this.dbName = "db_" + name;
        File file = new File(config.localDir, dbName);
        if (!file.exists()) {
            AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();
            for (S3ObjectSummary s3ObjectSummary: s3Client.listObjects(config.workS3BucketName, config.workS3BucketPrefix + this.dbName).getObjectSummaries()) {
                File dbFile = new File(config.localDir, s3ObjectSummary.getKey().substring(config.workS3BucketPrefix.length()));
                AwsUtils.downloadFileIfNotExist(config.workS3BucketName, config.workS3BucketPrefix, dbFile);
            }
        }
        this.db = DBMaker.newFileDB(new File(config.localDir, this.dbName)).make();
        try {
            this.items = db.createHashMap(name, false, null, null);
        }
        catch (IllegalArgumentException e) {
            this.items = db.getHashMap(name);
            logger.info("found " + this.items.size() + " items from mapdb for " + name);
        }
    }

    String getResource(Account account, Region region, String resourceId) {
        return this.items.get(resourceId + "|" + account + "|" + region);
    }

    void SetResource(Account account, Region region, String resourceId, String resource, long millisStart) {
        if (StringUtils.isEmpty(resource))
            return;

        String key = resourceId + "|" + account + "|" + region;
        String resourceInDb = this.items.get(key);

        if (resourceInDb == null) {
            this.items.put(key, resource);

            numItemsToCommit ++;
            if (numItemsToCommit >= 1000) {
                this.commit();
                numItemsToCommit = 0;
            }
        }
        else if (!resourceInDb.equals(resource)) {
            logger.error("different resources " + resourceInDb + " " + resource + " for " + resourceId);
            this.items.put(key, resource);
            resourceInDb = resource;

            numItemsToCommit ++;
            if (numItemsToCommit >= 1000) {
                this.commit();
                numItemsToCommit = 0;
            }
        }
    }

    void commit() {
        this.db.commit();
        upload();
        logger.info("committed " + this.items.size() + ".");
    }

    void upload() {
        AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();

        File dir = new File(config.localDir);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File file, String fileName) {
                return fileName.startsWith(dbName);
            }
        });
        for (File file: files)
            s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + file.getName(), file);

        for (File file: files)
            s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + "copy" + file.getName(), file);
    }

}
