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
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.tag.*;

import java.util.List;

public class TagLists {
    public final List<Account> accounts;
    public final List<Region> regions;
    public final List<Zone> zones;
    public final List<Product> products;
    public final List<Operation> operations;
    public final List<UsageType> usageTypes;
    public final List<ResourceGroup> resourceGroups;

    public TagLists() {
        this.accounts = null;
        this.regions = null;
        this.zones = null;
        this.products = null;
        this.operations = null;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts) {
        this.accounts = accounts;
        this.regions = null;
        this.zones = null;
        this.products = null;
        this.operations = null;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = null;
        this.products = null;
        this.operations = null;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions, List<Zone> zones) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = zones;
        this.products = null;
        this.operations = null;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions, List<Zone> zones, List<Product> products) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = zones;
        this.products = products;
        this.operations = null;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions, List<Zone> zones, List<Product> products, List<Operation> operations) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = zones;
        this.products = products;
        this.operations = operations;
        this.usageTypes = null;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions, List<Zone> zones, List<Product> products, List<Operation> operations, List<UsageType> usageTypes) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = zones;
        this.products = products;
        this.operations = operations;
        this.usageTypes = usageTypes;
        this.resourceGroups = null;
    }

    public TagLists(List<Account> accounts, List<Region> regions, List<Zone> zones, List<Product> products, List<Operation> operations, List<UsageType> usageTypes, List<ResourceGroup> resourceGroups) {
        this.accounts = accounts;
        this.regions = regions;
        this.zones = zones;
        this.products= products;
        this.operations = operations;
        this.usageTypes = usageTypes;
        this.resourceGroups = resourceGroups;
    }

    public boolean contains(TagGroup tagGroup) {
        boolean result = true;

        if (result && accounts != null && accounts.size() > 0) {
            result = accounts.contains(tagGroup.account);
        }
        if (result && regions != null && regions.size() > 0) {
            result = regions.contains(tagGroup.region);
        }
        if (result && zones != null && zones.size() > 0) {
            result = zones.contains(tagGroup.zone);
        }
        if (result && products != null && products.size() > 0) {
            result = products.contains(tagGroup.product);
        }
        if (result && operations != null && operations.size() > 0) {
            result = operations.contains(tagGroup.operation);
        }
        if (result && usageTypes != null && usageTypes.size() > 0) {
            result = usageTypes.contains(tagGroup.usageType);
        }
        if (result && resourceGroups != null && resourceGroups.size() > 0) {
            result = resourceGroups.contains(tagGroup.resourceGroup);
        }
        return result;
    }

    public boolean contains(Tag tag, TagType groupBy) {
        boolean result = true;

        switch (groupBy) {
            case Account:
                result = accounts == null || accounts.size() == 0 || accounts.contains(tag);
                break;
            case Region:
                result = regions == null || regions.size() == 0 || regions.contains(tag);
                break;
            case Zone:
                result = zones == null || zones.size() == 0 || zones.contains(tag);
                break;
            case Product:
                result = products == null || products.size() == 0 || products.contains(tag);
                break;
            case Operation:
                result = operations == null || operations.size() == 0 || operations.contains(tag);
                break;
            case UsageType:
                result = usageTypes == null || usageTypes.size() == 0 || usageTypes.contains(tag);
                break;
            case ResourceGroup:
                result = resourceGroups == null || resourceGroups.size() == 0 || resourceGroups.contains(tag);
                break;
        }
        return result;
    }

    public TagLists getTagLists(Tag tag, TagType groupBy) {
        TagLists result = null;

        switch (groupBy) {
            case Account:
                result = new TagLists(Lists.newArrayList((Account)tag), this.regions, this.zones, this.products, this.operations, this.usageTypes, this.resourceGroups);
                break;
            case Region:
                result = new TagLists(this.accounts, Lists.newArrayList((Region)tag), this.zones, this.products, this.operations, this.usageTypes, this.resourceGroups);
                break;
            case Zone:
                result = new TagLists(this.accounts, this.regions, Lists.newArrayList((Zone)tag), this.products, this.operations, this.usageTypes, this.resourceGroups);
                break;
            case Product:
                result = new TagLists(this.accounts, this.regions, this.zones, Lists.newArrayList((Product)tag), this.operations, this.usageTypes, this.resourceGroups);
                break;
            case Operation:
                result = new TagLists(this.accounts, this.regions, this.zones, this.products, Lists.newArrayList((Operation)tag), this.usageTypes, this.resourceGroups);
                break;
            case UsageType:
                result = new TagLists(this.accounts, this.regions, this.zones, this.products, this.operations, Lists.newArrayList((UsageType)tag), this.resourceGroups);
                break;
            case ResourceGroup:
                result = new TagLists(this.accounts, this.regions, this.zones, this.products, this.operations, this.usageTypes, Lists.newArrayList((ResourceGroup)tag));
                break;
        }
        return result;
    }
}
