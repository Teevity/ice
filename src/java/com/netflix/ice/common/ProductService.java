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

import com.netflix.ice.tag.Product;

import java.util.Collection;
import java.util.List;

public interface ProductService {

    /**
     * Get product by aws name, e.g. "Amazon Elastic Compute Cloud", "AWS Elastic MapReduce"
     * @param name
     * @return product
     */
    Product getProductByAwsName(String name);

    /**
     * Get product by name, e.g. ec2, emr
     * @param name
     * @return
     */
    Product getProductByName(String name);

    /**
     * Get list of products from given names
     * @param names
     * @return list of products
     */
    public List<Product> getProducts(List<String> names);

    /**
     * Get list of products
     * @return list of products
     */
    public Collection<Product> getProducts();
}
