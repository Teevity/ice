/*
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
package com.netflix.ice.login;

import com.netflix.ice.common.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Boolean;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.lang.Class;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.ClassNotFoundException;
import java.lang.NoSuchMethodException;

/**
 * Configuration class for Login Features.
 */
public class LoginConfig implements BaseConfig {
    private static LoginConfig instance;
    private static final Logger logger = LoggerFactory.getLogger(LoginConfig.class);

    public final String loginClasses;
    public final String noAccessMessage;
    public final String loginLogFile;
    public final String loginEndpoints;
    public boolean loginEnable = false;
    public final String loginDefaultEndpoint;
    public final Map<String, LoginMethod> loginMethods = new HashMap<String, LoginMethod>();

    /**
     * @param properties (required)
     */
    public LoginConfig(Properties properties) {
        loginEnable = Boolean.parseBoolean(properties.getProperty(IceOptions.LOGIN_ENABLE));
        loginLogFile = properties.getProperty(LoginOptions.LOGIN_LOG);
        loginClasses = properties.getProperty(LoginOptions.LOGIN_CLASSES);
        loginEndpoints = properties.getProperty(LoginOptions.LOGIN_ENDPOINTS);
        loginDefaultEndpoint = properties.getProperty(LoginOptions.LOGIN_DEFAULT);
        noAccessMessage = properties.getProperty(LoginOptions.NO_ACCESS_MESSAGE);

        loadLoginPlugins(loginEndpoints, loginClasses, properties);
        LoginConfig.instance = this;
    }

    /**
    * Load Plugins based on config.
    */
    private void loadLoginPlugins(String endpoints, String classes, Properties properties) {
        String[] endpoints_arr = endpoints.split(",");
        String[] classes_arr = classes.split(",");
        try {
            for(int i=0;i<endpoints_arr.length;i++) {
                String endpoint = endpoints_arr[i];
                String className = classes_arr[i];
                logger.info("Loading " + endpoint + " using class " + className);
                Class loginMethodClass = Class.forName(className);
                Constructor ctor = loginMethodClass.getDeclaredConstructor(Properties.class);
                ctor.setAccessible(true);
                LoginMethod loginObject = (LoginMethod)ctor.newInstance(properties);
                loginMethods.put(endpoint, loginObject);
            }
        } catch(ClassNotFoundException x) {
            logger.error(x.toString(), x);
        } catch (InstantiationException x) {
            logger.error(x.toString(), x);
        } catch (IllegalAccessException x) {
            logger.error(x.toString(), x);
        } catch (InvocationTargetException x) {
            logger.error(x.toString(), x);
        } catch (NoSuchMethodException x) {
            logger.error(x.toString(), x);
        }
    }

    /**
     *
     * @return singlton instance
     */
    public static LoginConfig getInstance() {
        return instance;
    }
}
