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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.Poller;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.processor.TagGroupWriter;
import com.netflix.ice.reader.ReaderConfig;
import com.netflix.ice.reader.TagGroupManager;
import com.netflix.ice.reader.TagLists;
import com.netflix.ice.tag.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class BasicTagGroupManager extends Poller implements TagGroupManager {

    private ReaderConfig config = ReaderConfig.getInstance();
    private String dbName;
    private File file;
    private TreeMap<Long, Collection<TagGroup>> tagGroups;
    private TreeMap<Long, Collection<TagGroup>> tagGroupsWithResourceGroups;
    private Interval totalInterval;

    BasicTagGroupManager(Product product) {
        this.dbName = TagGroupWriter.DB_PREFIX + (product == null ? "all" : product.s3Name);
        file = new File(config.localDir, dbName);
        try {
            poll();
        }
        catch (Exception e) {
            logger.error("cannot poll data", e);
        }
        start(300);
    }

    @Override
    protected void poll() throws IOException {
        boolean downloaded = AwsUtils.downloadFileIfChanged(config.workS3BucketName, config.workS3BucketPrefix, file, 0);
        if (downloaded || tagGroups == null) {
            logger.info("trying to read from " + file);
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            try {
                TreeMap<Long, Collection<TagGroup>> tagGroupsWithResourceGroups = TagGroup.Serializer.deserializeTagGroups(config, in);
                TreeMap<Long, Collection<TagGroup>> tagGroups = removeResourceGroups(tagGroupsWithResourceGroups);
                Interval totalInterval = null;
                if (tagGroups.size() > 0) {
                    totalInterval = new Interval(tagGroups.firstKey(), new DateTime(tagGroups.lastKey()).plusMonths(1).getMillis(), DateTimeZone.UTC);
                }
                this.totalInterval = totalInterval;
                this.tagGroups = tagGroups;
                this.tagGroupsWithResourceGroups = tagGroupsWithResourceGroups;
                logger.info("done reading " + file);
            }
            finally {
                in.close();
            }
        }
    }

    @Override
    protected String getThreadName() {
        return this.dbName;
    }

    private TreeMap<Long, Collection<TagGroup>> removeResourceGroups(TreeMap<Long, Collection<TagGroup>> tagGroups) {
        TreeMap<Long, Collection<TagGroup>> result = Maps.newTreeMap();
        for (Long key: tagGroups.keySet()) {
            Collection<TagGroup> from = tagGroups.get(key);
            Set<TagGroup> to = Sets.newHashSet();
            for (TagGroup tagGroup: from) {
                if (tagGroup.resourceGroup != null)
                    to.add(new TagGroup(tagGroup.account, tagGroup.region, tagGroup.zone, tagGroup.product, tagGroup.operation, tagGroup.usageType, null));
                else
                    to.add(tagGroup);
            }

            result.put(key, to);
        }
        return result;
    }

    private Set<TagGroup> getTagGroupsInRange(Collection<Long> monthMillis) {
        Set<TagGroup> tagGroupsInRange = Sets.newHashSet();
        for (Long monthMilli: monthMillis) {
            tagGroupsInRange.addAll(this.tagGroups.get(monthMilli));
        }
        return tagGroupsInRange;
    }

    private Set<TagGroup> getTagGroupsWithResourceGroupsInRange(Collection<Long> monthMillis) {
        Set<TagGroup> tagGroupsInRange = Sets.newHashSet();
        for (Long monthMilli: monthMillis) {
            tagGroupsInRange.addAll(this.tagGroupsWithResourceGroups.get(monthMilli));
        }
        return tagGroupsInRange;
    }

    private Collection<Long> getMonthMillis(Interval interval) {
        Set<Long> result = Sets.newTreeSet();
        for (Long milli: tagGroups.keySet()) {
            DateTime monthDate = new DateTime(milli, DateTimeZone.UTC);
            if (new Interval(monthDate, monthDate.plusMonths(1)).overlap(interval) != null)
                result.add(milli);
        }

        return result;
    }

    public Collection<Account> getAccounts(Interval interval, TagLists tagLists) {
        Set<Account> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup))
                result.add(tagGroup.account);
        }

        return result;
    }

    public Collection<Region> getRegions(Interval interval, TagLists tagLists) {
        Set<Region> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup))
                result.add(tagGroup.region);
        }

        return result;
    }

    public Collection<Zone> getZones(Interval interval, TagLists tagLists) {
        Set<Zone> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup) && tagGroup.zone != null)
                result.add(tagGroup.zone);
        }

        return result;
    }

    public Collection<Product> getProducts(Interval interval, TagLists tagLists) {
        Set<Product> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup))
                result.add(tagGroup.product);
        }

        return result;
    }

    public Collection<Operation> getOperations(Interval interval, TagLists tagLists) {
        Set<Operation> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagGroup.product == Product.emr || tagGroup.product == Product.redshift) {
                int iii = 0;
            }
            if (tagLists.contains(tagGroup))
                result.add(tagGroup.operation);
        }

        return result;
    }

    public Collection<UsageType> getUsageTypes(Interval interval, TagLists tagLists) {
        Set<UsageType> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup))
                result.add(tagGroup.usageType);
        }

        return result;
    }

    public Collection<ResourceGroup> getResourceGroups(Interval interval, TagLists tagLists) {
        Set<ResourceGroup> result = Sets.newTreeSet();
        Set<TagGroup> tagGroupsInRange = getTagGroupsWithResourceGroupsInRange(getMonthMillis(interval));

        for (TagGroup tagGroup: tagGroupsInRange) {
            if (tagLists.contains(tagGroup) && tagGroup.resourceGroup != null)
                result.add(tagGroup.resourceGroup);
        }

        return result;
    }

    public Collection<Account> getAccounts(TagLists tagLists) {
        return this.getAccounts(totalInterval, tagLists);
    }

    public Collection<Region> getRegions(TagLists tagLists) {
        return this.getRegions(totalInterval, tagLists);
    }

    public Collection<Zone> getZones(TagLists tagLists) {
        return this.getZones(totalInterval, tagLists);
    }

    public Collection<Product> getProducts(TagLists tagLists) {
        return this.getProducts(totalInterval, tagLists);
    }

    public Collection<Operation> getOperations(TagLists tagLists) {
        return this.getOperations(totalInterval, tagLists);
    }

    public Collection<UsageType> getUsageTypes(TagLists tagLists) {
        return this.getUsageTypes(totalInterval, tagLists);
    }

    public Collection<ResourceGroup> getResourceGroups(TagLists tagLists) {
        return this.getResourceGroups(totalInterval, tagLists);
    }

    public Interval getOverlapInterval(Interval interval) {
        return totalInterval == null ? null : totalInterval.overlap(interval);
    }

    public Map<Tag, TagLists> getTagListsMap(Interval interval, TagLists tagLists, TagType groupBy, boolean forReservation) {
        Map<Tag, TagLists> result = Maps.newHashMap();

        TagLists tagListsForTag = tagLists;
        if (groupBy != TagType.ResourceGroup && tagLists.resourceGroups != null && tagLists.resourceGroups.size() > 0) {
            tagListsForTag = new TagLists(tagLists.accounts, tagLists.regions, tagLists.zones, tagLists.products, tagLists.operations, tagLists.usageTypes);
        }

        List<Tag> groupByTags = Lists.newArrayList();
        switch (groupBy) {
            case Account:
                groupByTags.addAll(getAccounts(interval, tagListsForTag));
                break;
            case Region:
                groupByTags.addAll(getRegions(interval, tagListsForTag));
                break;
            case Zone:
                groupByTags.addAll(getZones(interval, tagListsForTag));
                break;
            case Product:
                groupByTags.addAll(getProducts(interval, tagListsForTag));
                break;
            case Operation:
                groupByTags.addAll(getOperations(interval, tagListsForTag));
                break;
            case UsageType:
                groupByTags.addAll(getUsageTypes(interval, tagListsForTag));
                break;
            case ResourceGroup:
                groupByTags.addAll(getResourceGroups(interval, tagListsForTag));
                break;
        }
        if (groupBy == TagType.Operation && !forReservation) {
            for (Operation.ReservationOperation lentOp: Operation.getLentInstances())
                groupByTags.remove(lentOp);
        }
        for (Tag tag: groupByTags) {
            if (tagLists.contains(tag, groupBy)) {
                TagLists tmp = tagLists.getTagLists(tag, groupBy);
                if (!forReservation && groupBy != TagType.Operation) {
                    if (tmp.operations == null || tmp.operations.size() == 0) {
                        List<Operation> operations = Lists.newArrayList(getOperations(tmp));
                        tmp = new TagLists(tmp.accounts, tmp.regions, tmp.zones, tmp.products, operations, tmp.usageTypes, tmp.resourceGroups);
                    }
                    for (Operation.ReservationOperation lentOp: Operation.getLentInstances())
                        tmp.operations.remove(lentOp);
                }
                result.put(tag, tmp);
            }
        }
        return result;
    }
}
