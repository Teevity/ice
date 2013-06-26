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

public class ResourceGroup extends Tag {
    private ResourceGroup (String name) {
        super(name);
    }
    private static ConcurrentMap<String, ResourceGroup> resourceGroups = Maps.newConcurrentMap();

    public static ResourceGroup getResourceGroup(String name) {
        ResourceGroup resourceGroup = resourceGroups.get(name);
        if (resourceGroup == null) {
            resourceGroups.putIfAbsent(name, new ResourceGroup(name));
            resourceGroup = resourceGroups.get(name);
        }
        return resourceGroup;
    }

    public static List<ResourceGroup> getResourceGroups(List<String> names) {
        List<ResourceGroup> result = Lists.newArrayList();
        if (names != null) {
            for (String name: names) {
                ResourceGroup resourceGroup = resourceGroups.get(name);
                if (resourceGroup != null)
                    result.add(resourceGroup);
            }
        }
        return result;
    }
}
