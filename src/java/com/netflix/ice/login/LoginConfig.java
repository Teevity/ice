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
 * COnfiguration class for UI login.
 */
public class LoginConfig implements BaseConfig {
    private static LoginConfig instance;
    private static final Logger logger = LoggerFactory.getLogger(LoginConfig.class);

    public final String loginClasses;
    public final String loginEndpoints;
    public boolean loginEnable = false;
    public final String loginDefaultEndpoint;
    public final Map<String, LoginMethod> loginMethods = new HashMap<String, LoginMethod>();

    /**
     *
     * @param properties (required)
     */
    public LoginConfig(Properties properties) {
        System.out.println("Construct LoginConfig");
        System.out.println(properties.toString());
        loginEnable = Boolean.parseBoolean(properties.getProperty(IceOptions.LOGIN_ENABLE));
        loginClasses = properties.getProperty(IceOptions.LOGIN_CLASSES);
        loginEndpoints = properties.getProperty(IceOptions.LOGIN_ENDPOINTS);
        loginDefaultEndpoint = properties.getProperty(IceOptions.LOGIN_DEFAULT);

        System.out.println("Constructed LoginConfig");
        loadLoginPlugins(loginEndpoints, loginClasses, properties);
        LoginConfig.instance = this;
    }

    private void loadLoginPlugins(String endpoints, String classes, Properties properties) {
        String[] endpoints_arr = endpoints.split(",");
        String[] classes_arr = classes.split(",");
        try {
            for(int i=0;i<endpoints_arr.length;i++) {
                String endpoint = endpoints_arr[i];
                String className = classes_arr[i];
                System.out.println("Loading " + endpoint + " using class " + className);
                Class loginMethodClass = Class.forName(className);
                Constructor ctor = loginMethodClass.getDeclaredConstructor(Properties.class);
	        ctor.setAccessible(true);
                LoginMethod loginObject = (LoginMethod)ctor.newInstance(properties);
                loginMethods.put(endpoint, loginObject);
            }
        } catch(ClassNotFoundException x) {
	    x.printStackTrace();
        } catch (InstantiationException x) {
	    x.printStackTrace();
	} catch (IllegalAccessException x) {
	    x.printStackTrace();
        } catch (InvocationTargetException x) {
	    x.printStackTrace();
	} catch (NoSuchMethodException x) {
	    x.printStackTrace();
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
