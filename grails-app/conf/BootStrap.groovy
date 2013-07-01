/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.netflix.ice.reader.ReaderConfig
import com.netflix.ice.processor.ProcessorConfig
import com.netflix.ice.JSONConverter
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.netflix.ice.common.IceOptions
import com.netflix.ice.processor.ReservationCapacityPoller
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.netflix.ice.basic.BasicAccountService
import com.google.common.collect.Lists
import com.netflix.ice.tag.Account
import com.google.common.collect.Maps
import com.netflix.ice.basic.BasicProductService
import com.netflix.ice.basic.BasicReservationService
import com.netflix.ice.basic.BasicLineItemProcessor
import com.netflix.ice.processor.Ec2InstanceReservationPrice
import com.netflix.ice.basic.BasicS3ApplicationGroupService
import com.netflix.ice.basic.BasicManagers
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class BootStrap {
    private static boolean initialized = false;
    private static Logger logger = LoggerFactory.getLogger(BootStrap.class);

    private ReaderConfig readerConfig;
    private ProcessorConfig processorConfig;

    def init = { servletContext ->
        if (initialized) {
            return;
        }

        InputStream is = null;
        try {

            logger.info('Starting ice...');

            Properties prop = new Properties();
            is = getClass().getClassLoader().getResourceAsStream(System.getProperty("ice.propertiesfile", "ice.properties"));
            if (is == null) {
                if (System.getenv().get("ICE_HOME") == null)
                    throw new IllegalArgumentException("ICE_HOME is not set.");
                is = new FileInputStream(new File(System.getenv().get("ICE_HOME"), System.getProperty("ice.propertiesfile", "ice.properties")));
            }
            prop.load(is);

            AWSCredentialsProvider credentialsProvider;
            if (StringUtils.isEmpty(System.getProperty("ice.s3AccessKeyId")) || StringUtils.isEmpty(System.getProperty("ice.s3SecretKey"))) {
                credentialsProvider = new InstanceProfileCredentialsProvider();
            }
            else {
                credentialsProvider = new AWSCredentialsProvider() {
                    com.amazonaws.auth.AWSCredentials getCredentials() {
                        return new com.amazonaws.auth.AWSCredentials() {
                            String getAWSAccessKeyId() {
                                return System.getProperty("ice.s3AccessKeyId");
                            }
                            String getAWSSecretKey() {
                                return System.getProperty("ice.s3SecretKey");
                            }
                        }
                    }

                    void refresh() {}
                };
            }

            JSONConverter.register();

            Map<String, Account> accounts = Maps.newHashMap();
            for (String name: prop.stringPropertyNames()) {
                if (name.startsWith("ice.account.")) {
                    String accountName = name.substring("ice.account.".length());
                    accounts.put(accountName, new Account(prop.getProperty(name), accountName));
                }
            }
            Map<Account, List<Account>> reservationAccounts = Maps.newHashMap();
            for (String name: prop.stringPropertyNames()) {
                if (name.startsWith("ice.owneraccount.")) {
                    String accountName = name.substring("ice.owneraccount.".length());
                    String[] childen = prop.getProperty(name).split(",");
                    List<Account> childAccouns = Lists.newArrayList();
                    for (String child: childen) {
                        Account childAccount = accounts.get(child);
                        if (childAccount != null)
                            childAccouns.add(childAccount);
                    }
                    reservationAccounts.put(accounts.get(accountName), childAccouns);
                }
            }

            BasicAccountService accountService = new BasicAccountService(Lists.newArrayList(accounts.values()), reservationAccounts);
            Properties properties = new Properties();
            if (!StringUtils.isEmpty(prop.getProperty(IceOptions.START_MILLIS)))
                properties.setProperty(IceOptions.START_MILLIS, prop.getProperty(IceOptions.START_MILLIS));
            else
                properties.setProperty(IceOptions.START_MILLIS, "" + new DateTime(DateTimeZone.UTC).withMillisOfDay(0).withDayOfMonth(1).getMillis());
            properties.setProperty(IceOptions.COMPANY_NAME, prop.getProperty(IceOptions.COMPANY_NAME));
            properties.setProperty(IceOptions.BILLING_S3_BUCKET_NAME, prop.getProperty(IceOptions.BILLING_S3_BUCKET_NAME));
            properties.setProperty(IceOptions.BILLING_S3_BUCKET_PREFIX, prop.getProperty(IceOptions.BILLING_S3_BUCKET_PREFIX));
            properties.setProperty(IceOptions.WORK_S3_BUCKET_NAME, prop.getProperty(IceOptions.WORK_S3_BUCKET_NAME));
            properties.setProperty(IceOptions.WORK_S3_BUCKET_PREFIX, prop.getProperty(IceOptions.WORK_S3_BUCKET_PREFIX));

            if ("true".equals(prop.getProperty("ice.processor"))) {

                properties.setProperty(IceOptions.LOCAL_DIR, prop.getProperty("ice.processor.localDir"));
                if (prop.getProperty(IceOptions.COST_PER_MONITORMETRIC_PER_HOUR) != null)
                    properties.setProperty(IceOptions.COST_PER_MONITORMETRIC_PER_HOUR, prop.getProperty(IceOptions.COST_PER_MONITORMETRIC_PER_HOUR));
                if (prop.getProperty(IceOptions.FROM_EMAIL) != null)
                    properties.setProperty(IceOptions.FROM_EMAIL, prop.getProperty(IceOptions.FROM_EMAIL));
                if (prop.getProperty(IceOptions.ONDEMAND_COST_ALERT_EMAILS) != null)
                    properties.setProperty(IceOptions.ONDEMAND_COST_ALERT_EMAILS, prop.getProperty(IceOptions.ONDEMAND_COST_ALERT_EMAILS));
                if (prop.getProperty(IceOptions.ONDEMAND_COST_ALERT_THRESHOLD) != null)
                    properties.setProperty(IceOptions.ONDEMAND_COST_ALERT_THRESHOLD, prop.getProperty(IceOptions.ONDEMAND_COST_ALERT_THRESHOLD));
                if (prop.getProperty(IceOptions.URL_PREFIX) != null)
                    properties.setProperty(IceOptions.URL_PREFIX, prop.getProperty(IceOptions.URL_PREFIX));

                ReservationCapacityPoller reservationCapacityPoller = null;
                if ("true".equals(prop.getProperty("ice.reservationCapacityPoller"))) {
                    AWSCredentialsProvider reservationCredentialProvider;
                    if (StringUtils.isEmpty(System.getProperty("ice.reservationAccessKeyId")) || StringUtils.isEmpty(System.getProperty("ice.reservationSecretKey"))) {
                        reservationCredentialProvider = new InstanceProfileCredentialsProvider();
                    }
                    else {
                        reservationCredentialProvider = new AWSCredentialsProvider() {
                            com.amazonaws.auth.AWSCredentials getCredentials() {
                                return new com.amazonaws.auth.AWSCredentials() {
                                    String getAWSAccessKeyId() {
                                        return System.getProperty("ice.reservationAccessKeyId");
                                    }
                                    String getAWSSecretKey() {
                                        return System.getProperty("ice.reservationSecretKey");
                                    }
                                }
                            }

                            void refresh() {}
                        };
                    }
                    reservationCapacityPoller = new ReservationCapacityPoller(reservationCredentialProvider,
                        System.getProperty("ice.reservationRoleResourceName"), System.getProperty("ice.reservationRoleSessionName"));
                }

                processorConfig = new ProcessorConfig(
                        properties,
                        credentialsProvider,
                        accountService,
                        new BasicProductService(),
                        new BasicReservationService(Ec2InstanceReservationPrice.ReservationPeriod.threeyear, Ec2InstanceReservationPrice.ReservationUtilization.HEAVY),
                        null,
                        new BasicLineItemProcessor(),
                        null)
                processorConfig.start(reservationCapacityPoller);
            }

            if ("true".equals(prop.getProperty("ice.reader"))) {
                properties.setProperty(IceOptions.LOCAL_DIR, prop.getProperty("ice.reader.localDir"));
                if (prop.getProperty(IceOptions.MONTHLY_CACHE_SIZE) != null)
                    properties.setProperty(IceOptions.MONTHLY_CACHE_SIZE, prop.getProperty(IceOptions.MONTHLY_CACHE_SIZE));
                if (prop.getProperty(IceOptions.CURRENCY_RATE) != null)
                    properties.setProperty(IceOptions.CURRENCY_RATE, prop.getProperty(IceOptions.CURRENCY_RATE));
                if (prop.getProperty(IceOptions.CURRENCY_SIGN) != null)
                    properties.setProperty(IceOptions.CURRENCY_SIGN, prop.getProperty(IceOptions.CURRENCY_SIGN));

                readerConfig = new ReaderConfig(
                        properties,
                        credentialsProvider,
                        new BasicManagers(),
                        accountService,
                        new BasicProductService(),
                        null,
                        new BasicS3ApplicationGroupService(),
                        null,
                        null)
                readerConfig.start();
            }

            initialized = true;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Startup failed", e);
            System.exit(0);
        }
        finally {
            if (is != null)
                is.close();
        }
    }

    def destroy = {
        logger.info("Shutting down ice...");

        try {
            if (processorConfig != null)
                processorConfig.shutdown();
            if (readerConfig != null)
                readerConfig.shutdown();
        }
        catch (Exception e) {
            logger.error("Failed to shut down...", e);
        }

        logger.info("Shut down complete.");
        initialized = false;
    }
}
