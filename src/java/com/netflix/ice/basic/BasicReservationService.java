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
import com.netflix.ice.common.TagGroup;
import com.netflix.ice.processor.Ec2InstanceReservationPrice;
import com.netflix.ice.processor.ProcessorConfig;
import com.netflix.ice.processor.ReservationService;
import com.netflix.ice.tag.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BasicReservationService implements ReservationService {
    protected Logger logger = LoggerFactory.getLogger(BasicReservationService.class);
    protected ProcessorConfig config;
    protected Map<Ec2InstanceReservationPrice.Key, Ec2InstanceReservationPrice> ec2InstanceReservationPrices = Maps.newTreeMap();
    protected Map<TagGroup, List<Reservation>> reservations = Maps.newHashMap();
    protected Ec2InstanceReservationPrice.ReservationPeriod reservationPeriod;
    private Ec2InstanceReservationPrice.ReservationUtilization reservationUtilization;

    public BasicReservationService(Ec2InstanceReservationPrice.ReservationPeriod reservationPeriod, Ec2InstanceReservationPrice.ReservationUtilization reservationUtilization) {
        this.reservationPeriod = reservationPeriod;
        this.reservationUtilization = reservationUtilization;
        setPrices();
    }

    public void init() {
        this.config = ProcessorConfig.getInstance();
    }

    private void setPrices() {
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$61 	","	$0.034 per Hour	","	$96 	","	$0.027 per Hour	","	m1.small	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$122 	","	$0.068 per Hour	","	$192 	","	$0.054 per Hour	","	m1.medium	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$243 	","	$0.136 per Hour	","	$384 	","	$0.108 per Hour	","	m1.large	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$486 	","	$0.271 per Hour	","	$768 	","	$0.215 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$517 	","	$0.299 per Hour	","	$807 	","	$0.236 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$1,034 	","	$0.598 per Hour	","	$1,614 	","	$0.472 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$23 	","	$0.012 per Hour	","	$35 	","	$0.012 per Hour	","	t1.micro	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$272 	","	$0.169 per Hour	","	$398 	","	$0.136 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$544 	","	$0.338 per Hour	","	$796 	","	$0.272 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$1,088 	","	$0.676 per Hour	","	$1,592 	","	$0.544 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$161 	","	$0.09 per Hour	","	$243 	","	$0.079 per Hour	","	c1.medium	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$644 	","	$0.36 per Hour	","	$972 	","	$0.316 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$1,762 	","	$0.904 per Hour	","	$2,710 	","	$0.904 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$2,474 	","	$1.54 per Hour	","	$3,846 	","	$1.225 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$2,576 	","	$1.477 per Hour	","	$3,884 	","	$1.15 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-east-1	","	$3,968 	","	$2.24 per Hour	","	$5,997 	","	$1.81 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$61 	","	$0.042 per Hour	","	$96 	","	$0.034 per Hour	","	m1.small	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$122 	","	$0.085 per Hour	","	$192 	","	$0.067 per Hour	","	m1.medium	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$243 	","	$0.17 per Hour	","	$384 	","	$0.135 per Hour	","	m1.large	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$486 	","	$0.339 per Hour	","	$768 	","	$0.269 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$517 	","	$0.376 per Hour	","	$807 	","	$0.297 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$1,034 	","	$0.752 per Hour	","	$1,614 	","	$0.594 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$23 	","	$0.015 per Hour	","	$35 	","	$0.015 per Hour	","	t1.micro	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$272 	","	$0.206 per Hour	","	$398 	","	$0.166 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$544 	","	$0.412 per Hour	","	$796 	","	$0.332 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$1,088 	","	$0.824 per Hour	","	$1,592 	","	$0.664 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$161 	","	$0.113 per Hour	","	$243 	","	$0.099 per Hour	","	c1.medium	");
        setPrice("	linux	","	LIGHT	","	us-west-1	","	$644 	","	$0.452 per Hour	","	$972 	","	$0.396 per Hour	","	c1.xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$61 	","	$0.034 per Hour	","	$96 	","	$0.027 per Hour	","	m1.small	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$122 	","	$0.068 per Hour	","	$192 	","	$0.054 per Hour	","	m1.medium	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$243 	","	$0.136 per Hour	","	$384 	","	$0.108 per Hour	","	m1.large	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$486 	","	$0.271 per Hour	","	$768 	","	$0.215 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$517 	","	$0.299 per Hour	","	$807 	","	$0.236 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$1,034 	","	$0.598 per Hour	","	$1,614 	","	$0.472 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$23 	","	$0.012 per Hour	","	$35 	","	$0.012 per Hour	","	t1.micro	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$272 	","	$0.169 per Hour	","	$398 	","	$0.136 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$544 	","	$0.338 per Hour	","	$796 	","	$0.272 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$1,088 	","	$0.676 per Hour	","	$1,592 	","	$0.544 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$161 	","	$0.09 per Hour	","	$243 	","	$0.079 per Hour	","	c1.medium	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$644 	","	$0.36 per Hour	","	$972 	","	$0.316 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$1,762 	","	$0.904 per Hour	","	$2,710 	","	$0.904 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$2,474 	","	$1.54 per Hour	","	$3,846 	","	$1.225 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$2,576 	","	$1.477 per Hour	","	$3,884 	","	$1.15 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	us-west-2	","	$3,968 	","	$2.24 per Hour	","	$5,997 	","	$1.81 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$61 	","	$0.042 per Hour	","	$96 	","	$0.034 per Hour	","	m1.small	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$122 	","	$0.085 per Hour	","	$192 	","	$0.067 per Hour	","	m1.medium	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$243 	","	$0.17 per Hour	","	$384 	","	$0.134 per Hour	","	m1.large	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$486 	","	$0.339 per Hour	","	$768 	","	$0.268 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$517 	","	$0.374 per Hour	","	$807 	","	$0.296 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$1,034 	","	$0.748 per Hour	","	$1,614 	","	$0.592 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$23 	","	$0.015 per Hour	","	$35 	","	$0.015 per Hour	","	t1.micro	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$272 	","	$0.206 per Hour	","	$398 	","	$0.166 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$544 	","	$0.412 per Hour	","	$796 	","	$0.332 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$1,088 	","	$0.824 per Hour	","	$1,592 	","	$0.664 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$155 	","	$0.113 per Hour	","	$243 	","	$0.099 per Hour	","	c1.medium	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$620 	","	$0.452 per Hour	","	$972 	","	$0.396 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$1,762 	","	$1.18 per Hour	","	$2,710 	","	$1.18 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$2,474 	","	$1.988 per Hour	","	$3,846 	","	$1.536 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$2,576 	","	$2.605 per Hour	","	$3,884 	","	$2.035 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	LIGHT	","	eu-west-1	","	$3,968 	","	$3.09 per Hour	","	$5,997 	","	$2.396 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$69 	","	$0.059 per Hour	","	$106.30 	","	$0.051 per Hour	","	m1.small	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$138 	","	$0.118 per Hour	","	$212.50 	","	$0.103 per Hour	","	m1.medium	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$276 	","	$0.235 per Hour	","	$425.20 	","	$0.204 per Hour	","	m1.large	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$552 	","	$0.47 per Hour	","	$850.40 	","	$0.408 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$607 	","	$0.504 per Hour	","	$935 	","	$0.432 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$1,214 	","	$1.008 per Hour	","	$1,870 	","	$0.864 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$23 	","	$0.014 per Hour	","	$35 	","	$0.012 per Hour	","	t1.micro	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$353 	","	$0.29 per Hour	","	$548 	","	$0.245 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$706 	","	$0.58 per Hour	","	$1,096 	","	$0.49 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$1,412 	","	$1.16 per Hour	","	$2,192 	","	$0.98 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$178 	","	$0.165 per Hour	","	$273 	","	$0.153 per Hour	","	c1.medium	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$712 	","	$0.66 per Hour	","	$1,092 	","	$0.612 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$1,762 	","	$1.114 per Hour	","	$2,710 	","	$1.114 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$2,474 	","	$1.871 per Hour	","	$3,846 	","	$1.555 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$2,576 	","	$1.957 per Hour	","	$3,884 	","	$1.63 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-east-1	","	$3,968 	","	$2.571 per Hour	","	$5,997 	","	$2.141 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$69 	","	$0.069 per Hour	","	$106.30 	","	$0.059 per Hour	","	m1.small	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$138 	","	$0.138 per Hour	","	$212.50 	","	$0.118 per Hour	","	m1.medium	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$276 	","	$0.275 per Hour	","	$425.20 	","	$0.236 per Hour	","	m1.large	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$552 	","	$0.55 per Hour	","	$850.40 	","	$0.472 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$607 	","	$0.592 per Hour	","	$935 	","	$0.502 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$1,214 	","	$1.183 per Hour	","	$1,870 	","	$1.005 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$23 	","	$0.021 per Hour	","	$35 	","	$0.021 per Hour	","	t1.micro	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$353 	","	$0.368 per Hour	","	$548 	","	$0.31 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$706 	","	$0.735 per Hour	","	$1,096 	","	$0.62 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$1,412 	","	$1.47 per Hour	","	$2,192 	","	$1.24 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$178 	","	$0.19 per Hour	","	$273 	","	$0.175 per Hour	","	c1.medium	");
        setPrice("	windows	","	LIGHT	","	us-west-1	","	$712 	","	$0.76 per Hour	","	$1,092 	","	$0.70 per Hour	","	c1.xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$69 	","	$0.059 per Hour	","	$106.30 	","	$0.051 per Hour	","	m1.small	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$138 	","	$0.118 per Hour	","	$212.50 	","	$0.103 per Hour	","	m1.medium	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$276 	","	$0.235 per Hour	","	$425.20 	","	$0.204 per Hour	","	m1.large	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$552 	","	$0.47 per Hour	","	$850.40 	","	$0.408 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$607 	","	$0.504 per Hour	","	$935 	","	$0.432 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$1,214 	","	$1.007 per Hour	","	$1,870 	","	$0.865 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$23 	","	$0.014 per Hour	","	$35 	","	$0.012 per Hour	","	t1.micro	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$353 	","	$0.29 per Hour	","	$548 	","	$0.245 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$706 	","	$0.58 per Hour	","	$1,096 	","	$0.49 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$1,412 	","	$1.16 per Hour	","	$2,192 	","	$0.98 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$178 	","	$0.165 per Hour	","	$273 	","	$0.153 per Hour	","	c1.medium	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$712 	","	$0.66 per Hour	","	$1,092 	","	$0.612 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$1,762 	","	$1.114 per Hour	","	$2,710 	","	$1.114 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$2,474 	","	$1.871 per Hour	","	$3,846 	","	$1.555 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$2,576 	","	$1.957 per Hour	","	$3,884 	","	$1.63 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	us-west-2	","	$3,968 	","	$2.571 per Hour	","	$5,997 	","	$2.141 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$69 	","	$0.069 per Hour	","	$106.30 	","	$0.059 per Hour	","	m1.small	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$138 	","	$0.138 per Hour	","	$212.50 	","	$0.118 per Hour	","	m1.medium	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$276 	","	$0.275 per Hour	","	$425.20 	","	$0.236 per Hour	","	m1.large	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$552 	","	$0.55 per Hour	","	$850.40 	","	$0.472 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$607 	","	$0.592 per Hour	","	$935 	","	$0.502 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$1,214 	","	$1.183 per Hour	","	$1,870 	","	$1.005 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$23 	","	$0.016 per Hour	","	$35 	","	$0.016 per Hour	","	t1.micro	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$353 	","	$0.368 per Hour	","	$548 	","	$0.31 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$706 	","	$0.735 per Hour	","	$1,096 	","	$0.62 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$1,412 	","	$1.47 per Hour	","	$2,192 	","	$1.24 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$178 	","	$0.19 per Hour	","	$273 	","	$0.175 per Hour	","	c1.medium	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$712 	","	$0.76 per Hour	","	$1,092 	","	$0.70 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$1,762 	","	$1.443 per Hour	","	$2,710 	","	$1.418 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$2,474 	","	$2.409 per Hour	","	$3,846 	","	$1.98 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$2,576 	","	$2.52 per Hour	","	$3,884 	","	$2.074 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	LIGHT	","	eu-west-1	","	$3,968 	","	$3.327 per Hour	","	$5,997 	","	$2.733 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$169 	","	$0.014 per Hour	","	$257 	","	$0.012 per Hour	","	m1.small	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$338 	","	$0.028 per Hour	","	$514 	","	$0.023 per Hour	","	m1.medium	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$676 	","	$0.056 per Hour	","	$1,028 	","	$0.046 per Hour	","	m1.large	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$1,352 	","	$0.112 per Hour	","	$2,056 	","	$0.092 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$1,489 	","	$0.123 per Hour	","	$2,261 	","	$0.101 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$2,978 	","	$0.246 per Hour	","	$4,522 	","	$0.202 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$62 	","	$0.005 per Hour	","	$100 	","	$0.005 per Hour	","	t1.micro	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$789 	","	$0.068 per Hour	","	$1,198 	","	$0.053 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$1,578 	","	$0.136 per Hour	","	$2,396 	","	$0.106 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$3,156 	","	$0.272 per Hour	","	$4,792 	","	$0.212 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$450 	","	$0.036 per Hour	","	$701 	","	$0.031 per Hour	","	c1.medium	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$1,800 	","	$0.144 per Hour	","	$2,804 	","	$0.124 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$5,000 	","	$0.361 per Hour	","	$7,670 	","	$0.361 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$7,220 	","	$0.62 per Hour	","	$10,880 	","	$0.49 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$7,280 	","	$0.621 per Hour	","	$10,960 	","	$0.482 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-east-1	","	$11,213 	","	$0.92 per Hour	","	$16,924 	","	$0.76 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$169 	","	$0.022 per Hour	","	$257 	","	$0.018 per Hour	","	m1.small	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$338 	","	$0.044 per Hour	","	$514 	","	$0.035 per Hour	","	m1.medium	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$676 	","	$0.087 per Hour	","	$1,028 	","	$0.07 per Hour	","	m1.large	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$1,352 	","	$0.174 per Hour	","	$2,056 	","	$0.14 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$1,489 	","	$0.191 per Hour	","	$2,261 	","	$0.154 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$2,978 	","	$0.382 per Hour	","	$4,522 	","	$0.308 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$62 	","	$0.008 per Hour	","	$100 	","	$0.008 per Hour	","	t1.micro	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$789 	","	$0.102 per Hour	","	$1,198 	","	$0.082 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$1,578 	","	$0.204 per Hour	","	$2,396 	","	$0.164 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$3,156 	","	$0.408 per Hour	","	$4,792 	","	$0.328 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$450 	","	$0.057 per Hour	","	$701 	","	$0.049 per Hour	","	c1.medium	");
        setPrice("	linux	","	HEAVY	","	us-west-1	","	$1,800 	","	$0.228 per Hour	","	$2,804 	","	$0.196 per Hour	","	c1.xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$169 	","	$0.014 per Hour	","	$257 	","	$0.012 per Hour	","	m1.small	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$338 	","	$0.028 per Hour	","	$514 	","	$0.023 per Hour	","	m1.medium	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$676 	","	$0.056 per Hour	","	$1,028 	","	$0.046 per Hour	","	m1.large	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$1,352 	","	$0.112 per Hour	","	$2,056 	","	$0.092 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$1,489 	","	$0.123 per Hour	","	$2,261 	","	$0.101 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$2,978 	","	$0.246 per Hour	","	$4,522 	","	$0.202 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$62 	","	$0.005 per Hour	","	$100 	","	$0.005 per Hour	","	t1.micro	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$789 	","	$0.068 per Hour	","	$1,198 	","	$0.053 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$1,578 	","	$0.136 per Hour	","	$2,396 	","	$0.106 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$3,156 	","	$0.272 per Hour	","	$4,792 	","	$0.212 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$450 	","	$0.036 per Hour	","	$701 	","	$0.031 per Hour	","	c1.medium	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$1,800 	","	$0.144 per Hour	","	$2,804 	","	$0.124 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$5,000 	","	$0.361 per Hour	","	$7,670 	","	$0.361 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$7,220 	","	$0.62 per Hour	","	$10,880 	","	$0.49 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$7,280 	","	$0.621 per Hour	","	$10,960 	","	$0.482 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	us-west-2	","	$11,213 	","	$0.92 per Hour	","	$16,924 	","	$0.76 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$169 	","	$0.022 per Hour	","	$257 	","	$0.018 per Hour	","	m1.small	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$338 	","	$0.044 per Hour	","	$514 	","	$0.035 per Hour	","	m1.medium	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$676 	","	$0.087 per Hour	","	$1,028 	","	$0.07 per Hour	","	m1.large	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$1,352 	","	$0.174 per Hour	","	$2,056 	","	$0.14 per Hour	","	m1.xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$1,489 	","	$0.189 per Hour	","	$2,261 	","	$0.153 per Hour	","	m3.xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$2,978 	","	$0.378 per Hour	","	$4,522 	","	$0.306 per Hour	","	m3.2xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$62 	","	$0.008 per Hour	","	$100 	","	$0.008 per Hour	","	t1.micro	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$789 	","	$0.102 per Hour	","	$1,198 	","	$0.083 per Hour	","	m2.xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$1,578 	","	$0.204 per Hour	","	$2,396 	","	$0.166 per Hour	","	m2.2xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$3,156 	","	$0.408 per Hour	","	$4,792 	","	$0.332 per Hour	","	m2.4xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$450 	","	$0.057 per Hour	","	$701 	","	$0.049 per Hour	","	c1.medium	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$1,800 	","	$0.228 per Hour	","	$2,804 	","	$0.196 per Hour	","	c1.xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$5,000 	","	$0.61 per Hour	","	$7,670 	","	$0.61 per Hour	","	cc2.8xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$7,220 	","	$0.742 per Hour	","	$10,880 	","	$0.746 per Hour	","	cr1.8xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$7,280 	","	$0.931 per Hour	","	$10,960 	","	$0.742 per Hour	","	hi1.4xlarge	");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	linux	","	HEAVY	","	eu-west-1	","	$11,213 	","	$1.163 per Hour	","	$16,924 	","	$1.166 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$195 	","	$0.036 per Hour	","	$300 	","	$0.033 per Hour	","	m1.small	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$390 	","	$0.073 per Hour	","	$600 	","	$0.066 per Hour	","	m1.medium	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$780 	","	$0.145 per Hour	","	$1,200 	","	$0.132 per Hour	","	m1.large	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$1,560 	","	$0.29 per Hour	","	$2,400 	","	$0.264 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$1,716 	","	$0.301 per Hour	","	$2,640 	","	$0.275 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$3,432 	","	$0.602 per Hour	","	$5,280 	","	$0.55 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$62 	","	$0.006 per Hour	","	$100 	","	$0.007 per Hour	","	t1.micro	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$1,030 	","	$0.158 per Hour	","	$1,550 	","	$0.14 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$2,060 	","	$0.315 per Hour	","	$3,100 	","	$0.28 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$4,120 	","	$0.63 per Hour	","	$6,200 	","	$0.56 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$500 	","	$0.105 per Hour	","	$775 	","	$0.10 per Hour	","	c1.medium	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$2,000 	","	$0.42 per Hour	","	$3,100 	","	$0.40 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$5,000 	","	$0.571 per Hour	","	$7,670 	","	$0.571 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$7,220 	","	$0.951 per Hour	","	$10,880 	","	$0.821 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$7,280 	","	$1.101 per Hour	","	$10,960 	","	$0.962 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-east-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-east-1	","	$11,213 	","	$1.251 per Hour	","	$16,924 	","	$1.091 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$195 	","	$0.045 per Hour	","	$300 	","	$0.04 per Hour	","	m1.small	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$390 	","	$0.09 per Hour	","	$600 	","	$0.08 per Hour	","	m1.medium	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$780 	","	$0.18 per Hour	","	$1,200 	","	$0.16 per Hour	","	m1.large	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$1,560 	","	$0.36 per Hour	","	$2,400 	","	$0.32 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$1,716 	","	$0.38 per Hour	","	$2,640 	","	$0.336 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$3,432 	","	$0.76 per Hour	","	$5,280 	","	$0.671 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$62 	","	$0.014 per Hour	","	$100 	","	$0.014 per Hour	","	t1.micro	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$1,030 	","	$0.228 per Hour	","	$1,550 	","	$0.198 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$2,060 	","	$0.455 per Hour	","	$3,100 	","	$0.396 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$4,120 	","	$0.91 per Hour	","	$6,200 	","	$0.792 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$500 	","	$0.128 per Hour	","	$775 	","	$0.12 per Hour	","	c1.medium	");
        setPrice("	windows	","	HEAVY	","	us-west-1	","	$2,000 	","	$0.51 per Hour	","	$3,100 	","	$0.48 per Hour	","	c1.xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$195 	","	$0.036 per Hour	","	$300 	","	$0.033 per Hour	","	m1.small	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$390 	","	$0.073 per Hour	","	$600 	","	$0.066 per Hour	","	m1.medium	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$780 	","	$0.145 per Hour	","	$1,200 	","	$0.132 per Hour	","	m1.large	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$1,560 	","	$0.29 per Hour	","	$2,400 	","	$0.264 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$1,716 	","	$0.301 per Hour	","	$2,640 	","	$0.275 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$3,432 	","	$0.602 per Hour	","	$5,280 	","	$0.549 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$62 	","	$0.006 per Hour	","	$100 	","	$0.007 per Hour	","	t1.micro	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$1,030 	","	$0.158 per Hour	","	$1,550 	","	$0.14 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$2,060 	","	$0.315 per Hour	","	$3,100 	","	$0.28 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$4,120 	","	$0.63 per Hour	","	$6,200 	","	$0.56 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$500 	","	$0.105 per Hour	","	$775 	","	$0.10 per Hour	","	c1.medium	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$2,000 	","	$0.42 per Hour	","	$3,100 	","	$0.40 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$5,000 	","	$0.571 per Hour	","	$7,670 	","	$0.571 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$7,220 	","	$0.951 per Hour	","	$10,880 	","	$0.821 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$7,280 	","	$1.101 per Hour	","	$10,960 	","	$0.962 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	us-west-2	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	us-west-2	","	$11,213 	","	$1.251 per Hour	","	$16,924 	","	$1.091 per Hour	","	hs1.8xlarge	");
        setPrice("		","		","		","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$195 	","	$0.045 per Hour	","	$300 	","	$0.04 per Hour	","	m1.small	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$390 	","	$0.09 per Hour	","	$600 	","	$0.08 per Hour	","	m1.medium	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$780 	","	$0.18 per Hour	","	$1,200 	","	$0.16 per Hour	","	m1.large	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$1,560 	","	$0.36 per Hour	","	$2,400 	","	$0.32 per Hour	","	m1.xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$1,716 	","	$0.38 per Hour	","	$2,640 	","	$0.336 per Hour	","	m3.xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$3,432 	","	$0.76 per Hour	","	$5,280 	","	$0.671 per Hour	","	m3.2xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$62 	","	$0.009 per Hour	","	$100 	","	$0.009 per Hour	","	t1.micro	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$1,030 	","	$0.228 per Hour	","	$1,550 	","	$0.198 per Hour	","	m2.xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$2,060 	","	$0.455 per Hour	","	$3,100 	","	$0.396 per Hour	","	m2.2xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$4,120 	","	$0.91 per Hour	","	$6,200 	","	$0.792 per Hour	","	m2.4xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$500 	","	$0.128 per Hour	","	$775 	","	$0.12 per Hour	","	c1.medium	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$2,000 	","	$0.51 per Hour	","	$3,100 	","	$0.48 per Hour	","	c1.xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cc1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$5,000 	","	$0.855 per Hour	","	$7,670 	","	$0.785 per Hour	","	cc2.8xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$7,220 	","	$1.395 per Hour	","	$10,880 	","	$1.13 per Hour	","	cr1.8xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	N/A	","	N/A	","	N/A	","	N/A	","	cg1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$7,280 	","	$1.585 per Hour	","	$10,960 	","	$1.307 per Hour	","	hi1.4xlarge	");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","		","		","		","		","		");
        setPrice("	windows	","	HEAVY	","	eu-west-1	","	$11,213 	","	$1.883 per Hour	","	$16,924 	","	$1.524 per Hour	","	hs1.8xlarge	");
    }

    private void setPrice(String os, String utilization, String regionStr, String oneyearUpfrontStr, String oneyearHourlyStr, String threeyearUpfrontStr, String threeyearHourlyStr, String instanceType) {
        oneyearUpfrontStr = oneyearUpfrontStr.trim();
        if (oneyearUpfrontStr.isEmpty() || oneyearUpfrontStr.equals("N/A"))
            return;

        Ec2InstanceReservationPrice.ReservationUtilization reservationUtilization = Ec2InstanceReservationPrice.ReservationUtilization.valueOf(utilization.trim());
        if (this.reservationUtilization  != reservationUtilization)
            return;

        InstanceOs instanceOs = InstanceOs.valueOf(os.trim());
        Region region = Region.getRegionByName(regionStr.trim());

        oneyearUpfrontStr = oneyearUpfrontStr.substring(1).replace(",", "");
        oneyearHourlyStr = oneyearHourlyStr.substring(1).replace(",", "");
        if (oneyearHourlyStr.indexOf(" ") > 0)
            oneyearHourlyStr = oneyearHourlyStr.substring(0, oneyearHourlyStr.indexOf(" "));
        threeyearUpfrontStr = threeyearUpfrontStr.trim().substring(1).replace(",", "");
        threeyearHourlyStr = threeyearHourlyStr.trim().substring(1).replace(",", "");
        if (threeyearHourlyStr.indexOf(" ") > 0)
            threeyearHourlyStr = threeyearHourlyStr.substring(0, threeyearHourlyStr.indexOf(" "));

        UsageType usageType = UsageType.getUsageType(instanceType.trim() + (instanceOs == InstanceOs.windows ? "." + instanceOs  : ""), "hours");

        Ec2InstanceReservationPrice.Key key = new Ec2InstanceReservationPrice.Key(region, usageType);
        Ec2InstanceReservationPrice price = new Ec2InstanceReservationPrice();
        ec2InstanceReservationPrices.put(key, price);

        if (this.reservationPeriod == Ec2InstanceReservationPrice.ReservationPeriod.oneyear) {
            price.upfrontPrice.getCreatePrice(0L).setListPrice(Double.parseDouble(oneyearUpfrontStr));
            price.hourlyPrice.getCreatePrice(0L).setListPrice(Double.parseDouble(oneyearHourlyStr));
        }
        else {
            price.upfrontPrice.getCreatePrice(0L).setListPrice(Double.parseDouble(threeyearUpfrontStr));
            price.hourlyPrice.getCreatePrice(0L).setListPrice(Double.parseDouble(threeyearHourlyStr));
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
