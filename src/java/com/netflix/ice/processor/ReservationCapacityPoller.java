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
package com.netflix.ice.processor;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeReservedInstancesResult;
import com.amazonaws.services.ec2.model.ReservedInstances;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.Poller;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.Region;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.Map;

/**
 * Class to poll reservation capacities.
 */
public class ReservationCapacityPoller extends Poller {
    private boolean updatedConfig = false;

    public boolean updatedConfig() {
        return updatedConfig;
    }

    @Override
    protected void poll() throws Exception {
        ProcessorConfig config = ProcessorConfig.getInstance();

        // read from s3 if not exists
        File file = new File(config.localDir, "reservation_capacity.txt");

        if (!file.exists()) {
            logger.info("downloading " + file + "...");
            AwsUtils.downloadFileIfNotExist(config.workS3BucketName, config.workS3BucketPrefix, file);
            logger.info("downloaded " + file);
        }

        // read from file
        Map<String, ReservedInstances> reservations = Maps.newTreeMap();
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    String accountId = tokens[0];
                    String region = tokens[1];
                    String reservationId = tokens[2];
                    String zone = tokens[3];
                    Long start = Long.parseLong(tokens[4]);
                    long duration = Long.parseLong(tokens[5]);
                    String instanceType = tokens[6];
                    String productDescription = tokens[7];
                    int instanceCount = Integer.parseInt(tokens[8]);
                    String offeringType = tokens[9];
                    String state = tokens[10];
                    Long end = tokens.length > 11 ? Long.parseLong(tokens[11]) : null;
                    float fixedPrice = tokens.length > 12 ? Float.parseFloat(tokens[12]) : 0;
                    float usagePrice = tokens.length > 13 ? Float.parseFloat(tokens[13]) : 0;

                    ReservedInstances reservation = new ReservedInstances()
                            .withAvailabilityZone(zone)
                            .withStart(new Date(start))
                            .withDuration(duration)
                            .withInstanceType(instanceType)
                            .withProductDescription(productDescription)
                            .withInstanceCount(instanceCount)
                            .withOfferingType(offeringType)
                            .withState(state)
                            .withFixedPrice(fixedPrice)
                            .withUsagePrice(usagePrice);
                    if (end != null)
                        reservation.setEnd(new Date(end));
                    else
                        reservation.setEnd(new Date(start + duration * 1000));

                    reservations.put(accountId + "," + region + "," + reservationId, reservation);
                }
            }
            catch (Exception e) {
                logger.error("error in reading " + file, e);
            }
            finally {
                if (reader != null)
                    try {reader.close();} catch (Exception e) {}
            }
        }
        logger.info("read " + reservations.size() + " reservations.");

        for (Account account: config.accountService.getReservationAccounts().keySet()) {
            try {
                AmazonEC2Client ec2Client;
                String assumeRole = config.accountService.getReservationAccessRoles().get(account);
                if (assumeRole != null) {
                    String externalId = config.accountService.getReservationAccessExternalIds().get(account);
                    final Credentials credentials = AwsUtils.getAssumedCredentials(account.id, assumeRole, externalId);
                    ec2Client = new AmazonEC2Client(new AWSSessionCredentials() {
                        public String getAWSAccessKeyId() {
                            return credentials.getAccessKeyId();
                        }

                        public String getAWSSecretKey() {
                            return credentials.getSecretAccessKey();
                        }

                        public String getSessionToken() {
                            return credentials.getSessionToken();
                        }
                    });
                }
                else
                    ec2Client = new AmazonEC2Client(AwsUtils.awsCredentialsProvider.getCredentials(), AwsUtils.clientConfig);

                for (Region region: Region.getAllRegions()) {
                    // GovCloud uses different credentials than standard AWS, so you would need two separate
                    // sets of credentials if you wanted to poll for RIs in both environments. For now, we
                    // just ignore GovCloud when polling for RIs in order to prevent AuthFailure errors.
                    if (region == Region.US_GOV_WEST_1) {
                        continue;
                    }

                    ec2Client.setEndpoint("ec2." + region.name + ".amazonaws.com");

                    try {
                        DescribeReservedInstancesResult result = ec2Client.describeReservedInstances();
                        for (ReservedInstances reservation: result.getReservedInstances()) {
                            String key = account.id + "," + region.name + "," + reservation.getReservedInstancesId();
                            reservations.put(key, reservation);
                            if (reservation.getEnd() == null)
                                reservation.setEnd(new Date(reservation.getStart().getTime() + reservation.getDuration() * 1000L));
                            if (reservation.getFixedPrice() == null)
                                reservation.setFixedPrice(0f);
                            if (reservation.getUsagePrice() == null)
                                reservation.setUsagePrice(0f);
                        }
                    }
                    catch (Exception e) {
                        logger.error("error in describeReservedInstances for " + region.name + " " + account.name, e);
                    }
                }

                ec2Client.shutdown();
            }
            catch (Exception e) {
                logger.error("Error in describeReservedInstances for " + account.name, e);
            }
        }

        config.reservationService.updateEc2Reservations(reservations);
        updatedConfig = true;

        // archive to disk
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for (String key: reservations.keySet()) {
                ReservedInstances reservation = reservations.get(key);
                String[] line = new String[] {key,
                        reservation.getAvailabilityZone(),
                        reservation.getStart().getTime() + "",
                        reservation.getDuration().toString(),
                        reservation.getInstanceType(),
                        reservation.getProductDescription(),
                        reservation.getInstanceCount().toString(),
                        reservation.getOfferingType(),
                        reservation.getState(),
                        reservation.getEnd().getTime() + "",
                        reservation.getFixedPrice() + "",
                        reservation.getUsagePrice() + "",
                };
                writer.write(StringUtils.join(line, ","));
                writer.newLine();
            }
        }
        catch (Exception e) {
            logger.error("",  e);
        }
        finally {
            if (writer != null)
                try {writer.close();} catch (Exception e) {}
        }
        logger.info("archived " + reservations.size() + " reservations.");

        // archive to s3
        logger.info("uploading " + file + "...");
        AwsUtils.upload(config.workS3BucketName, config.workS3BucketPrefix, config.localDir, file.getName());
        logger.info("uploaded " + file);
    }
}
