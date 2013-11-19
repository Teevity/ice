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

public class Product extends Tag {
    public static final Product cloudfront = new Product("cloudfront");
    public static final Product cloudhsm = new Product("cloudhsm");
    public static final Product cloudwatch = new Product("cloudwatch");
    public static final Product data_pipeline = new Product("data_pipeline");
    public static final Product data_transfer = new Product("data_transfer");
    public static final Product direct_connect = new Product("direct_connect");
    public static final Product dynamodb = new Product("dynamodb");
    public static final Product ebs = new Product("ebs");
    public static final Product ec2 = new Product("ec2");
    public static final Product ec2_instance = new Product("ec2_instance");
    public static final Product eip = new Product("eip");
    public static final Product elasticache = new Product("elasticache");
    public static final Product emr = new Product("emr");
    public static final Product glacier = new Product("glacier");
    public static final Product monitor = new Product("monitor");
    public static final Product rds = new Product("rds");
    public static final Product redshift = new Product("redshift");
    public static final Product route53 = new Product("route53");
    public static final Product s3 = new Product("s3");
    public static final Product simpledb = new Product("simpledb");
    public static final Product ses = new Product("ses");
    public static final Product sns = new Product("sns");
    public static final Product sqs = new Product("sqs");
    public static final Product storage_gateway = new Product("storage_gateway");
    public static final Product sws = new Product("sws");
    public static final Product vpc = new Product("vpc");

    public Product (String name) {
        super(name);
    }
}
