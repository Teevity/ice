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
import com.netflix.ice.common.ResourceService;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Region;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class AppnetaMapDbResourceService extends ResourceService {

    public static final String UNKNOWN = "unknown";
    private static final Logger logger = LoggerFactory.getLogger(AppnetaMapDbResourceService.class);
    private static List<List<Product>> productsWithResources = Lists.<List<Product>>newArrayList(
            Lists.newArrayList(Product.ec2, Product.ec2_instance, Product.ebs),
            Lists.newArrayList(Product.rds),
            Lists.newArrayList(Product.s3));
    MapDb instanceDb;
    ProcessorConfig config;

    public void init() {
        config = ProcessorConfig.getInstance();
        instanceDb = new MapDb("instances");
    }

    @Override
    public void commit() {
        instanceDb.commit();
    }

    @Override
    public List<List<Product>> getProductsWithResources() {
        return productsWithResources;
    }

    @Override
    public String getResource(Account account, Region region, Product product, String resourceId, String[] lineItem, long millisStart) {

        if (product == Product.ec2 || product == Product.ec2_instance || product == Product.ebs || product == Product.cloudwatch) {
            return getEc2Resource(account, region, resourceId, lineItem, millisStart);
        }
        else if (product == Product.rds) {
            return getRdsResource(account, region, resourceId, lineItem, millisStart);
        }
        else if (product == Product.s3) {
            return getS3Resource(account, region, resourceId, lineItem, millisStart);
        }
        else if (product == Product.eip) {
            return null;
        }
        else {
            return resourceId;
        }
    }

    protected String getEc2Resource(Account account, Region region, String resourceId, String[] lineItem, long millisStart) {
        // custom tags to AppNeta, adjust as necessary if more tags are added
        // Creator, Name, Role are predefined tags in AWS, we use creator, Name, role
        // TODO: move to Creator/Name/Role
        int userTagIndex = config.lineItemProcessor.getUserTagStartIndex();
        int autoScalingGroupNameTagIndex = userTagIndex;
        int originalCreatorTagIndex = userTagIndex + 1;
        int nameTagIndex = userTagIndex + 2;
        int originalRoleTagIndex = userTagIndex + 3;
        int amiTagIndex = userTagIndex + 4;
        int amiNameTagIndex = userTagIndex + 5;
        int archTagIndex = userTagIndex + 6;
        int creatorTagIndex = userTagIndex + 7;
        int duplicatedNameTagIndex = userTagIndex + 8;
        int ownerTagIndex = userTagIndex + 9;
        int purposeTagIndex = userTagIndex + 10;
        int roleTagIndex = userTagIndex + 11;
        int statusTagIndex = userTagIndex + 12;

        if (lineItem.length <= config.lineItemProcessor.getUserTagStartIndex()) {
            return UNKNOWN;
        } else if (StringUtils.isEmpty(lineItem[roleTagIndex])) {
            return UNKNOWN;
        } else {
            String roleName = lineItem[roleTagIndex];
            instanceDb.SetResource(account, region, resourceId, roleName, millisStart);
            logger.debug("resource set, role name added. resourceId: {}, roleName: {}", resourceId, roleName);
            return roleName;
        }
    }

    protected String getRdsResource(Account account, Region region, String resourceId, String[] lineItem, long millisStart) {
        if (resourceId.indexOf(":db:") > 0)
            return resourceId.substring(resourceId.indexOf(":db:") + 4);
        else
            return resourceId;
    }

    protected String getS3Resource(Account account, Region region, String resourceId, String[] lineItem, long millisStart) {
        return resourceId;
    }
}
