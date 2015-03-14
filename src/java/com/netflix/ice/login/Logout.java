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


/**
 * Simple Login Method to logout a session
 */
public class Logout extends LoginMethod {

    public Logout(Properties properties) throws LoginMethodException {
        super(properties);
    }

    public String propertyName(String name) {
        return null;
    }

    public LoginResponse processLogin(HttpServletRequest request, HttpServletResponse response) throws LoginMethodException {
        IceSession session = new IceSession(request.getSession());
        session.voidSession();
        LoginResponse lr = new LoginResponse();
        lr.loggedOut = true;
        return lr;
    }
} 

