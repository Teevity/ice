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
package com.netflix.ice.login;

import com.netflix.ice.common.IceOptions;

public class LoginOptions {

    /**
     * Base for all things LOGIN
     */
    public final static String LOGIN = "ice.login";

       /**
    * Prefix/Namespace for login related configuration items.
    */
    public static final String LOGIN_PREFIX = "ice.login";

    /**
    * true/false for using Login.
    */
    public static final String LOGIN_ENABLE = "ice.login";

    /**
    * true/false for using Login
    */
    public static final String LOGIN_DEFAULT = "ice.login.default_endpoint";

    /**
    * Implementation login classes
    */
    public static final String LOGIN_CLASSES = "ice.login.classes";

    /**
    * Implementation login endpoints.  Will map to /logic/<endpoint>
    */
    public static final String LOGIN_ENDPOINTS = "ice.login.endpoints";

    /**
    * Simple passphrase for allowing Authentication
    */
    public static final String LOGIN_PASSPHRASE = "ice.login.passphrase";

    /**
    * Message to display when a user fails access.  Nice
    * directions to get them access
    */
    public static final String NO_ACCESS_MESSAGE = "ice.login.no_access_message";

    /**
    * Audit log location
    */
    public static final String LOGIN_LOG = "ice.login.log";

}

