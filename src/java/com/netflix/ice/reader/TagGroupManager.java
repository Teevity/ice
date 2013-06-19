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

import com.netflix.ice.tag.*;
import org.joda.time.Interval;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to manager tag groups.
 */
public interface TagGroupManager {

    /**
     * Get all accounts that meet query in tagLists.
     * @param tagLists
     * @return collection of accounts
     */
    Collection<Account> getAccounts(TagLists tagLists);

    /**
     * Get all regions that meet query in tagLists.
     * @param tagLists
     * @return collection of regions
     */
    Collection<Region> getRegions(TagLists tagLists);

    /**
     * Get all zones that meet query in tagLists.
     * @param tagLists
     * @return collection of zones
     */
    Collection<Zone> getZones(TagLists tagLists);

    /**
     * Get all products that meet query in tagLists.
     * @param tagLists
     * @return collection of products
     */
    Collection<Product> getProducts(TagLists tagLists);

    /**
     * Get all operations that meet query in tagLists.
     * @param tagLists
     * @return collection of operations
     */
    Collection<Operation> getOperations(TagLists tagLists);

    /**
     * Get all usage types that meet query in tagLists.
     * @param tagLists
     * @return collection of usage types
     */
    Collection<UsageType> getUsageTypes(TagLists tagLists);

    /**
     * Get all resource groups that meet query in tagLists.
     * @param tagLists
     * @return collection of resource groups
     */
    Collection<ResourceGroup> getResourceGroups(TagLists tagLists);

    /**
     * Get all accounts that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of accounts
     */
    Collection<Account> getAccounts(Interval interval, TagLists tagLists);

    /**
     * Get all regions that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of regions
     */
    Collection<Region> getRegions(Interval interval, TagLists tagLists);

    /**
     * Get all zones that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of zones
     */
    Collection<Zone> getZones(Interval interval, TagLists tagLists);

    /**
     * Get all products that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of products
     */
    Collection<Product> getProducts(Interval interval, TagLists tagLists);

    /**
     * Get all operations that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of operations
     */
    Collection<Operation> getOperations(Interval interval, TagLists tagLists);

    /**
     * Get all usage types that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of usage types
     */
    Collection<UsageType> getUsageTypes(Interval interval, TagLists tagLists);

    /**
     * Get all resource groups that meet query in tagLists and in specifed interval.
     * @param interval
     * @param tagLists
     * @return collection of resource groups
     */
    Collection<ResourceGroup> getResourceGroups(Interval interval, TagLists tagLists);

    /**
     * Get overlapping interval
     * @param interval
     * @return overlapping interval
     */
    Interval getOverlapInterval(Interval interval);

    /**
     * Get map of tag lists based on group by.
     * @param interval
     * @param tagLists
     * @param groupBy
     * @param forReservation
     * @return
     */
    Map<Tag, TagLists> getTagListsMap(Interval interval, TagLists tagLists, TagType groupBy, boolean forReservation);
}
