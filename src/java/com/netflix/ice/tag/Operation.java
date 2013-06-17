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
package com.netflix.ice.tag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class Operation extends Tag {

    protected int seq = Integer.MAX_VALUE;
    private Operation (String name) {
        super(name);
    }
    private static ConcurrentMap<String, Operation> operations = Maps.newConcurrentMap();

    public static final ReservationOperation ondemandInstances = new ReservationOperation("OndemandInstances", 0);
    public static final ReservationOperation reservedInstances = new ReservationOperation("ReservedInstances", 1);
    public static final ReservationOperation borrowedInstances = new ReservationOperation("BorrowedInstances", 2);
    public static final ReservationOperation lentInstances = new ReservationOperation("LentInstances", 3);
    public static final ReservationOperation unusedInstances = new ReservationOperation("UnusedInstances", 4);
    public static final ReservationOperation upfrontAmortized = new ReservationOperation("UpfrontAmortized", 5);

    public static Operation getOperation(String name) {

        Operation operation = operations.get(name);
        if (operation == null) {
            operations.putIfAbsent(name, new Operation(name));
            operation = operations.get(name);
        }

        return operation;
    }

    public static List<Operation> getOperations(List<String> names) {
        List<Operation> result = Lists.newArrayList();
        for (String name: names)
            result.add(operations.get(name));
        return result;
    }

    public static class ReservationOperation extends Operation {
        private ReservationOperation(String name, int seq) {
            super(name);
            this.seq = seq;
            operations.put(name, this);
        }
    }

    @Override
    public int compareTo(Tag t) {
        if (t instanceof Operation) {
            Operation o = (Operation)t;
            int result = this.seq - o.seq;
            return result == 0 ? this.name.compareTo(t.name) : result;
        }
        else
            return this.name.compareTo(t.name);
    }
}
