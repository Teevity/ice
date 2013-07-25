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

                    ReservedInstances reservation = new ReservedInstances()
                            .withAvailabilityZone(zone)
                            .withStart(new Date(start))
                            .withDuration(duration)
                            .withInstanceType(instanceType)
                            .withProductDescription(productDescription)
                            .withInstanceCount(instanceCount)
                            .withOfferingType(offeringType)
                            .withState(state);

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

        // poll each account and update map
        boolean hasNewReservations = false;

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
                    ec2Client = new AmazonEC2Client(AwsUtils.awsCredentialsProvider.getCredentials());

                for (Region region: Region.getAllRegions()) {

                    ec2Client.setEndpoint("ec2." + region.name + ".amazonaws.com");

                    try {
                        DescribeReservedInstancesResult result = ec2Client.describeReservedInstances();
                        for (ReservedInstances reservation: result.getReservedInstances()) {
                            String key = account.id + "," + region.name + "," + reservation.getReservedInstancesId();
                            if (!reservations.containsKey(key)) {
                                if (reservation.getState().equals("active")) {
                                    hasNewReservations = true;
                                    reservations.put(key, reservation);
                                }
                            }
                            else if (reservation.getState().equals("retired") && reservations.get(key).getState().equals("active")) {
                                ReservedInstances existingOne = reservations.get(key);
                                existingOne.setDuration((new Date().getTime() - existingOne.getStart().getTime())/1000);
                                existingOne.setState("retired");
                                hasNewReservations = true;
                                logger.info("retiring " + key);
                            }
                            else if (reservation.getState().equals("active") && reservations.get(key).getState().equals("retired")) {
                                ReservedInstances existingOne = reservations.get(key);
                                reservations.put(key, reservation);
                                reservations.put(key + new Date().getTime(), existingOne);
                                hasNewReservations = true;
                                logger.info("reusing reservation key " + key);
                            }
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

        if (hasNewReservations || !updatedConfig) {
            config.reservationService.updateEc2Reservations(reservations);
            updatedConfig = true;
        }

        if (hasNewReservations) {
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
                            reservation.getState()
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
        else {
            logger.info("no new reservations found");
        }
    }
}
