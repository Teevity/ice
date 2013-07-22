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
package com.netflix.ice.basic;

import com.amazonaws.services.ec2.model.ReservedInstances;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.ice.common.AwsUtils;
import com.netflix.ice.common.Poller;
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.processor.Ec2InstanceReservationPrice;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.processor.ReservationService;
import com.netflix.ice.tag.*;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateMidnight;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class BasicReservationService extends Poller implements ReservationService {
    protected Logger logger = LoggerFactory.getLogger(BasicReservationService.class);
    protected ProcessorConfig config;
    protected Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> ec2InstanceReservationPrices =
            new ConcurrentSkipListMap<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice>();
    protected Map<TagGroup, List<Reservation>> reservations = Maps.newHashMap();
    protected Ec2InstanceReservationPrice.ReservationPeriod reservationPeriod;
    protected Ec2InstanceReservationPrice.ReservationUtilization reservationUtilization;
    protected File file;
    protected String linuxUrl;
    protected String windowsUrl;
    protected Long futureMillis = new DateMidnight().withYearOfCentury(99).getMillis();

    protected static Map<String, String> instanceTypes = Maps.newHashMap();
    protected static Map<String, String> instanceSizes = Maps.newHashMap();
    static {
        instanceTypes.put("stdResI", "m1");
        instanceTypes.put("secgenstdResI", "m3");
        instanceTypes.put("uResI", "t1");
        instanceTypes.put("hiMemResI", "m2");
        instanceTypes.put("hiCPUResI", "c1");
        instanceTypes.put("clusterCompResI", "cc1");
        instanceTypes.put("clusterHiMemResI", "cr1");
        instanceTypes.put("clusterGPUResI", "cg1");
        instanceTypes.put("hiIoResI", "hi1");
        instanceTypes.put("hiStoreResI", "hs1");

        instanceSizes.put("xxxxxxxxl", "8xlarge");
        instanceSizes.put("xxxxl", "4xlarge");
        instanceSizes.put("xxl", "2xlarge");
        instanceSizes.put("xl", "xlarge");
        instanceSizes.put("sm", "small");
        instanceSizes.put("med", "medium");
        instanceSizes.put("lg", "large");
        instanceSizes.put("u", "micro");
    }

    public BasicReservationService(Ec2InstanceReservationPrice.ReservationPeriod reservationPeriod, Ec2InstanceReservationPrice.ReservationUtilization reservationUtilization) {
        this.reservationPeriod = reservationPeriod;
        this.reservationUtilization = reservationUtilization;
    }

    public void init() {
        this.config = ProcessorConfig.getInstance();
        file = new File(config.localDir, "reservation_prices." + reservationPeriod.name() + "." + reservationUtilization.name());
        linuxUrl = "http://aws.amazon.com/ec2/pricing/ri-" + reservationUtilization.name().toLowerCase() + "-linux.json";
        windowsUrl = "http://aws.amazon.com/ec2/pricing/ri-" + reservationUtilization.name().toLowerCase() + "-mswin.json";

        AwsUtils.downloadFileIfNotExist(config.workS3BucketName, config.workS3BucketPrefix, file);
        if (!file.exists()) {
            try {
                poll();
            }
            catch (Exception e) {
                throw new RuntimeException("failed to poll reservation prices " + e.getMessage());
            }
            start(3600*24, 3600*24, true);
        }
        else {
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                ec2InstanceReservationPrices = Serializer.deserialize(in);
                in.close();
            }
            catch (Exception e) {
                throw new RuntimeException("failed to load reservation prices " + e.getMessage());
            }
            start(10, 3600*24, true);
        }
    }

    @Override
    protected void poll() throws Exception {
        logger.info("start polling reservation prices...");

        long currentTime = new DateMidnight().getMillis();

        URL url = new URL(linuxUrl);
        URLConnection urlConnection = url.openConnection();
        InputStream input = urlConnection.getInputStream();
        String linuxJsonStr = IOUtils.toString(input).trim();
        input.close();

        url = new URL(windowsUrl);
        urlConnection = url.openConnection();
        input = urlConnection.getInputStream();
        String windowsJsonStr = IOUtils.toString(input).trim();
        input.close();

        JSONObject linuxJsonObject = new JSONObject(new JSONTokener(linuxJsonStr));
        JSONObject windowsJsonObject = new JSONObject(new JSONTokener(windowsJsonStr));

        boolean hasNewPrice = readPrices(currentTime, false, linuxJsonObject);
        hasNewPrice = readPrices(currentTime, true, windowsJsonObject) || hasNewPrice;

        if (hasNewPrice) {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
            try {
                Serializer.serialize(out, this.ec2InstanceReservationPrices);
                AwsUtils.upload(config.workS3BucketName, config.workS3BucketPrefix, file);
            }
            finally {
                out.close();
            }
        }
    }

    private boolean readPrices(long currentTime, boolean isWindows, JSONObject jsonObject) throws JSONException {
        boolean hasNewPrice = false;

        jsonObject = jsonObject.getJSONObject("config");
        JSONArray regions = jsonObject.getJSONArray("regions");
        for (int i = 0; i < regions.length(); i++) {
            JSONObject regionJson = regions.getJSONObject(i);

            String regionStr = regionJson.getString("region");
            if (regionStr.equals("us-east"))
                regionStr = "us-east-1";
            Region region = Region.getRegionByName(regionStr);

            JSONArray instanceTypes = regionJson.getJSONArray("instanceTypes");
            for (int j = 0; j < instanceTypes.length(); j++) {
                JSONObject instanceType = instanceTypes.getJSONObject(j);
                String type = instanceType.getString("type");

                JSONArray sizes = instanceType.getJSONArray("sizes");
                for (int k = 0; k < sizes.length(); k++) {
                    JSONObject size = sizes.getJSONObject(k);
                    UsageType usageType = getUsageType(type, size.getString("size"), isWindows);

                    JSONArray valueColumns = size.getJSONArray("valueColumns");
                    Double hourly = null;
                    Double upfront = null;

                    for (int l = 0; l < valueColumns.length(); l++) {
                        JSONObject valueColumn = valueColumns.getJSONObject(l);

                        String name = valueColumn.getString("name");
                        String price = valueColumn.getJSONObject("prices").getString("USD");
                        if (name.equals(reservationPeriod.jsonTag) && !price.equals("N/A")) {
                            upfront = Double.parseDouble(price);
                        }
                        else if (name.startsWith(reservationPeriod.jsonTag) && !price.equals("N/A")) {
                            hourly = Double.parseDouble(price);
                        }

                    }
                    if (upfront != null && hourly != null)
                        hasNewPrice = setPrice(currentTime, region, usageType, upfront, hourly) || hasNewPrice;
                }
            }
        }

        return hasNewPrice;
    }

    private UsageType getUsageType(String type, String size, boolean isWindows) {
        type = instanceTypes.get(type);
        size = instanceSizes.get(size);

        return UsageType.getUsageType(type + "." + size + (isWindows ? "." + InstanceOs.windows : ""), Operation.reservedInstances, "");
    }

    public static class Serializer {
        public static void serialize(DataOutput out,
                                     Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> reservationPrices)
                throws IOException {

            out.writeInt(reservationPrices.size());
            for (Ec2InstanceReservationPrice.Key key: reservationPrices.keySet()) {
                Ec2InstanceReservationPrice.Key.Serializer.serialize(out, key);
                Ec2InstanceReservationPrice.Serializer.serialize(out, reservationPrices.get(key));
            }
        }

        public static Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> deserialize(DataInput in)
                throws IOException {

            int size = in.readInt();
            Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> result =
                    new ConcurrentSkipListMap<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice>();
            for (int i = 0; i < size; i++) {
                Ec2InstanceReservationPrice.Key key = Ec2InstanceReservationPrice.Key.Serializer.deserialize(in);
                Ec2InstanceReservationPrice price = Ec2InstanceReservationPrice.Serializer.deserialize(in);
                result.put(key, price);
            }

            return result;
        }
    }

    private boolean setPrice(long currentTime, Region region, UsageType usageType, double upfront, double hourly) {

        Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(region, usageType);
        Ec2InstanceReservationPrice reservationPrice = ec2InstanceReservationPrices.get(key);

        if (reservationPrice == null)  {
            reservationPrice = new Ec2InstanceReservationPrice();
            ec2InstanceReservationPrices.put(key, reservationPrice);
        }

        Ec2InstanceReservationPrice.Price latestHourly = reservationPrice.hourlyPrice.getCreatePrice(futureMillis);
        Ec2InstanceReservationPrice.Price latestUpfront = reservationPrice.upfrontPrice.getCreatePrice(futureMillis);

        if (latestHourly.getListPrice() == null) {
            latestHourly.setListPrice(hourly);
            latestUpfront.setListPrice(upfront);

            logger.info("setting reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return true;
        }
        else if (latestHourly.getListPrice() != hourly || latestUpfront.getListPrice() != upfront) {
            Ec2InstanceReservationPrice.Price oldHourly = reservationPrice.hourlyPrice.getCreatePrice(currentTime);
            Ec2InstanceReservationPrice.Price oldUpfront = reservationPrice.upfrontPrice.getCreatePrice(currentTime);
            oldHourly.setListPrice(latestHourly.getListPrice());
            oldUpfront.setListPrice(latestUpfront.getListPrice());

            latestHourly.setListPrice(hourly);
            latestUpfront.setListPrice(upfront);

            logger.info("changing reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return true;
        }
        else {
            logger.info("exisitng reservation price for " + usageType + " in " + region + ": " + upfront + " "  + hourly);
            return false;
        }
    }

    public static class Reservation {
        final int count;
        final long start;
        final long end;

        public Reservation(
                int count,
                long start,
                long end) {
            this.count = count;
            this.start = start;
            this.end = end;
        }
    }

    protected double getEc2Tier(long time) {
        return 0;
    }

    public Collection<TagGroup> getTaGroups() {
        return reservations.keySet();
    }

    public Ec2InstanceReservationPrice.ReservationPeriod getReservationPeriod() {
        return reservationPeriod;
    }

    public Ec2InstanceReservationPrice.ReservationUtilization getReservationUtilization() {
        return reservationUtilization;
    }

    public double getLatestHourlyTotalPrice(
            long time,
            Region region,
            UsageType usageType) {
        Ec2InstanceReservationPrice ec2Price =
            ec2InstanceReservationPrices.get(new Ec2InstanceReservationPrice.Key(region, usageType));

        double tier = getEc2Tier(time);
        return ec2Price.hourlyPrice.getPrice(null).getPrice(tier) +
               ec2Price.upfrontPrice.getPrice(null).getUpfrontAmortized(time, reservationPeriod, tier);
    }

    public ReservationInfo getReservation(
            long time,
            TagGroup tagGroup) {

        Ec2InstanceReservationPrice ec2Price =
            ec2InstanceReservationPrices.get(new Ec2InstanceReservationPrice.Key(tagGroup.region, tagGroup.usageType));

        double tier = getEc2Tier(time);

        double upfrontAmortized = 0;
        double houlyCost = 0;

        int count = 0;
        if (this.reservations.containsKey(tagGroup)) {
            for (Reservation reservation : this.reservations.get(tagGroup)) {
                if (time >= reservation.start && time < reservation.end) {
                    count += reservation.count;
                    if (ec2Price != null) { // remove this...
                    upfrontAmortized += reservation.count * ec2Price.upfrontPrice.getPrice(reservation.start).getUpfrontAmortized(reservation.start, reservationPeriod, tier);
                    houlyCost += reservation.count * ec2Price.hourlyPrice.getPrice(reservation.start).getPrice(tier);
                    }
                }
            }
        }

        if (count == 0) {
            if (ec2Price != null) { // remove this...
            upfrontAmortized = ec2Price.upfrontPrice.getPrice(null).getUpfrontAmortized(time, reservationPeriod, tier);
            houlyCost = ec2Price.hourlyPrice.getPrice(null).getPrice(tier);
            }
        }
        else {
            upfrontAmortized = upfrontAmortized / count;
            houlyCost = houlyCost / count;
        }

        return new ReservationInfo(count, upfrontAmortized, houlyCost);
    }

    public void updateEc2Reservations(Map<String, ReservedInstances> reservationsFromApi) {
        Map<TagGroup, List<Reservation>> reservationMap = Maps.newTreeMap();

        for (String key: reservationsFromApi.keySet()) {
            ReservedInstances reservedInstances = reservationsFromApi.get(key);
            if (reservedInstances.getInstanceCount() <= 0)
                continue;

            String accountId = key.substring(0, key.indexOf(","));
            Account account = config.accountService.getAccountById(accountId);
            Zone zone = Zone.getZone(reservedInstances.getAvailabilityZone());

            String offeringType = reservedInstances.getOfferingType();
            if (offeringType.indexOf(" ") > 0)
                offeringType = offeringType.substring(0, offeringType.indexOf(" "));
            Reservation reservation = new Reservation(
                    reservedInstances.getInstanceCount(), reservedInstances.getStart().getTime(), reservedInstances.getStart().getTime() + reservedInstances.getDuration() * 1000);

            String osStr = reservedInstances.getProductDescription().toLowerCase();
            InstanceOs os = osStr.contains("linux") ? InstanceOs.linux : InstanceOs.windows;

            UsageType usageType = UsageType.getUsageType(reservedInstances.getInstanceType() + (os == InstanceOs.windows ? "." + InstanceOs.windows : ""), "hours");

            TagGroup reservationKey = new TagGroup(account, zone.region, zone, Product.ec2_instance, Operation.reservedInstances, usageType, null);

            List<Reservation> reservations = reservationMap.get(reservationKey);
            if (reservations == null) {
                reservationMap.put(reservationKey, Lists.<Reservation>newArrayList(reservation));
            }
            else {
                reservations.add(reservation);
            }
        }

        this.reservations = reservationMap;
    }
}
