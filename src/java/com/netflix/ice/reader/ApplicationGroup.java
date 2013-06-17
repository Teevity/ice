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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ApplicationGroup {
    public final Map<String, List<String>> data;
    public final String name;
    public final String owner;

    // todo: remove this constructor
    public ApplicationGroup(String name, String owner, List<String> ec2s, List<String> s3s) {
        this.name = name;
        this.owner = owner;
        this.data = Maps.newHashMap();
        this.data.put("rds", Lists.<String>newArrayList());
        this.data.put("ec2", ec2s);
        this.data.put("s3", s3s);
    }

    public ApplicationGroup(String name, String owner, Map<String, List<String>> data) {
        this.name = name;
        this.owner = owner;
        this.data = data;
    }

    public ApplicationGroup(String jsonStr) throws JSONException {
        data = Maps.newHashMap();
        JSONObject json = new JSONObject(new JSONTokener(jsonStr));
        name = json.getString("name");
        owner = json.getString("owner");
        json = json.getJSONObject("data");
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray jsonArray = json.getJSONArray(key);
            List<String> values = Lists.newArrayList();
            for (int i = 0; i < jsonArray.length(); i++)
                values.add(jsonArray.getString(i));
            data.put(key, values);
        }
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("owner", owner);
        json.put("data", new JSONObject(data));

        return json;
    }

    @Override
    public String toString() {
        try {
            return getJSON().toString();
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

    public String getDisplayName() {
        return "Application Group " + name;
    }

    public String getLink() {
        return "dashboard/appgroup#appgroup=" + name;
    }
}
