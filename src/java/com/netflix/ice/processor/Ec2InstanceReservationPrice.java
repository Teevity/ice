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

import com.google.common.collect.Maps;
import com.netflix.ice.tag.Region;
import com.netflix.ice.tag.UsageType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Hours;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Ec2InstanceReservationPrice {

    public final VersionedPrice upfrontPrice;
    public final VersionedPrice hourlyPrice;

    public Ec2InstanceReservationPrice() {
        upfrontPrice = new VersionedPrice();
        hourlyPrice = new VersionedPrice();
    }

    private Ec2InstanceReservationPrice(VersionedPrice upfrontPrice, VersionedPrice hourlyPrice) {
        this.upfrontPrice = upfrontPrice;
        this.hourlyPrice = hourlyPrice;
    }

    public static class VersionedPrice {
        private ConcurrentSkipListMap<Long, Price> prices = new ConcurrentSkipListMap<Long, Price>();

        public Price getCreatePrice(Long millis) {
            Price price = prices.get(millis);
            if (price == null) {
                price = new Price();
                prices.put(millis, price);
            }
            return price;
        }

        public Price getPrice(Long millis) {
            if (millis == null)
                return prices.lastEntry().getValue();

            for (Map.Entry<Long, Price> entry : prices.entrySet()) {
                if (entry.getKey() > millis) {
                    return entry.getValue();
                }
            }
            return prices.lastEntry().getValue();
            //throw new RuntimeException("Cannot find list price");
        }

        public double getListPrice(long millis) {
            for (Map.Entry<Long, Price> entry : prices.entrySet()) {
                if (entry.getKey() > millis) {
                    return entry.getValue().listPrice;
                }
            }
            throw new RuntimeException("Cannot find list price");
        }
    }

    public static class Price {
        private Double listPrice = null;
        private TreeMap<Long, Double> tierPrice = Maps.newTreeMap();

        public void setListPrice(double listPrice) {
            this.listPrice = listPrice;
        }

        public Double getListPrice() {
            return this.listPrice;
        }

        public void setPrice(Long tier, Double price) {
            tierPrice.put(tier, price);
        }

        public double getPrice(double tier) {
            for (Map.Entry<Long, Double> entry : tierPrice.descendingMap().entrySet()) {
                if (tier > entry.getKey()) {
                    return entry.getValue();
                }
            }
            return listPrice;
        }

        public double getUpfrontAmortized(long startMillis, ReservationPeriod reservationPeriod, double tier) {
            double price = -1;
            for (Map.Entry<Long, Double> entry : tierPrice.descendingMap().entrySet()) {
                if (tier > entry.getKey()) {
                    price = entry.getValue();
                    break;
                }
            }
            if (price < 0)
                price = listPrice;

            DateTime start = new DateTime(startMillis, DateTimeZone.UTC);
            DateTime end = start.plusYears(reservationPeriod.years);
            int hours = Hours.hoursBetween(start, end).getHours();
            return price / hours;
        }
    }

    public static class Serializer {
        private static void serializeVersionedPrice(DataOutput out, VersionedPrice versionedPrice) throws IOException {
            out.writeInt(versionedPrice.prices.size());
            for (Long millis: versionedPrice.prices.keySet()) {
                out.writeLong(millis);
                out.writeDouble(versionedPrice.prices.get(millis).listPrice);
            }
        }

        private static VersionedPrice deserializeVersionedPrice(DataInput in) throws IOException {
            VersionedPrice versionedPrice = new VersionedPrice();
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                long millis = in.readLong();
                double price = in.readDouble();
                versionedPrice.getCreatePrice(millis).setListPrice(price);
            }
            return versionedPrice;
        }

        public static void serialize(DataOutput out, Ec2InstanceReservationPrice reservationPrice) throws IOException {
            serializeVersionedPrice(out, reservationPrice.upfrontPrice);
            serializeVersionedPrice(out, reservationPrice.hourlyPrice);
        }

        public static Ec2InstanceReservationPrice deserialize(DataInput in) throws IOException {
            VersionedPrice upfrontPrice = deserializeVersionedPrice(in);
            VersionedPrice hourlyPrice = deserializeVersionedPrice(in);

            return new Ec2InstanceReservationPrice(upfrontPrice, hourlyPrice);
        }
    }

    public static class Key implements Comparable<Key> {
        public final Region region;
        public final UsageType usageType;

        public Key(Region region, UsageType usageType) {
            this.region = region;
            this.usageType = usageType;
        }

        public int compareTo(Key t) {
            int result = this.region.compareTo(t.region);
            if (result != 0)
                return result;
            result = this.usageType.compareTo(t.usageType);
            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(region.toString()).append("|").append(usageType);
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            Key other = (Key)o;
            return
                this.region == other.region &&
                this.usageType == other.usageType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.region.hashCode();
            result = prime * result + this.usageType.hashCode();
            return result;
        }

        public static class Serializer {
            public static void serialize(DataOutput out, Key key) throws IOException {
                out.writeUTF(key.region.toString());
                UsageType.serialize(out, key.usageType);
            }

            public static Key deserialize(DataInput in) throws IOException {
                Region region = Region.getRegionByName(in.readUTF());
                UsageType usageType = UsageType.deserialize(in);

                return new Key(region, usageType);
            }
        }
    }

    public static enum ReservationPeriod {
        oneyear(1, "yrTerm1"),
        threeyear(3, "yrTerm3");

        public final int years;
        public final String jsonTag;

        private ReservationPeriod(int years, String jsonTag) {
            this.years = years;
            this.jsonTag = jsonTag;
        }

    }

    public static enum ReservationUtilization {
        LIGHT,
        MEDIUM,
        HEAVY,
        FIXED;

        static final Map<String, String> reservationTypeMap = new HashMap<String, String>();
        static {
            reservationTypeMap.put("ALL", "HEAVY");
            reservationTypeMap.put("PARTIAL", "HEAVY");
            reservationTypeMap.put("NO", "HEAVY");
        }

        public static ReservationUtilization get(String offeringType) {
            int idx = offeringType.indexOf(" ");
            if (idx > 0) {
                offeringType = offeringType.substring(0, idx).toUpperCase();
                String mappedValue = reservationTypeMap.get(offeringType);
                if (mappedValue != null)
                    offeringType = mappedValue;
                return valueOf(offeringType);
            }
            else {
                for (ReservationUtilization utilization: values()) {
                    if (offeringType.toUpperCase().startsWith(utilization.name()))
                        return utilization;
                }
                throw new RuntimeException("Unknown ReservationUtilization " + offeringType);
            }
        }
    }
}

