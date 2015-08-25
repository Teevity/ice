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

import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.TreeMap;

public class DataWriter {
    private final static Logger logger = LoggerFactory.getLogger(DataWriter.class);


    private TreeMap<Integer, Collection<TagGroup>> tagGroups;
    private ProcessorConfig config = ProcessorConfig.getInstance();
    private String dbName;
    private File file;
    private ReadWriteData data;

    DataWriter(String name, boolean loadData) throws Exception {

        name = Tag.toS3(name);
        dbName = name;
        file = new File(config.localDir, dbName);
        if (loadData) {
            AwsUtils.downloadFileIfNotExist(config.workS3BucketName, config.workS3BucketPrefix, file);
        }

        if (file.exists()) {
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            try {
                data = ReadWriteData.Serializer.deserialize(in);
            }
            finally {
                in.close();
            }
        }
        else {
            data = new ReadWriteData();
        }
    }

    ReadWriteData getData() {
        return data;
    }

    void archive() throws IOException {
        archive(data);
    }

    void archive(ReadWriteData data) throws IOException {

        DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
        try {
            ReadWriteData.Serializer.serialize(out, data);
        }
        finally {
            out.close();
        }

        logger.info(this.dbName + " uploading to s3...");
        AwsUtils.upload(config.workS3BucketName, config.workS3BucketPrefix, config.localDir, dbName);
        logger.info(this.dbName + " uploading done.");
    }
}
