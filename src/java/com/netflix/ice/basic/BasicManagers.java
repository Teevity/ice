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
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.*;
import com.netflix.ice.processor.TagGroupWriter;
import com.netflix.ice.reader.*;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Tag;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class manages all BasicTagGroupManager and BasicDataManager instances.
 */
public class BasicManagers extends Poller implements Managers {
    private ReaderConfig config;

    private Set<Product> products = Sets.newHashSet();
    private Map<Product, BasicTagGroupManager> tagGroupManagers = Maps.newHashMap();
    private TreeMap<Key, BasicDataManager> costManagers = Maps.newTreeMap();
    private TreeMap<Key, BasicDataManager> usageManagers = Maps.newTreeMap();

    public void shutdown() {
        for (BasicTagGroupManager tagGroupManager: tagGroupManagers.values()) {
            tagGroupManager.shutdown();
        }
        for (BasicDataManager dataManager: costManagers.values()) {
            dataManager.shutdown();
        }
        for (BasicDataManager dataManager: usageManagers.values()) {
            dataManager.shutdown();
        }
    }

    public void init() {
        config = ReaderConfig.getInstance();
        doWork();
        start(300);
    }

    public Collection<Product> getProducts() {
        return products;
    }

    public TagGroupManager getTagGroupManager(Product product) {
        return tagGroupManagers.get(product);
    }

    public DataManager getCostManager(Product product, ConsolidateType consolidateType) {
        return costManagers.get(new Key(product, consolidateType));
    }

    public DataManager getUsageManager(Product product, ConsolidateType consolidateType) {
        return usageManagers.get(new Key(product, consolidateType));
    }

    @Override
    protected void poll() throws Exception {
        doWork();
    }

    private void doWork() {

        logger.info("trying to find new tag group and data managers...");
        Set<Product> products = Sets.newHashSet(this.products);
        Map<Product, BasicTagGroupManager> tagGroupManagers = Maps.newHashMap(this.tagGroupManagers);
        TreeMap<Key, BasicDataManager> costManagers = Maps.newTreeMap(this.costManagers);
        TreeMap<Key, BasicDataManager> usageManagers = Maps.newTreeMap(this.usageManagers);

        Set<Product> newProducts = Sets.newHashSet();
        AmazonS3Client s3Client = AwsUtils.getAmazonS3Client();
        for (S3ObjectSummary s3ObjectSummary: s3Client.listObjects(config.workS3BucketName, config.workS3BucketPrefix + TagGroupWriter.DB_PREFIX).getObjectSummaries()) {
            String key = s3ObjectSummary.getKey();
            Product product;
            if (key.endsWith("_all")) {
                product = null;
            }
            else {
                String name = key.substring((config.workS3BucketPrefix + TagGroupWriter.DB_PREFIX).length());
                name = Tag.fromS3(name);
                product = config.productService.getProductByName(name);
            }
            if (!products.contains(product)) {
                products.add(product);
                newProducts.add(product);
            }
        }

        for (Product product: newProducts) {
            tagGroupManagers.put(product, new BasicTagGroupManager(product));
            for (ConsolidateType consolidateType: ConsolidateType.values()) {
                Key key = new Key(product, consolidateType);
                costManagers.put(key, new BasicDataManager(product, consolidateType, true));
                usageManagers.put(key, new BasicDataManager(product, consolidateType, false));
            }
        }

        if (newProducts.size() > 0) {
            this.costManagers = costManagers;
            this.usageManagers = usageManagers;
            this.tagGroupManagers = tagGroupManagers;
            this.products = products;
        }
    }

    private static class Key implements Comparable<Key> {
        Product product;
        ConsolidateType consolidateType;
        Key(Product product, ConsolidateType consolidateType) {
            this.product = product;
            this.consolidateType = consolidateType;
        }

        public int compareTo(Key t) {
            int result = this.product == t.product ? 0 : (this.product == null ? 1 : (t.product == null ? -1 : t.product.compareTo(this.product)));
            if (result != 0)
                return result;
            return consolidateType.compareTo(t.consolidateType);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            Key other = (Key)o;
            return
                    this.product == other.product &&
                    this.consolidateType == other.consolidateType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            if (product != null)
                result = prime * result + this.product.hashCode();
            result = prime * result + this.consolidateType.hashCode();

            return result;
        }
    }


}
