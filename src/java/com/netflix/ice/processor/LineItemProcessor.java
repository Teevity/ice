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

import com.netflix.ice.tag.Product;
import java.util.List;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Map;

/**
 * Interface to process each line item in billing file.
 */
public interface LineItemProcessor {
    public static final DateTimeFormatter amazonBillingDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(DateTimeZone.UTC);
    public static final DateTimeFormatter amazonBillingDateFormat2 = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm:ss").withZone(DateTimeZone.UTC);

    void initIndexes(ProcessorConfig config, boolean withTags, String[] header);
    List<String> getHeader();
    int getUserTagStartIndex();
    long getEndMillis(String[] items);
    Result process(long startMilli, boolean processAll, ProcessorConfig config, String[] items, Map<Product, ReadWriteData> usageDataByProduct, Map<Product, ReadWriteData> costDataByProduct, Map<String, Double> ondemandRate);

    public static enum Result {
        delay,
        ignore,
        hourly,
        monthly,
        daily
    }
}
