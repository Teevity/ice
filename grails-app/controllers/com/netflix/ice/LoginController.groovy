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

import java.io.FileInputStream
import groovy.text.SimpleTemplateEngine
import grails.converters.JSON
import org.apache.commons.io.IOUtils
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import org.joda.time.Interval
import com.netflix.ice.tag.Tag
import com.netflix.ice.login.*
import com.netflix.ice.common.IceSession
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.google.common.collect.Maps
import org.json.JSONObject
import grails.util.Holders


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
            redirect(controller: "dashboard", absolute: true)
        }
        LoginMethod loginMethod = config.loginMethods.get(params.login_action);
        if (loginMethod == null) {
             redirect(action: "error");
        }
        LoginResponse loginResponse = loginMethod.processLogin(request, response);

        if (loginResponse.responded) {
            // no-op
            return null;
        } else if (loginResponse.redirectTo != null) {
            redirect(url: loginResponse.redirectTo);    
        } else if (loginResponse.loggedOut) {
            redirect(action: "logout");
        } else if (loginResponse.loginSuccess) {
            IceSession iceSession = new IceSession(session);
            iceSession.authenticate(new Boolean(true));
            if (iceSession.authenticated) { //ensure we are good
                if (iceSession.url != null) {
                    String redirectURL = "" + iceSession.url
                    redirect(url: redirectURL, absolute: true);
                } else {
                    redirect(controller: "dashboard");
                }
            } else {
                redirect(action: "failure");
            }
        } else if (loginResponse.loginFailed) {
            redirect(action: "failure");
        } else if (loginResponse.renderData) {
            render(text: loginResponse.renderData, contentType: loginResponse.contentType)
        } else if (loginResponse.templateFile) {
            // Fetch the template into memory
            FileInputStream inputStream = new FileInputStream(loginResponse.templateFile);
            String templateData = ""
            try {
                templateData = IOUtils.toString(inputStream);
            } finally {
                inputStream.close();
            }
            SimpleTemplateEngine engine = new SimpleTemplateEngine()
            String processedText = engine.createTemplate(templateData).make(loginResponse.templateBindings)
            render(text: processedText, contentType: loginResponse.contentType)
        } else {
            redirect(action: "error");
        }
    }

    /** A Login Failure, pass in the config so that we can give a configurable 
     *  message 
     */
    def failure = {
      [loginConfig: getConfig()]
    }

    /** A Login Error(code issues perhaps) */
    def error = {
      [loginConfig: getConfig()]
    }

    /** A Login Logout */
    def logout = {
      [loginConfig: getConfig()]
    }

    /**
     * Redirect Authentication request to the appropriate place.
     */
    def index = {
        getConfig();
        if (config.loginEnable == false) {
            redirect(controller: "dashboard")
        } else {
            redirect(uri: "/login/handler/" + config.loginDefaultEndpoint)
        } 
    }
}
