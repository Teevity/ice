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
package com.netflix.ice.login.saml;

import com.netflix.ice.login.LoginOptions;

public class SamlOptions {

    /**
     * Base for all our SAML properties
     */
    public static final String SAML = LoginOptions.LOGIN + ".saml";

    /**
     * Signin-Url for our service
     */
    public static final String SIGNIN_URL = SAML + ".signing_url";

    /**
     * Property for Service Name
     */
    public static final String SERVICE_NAME = SAML + ".service_name";

    /**
     * Property for organization name. 
     */
    public static final String ORGANIZATION_NAME = SAML + ".organization_name";

    /**
     * Property for organization display name. 
     */
    public static final String ORGANIZATION_DISPLAY_NAME = SAML + ".organization_display_name";

    /**
     * Property for organization url
     */
    public static final String ORGANIZATION_URL = SAML + ".organization_url";

    /**
     * Property for Keystore where we can find certificates
     */
    public static final String KEYSTORE = SAML + ".keystore";

    /**
     * Property for Keystore Password
     */
    public static final String KEYSTORE_PASSWORD = SAML + ".keystore_password";

    /**
     * Property for Keystore Key alias
     */
    public static final String KEY_ALIAS = SAML + ".key_alias";

    /**
     * Property for Keystore Key password
     */
    public static final String KEY_PASSWORD = SAML + ".key_password";

     /**
     * Property for Keystore Key password
     */
    public static final String TRUSTED_SIGNING_CERTS = SAML + ".trusted_signing_certs";

    /**
    * Property for special account text to allows access to all accounts.
    * This would be supplied in the saml assertion account attribute.
    */
    public static final String ALL_ACCOUNTS = SAML + ".all_accounts";

    /**
    * Property for where to re-direct someone when they need to provide some 
    * SAML creds
    */
    public static final String SINGLE_SIGN_ON_URL = SAML + ".single_sign_on_url";

}
