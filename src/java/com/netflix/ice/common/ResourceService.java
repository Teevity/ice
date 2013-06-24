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

import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Region;

import java.util.List;

/**
 * Please see a sample of subclass in SampleMapDbResourceService class.
 */
public abstract class ResourceService {

    /**
     * Subclass can choose different technologies to store the mapping of resource ids and resource group names.
     * E.g. SampleMapDbResourceService used MapDB to maintain a mapping of instance ids and application names.
     * You can initialize you mapping here.
     * You can also get the reference to ProcessorConfig instance here.
     */
    abstract public void init();

    /**
     * Get resource group name. Subclass can maintain a mapping of resource ids and resource group names.
     * Users can also choose to add user-defined tags in the billing file. E.g. in SampleMapDbResourceService,
     * the auto-scaling-group name is used to generate the resource group name.
     * @param account
     * @param region
     * @param product
     * @param resourceId: depending on product, resourceId could be:
     * 1) instance id, if product is ec2_instance. You can use Edda (https://github.com/Netflix/edda) to query application name from instance id.
     * 2) volumn id, if product is ebs. You can use Edda (https://github.com/Netflix/edda) to query instance id from volumn id, then application name from instance id.
     * 3) s3 bucket name if product is s3
     * 4) db name if product is rds
     * 5) etc.
     * @param lineItem: the line item in the billing file. You can access your user-defined tags here.
     * @param millisStart
     * @return
     */
    public String getResource(Account account, Region region, Product product, String resourceId, String[] lineItem, long millisStart){
        return product.name;
    }

    /**
     * Get products with resources. See example in SampleMapDbResourceService. This method will be used by UI to list
     * products with resources.
     * @return List of list of products. The inner list of products share the same resource groups.
     * E.g. in SampleMapDbResourceService, for products (ec2, ec2_instance, ebs), the resource group names are application names.
     */
    abstract public List<List<Product>> getProductsWithResources();

    /**
     * Commit resource mappings. This method will be called at the end of billing file processing to commit your mappings.
     */
    abstract public void commit();
}
