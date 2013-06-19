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

public abstract class ResourceService {

    abstract public void init();

    /**
     * Get resource group name, e.g. auto scaling group name.
     * @param account
     * @param region
     * @param product
     * @param productService
     * @param resourceId
     * @param lineItem
     * @param millisStart
     * @return
     */
    public String getResource(Account account, Region region, Product product, ProductService productService, String resourceId, String[] lineItem, long millisStart){
        return product.name;
    }

    /**
     * Get products with resources.
     * @return
     */
    abstract public List<List<Product>> getProductsWithResources();

    /**
     * Commit resource mappings.
     */
    abstract public void commit();
}
