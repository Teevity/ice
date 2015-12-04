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

public class IceOptions {

    /**
     * Data start date in millis.
     */
    public static final String START_MILLIS = "ice.startmillis";

    /**
     * Property for company name. It must be specified in ReaderConfig.
     */
    public static final String COMPANY_NAME = "ice.companyName";

    /**
     * Property for currency sign. The default value is "$".
     */
    public static final String CURRENCY_SIGN = "ice.currencySign";

    /**
     * Property for currency rate. The default value is "1".
     */
    public static final String CURRENCY_RATE = "ice.currencyRate";

    /**
     * The URL of highstock.js. The default value is the Highcharts CDN; change this if you need to
     * serve it from somewhere else (for example, if you need HTTPS).
     */
    public static final String HIGHSTOCK_URL = "ice.highstockUrl";

    /**
     * s3 bucket name where billing files are located. For multiple payer accounts, multiple bucket names can be specified delimited by comma ",".
     * Only read permission is needed. It must be specified in Config.
     */
    public static final String BILLING_S3_BUCKET_NAME = "ice.billing_s3bucketname";

    /**
     * Region for billing s3 bucket. It should be specified for buckets using v4 validation ",".
     * It must be specified in Config.
     */
    public static final String BILLING_S3_BUCKET_REGION = "ice.billing_s3bucketregion";

    /**
     * Prefix of billing files in billing s3 bucket. For multiple payer accounts, multiple bucket prefixes can be specified delimited by comma ",".
     * It must be specified in Config.
     */
    public static final String BILLING_S3_BUCKET_PREFIX = "ice.billing_s3bucketprefix";

    /**
     * Payer account id. Must be specified if across-accounts role is used to access billing files. For multiple payer accounts, acocunt ids can
     * be specified delimited by comma ",".
     */
    public static final String BILLING_PAYER_ACCOUNT_ID = "ice.billing_payerAccountId";

    /**
     * Billing file access role name to assume. Must be specified if across-accounts role is used to access billing files. For multiple payer accounts,
     * role names can be specified delimited by comma ",".
     */
    public static final String BILLING_ACCESS_ROLENAME = "ice.billing_accessRoleName";

    /**
     * Billing file access external ID. It is optional. Specify it if cross-accounts role is used to access billing files and external id is needed.
     * For multiple payer accounts, external ids can be specified delimited by comma ",".
     */
    public static final String BILLING_ACCESS_EXTERNALID = "ice.billing_accessExternalId";

    /**
     * User can configure their custom tags.
     */
    public static final String CUSTOM_TAGS = "ice.customTags";

    /**
     * Boolean Flag whether to use blended or Unblended Costs.  Default is UnBlended Cost(false)
     */
    public static final String USE_BLENDED = "ice.use_blended";

    /**
     * s3 bucket name where output files are to be store. Both read and write permissions are needed. It must be specified in Config.
     */
    public static final String WORK_S3_BUCKET_NAME = "ice.work_s3bucketname";

    /**
     * Prefix of output files in output s3 bucket. It must be specified in Config.
     */
    public static final String WORK_S3_BUCKET_PREFIX = "ice.work_s3bucketprefix";

    /**
     * Local directory. It must be specified in Config.
     */
    public static final String LOCAL_DIR = "ice.localDir";

    /**
     * Monthly data cache size for reader. Default is 12.
     */
    public static final String MONTHLY_CACHE_SIZE = "ice.monthlycachesize";

    /**
     * Cost per monitor metric per hour, It's optional.
     */
    public static final String COST_PER_MONITORMETRIC_PER_HOUR = "ice.cost_per_monitormetric_per_hour";

    /**
     * url prefix, e.g. http://ice.netflix.com/
     */
    public static final String URL_PREFIX = "ice.urlPrefix";

    /**
     * from email address. It must be registered in aws ses.
     */
    public static final String FROM_EMAIL = "ice.fromEmail";

    /**
     * ec2 ondemand hourly cost threshold to send alert email. The alert email will be sent at most once per day.
     */
    public static final String ONDEMAND_COST_ALERT_THRESHOLD = "ice.ondemandCostAlertThreshold";

    /**
     * ec2 ondemand hourly cost alert emails, separated by ","
     */
    public static final String ONDEMAND_COST_ALERT_EMAILS = "ice.ondemandCostAlertEmails";

    /**
    * What pricing data ice should use when calculating usage costs for resource groups
    */
    public static final String RESOURCE_GROUP_COST = "ice.resourceGroupCost";

    /**
    * Enable weekly cost email per application groups
    */
    public static final String WEEKLYEMAILS = "ice.weeklyCostEmails";

    /**
    * from email address for weekly cost emails. Must be registered in aws ses.
    */
    public static final String WEEKLYFROM = "ice.weeklyCostEmails_fromEmail";

    /**
    * bcc email address for weekly cost emails.
    */
    public static final String WEEKLYBCC = "ice.weeklyCostEmails_bccEmail";

    /**
    * from email to use when test flag is enabled.
    */
    public static final String WEEKLYTEST = "ice.weeklyCostEmails_testEmail";

    /**
    * from email to use when test flag is enabled.
    */
    public static final String NUM_WEEKS_FOR_WEEKLYEMAILS = "ice.weeklyCostEmails_numWeeks";
}
