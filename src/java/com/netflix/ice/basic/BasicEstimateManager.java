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

import com.google.common.cache.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.ice.common.*;
import com.netflix.ice.reader.*;
import com.netflix.ice.tag.Product;
import com.netflix.ice.tag.Tag;
import com.netflix.ice.tag.Account;
import com.netflix.ice.tag.TagType;
import org.joda.time.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * This class reads data from s3 bucket and feeds the data to UI
 */
public class BasicEstimateManager extends Poller implements DataManager {

    protected ReaderConfig config = ReaderConfig.getInstance();
    protected ConsolidateType consolidateType;



    public BasicEstimateManager(ConsolidateType consolidateType) {
        this.consolidateType = consolidateType;

    }

    /**
     * We check if new data is available periodically
     * @throws Exception
     */
    @Override
    protected void poll() throws Exception {
      // no-op
    }

    @Override
    protected String getThreadName() {
        return "BasicEstimateManager";
    }

    /**
     *
     * @param interval
     * @param tagLists
     * @param groupBy
     * @param aggregate
     * @param forReservation
     * @return
     */
    public double[] getEstimatesData(Interval interval, Account account, TagType groupBy, AggregateType aggregate, boolean forReservation) {

        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();
        Map<DateTime, Double> accountEstimates = account.dailyEstimates;

        if (accountEstimates == null) {
            return new double[0];
        }

        if (consolidateType == ConsolidateType.hourly) {
            start = interval.getStart().withDayOfMonth(1).withMillisOfDay(0);
            end = interval.getEnd();
        }
        else if (consolidateType == ConsolidateType.daily) {
            start = interval.getStart().withDayOfYear(1).withMillisOfDay(0);
            end = interval.getEnd();
        }

        int num = 0;
        if (consolidateType == ConsolidateType.hourly) {
            num = interval.toPeriod(PeriodType.hours()).getHours();
            if (interval.getStart().plusHours(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.daily) {
            num = interval.toPeriod(PeriodType.days()).getDays();
            if (interval.getStart().plusDays(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.weekly) {
            num = interval.toPeriod(PeriodType.weeks()).getWeeks();
            if (interval.getStart().plusWeeks(num).isBefore(interval.getEnd()))
                num++;
        }
        else if (consolidateType == ConsolidateType.monthly) {
            num = interval.toPeriod(PeriodType.months()).getMonths();
            if (interval.getStart().plusMonths(num).isBefore(interval.getEnd()))
                num++;
        }

        double[] accountResults = new double[num];
        DateTime currentEstimateDate=start;
        DateTime estimateChangeAt=null;
        double currentEstimate=0;


        //we have start, num of items and a ConsolidateType
        for(int i=0;i<num;i++) {
            // for month and week, we still need to iterate by days
            // so that we can account for estimate changes mid-week/mid-month
            boolean nextIndex=false;
            while (!nextIndex) {
                if (
                    (accountEstimates.size() > 0 && currentEstimate == 0) ||
                    (accountEstimates.size() > 0 && currentEstimateDate.isAfter(estimateChangeAt))
                ) {

                    boolean foundCurrent = false;
                    boolean foundNext = false;

                    // accountEstimates is a sorted TreeMap
                    for (Map.Entry<DateTime, Double> accountEstimate : accountEstimates.entrySet()) {

                        // This might be our only option
                        if  (accountEstimates.size() == 1) {
                            currentEstimate = accountEstimate.getValue().doubleValue();
                            estimateChangeAt = new DateTime(2042); // No further estimates
                            break;
                        }

                        if (foundCurrent && foundNext) {
                            estimateChangeAt = accountEstimate.getKey();
                            break;
                        }

                        if ((accountEstimate.getKey().equals(currentEstimateDate) || accountEstimate.getKey().isBefore(currentEstimateDate))) {
                            foundCurrent = true;
                            continue;
                        }

                        if (foundCurrent) {
                            foundNext = true;
                            currentEstimate = accountEstimate.getValue().doubleValue();
                            estimateChangeAt = null;
                            continue;
                        }
                    }
                    if (estimateChangeAt == null) {
                        estimateChangeAt = new DateTime(2042); // No further estimates
                    }

                }

                DateTime newEstimateDate = null;
                if (consolidateType == ConsolidateType.hourly) {
                    accountResults[i] = currentEstimate / 24.0;
                    newEstimateDate = currentEstimateDate.plusHours(1);
                    nextIndex = true;
                } else if (consolidateType == ConsolidateType.daily) {
                    accountResults[i] = currentEstimate;
                    newEstimateDate = currentEstimateDate.plusDays(1);
                    nextIndex = true;
                } else if (consolidateType == ConsolidateType.weekly) {
                    accountResults[i] += currentEstimate;
                    newEstimateDate = currentEstimateDate.plusDays(1);
                    if (newEstimateDate.getDayOfWeek() == 1)
                        nextIndex = true;
                } else if (consolidateType == ConsolidateType.monthly) {
                    accountResults[i] += currentEstimate;
                    newEstimateDate = currentEstimateDate.plusDays(1);
                    if (newEstimateDate.getDayOfMonth() == 1)
                        nextIndex = true;
                }

                if (newEstimateDate.isBefore(currentEstimateDate)) {
                    newEstimateDate = newEstimateDate.plusYears(1);
                }

                currentEstimateDate = newEstimateDate;
            }
        }

        return accountResults;
    }

    private void addData(double[] from, double[] to) {
        for (int i = 0; i < from.length; i++)
            to[i] += from[i];
    }

    /**
     * Get Estimates for given interval
     */
    public Map<Tag, double[]> getData(Interval interval, TagLists tagLists, TagType groupBy, AggregateType aggregate, boolean forReservation) {

        DateTime start = config.startDate;
        DateTime end = config.startDate;
        Map<Tag, double[]> results = new HashMap<Tag, double[]>();

        double[] aggregated = null;
        for (Account account : tagLists.accounts) {
            logger.info("Get Estimates for " + account.name);
            double[] accountResults = getEstimatesData(interval, account, groupBy, aggregate, forReservation);
            if (aggregated == null)
                aggregated = new double[accountResults.length];
            addData(accountResults, aggregated);
            if (aggregated != null)
                results.put(Tag.aggregated, aggregated);
            results.put(account, accountResults);
        }

        return results;

    }

    public int getDataLength(DateTime start) {
        return 0;
    }
}
