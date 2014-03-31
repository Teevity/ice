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
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import org.joda.time.Interval
import com.netflix.ice.tag.Tag
import com.netflix.ice.login.*;
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.collect.Maps
import org.json.JSONObject


class LoginController {
    private static LoginConfig config = LoginConfig.getInstance();

    private static LoginConfig getConfig() {
        if (config == null) {
            config = LoginConfig.getInstance();
        }
        return config;
    }

    def handler = {
        if (config.loginEnable == false) {
            redirect(controller: "dashboard")
        }
        LoginMethod loginMethod = config.loginMethods.get(params.login_action);
        if (loginMethod == null) {
             redirect(action: "error");
        }
        LoginResponse loginResponse = loginMethod.processLogin(params);

        if (loginResponse.loginSuccess) {
             session.authenticated = true;
             redirect(controller: "dashboard");
        } else if (loginResponse.loginFailed) {
             redirect(action: "failure");
        } else if (loginResponse.renderFile) {
             System.out.println("Render " + loginResponse.renderFile + " - " + loginResponse.contentType);
             render(file: loginResponse.renderFile, contentType: loginResponse.contentType);
           
        } else {
             redirect(action: "error");
        }
    }

    def failure = {}
    def error = {}

    def index = {
        getConfig();
        if (config.loginEnable == false) {
            System.out.println("Login Disabled, Goto Dashboard")
            redirect(controller: "dashboard")
        } else {
            System.out.println("No Action, Render Default Endpoint")
            redirect(uri: "/login/handler/" + config.loginDefaultEndpoint)
        } 
    }
}
