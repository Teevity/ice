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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.TagGroup;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadWriteData {
    private List<Map<TagGroup, Double>> data;

    public ReadWriteData() {
        data = Lists.newArrayList();
    }

    private ReadWriteData(List<Map<TagGroup, Double>> data) {
        this.data = data;
    }

    public int getNum() {
        return data.size();
    }

    void cutData(int num) {
        if (data.size() > num)
            data = data.subList(0, num);
    }

    public Map<TagGroup, Double> getData(int i) {
        return getCreateData(data, i);
    }

    void setData(List<Map<TagGroup, Double>> newData, int startIndex, boolean merge) {
        for (int i = 0; i < newData.size(); i++) {
            int index = startIndex + i;

            if (index > data.size()) {
                getCreateData(data, index-1);
            }
            if (index >= data.size()) {
                data.add(newData.get(i));
            }
            else {
                if (merge) {
                    Map<TagGroup, Double> existed = data.get(index);
                    for (Map.Entry<TagGroup, Double> entry: newData.get(i).entrySet()) {
                        existed.put(entry.getKey(), entry.getValue());
                    }
                }
                else {
                    data.set(index, newData.get(i));
                }
            }
        }
    }

    static Map<TagGroup, Double> getCreateData(List<Map<TagGroup, Double>> data, int i) {
        if (i >= data.size()) {
            for (int j = data.size(); j <= i; j++) {
                data.add(Maps.<TagGroup, Double>newHashMap());
            }
        }
        return data.get(i);
    }

    public Collection<TagGroup> getTagGroups() {
        Set<TagGroup> keys = Sets.newTreeSet();

        for (Map<TagGroup, Double> map: data) {
            keys.addAll(map.keySet());
        }

        return keys;
    }

    public static class Serializer {
        public static void serialize(DataOutput out, ReadWriteData data) throws IOException {

            Collection<TagGroup> keys = data.getTagGroups();
            out.writeInt(keys.size());
            for (TagGroup tagGroup: keys) {
                TagGroup.Serializer.serialize(out, tagGroup);
            }

            out.writeInt(data.data.size());
            for (int i = 0; i < data.data.size(); i++) {
                Map<TagGroup, Double> map = data.getData(i);
                out.writeBoolean(map.size() > 0);
                if (map.size() > 0) {
                    for (TagGroup tagGroup: keys) {
                        Double v = map.get(tagGroup);
                        out.writeDouble(v == null ? 0 : v);
                    }
                }
            }
        }

        public static ReadWriteData deserialize(DataInput in) throws IOException {

            int numKeys = in.readInt();
            List<TagGroup> keys = Lists.newArrayList();
            for (int j = 0; j < numKeys; j++) {
                keys.add(TagGroup.Serializer.deserialize(ProcessorConfig.getInstance(), in));
            }

            List<Map<TagGroup, Double>> data = Lists.newArrayList();
            int num = in.readInt();
            for (int i = 0; i < num; i++)  {
                Map<TagGroup, Double> map = Maps.newHashMap();
                boolean hasData = in.readBoolean();
                if (hasData) {
                    for (int j = 0; j < keys.size(); j++) {
                        double v = in.readDouble();
                        if (v != 0) {
                            map.put(keys.get(j), v);
                        }
                    }
                }
                data.add(map);
            }

            return new ReadWriteData(data);
        }
    }
}
