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

import com.google.common.collect.Lists;
import com.netflix.ice.common.TagGroup;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ReadOnlyData {
    double[][] data;
    private Collection<TagGroup> tagGroups;

    public ReadOnlyData(double[][] data, Collection<TagGroup> tagGroups) {
        this.data = data;
        this.tagGroups = tagGroups;
    }

    public double[] getData(int i) {
        return data[i];
    }

    public int getNum() {
        return data.length;
    }

    public Collection<TagGroup> getTagGroups() {
        return tagGroups;
    }

    public static class Serializer {

        public static ReadOnlyData deserialize(DataInput in) throws IOException {

            int numKeys = in.readInt();
            List<TagGroup> keys = Lists.newArrayList();
            for (int j = 0; j < numKeys; j++) {
                keys.add(TagGroup.Serializer.deserialize(ReaderConfig.getInstance(), in));
            }

            int num = in.readInt();
            double[][] data = new double[num][];
            for (int i = 0; i < num; i++)  {
                data[i] = new double[keys.size()];
                boolean hasData = in.readBoolean();
                if (hasData) {
                    for (int j = 0; j < keys.size(); j++) {
                        double v = in.readDouble();
                        if (v != 0) {
                            data[i][j] = v;
                        }
                    }
                }
            }

            return new ReadOnlyData(data, keys);
        }
    }
}
