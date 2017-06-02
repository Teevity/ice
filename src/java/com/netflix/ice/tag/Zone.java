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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class Zone extends Tag {

    public final Region region;

    private Zone (Region region, String name) {
        super(name);
        this.region = region;
        region.addZone(this);
    }

    public static final Zone US_EAST_1A = new Zone(Region.US_EAST_1, "us-east-1a");
    public static final Zone US_EAST_1B = new Zone(Region.US_EAST_1, "us-east-1b");
    public static final Zone US_EAST_1C = new Zone(Region.US_EAST_1, "us-east-1c");
    public static final Zone US_EAST_1D = new Zone(Region.US_EAST_1, "us-east-1d");
    public static final Zone US_EAST_1E = new Zone(Region.US_EAST_1, "us-east-1e");

    public static final Zone US_EAST_2A = new Zone(Region.US_EAST_2, "us-east-2a");
    public static final Zone US_EAST_2B = new Zone(Region.US_EAST_2, "us-east-2b");
    public static final Zone US_EAST_2C = new Zone(Region.US_EAST_2, "us-east-2c");

    public static final Zone US_WEST_1A = new Zone(Region.US_WEST_1, "us-west-1a");
    public static final Zone US_WEST_1B = new Zone(Region.US_WEST_1, "us-west-1b");
    public static final Zone US_WEST_1C = new Zone(Region.US_WEST_1, "us-west-1c");

    public static final Zone US_WEST_2A = new Zone(Region.US_WEST_2, "us-west-2a");
    public static final Zone US_WEST_2B = new Zone(Region.US_WEST_2, "us-west-2b");
    public static final Zone US_WEST_2C = new Zone(Region.US_WEST_2, "us-west-2c");

    public static final Zone EU_WEST_1A = new Zone(Region.EU_WEST_1, "eu-west-1a");
    public static final Zone EU_WEST_1B = new Zone(Region.EU_WEST_1, "eu-west-1b");
    public static final Zone EU_WEST_1C = new Zone(Region.EU_WEST_1, "eu-west-1c");

    public static final Zone AP_SOUTH_1A = new Zone(Region.AP_SOUTH_1, "ap-south-1a");
    public static final Zone AP_SOUTH_1B = new Zone(Region.AP_SOUTH_1, "ap-south-1b");

    public static final Zone EU_CENTRAL_1A = new Zone(Region.EU_CENTRAL_1, "eu-central-1a");
    public static final Zone EU_CENTRAL_1B = new Zone(Region.EU_CENTRAL_1, "eu-central-1b");

    public static final Zone SA_EAST_1A = new Zone(Region.SA_EAST_1, "sa-east-1a");
    public static final Zone SA_EAST_1B = new Zone(Region.SA_EAST_1, "sa-east-1b");
    public static final Zone SA_EAST_1C = new Zone(Region.SA_EAST_1, "sa-east-1c");

    public static final Zone AP_NORTHEAST_1A = new Zone(Region.AP_NORTHEAST_1, "ap-northeast-1a");
    public static final Zone AP_NORTHEAST_1B = new Zone(Region.AP_NORTHEAST_1, "ap-northeast-1b");
    public static final Zone AP_NORTHEAST_1C = new Zone(Region.AP_NORTHEAST_1, "ap-northeast-1c");

    public static final Zone AP_NORTHEAST_2A = new Zone(Region.AP_NORTHEAST_2, "ap-northeast-2a");
    public static final Zone AP_NORTHEAST_2B = new Zone(Region.AP_NORTHEAST_2, "ap-northeast-2b");

    public static final Zone AP_SOUTHEAST_1A = new Zone(Region.AP_SOUTHEAST_1, "ap-southeast-1a");
    public static final Zone AP_SOUTHEAST_1B = new Zone(Region.AP_SOUTHEAST_1, "ap-southeast-1b");

    public static final Zone AP_SOUTHEAST_2A = new Zone(Region.AP_SOUTHEAST_2, "ap-southeast-2a");
    public static final Zone AP_SOUTHEAST_2B = new Zone(Region.AP_SOUTHEAST_2, "ap-southeast-2b");
    public static final Zone AP_SOUTHEAST_2C = new Zone(Region.AP_SOUTHEAST_2, "ap-southeast-2c");

    private static ConcurrentMap<String, Zone> zonesByName = Maps.newConcurrentMap();

    static {
        zonesByName.put(US_EAST_1A.name, US_EAST_1A);
        zonesByName.put(US_EAST_1B.name, US_EAST_1B);
        zonesByName.put(US_EAST_1C.name, US_EAST_1C);
        zonesByName.put(US_EAST_1D.name, US_EAST_1D);
        zonesByName.put(US_EAST_1E.name, US_EAST_1E);

        zonesByName.put(US_EAST_2A.name, US_EAST_2A);
        zonesByName.put(US_EAST_2B.name, US_EAST_2B);
        zonesByName.put(US_EAST_2C.name, US_EAST_2C);

        zonesByName.put(US_WEST_1A.name, US_WEST_1A);
        zonesByName.put(US_WEST_1B.name, US_WEST_1B);
        zonesByName.put(US_WEST_1C.name, US_WEST_1C);

        zonesByName.put(US_WEST_2A.name, US_WEST_2A);
        zonesByName.put(US_WEST_2B.name, US_WEST_2B);
        zonesByName.put(US_WEST_2C.name, US_WEST_2C);

        zonesByName.put(AP_SOUTH_1A.name, AP_SOUTH_1A);
        zonesByName.put(AP_SOUTH_1B.name, AP_SOUTH_1B);

        zonesByName.put(EU_WEST_1A.name, EU_WEST_1A);
        zonesByName.put(EU_WEST_1B.name, EU_WEST_1B);
        zonesByName.put(EU_WEST_1C.name, EU_WEST_1C);

        zonesByName.put(EU_CENTRAL_1A.name, EU_CENTRAL_1A);
        zonesByName.put(EU_CENTRAL_1B.name, EU_CENTRAL_1B);

        zonesByName.put(SA_EAST_1A.name, SA_EAST_1A);
        zonesByName.put(SA_EAST_1B.name, SA_EAST_1B);
        zonesByName.put(SA_EAST_1C.name, SA_EAST_1C);

        zonesByName.put(AP_NORTHEAST_1A.name, AP_NORTHEAST_1A);
        zonesByName.put(AP_NORTHEAST_1B.name, AP_NORTHEAST_1B);
        zonesByName.put(AP_NORTHEAST_1C.name, AP_NORTHEAST_1C);

        zonesByName.put(AP_NORTHEAST_2A.name, AP_NORTHEAST_2A);
        zonesByName.put(AP_NORTHEAST_2B.name, AP_NORTHEAST_2B);

        zonesByName.put(AP_SOUTHEAST_1A.name, AP_SOUTHEAST_1A);
        zonesByName.put(AP_SOUTHEAST_1B.name, AP_SOUTHEAST_1B);

        zonesByName.put(AP_SOUTHEAST_2A.name, AP_SOUTHEAST_2A);
        zonesByName.put(AP_SOUTHEAST_2B.name, AP_SOUTHEAST_2B);
        zonesByName.put(AP_SOUTHEAST_2C.name, AP_SOUTHEAST_2C);
    }

    public static void addZone(Zone zone) {
        Zone existedZone = zonesByName.putIfAbsent(zone.name, zone);
        if (existedZone != null) {
            throw new RuntimeException("Zone with shortname already exists " + existedZone);
        }
    }

    public static Zone getZone(String name, Region region) {
        if (name.isEmpty() || name.equals(region.name))
            return null;
        Zone zone = zonesByName.get(name);
        if (zone == null) {
            zonesByName.putIfAbsent(name, new Zone(region, name));
            zone = zonesByName.get(name);
        }
        return zone;
    }

    public static Zone getZone(String name) {
        return zonesByName.get(name);
    }

    public static Collection<Zone> getZones() {
        return zonesByName.values();
    }

    public static List<Zone> getZones(List<String> names) {
        List<Zone> result = Lists.newArrayList();
        for (String name: names)
            result.add(zonesByName.get(name));
        return result;
    }
}