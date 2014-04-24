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

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;

import com.netflix.ice.common.IceOptions;
import com.netflix.ice.common.IceSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Base Class for Classes that facilitate login.
*/
public abstract class LoginMethod {
    private final Properties properties;
    Logger logger = LoggerFactory.getLogger(getClass());

    public LoginMethod(Properties properties) throws LoginMethodException { 
         this.properties = properties;
    }
    public abstract LoginResponse processLogin(HttpServletRequest request) throws LoginMethodException;

    public String propertyPrefix(String name) {
       return IceOptions.LOGIN_PREFIX + "." + name;
    }

    public abstract String propertyName(String name);

    public String propertyValue(String name) {
        return (String)properties.get(propertyName(name));
    }
   
    /**
     * Whitelist the session to see all accounts.
     */ 
    public void whitelistAllAccounts(IceSession session) {
        session.allowAllAccounts();         
    }

    /**
     * Whitelist the session for a specific account.
     */ 
    public boolean whitelistAccount(IceSession session, String name) {
        session.allowAccount(name);         
        return true;
    }
}
