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

import java.util.Map;

/**
 * Used to generate random/fake data
 */
public interface Randomizer {

    /**
     * Get random/fake usage for given tag group.
     * @param time
     * @param tagGroup
     * @param usage
     * @return random/fake usage
     */
    double randomizeUsage(long time, TagGroup tagGroup, double usage);

    /**
     * Get random/fake cost for given tag group.
     * @param tagGroup
     * @return random/fake cost
     */
    double randomizeCost(TagGroup tagGroup);

    /**
     * Get map of resource distribution.
     * @return map of resource distribution
     */
    Map<String, Double> getDistribution(TagGroup tagGroup);
}
