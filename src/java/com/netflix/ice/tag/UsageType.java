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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class UsageType extends Tag {
    private static final Logger logger = LoggerFactory.getLogger(Operation.class);
    public final String unit;

    private UsageType (String name, String unit) {
        super(name);
        this.unit = unit;
    }
    private static ConcurrentMap<String, UsageType> usageTypes = Maps.newConcurrentMap();

    public static void serialize(DataOutput out, UsageType usageType) throws IOException {
        out.writeUTF(usageType.name);
        out.writeUTF(usageType.unit);
    }

    public static UsageType deserialize(DataInput in) throws IOException {
        String name = in.readUTF();
        String unit = in.readUTF();

        UsageType usageType = usageTypes.get(name);
        if (usageType == null) {
            usageTypes.putIfAbsent(name, new UsageType(name, unit));
            usageType = usageTypes.get(name);
        }
        else if (!usageType.unit.equals(unit)) {
            logger.error("found different units for " + usageType + usageType.unit + " " + unit);
        }
        return usageType;
    }

    public static UsageType getUsageType(String name, Operation operation, String description) {
        String unit = "";
        if (name.contains("Bytes") || name.contains("ByteHrs") || description.contains("GB"))
            unit = "GB";
        if (operation instanceof Operation.ReservationOperation)
            unit = "hours";

        return getUsageType(name, unit);
    }

    public static UsageType getUsageType(String name, String unit) {

        UsageType usageType = usageTypes.get(name);
        if (usageType == null) {
            usageTypes.putIfAbsent(name, new UsageType(name, unit));
            usageType = usageTypes.get(name);
        }
        else if (!usageType.unit.equals(unit)) {
            logger.error("found different units for " + usageType + usageType.unit + " " + unit);
        }
        return usageType;
    }

    public static List<UsageType> getUsageTypes(List<String> names) {
        List<UsageType> result = Lists.newArrayList();
        for (String name: names)
            result.add(usageTypes.get(name));
        return result;
    }
}
