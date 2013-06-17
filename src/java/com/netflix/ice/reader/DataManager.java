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

import com.netflix.ice.tag.Tag;
import com.netflix.ice.tag.TagType;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.Map;

/**
 * Interface to feed data to UI.
 */
public interface DataManager {

    /**
     * Get map of data.
     * @param interval
     * @param tagLists
     * @param groupBy
     * @param aggregate
     * @param forReservation
     * @return
     */
    Map<Tag, double[]> getData(Interval interval, TagLists tagLists, TagType groupBy, AggregateType aggregate, boolean forReservation);

    /**
     * Get data length.
     * @param start
     * @return
     */
    int getDataLength(DateTime start);
}
