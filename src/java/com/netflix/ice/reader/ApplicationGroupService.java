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
package com.netflix.ice.reader;

import java.util.Map;

/**
 * Interface to manager user defined application groups.
 */
public interface ApplicationGroupService {

    void init();

    /**
     * Get a map of application groups. Key will be the application group name.
     * @return map of application groups
     */
    Map<String, ApplicationGroup> getApplicationGroups();

    /**
     * Get application group by name.
     * @param name
     * @return application group or null if not exist
     */
    ApplicationGroup getApplicationGroup(String name);

    /**
     * Save application group configuration.
     * @param applicationGroup
     * @return whether or not save was successfull
     */
    boolean saveApplicationGroup(ApplicationGroup applicationGroup);

    /**
     * Delete application group.
     * @param name
     * @return whether or not delete was successfull
     */
    boolean deleteApplicationGroup(String name);
}
