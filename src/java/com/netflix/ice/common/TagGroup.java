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
package com.netflix.ice.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.ice.tag.*;
import org.apache.commons.lang.StringUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TagGroup implements Comparable<TagGroup>, Serializable {
    public final Account account;
    public final Product product;
    public final Operation operation;
    public final UsageType usageType;
    public final Region region;
    public final Zone zone;
    public final ResourceGroup resourceGroup;
    public TagGroup(Account account, Region region, Zone zone, Product product, Operation operation, UsageType usageType, ResourceGroup resourceGroup) {
        this.account = account;
        this.region = region;
        this.zone = zone;
        this.product = product;
        this.operation = operation;
        this.usageType = usageType;
        this.resourceGroup = resourceGroup;
    }

    @Override
    public String toString() {
        return "\"" + account + "\",\"" + region + "\",\"" + zone + "\",\"" + product + "\",\"" + operation + "\",\"" + usageType + "\",\"" + resourceGroup + "\"";
    }

    public int compareTo(TagGroup t) {
        int result = this.account.compareTo(t.account);
        if (result != 0)
            return result;
        result = this.region.compareTo(t.region);
        if (result != 0)
            return result;
        result = this.zone == t.zone ? 0 : (this.zone == null ? 1 : (t.zone == null ? -1 : t.zone.compareTo(this.zone)));
        if (result != 0)
            return result;
        result = this.product.compareTo(t.product);
        if (result != 0)
            return result;
        result = this.operation.compareTo(t.operation);
        if (result != 0)
            return result;
        result = this.usageType.compareTo(t.usageType);
        if (result != 0)
            return result;
        result = this.resourceGroup == t.resourceGroup ? 0 : (this.resourceGroup == null ? 1 : (t.resourceGroup == null ? -1 : t.resourceGroup.compareTo(this.resourceGroup)));
            return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        TagGroup other = (TagGroup)o;
        return
                this.zone == other.zone &&
                this.account == other.account &&
                this.region == other.region &&
                this.product == other.product &&
                this.operation == other.operation &&
                this.usageType == other.usageType &&
                this.resourceGroup == other.resourceGroup;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (this.zone != null)
            result = prime * result + this.zone.hashCode();
        result = prime * result + this.account.hashCode();
        if (this.region == null || this.product == null) {
            int iii = 0;
        }
        result = prime * result + this.region.hashCode();
        result = prime * result + this.product.hashCode();
        result = prime * result + this.operation.hashCode();
        result = prime * result + this.usageType.hashCode();
        if (this.resourceGroup != null)
            result = prime * result + this.resourceGroup.hashCode();

        return result;
    }

    private static Map<TagGroup, TagGroup> tagGroups = Maps.newConcurrentMap();

    public static TagGroup getTagGroup(Account account, Region region, Zone zone, Product product, Operation operation, UsageType usageType, ResourceGroup resourceGroup) {
        TagGroup newOne = new TagGroup(account, region, zone, product, operation, usageType, resourceGroup);
        TagGroup oldOne = tagGroups.get(newOne);
        if (oldOne != null) {
            return oldOne;
        }
        else {
            tagGroups.put(newOne, newOne);
            return newOne;
        }
    }

    public static class Serializer {

        public static void serializeTagGroups(DataOutput out, TreeMap<Long, Collection<TagGroup>> tagGroups) throws IOException {
            out.writeInt(tagGroups.size());
            for (Long monthMilli: tagGroups.keySet()) {
                out.writeLong(monthMilli);
                Collection<TagGroup> keys = tagGroups.get(monthMilli);
                out.writeInt(keys.size());
                for (TagGroup tagGroup: keys) {
                    serialize(out, tagGroup);
                }
            }
        }

        public static void serialize(DataOutput out, TagGroup tagGroup) throws IOException {
            out.writeUTF(tagGroup.account.toString());
            out.writeUTF(tagGroup.region.toString());
            out.writeUTF(tagGroup.zone == null ? "" : tagGroup.zone.toString());
            out.writeUTF(tagGroup.product.toString());
            out.writeUTF(tagGroup.operation.toString());
            UsageType.serialize(out, tagGroup.usageType);
            out.writeUTF(tagGroup.resourceGroup == null ? "" : tagGroup.resourceGroup.toString());
        }

        public static TreeMap<Long, Collection<TagGroup>> deserializeTagGroups(Config config, DataInput in) throws IOException {
            int numCollections = in.readInt();
            TreeMap<Long, Collection<TagGroup>> result = Maps.newTreeMap();
            for (int i = 0; i < numCollections; i++) {
                long monthMilli = in.readLong();
                int numKeys = in.readInt();
                List<TagGroup> keys = Lists.newArrayList();
                for (int j = 0; j < numKeys; j++) {
                    keys.add(deserialize(config, in));
                }
                result.put(monthMilli, keys);
            }

            return result;
        }

        public static TagGroup deserialize(Config config, DataInput in) throws IOException {
            Account account = config.accountService.getAccountByName(in.readUTF());
            Region region = Region.getRegionByName(in.readUTF());
            String zoneStr = in.readUTF();
            Zone zone = StringUtils.isEmpty(zoneStr) ? null : Zone.getZone(zoneStr, region);
            String prodStr = in.readUTF();
            Product product = config.productService.getProductByName(prodStr);
            if (product == null) {
                int iii = 0;
            }
            Operation operation = Operation.getOperation(in.readUTF());
            UsageType usageType = UsageType.deserialize(in);
            String resourceGroupStr = in.readUTF();
            ResourceGroup resourceGroup = StringUtils.isEmpty(resourceGroupStr) ? null : ResourceGroup.getResourceGroup(resourceGroupStr);

            return TagGroup.getTagGroup(account, region, zone, product, operation, usageType, resourceGroup);
        }
    }
}
