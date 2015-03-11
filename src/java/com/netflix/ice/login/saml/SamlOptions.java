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
     * The iDP Signin-Url for our service.  Use this if the SAML Redirect doesn't work
     * ADFS URL looks like this:  https://sso.it.here.com/adfs/ls/wia?LoginToRP=service name
     */
    public static final String SIGNIN_URL = SAML + ".signin_url";

    /**
    * Service/Entity Identifier
    */
    public static final String SERVICE_IDENTIFIER = SAML + ".service_identifier";

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
    /**
    * Path to IDP Metdata
    */
    public static final String IDP_METADATA_PATH = SAML + ".idp_metadata_path";

    /**
     * Maximum amount of time that we accept a SAML Assertion
     * ADFS defaults to 8 hours -
     */
    public static final String MAXIMUM_AUTHENTICATION_LIFETIME = SAML + ".maximum_authentication_lifetime";

}
