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

package com.netflix.ice

import grails.converters.JSON
import com.netflix.ice.tag.Product
import com.netflix.ice.tag.Account
import com.netflix.ice.tag.Region
import com.netflix.ice.tag.Zone
import com.netflix.ice.tag.UsageType
import com.netflix.ice.tag.Operation
import com.netflix.ice.tag.ResourceGroup
import com.netflix.ice.tag.TagType
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import org.joda.time.Interval
import com.netflix.ice.tag.Tag
import com.netflix.ice.reader.*;
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.collect.Maps
import org.json.JSONObject
import com.netflix.ice.common.ConsolidateType
import org.joda.time.Hours
import org.apache.commons.lang.StringUtils
import com.netflix.ice.common.AwsUtils


class DashboardController {
    private static ReaderConfig config = ReaderConfig.getInstance();
    private static Managers managers = config == null ? null : config.managers;
    private static DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd hha").withZone(DateTimeZone.UTC);
    private static DateTimeFormatter dayFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC);

    private static ReaderConfig getConfig() {
        if (config == null) {
            config = ReaderConfig.getInstance();
        }
        return config;
    }

    private static Managers getManagers() {
        if (managers == null) {
            managers = ReaderConfig.getInstance().managers;
        }
        return managers;
    }

    def index = {
        redirect(action: "summary")
    }

    def getAccounts = {
        TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
        Collection<Account> data = tagGroupManager == null ? [] : tagGroupManager.getAccounts(new TagLists());

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getRegions = {
        List<Account> accounts = getConfig().accountService.getAccounts(listParams("account"));

        TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
        Collection<Region> data = tagGroupManager == null ? [] : tagGroupManager.getRegions(new TagLists(accounts));

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getZones = {
        List<Account> accounts = getConfig().accountService.getAccounts(listParams("account"));
        List<Region> regions = Region.getRegions(listParams("region"));

        TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
        Collection<Zone> data = tagGroupManager == null ? [] : tagGroupManager.getZones(new TagLists(accounts, regions));

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getResourceGroupLists = {
        List<List<Product>> products = getConfig().resourceService.getProductsWithResources();

        def result = [];
        for (List<Product> productList: products) {
            def resourceGroups = Sets.newTreeSet();
            for (Product product: productList) {
                TagGroupManager tagGroupManager = getManagers().getTagGroupManager(product);
                if (tagGroupManager == null)
                    continue;
                def temp = tagGroupManager.getResourceGroups(new TagLists(null, null, null, Lists.newArrayList(product), null, null, null));
                resourceGroups.addAll(temp);
            }

            result.add([product: productList.get(0), data: resourceGroups]);
        }
        result = [status: 200, data: result]
        render result as JSON
    }

    def getProducts = {
        Object o = params;
        List<Account> accounts = getConfig().accountService.getAccounts(listParams("account"));
        List<Region> regions = Region.getRegions(listParams("region"));
        List<Zone> zones = Zone.getZones(listParams("zone"));
        List<Operation> operations = Operation.getOperations(listParams("operation"));
        List<Product> products = getConfig().productService.getProducts(listParams("product"));
        boolean showResourceGroups = params.getBoolean("showResourceGroups");
        boolean showAppGroups = params.getBoolean("showAppGroups");
        boolean showZones = params.getBoolean("showZones");
        if (showZones && (zones == null || zones.size() == 0)) {
            zones = Lists.newArrayList(getManagers().getTagGroupManager(null).getZones(new TagLists(accounts)));
        }

        Collection<Product> data;
        if (showResourceGroups) {
            data = Sets.newTreeSet();
            for (Product product: getManagers().getProducts()) {
                if (product == null)
                    continue;

                TagGroupManager tagGroupManager = getManagers().getTagGroupManager(product);
                Collection<Product> tmp = tagGroupManager.getProducts(new TagLists(accounts, regions, zones));
                data.addAll(tmp);
            }
        }
        else {
            TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
            data = tagGroupManager == null ? [] : tagGroupManager.getProducts(new TagLists(accounts, regions, zones, products, operations));
        }

        if (showAppGroups) {
            List<List<Product>> tmp = getConfig().resourceService.getProductsWithResources();
            Set<Product> set = Sets.newTreeSet();

            for (Product product: data) {
                for (List<Product> list: tmp) {
                    if (list.contains(product)) {
                        set.add(product);
                        break;
                    }
                }
            }
            data = set;
        }

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getResourceGroups = {
        List<Account> accounts = getConfig().accountService.getAccounts(listParams("account"));
        List<Region> regions = Region.getRegions(listParams("region"));
        List<Zone> zones = Zone.getZones(listParams("zone"));
        List<Product> products = getConfig().productService.getProducts(listParams("product"));

        Collection<Product> data = Sets.newTreeSet();
        for (Product product: products) {

            TagGroupManager tagGroupManager = getManagers().getTagGroupManager(product);
            Collection<ResourceGroup> resourceGroups = tagGroupManager.getResourceGroups(new TagLists(accounts, regions, zones, Lists.newArrayList(product)));
            data.addAll(resourceGroups);
        }

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getOperations = {
        def text = request.reader.text;
        JSONObject query = (JSONObject)JSON.parse(text);
        List<Account> accounts = getConfig().accountService.getAccounts(listParams(query, "account"));
        List<Region> regions = Region.getRegions(listParams(query, "region"));
        List<Zone> zones = Zone.getZones(listParams(query, "zone"));
        List<Product> products = getConfig().productService.getProducts(listParams(query, "product"));
        List<Operation> operations = Operation.getOperations(listParams(query, "operation"));
        boolean showResourceGroups = query.has("showResourceGroups") ? query.getBoolean("showResourceGroups") : false;
        boolean forReservation = query.has("forReservation") ? query.getBoolean("forReservation") : false;

        Collection<Operation> data;
        System.out.println(new Date().getTime());
        if (showResourceGroups) {
            data = Sets.newTreeSet();
            if (products.size() == 0) {
                products = Lists.newArrayList(getManagers().getProducts());
            }
            for (Product product: products) {
                if (product == null)
                    continue;

                TagGroupManager tagGroupManager = getManagers().getTagGroupManager(product);
                Collection<Operation> tmp = tagGroupManager.getOperations(new TagLists(accounts, regions, zones, products, operations, null, null));
                data.addAll(tmp);
                System.out.println(new Date().getTime() + " " + product);
            }
        }
        else {
            TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
            data = tagGroupManager == null ? [] : tagGroupManager.getOperations(new TagLists(accounts, regions, zones, products, operations, null, null));
        }

        if (!forReservation) {
            for (Operation.ReservationOperation lentOp: Operation.getLentInstances())
                data.remove(lentOp);
        }

        def result = [status: 200, data: data]
        render result as JSON
    }

    def getUsageTypes = {
        def text = request.reader.text;
        JSONObject query = (JSONObject)JSON.parse(text);
        List<Account> accounts = getConfig().accountService.getAccounts(listParams(query, "account"));
        List<Region> regions = Region.getRegions(listParams(query, "region"));
        List<Zone> zones = Zone.getZones(listParams(query, "zone"));
        List<Product> products = getConfig().productService.getProducts(listParams(query, "product"));
        List<Operation> operations = Operation.getOperations(listParams(query, "operation"));
        boolean showResourceGroups = query.has("showResourceGroups") ? query.getBoolean("showResourceGroups") : false;

        Collection<Product> data;
        if (showResourceGroups) {
            data = Sets.newTreeSet();
            if (products.size() == 0) {
                products = Lists.newArrayList(getManagers().getProducts());
            }
            for (Product product: products) {
                if (product == null)
                    continue;

                TagGroupManager tagGroupManager = getManagers().getTagGroupManager(product);
                Collection<UsageType> result = tagGroupManager.getUsageTypes(new TagLists(accounts, regions, zones, null, operations, null, null));
                data.addAll(result);
            }
        }
        else {
            TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
            data = tagGroupManager == null ? [] : tagGroupManager.getUsageTypes(new TagLists(accounts, regions, zones, products, operations, null, null));
        }

        def result = [status: 200, data: data]
        render result as JSON
    }

    def download = {
        def o = params;
        JSONObject query = new JSONObject();
        for (Map.Entry entry: params.entrySet()) {
            query.put(entry.getKey(), entry.getValue());
        }

        def result = doGetData(query);

        File file = File.createTempFile("aws", "csv");

        BufferedWriter bwriter = new BufferedWriter(new FileWriter(file));
        Long start = result.start;
        int num = result.data.values().iterator().next().length;

        String[] record = new String[result.data.size() + 1];
        record[0] = "Time";
        int index = 1;
        for (Map.Entry entry: result.data) {
            record[index++] = entry.getKey().name;
        }
        bwriter.write(StringUtils.join(record, ","));
        bwriter.newLine();

        ConsolidateType consolidateType = ConsolidateType.valueOf(query.getString("consolidate"));
        for (int timeIndex = 0; timeIndex < num; timeIndex++) {
            record[0] = dateFormatter.print(start);
            index = 1;
            for (Map.Entry entry: result.data) {
                double[] values = entry.getValue();
                record[index++] = values[timeIndex];
            }
            bwriter.write(StringUtils.join(record, ","));
            bwriter.newLine();
            if (consolidateType != ConsolidateType.monthly)
                start += result.interval;
            else
                start = new DateTime(start, DateTimeZone.UTC).plusMonths(1).getMillis()
        }
        bwriter.close();

        response.setHeader("Content-Type","application/octet-stream;")
        response.setHeader("Content-Length", "${file.size()}")
        response.setHeader("Content-disposition", "attachment;filename=aws.csv")

        FileInputStream input = new FileInputStream(file);
        response.outputStream << input;
        response.outputStream.flush();
        input.close();
        file.delete();
        return;
    }

    def getData = {
        def text = request.reader.text;
        JSONObject query = (JSONObject)JSON.parse(text);

        def result = doGetData(query);
        render result as JSON
    }

    def getApplicationGroup = {
        String name = params.get("name");
        def result = getConfig().applicationGroupService.getApplicationGroup(name);

        result = result == null ? [status: 404] : [status: 200, data: result];
        render result as JSON
    }

    def saveApplicationGroup = {
        def text = request.reader.text;
        getConfig().applicationGroupService.saveApplicationGroup(new ApplicationGroup(text));

        def result = [status: 200];
        render result as JSON
    }

    def deleteApplicationGroup = {
        String name = params.get("name");
        getConfig().applicationGroupService.deleteApplicationGroup(name);

        def result = [status: 200];
        render result as JSON
    }

    def getTimeSpan = {
        int spans = Integer.parseInt(params.spans);
        DateTime end = dateFormatter.parseDateTime(params.end);
        ConsolidateType consolidateType = ConsolidateType.valueOf(params.consolidate);

        DateTime start;
        if (consolidateType == ConsolidateType.daily) {
            end = end.plusDays(1).withMillisOfDay(0);
            start = end.minusDays(spans);
        }
        else if (consolidateType == ConsolidateType.hourly) {
            start = end.minusHours(spans);
        }
        else if (consolidateType == ConsolidateType.weekly) {
            end = end.plusDays(1).withMillisOfDay(0);
            end = end.plusDays( (8-end.dayOfWeek) % 7 );
            start = end.minusWeeks(spans);
        }
        else if (consolidateType == ConsolidateType.monthly) {
            end = end.plusDays(1).withMillisOfDay(0).plusMonths(1).withDayOfMonth(1);
            start = end.minusMonths(spans);
        }

        def result = [status: 200, start: dateFormatter.print(start), end: dateFormatter.print(end)];
        render result as JSON
    }

    def summary = {}

    def detail = {}

    def reservation = {}

    def breakdown = {}

    def editappgroup = {}

    def appgroup = {}

    private Map doGetData(JSONObject query) {
        TagGroupManager tagGroupManager = getManagers().getTagGroupManager(null);
        if (tagGroupManager == null) {
            return [status: 200, start: 0, data: [:], stats: [:], groupBy: "None"];
        }

        TagType groupBy = query.getString("groupBy").equals("None") ? null : TagType.valueOf(query.getString("groupBy"));
        boolean isCost = query.getBoolean("isCost");
        boolean breakdown = query.getBoolean("breakdown");
        boolean showsps = query.getBoolean("showsps");
        boolean factorsps = query.getBoolean("factorsps");
        AggregateType aggregate = AggregateType.valueOf(query.getString("aggregate"));
        List<Account> accounts = getConfig().accountService.getAccounts(listParams(query, "account"));
        List<Region> regions = Region.getRegions(listParams(query, "region"));
        List<Zone> zones = Zone.getZones(listParams(query, "zone"));
        List<Product> products = getConfig().productService.getProducts(listParams(query, "product"));
        List<Operation> operations = Operation.getOperations(listParams(query, "operation"));
        List<UsageType> usageTypes = UsageType.getUsageTypes(listParams(query, "usageType"));
        List<ResourceGroup> resourceGroups = ResourceGroup.getResourceGroups(listParams(query, "resourceGroup"));
        DateTime end = query.has("spans") ? dayFormatter.parseDateTime(query.getString("end")) : dateFormatter.parseDateTime(query.getString("end"));
        ConsolidateType consolidateType = ConsolidateType.valueOf(query.getString("consolidate"));
        ApplicationGroup appgroup = query.has("appgroup") ? new ApplicationGroup(query.getString("appgroup")) : null;
        boolean forReservation = query.has("forReservation") ? query.getBoolean("forReservation") : false;
        boolean showZones = query.has("showZones") ? query.getBoolean("showZones") : false;
        boolean showResourceGroups = query.has("showResourceGroups") ? query.getBoolean("showResourceGroups") : false;
        if (showZones && (zones == null || zones.size() == 0)) {
            zones = Lists.newArrayList(tagGroupManager.getZones(new TagLists(accounts)));
        }

        DateTime start;
        if (query.has("spans")) {
            int spans = query.getInt("spans");
            if (consolidateType == ConsolidateType.daily)
                start = end.minusDays(spans);
            else if (consolidateType == ConsolidateType.weekly)
                start = end.minusWeeks(spans);
            else if (consolidateType == ConsolidateType.monthly)
                start = end.minusMonths(spans);
        }
        else
            start = dateFormatter.parseDateTime(query.getString("start"));

        Interval interval = new Interval(start, end);
        Interval overlap_interval = tagGroupManager.getOverlapInterval(interval);
        if (overlap_interval != null) {
            interval = overlap_interval
        }
        if (interval.getEnd().getMonthOfYear() == new DateTime(DateTimeZone.UTC).getMonthOfYear()) {
            DateTime curMonth = new DateTime(DateTimeZone.UTC).withDayOfMonth(1).withMillisOfDay(0);
            int hoursWithData = getManagers().getUsageManager(null, ConsolidateType.hourly).getDataLength(curMonth);
            if (interval.getEnd().getHourOfDay() + (interval.getEnd().getDayOfMonth()-1) * 24 > hoursWithData) {
                interval = new Interval(interval.getStart(), curMonth.plusHours(hoursWithData));
            }
        }
        interval = roundInterval(interval, consolidateType);

        Map<Tag, double[]> data;
        if (groupBy == TagType.ApplicationGroup) {
            data = Maps.newTreeMap();
            if (products.size() == 0) {
                products = Lists.newArrayList(getManagers().getProducts());
            }

            Map<String, ApplicationGroup> appgroups = getConfig().applicationGroupService.getApplicationGroups();
            List<List<Product>> productsWithResources = getConfig().resourceService.getProductsWithResources();
            for (String name: appgroups.keySet()) {
                appgroup = appgroups.get(name);
                if (appgroup.data == null)
                    continue;
                for (Product product: products) {
                    if (product == null)
                        continue;

                    Product appgroupProduct = null;
                    for (List<Product> list: productsWithResources) {
                        if (list.contains(product)) {
                            appgroupProduct = list.get(0);
                            break;
                        }
                    }
                    if (appgroupProduct == null)
                        continue;

                    List<ResourceGroup> resourceGroupsOfProduct = ResourceGroup.getResourceGroups(appgroup.data.get(appgroupProduct.toString()));
                    if (resourceGroupsOfProduct.size() == 0)
                        continue;

                    DataManager dataManager = isCost ? getManagers().getCostManager(product, consolidateType) : getManagers().getUsageManager(product, consolidateType);
                    if (dataManager == null)
                        continue;
                    Map<Tag, double[]> dataOfProduct = dataManager.getData(
                        interval,
                        new TagLists(accounts, regions, zones, Lists.newArrayList(product), operations, usageTypes, resourceGroupsOfProduct),
                        null,
                        aggregate,
                        forReservation
                    );

                    Map<Tag, double[]> tmp = Maps.newHashMap();
                    tmp.put(new com.netflix.ice.tag.ApplicationGroup(name), dataOfProduct.get(Tag.aggregated));

                    merge(tmp, data);
                    System.out.println(product);
                }
            }
        }
        else if (resourceGroups.size() > 0 || groupBy == TagType.ResourceGroup || appgroup != null || showResourceGroups) {
            data = Maps.newTreeMap();
            if ((groupBy == TagType.ResourceGroup || appgroup != null) && products.size() == 0) {
                products = Lists.newArrayList(getManagers().getProducts());
            }
            else if (resourceGroups.size() > 0 && products.size() == 0) {
                products = Lists.newArrayList(getManagers().getProducts());
            }
            else if (showResourceGroups && products.size() == 0) {
                Set productSet = Sets.newTreeSet();
                for (Product product: getManagers().getProducts()) {
                    if (product == null)
                        continue;

                    Collection<Product> tmp = getManagers().getTagGroupManager(product).getProducts(new TagLists(accounts, regions, zones));
                    productSet.addAll(tmp);
                }
                products = Lists.newArrayList(productSet);
            }
            for (Product product: products) {
                if (product == null)
                    continue;

                if (appgroup != null) {
                    boolean found = false;
                    List<List<Product>> tmp = getConfig().resourceService.getProductsWithResources();
                    for (List<Product> list: tmp) {
                        if (list.contains(product)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        continue;
                }
                DataManager dataManager = isCost ? getManagers().getCostManager(product, consolidateType) : getManagers().getUsageManager(product, consolidateType);
                Map<Tag, double[]> dataOfProduct = dataManager.getData(
                    interval,
                    new TagLists(accounts, regions, zones, Lists.newArrayList(product), operations, usageTypes, resourceGroups),
                    groupBy,
                    aggregate,
                    forReservation
                );
                
                if (groupBy == TagType.Product && dataOfProduct.size() > 0) {
                    double[] currentProductValues = dataOfProduct.get(dataOfProduct.keySet().iterator().next());
                    dataOfProduct.put(Tag.aggregated, Arrays.copyOf(currentProductValues, currentProductValues.size()));
                } 
                
                merge(dataOfProduct, data);
                System.out.println(product);
            }
        }
        else {
            DataManager dataManager = isCost ? getManagers().getCostManager(null, consolidateType) : getManagers().getUsageManager(null, consolidateType);
            data = dataManager.getData(
                interval,
                new TagLists(accounts, regions, zones, products, operations, usageTypes, resourceGroups),
                groupBy,
                aggregate,
                forReservation
            );
        }
        def stats = getStats(data);
        if (aggregate == AggregateType.stats && data.size() > 1)
            data.remove(Tag.aggregated);

        def result = [status: 200, start: interval.getStartMillis(), data: data, stats: stats, groupBy: groupBy == null ? "None" : groupBy.name()]
        if (breakdown && data.size() > 0 && data.values().iterator().next().length > 0) {
            result.time = new IntRange(0, data.values().iterator().next().length - 1).collect {
                if (consolidateType == ConsolidateType.daily)
                    interval.getStart().plusDays(it).getMillis()
                else if (consolidateType == ConsolidateType.weekly)
                    interval.getStart().plusWeeks(it).getMillis()
                else if (consolidateType == ConsolidateType.monthly)
                    interval.getStart().plusMonths(it).getMillis()
            }
            result.hours = new IntRange(0, result.time.size() - 1).collect {
                int hours;
                if (consolidateType == ConsolidateType.daily)
                    hours = 24
                else if (consolidateType == ConsolidateType.weekly)
                    hours = 24*7
                else if (consolidateType == ConsolidateType.monthly)
                    hours = interval.getStart().plusMonths(it).dayOfMonth().getMaximumValue() * 24;

                if (it == result.time.size() - 1) {
                    DateTime period = new DateTime(result.time.get(result.time.size() - 1), DateTimeZone.UTC);
                    DateTime periodEnd = consolidateType == ConsolidateType.daily ? period.plusDays(1) : (consolidateType == ConsolidateType.weekly ? period.plusWeeks(1) : period.plusMonths(1));
                    DateTime month = period.withMillisOfDay(0).withDayOfMonth(1);
                    int dataHours = getManagers().getCostManager(null, ConsolidateType.hourly).getDataLength(month);
                    DateTime dataEnd = month.plusHours(dataHours);

                    if (dataEnd.isBefore(periodEnd)) {
                        hours - Hours.hoursBetween(dataEnd, periodEnd).getHours()
                    }
                    else {
                        hours
                    }
                }
                else {
                    hours
                }

            }

            result.data = data.sort {-it.getValue()[it.getValue().length-1]}
        }

        if (showsps || factorsps) {
            result.sps = config.throughputMetricService.getData(interval, consolidateType);
        }

        if (factorsps) {
            double[] consolidatedSps = result.sps;
            double multiply = config.throughputMetricService.getFactoredCostMultiply();
            for (Tag tag: result.data.keySet()) {
                double[] values = result.data.get(tag);
                for (int i = 0; i < values.length; i++) {
                    double sps = i < consolidatedSps.length ? consolidatedSps[i] : 0.0;
                    if (sps == 0.0)
                        values[i] = 0.0;
                    else
                        values[i] = values[i] / sps * multiply;
                }
            }
        }

        if (isCost && config.currencyRate != 1) {
            for (Tag tag: result.data.keySet()) {
                double[] values = result.data.get(tag);
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i] * config.currencyRate;
                }
            }

            for (Tag tag: result.stats.keySet()) {
                Map<String, Double> stat = result.stats.get(tag);
                for (Map.Entry<String, Double> entry: stat.entrySet()) {
                    entry.setValue(entry.getValue() * config.currencyRate);
                }
            }
        }

        if (consolidateType != ConsolidateType.monthly) {
            result.interval = consolidateType.millis;
        }
        else {
            result.time = new IntRange(0, data.values().iterator().next().length - 1).collect { interval.getStart().plusMonths(it).getMillis() }
        }
        return result;
    }

    private void merge(Map<Tag, double[]> from, Map<Tag, double[]> to) {
        for (Map.Entry<Tag, double[]> entry: from.entrySet()) {
            Tag tag = entry.getKey();
            double[] newValues = entry.getValue();
            if (to.containsKey(tag)) {
                double[] oldValues = to.get(tag);
                for (int i = 0; i < newValues.length; i++) {
                    oldValues[i] += newValues[i];
                }
            }
            else {
                to.put(tag, newValues);
            }
        }
    }

    private Map<Tag, Map> getStats(Map<Tag, double[]> data) {
        def result = [:];

        for (Map.Entry<Tag, double[]> entry: data.entrySet()) {
            Tag tag = entry.getKey();
            double[] values = entry.getValue();
            double max = 0;
            double total = 0;

            if (values.length == 0)
                continue;

            for (double v: values) {
                if (v > max)
                    max = v;
                total += v;
            }
            result[tag] = [max: max, total: total, average: total / values.length];
        }

        return result;
    }

    private List<String> listParams(String name) {
        if (params.containsKey(name)) {
            String value = params.get(name);
            return Lists.newArrayList(value.split(","));
        }
        else {
            return Lists.newArrayList();
        }
    }

    private List<String> listParams(JSONObject params, String name) {
        if (params.has(name)) {
            String value = params.getString(name);
            return Lists.newArrayList(value.split(","));
        }
        else {
            return Lists.newArrayList();
        }
    }

    private Interval roundInterval(Interval interval, ConsolidateType consolidateType) {
        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();

        if (consolidateType == ConsolidateType.daily) {
            start = start.minusHours(1).withHourOfDay(0).plusDays(1);
            //end = end.withHourOfDay(0);
        }
        else if (consolidateType == ConsolidateType.weekly) {
            start = start.withHourOfDay(0).minusDays(1).withDayOfWeek(1).plusWeeks(1);
            //end = end.withHourOfDay(0).withDayOfWeek(1);
        }
        else if (consolidateType == ConsolidateType.monthly) {
            start = start.withHourOfDay(0).minusDays(1).withDayOfMonth(1).plusMonths(1);
            //end = end.withHourOfDay(0).withDayOfMonth(1);
        }

        return new Interval(start, end);
    }
}
