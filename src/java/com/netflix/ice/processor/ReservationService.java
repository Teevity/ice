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

import com.amazonaws.services.ec2.model.ReservedInstances;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.tag.Region;
import com.netflix.ice.tag.UsageType;

import java.util.Collection;
import java.util.Map;

/**
 * Interface for reservations.
 */
public interface ReservationService {

    void init();

    /**
     * Get all tag groups with reservations
     * @param utilization
     * @return
     */
    Collection<TagGroup> getTagGroups(Ec2InstanceReservationPrice.ReservationUtilization utilization);

    /**
     *
     * @return
     */
    Ec2InstanceReservationPrice.ReservationUtilization getDefaultReservationUtilization(long time);

    /**
     * Get reservation info.
     * @param time
     * @param tagGroup
     * @param utilization
     * @return
     */
    ReservationInfo getReservation(
            long time,
            TagGroup tagGroup,
            Ec2InstanceReservationPrice.ReservationUtilization utilization);

    /**
     * Some companies may get different price tiers at different times depending on reservation cost.
     * This method is to get the latest hourly price including amortized upfront for given, time, region and usage type.
     * @param time
     * @param region
     * @param usageType
     * @param utilization
     * @return
     */
    double getLatestHourlyTotalPrice(
            long time,
            Region region,
            UsageType usageType,
            Ec2InstanceReservationPrice.ReservationUtilization utilization);

    /**
     * Called by ReservationCapacityPoller to update reservations.
     * @param reservations
     */
    void updateEc2Reservations(Map<String, ReservedInstances> reservations);


    public static class ReservationInfo {
        public final int capacity;
        public final double upfrontAmortized;
        public final double reservationHourlyCost;

        public ReservationInfo (int capacity, double upfrontAmortized, double reservationHourlyCost) {
            this.capacity = capacity;
            this.upfrontAmortized = upfrontAmortized;
            this.reservationHourlyCost = reservationHourlyCost;
        }
    }
}
