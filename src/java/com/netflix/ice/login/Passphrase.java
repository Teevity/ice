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

import com.netflix.ice.common.IceOptions;
import com.netflix.ice.common.IceSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.io.File;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Simple Login Method to protect via a config Passphrase.  This is more of
* a reference implementation.
*/
public class Passphrase extends LoginMethod {
    Logger logger = LoggerFactory.getLogger(getClass());
    public final String passphrase;
    public final String PASSPHRASE_PREFIX = propertyPrefix("passphrase");
    public Passphrase(Properties properties) throws LoginMethodException {
        super(properties);
        passphrase = properties.getProperty(LoginOptions.LOGIN_PASSPHRASE);
    }

    public String propertyName(String name) {
       return PASSPHRASE_PREFIX + "." + name;
    }

    public LoginResponse processLogin(HttpServletRequest request, HttpServletResponse response) throws LoginMethodException {

        LoginResponse lr = new LoginResponse();
        String userPassphrase = (String)request.getParameter("passphrase");
        IceSession iceSession = new IceSession(request.getSession());

        if (userPassphrase == null) {
            /** embedded view simply to give a reference for how this would 
            *   be done with a self-contained, jar'd login plugin. 
            */
            URL viewUrl = this.getClass().getResource("/com/netflix/ice/login/views/passphrase.gsp");
            try {
                lr.templateFile=new File(viewUrl.toURI());
                lr.contentType="text/html";
            } catch(Exception e) {
                logger.error("Bad Resource " + viewUrl);
            }
        } else if (userPassphrase.equals(passphrase)) {
            iceSession.setUsername("Passphrase");
            whitelistAllAccounts(iceSession);
            // allow user
            lr.loginSuccess=true;
        } else {
            lr.loginFailed=true;
        }
        return lr;
    }
} 

