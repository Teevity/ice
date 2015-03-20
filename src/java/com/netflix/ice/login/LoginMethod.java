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
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.io.FileOutputStream;

import com.netflix.ice.common.IceOptions;
import com.netflix.ice.common.IceSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Use Java logging for our audit log
// Did not see a way to define the audit log at runtime with slf4j
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;

/**
* Base Class for Classes that facilitate authentication 
*/
public abstract class LoginMethod {
    private final Properties properties;
    Logger logger = LoggerFactory.getLogger(getClass());
    java.util.logging.Logger loginLogger;

    public LoginMethod(Properties properties) throws LoginMethodException { 
         this.properties = properties;
    }

    public abstract LoginResponse processLogin(HttpServletRequest request, HttpServletResponse response) throws LoginMethodException;

    public String propertyPrefix(String name) {
       return LoginOptions.LOGIN_PREFIX + "." + name;
    }

    public abstract String propertyName(String name);

    public String propertyValue(String name) {
        return (String)properties.get(propertyName(name));
    }

    public void initLogging() {
        LoginConfig lc = LoginConfig.getInstance();
        // only init if we need to.  Define here for flexibility
        if (lc.loginLogFile == null || loginLogger != null )
            return;
        try {
            loginLogger = java.util.logging.Logger.getLogger(this.getClass().getName(), null);
            loginLogger.setLevel(Level.ALL);
            // 10MB, 100 rolls, append
            FileHandler fileHandler = new FileHandler(lc.loginLogFile, 1024 * 1024 * 10, 100, true);
            fileHandler.setFormatter(new SimpleFormatter());
            loginLogger.addHandler(fileHandler);
        } catch(Exception e) {
            logger.error("Login Audit Log was defined as " + lc.loginLogFile + ", but unable to use it due to " + e.toString());
            logger.error("Login Audit Log must be functional before proceeding");
             // will not allow this to proceed
            System.exit(1);
        }
    }

    /**
     * Log a message to our audit log
     */
    public void log(IceSession session, String message) throws RuntimeException {
        initLogging();
        if (loginLogger != null) { 
            String username = session.getUsername();
            if (username == null) {
                throw new RuntimeException("Username not set on session, unable to properly log");
            } 
            loginLogger.info(this.getClass().getSimpleName() + ":" + session.getUsername() + ":" + message);
        }
    }
   
    /**
     * Whitelist the session to see all accounts.
     */ 
    public void whitelistAllAccounts(IceSession session) {
        log(session, "Allowing access to all account billing data");
        session.allowAllAccounts();         
    }

    /**
     * Whitelist the session for a specific account.
     */ 
    public boolean whitelistAccount(IceSession session, String name) {
        log(session, "Allowing access to " + name + " billing data");
        session.allowAccount(name);         
        return true;
    }
}
