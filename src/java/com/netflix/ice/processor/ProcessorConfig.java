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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.netflix.ice.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProcessorConfig extends Config {
    private static final Logger logger = LoggerFactory.getLogger(ProcessorConfig.class);
    private static ProcessorConfig instance;
    private static ReservationCapacityPoller reservationCapacityPoller;
    private static BillingFileProcessor billingFileProcessor;

    public final ReservationService reservationService;
    public final LineItemProcessor lineItemProcessor;
    public final Randomizer randomizer;
    public final double costPerMonitorMetricPerHour;

    /**
     *
     * @param properties (required)
     * @param accountService (required)
     * @param productService (required)
     * @param reservationService (required)
     * @param resourceService (optional)
     * @param lineItemProcessor (required)
     * @param randomizer (optional)
     */
    public ProcessorConfig(
            Properties properties,
            AWSCredentialsProvider credentialsProvider,
            AccountService accountService,
            ProductService productService,
            ReservationService reservationService,
            ResourceService resourceService,
            LineItemProcessor lineItemProcessor,
            Randomizer randomizer) {

        super(properties, credentialsProvider, accountService, productService, resourceService);

        if (reservationService == null) throw new IllegalArgumentException("reservationService must be specified");
        if (lineItemProcessor == null) throw new IllegalArgumentException("lineItemProcessor must be specified");

        this.reservationService = reservationService;
        this.lineItemProcessor = lineItemProcessor;
        this.randomizer = randomizer;

        if (properties.getProperty(IceOptions.COST_PER_MONITORMETRIC_PER_HOUR) != null)
            this.costPerMonitorMetricPerHour = Double.parseDouble(properties.getProperty(IceOptions.COST_PER_MONITORMETRIC_PER_HOUR));
        else
            this.costPerMonitorMetricPerHour = 0;

        ProcessorConfig.instance = this;

        reservationService.init();
        if (resourceService != null)
            resourceService.init();

        billingFileProcessor = new BillingFileProcessor(
            properties.getProperty(IceOptions.URL_PREFIX),
            properties.getProperty(IceOptions.ONDEMAND_COST_ALERT_THRESHOLD) == null ? null :  Double.parseDouble(properties.getProperty(IceOptions.ONDEMAND_COST_ALERT_THRESHOLD)),
            properties.getProperty(IceOptions.FROM_EMAIL),
            properties.getProperty(IceOptions.ONDEMAND_COST_ALERT_EMAILS));
    }

    public void start (ReservationCapacityPoller reservationCapacityPoller) {
        logger.info("starting up...");

        ProcessorConfig.reservationCapacityPoller = reservationCapacityPoller;
        if (reservationCapacityPoller != null)
            reservationCapacityPoller.start();

        while (reservationCapacityPoller != null && !reservationCapacityPoller.updatedConfig()) {
            try {
                Thread.sleep(10000L);
            }
            catch (InterruptedException e) {
            }
        }

        billingFileProcessor.start(300);
    }

    public void shutdown() {
        logger.info("Shutting down...");

        billingFileProcessor.shutdown();
        reservationCapacityPoller.shutdown();
    }

    /**
     *
     * @return singlton instance
     */
    public static ProcessorConfig getInstance() {
        return instance;
    }
}
