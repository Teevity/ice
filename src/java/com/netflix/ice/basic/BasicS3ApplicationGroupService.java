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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.reader.ApplicationGroup;
import com.netflix.ice.reader.ApplicationGroupService;
import com.netflix.ice.reader.ReaderConfig;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class BasicS3ApplicationGroupService implements ApplicationGroupService {
    private final static Logger logger = LoggerFactory.getLogger(BasicS3ApplicationGroupService.class);
    private AmazonS3Client s3Client;
    private ReaderConfig config;

    public void init() {
        this.config = ReaderConfig.getInstance();
        s3Client = AwsUtils.getAmazonS3Client();
    }

    private String getJson(Map<String, ApplicationGroup> appgroups) throws JSONException {
        JSONObject json = new JSONObject();
        for (String name: appgroups.keySet()) {
            ApplicationGroup appgroup = appgroups.get(name);
            json.put(name, appgroup.getJSON());
        }

        return json.toString();
    }

    public Map<String, ApplicationGroup> getApplicationGroups() {
        String jsonStr;
        try {
            InputStream in = s3Client.getObject(config.workS3BucketName, config.workS3BucketPrefix + "appgroups").getObjectContent();
            jsonStr = IOUtils.toString(in);
            in.close();
        }
        catch (Exception e) {
            logger.error("Error reading from appgroups file", e);
            try {
                InputStream in = s3Client.getObject(config.workS3BucketName, config.workS3BucketPrefix + "copy_appgroups").getObjectContent();
                jsonStr = IOUtils.toString(in);
                in.close();
            }
            catch (Exception r) {
                logger.error("Error reading from copy_appgroups file", r);
                return Maps.newHashMap();
            }
        }

        try {
            JSONObject json = new JSONObject(new JSONTokener(jsonStr));
            Map<String, ApplicationGroup> appgroups = Maps.newHashMap();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String str = json.getString(key);
                appgroups.put(key, new ApplicationGroup(str));
            }

            return appgroups;
        }
        catch (JSONException e) {
            logger.error("Error reading appgroups from json...", e);
            return Maps.newHashMap();
        }
    }

    public ApplicationGroup getApplicationGroup(String name) {
        Map<String, ApplicationGroup> appgroups = getApplicationGroups();
        return appgroups.get(name);
    }

    public boolean saveApplicationGroup(ApplicationGroup appgroup) {
        Map<String, ApplicationGroup> appgroups = getApplicationGroups();
        appgroups.put(appgroup.name, appgroup);

        try {
            String json = getJson(appgroups);
            s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + "appgroups", IOUtils.toInputStream(json), new ObjectMetadata());
            s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + "copy_appgroups", IOUtils.toInputStream(json), new ObjectMetadata());

            BasicS3ApplicationGroupService.logger.info("saved appgroup " + appgroup);
            return true;
        }
        catch (JSONException e) {
            logger.error("Error saving appgroup " + appgroup, e);
            return false;
        }
    }

    public boolean deleteApplicationGroup(String name) {
        Map<String, ApplicationGroup> appgroups = getApplicationGroups();
        ApplicationGroup appgroup = appgroups.remove(name);

        try {
            String json = getJson(appgroups);
            s3Client.putObject(config.workS3BucketName, config.workS3BucketPrefix + "appgroups", new ByteArrayInputStream(json.getBytes()), new ObjectMetadata());

            BasicS3ApplicationGroupService.logger.info("delete appgroup " + name + " " + appgroup);
            return true;
        }
        catch (JSONException e) {
            logger.error("Error deleting appgroup " + appgroup, e);
            return false;
        }
    }
}
