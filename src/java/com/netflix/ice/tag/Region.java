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
package com.netflix.ice.tag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class Region extends Tag {

    public static final Region US_EAST_1 = new Region("us-east-1", "USE1");
    public static final Region US_EAST_2 = new Region("us-east-2", "USE2");
    public static final Region US_WEST_1 = new Region("us-west-1", "USW1");
    public static final Region US_WEST_2 = new Region("us-west-2", "USW2");
    public static final Region EU_WEST_1 = new Region("eu-west-1", "EU");
    public static final Region AP_SOUTH_1 = new Region("ap-south-1", "APS1");
    public static final Region EU_CENTRAL_1 = new Region("eu-central-1", "EUC1");
    public static final Region AP_SOUTHEAST_1 = new Region("ap-southeast-1", "APSE1");
    public static final Region AP_SOUTHEAST_2 = new Region("ap-southeast-2", "APSE2");
    public static final Region AP_NORTHEAST_1 = new Region("ap-northeast-1","APN1");
    public static final Region AP_NORTHEAST_2 = new Region("ap-northeast-2","APN2");
    public static final Region SA_EAST_1 = new Region("sa-east-1", "SAE1");

    private static ConcurrentMap<String, Region> regionsByName = Maps.newConcurrentMap();
    private static ConcurrentMap<String, Region> regionsByShortName = Maps.newConcurrentMap();

    static {
        regionsByShortName.put(US_EAST_1.shortName, US_EAST_1);
        regionsByShortName.put(US_EAST_2.shortName, US_EAST_2);
        regionsByShortName.put(US_WEST_1.shortName, US_WEST_1);
        regionsByShortName.put(US_WEST_2.shortName, US_WEST_2);
        regionsByShortName.put(EU_WEST_1.shortName, EU_WEST_1);
        regionsByShortName.put(AP_SOUTH_1.shortName, AP_SOUTH_1);
        regionsByShortName.put(EU_CENTRAL_1.shortName, EU_CENTRAL_1);
        regionsByShortName.put(AP_SOUTHEAST_1.shortName, AP_SOUTHEAST_1);
        regionsByShortName.put(AP_SOUTHEAST_2.shortName, AP_SOUTHEAST_2);
        regionsByShortName.put(AP_NORTHEAST_1.shortName, AP_NORTHEAST_1);
        regionsByShortName.put(AP_NORTHEAST_2.shortName, AP_NORTHEAST_2);
        regionsByShortName.put(SA_EAST_1.shortName, SA_EAST_1);

        regionsByName.put(US_EAST_1.name, US_EAST_1);
        regionsByName.put(US_EAST_2.name, US_EAST_2);
        regionsByName.put(US_WEST_1.name, US_WEST_1);
        regionsByName.put(US_WEST_2.name, US_WEST_2);
        regionsByName.put(EU_WEST_1.name, EU_WEST_1);
        regionsByName.put(EU_CENTRAL_1.name, EU_CENTRAL_1);
        regionsByName.put(AP_SOUTHEAST_1.name, AP_SOUTHEAST_1);
        regionsByName.put(AP_SOUTHEAST_2.name, AP_SOUTHEAST_2);
        regionsByName.put(AP_NORTHEAST_1.name, AP_NORTHEAST_1);
        regionsByName.put(AP_NORTHEAST_2.name, AP_NORTHEAST_2);
        regionsByName.put(SA_EAST_1.name, SA_EAST_1);
    }

    public final String shortName;
    List<Zone> zones = Lists.newArrayList();

    private Region(String name, String shortName) {
        super(name);
        this.shortName = shortName;
    }

    public List<Zone> getZones() {
        return Lists.newArrayList(zones);
    }

    void addZone(Zone zone) {
        zones.add(zone);
    }

    public static Region getRegionByShortName(String shortName) {
        return regionsByShortName.get(shortName);
    }

    public static Region getRegionByName(String name) {
        return regionsByName.get(name);
    }

    public static List<Region> getRegions(List<String> names) {
        List<Region> result = Lists.newArrayList();
        for (String name: names)
            result.add(regionsByName.get(name));
        return result;
    }

    public static List<Region> getAllRegions() {
        return Lists.newArrayList(regionsByName.values());
    }
}
