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

import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.io.File;
import java.net.URL;

/**
 * Simple Login Method to protect via a config Passphrase.
 */
public class Passphrase extends LoginMethod {
    public final String passphrase;
    public Passphrase(Properties properties) {
        super(properties);
        passphrase = properties.getProperty(IceOptions.LOGIN_PASSPHRASE);
    }

    public LoginResponse processLogin(Map params) {
        LoginResponse lr = new LoginResponse();
        String user_passphrase = (String)params.get("passphrase");
        System.out.println(user_passphrase + " does it equal " + passphrase);
        if (user_passphrase == null) {
            URL viewUrl = this.getClass().getResource("/com/netflix/ice/login/views/passphrase.gsp");
            try {
                lr.renderFile=new File(viewUrl.toURI());
                lr.contentType="text/html";
            } catch(Exception e) {
                System.out.println("Bad Resource " + viewUrl);
            }
        }
        else if (user_passphrase.equals(passphrase)) {
            lr.loginSuccess=true;
        } else {
            lr.loginFailed=true;
        }
        return lr;
    }
} 

