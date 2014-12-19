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
package com.netflix.ice.login.saml;

import com.netflix.ice.login.*;
import com.netflix.ice.common.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.lang.Boolean;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

/**
 * COnfiguration class for UI login.
 */
public class SamlConfig implements BaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(SamlConfig.class);

    public final List<String> trustedSigningCerts = new ArrayList<String>();
    public final List<String> trustedMetadata = new ArrayList<String>();

    private final String keystore;
    private final String keystorePassword;
    private final String keyAlias;
    private final String keyPassword;
    public final String organizationName;
    public final String organizationDisplayName;
    public final String organizationUrl;
    public final String signInUrl;
    public final String serviceName;
    public final String allAccounts;
    public final String singleSignOnUrl;

    public SamlConfig(Properties properties) {
        loadSigningCerts(properties);
        keystore = properties.getProperty(SamlOptions.KEYSTORE);
        keystorePassword = properties.getProperty(SamlOptions.KEYSTORE_PASSWORD);
        keyAlias = properties.getProperty(SamlOptions.KEY_ALIAS);
        keyPassword = properties.getProperty(SamlOptions.KEY_PASSWORD);
        organizationName = properties.getProperty(SamlOptions.ORGANIZATION_NAME);
        organizationDisplayName = properties.getProperty(SamlOptions.ORGANIZATION_DISPLAY_NAME);
        organizationUrl = properties.getProperty(SamlOptions.ORGANIZATION_URL);
        signInUrl = properties.getProperty(SamlOptions.SIGNIN_URL);
        serviceName = properties.getProperty(SamlOptions.SERVICE_NAME);
        allAccounts = properties.getProperty(SamlOptions.ALL_ACCOUNTS);
        singleSignOnUrl = properties.getProperty(SamlOptions.SINGLE_SIGN_ON_URL);
        loadSigningCerts(properties);

    }

    private void loadSigningCerts(Properties properties) {
        String trustedCerts = properties.getProperty(SamlOptions.TRUSTED_SIGNING_CERTS);
        if (trustedCerts == null) {
            logger.warn("No Trusted Certs found");
            return;
        } 
        String[] certLocations = trustedCerts.split(",");
        for(String certLocation : certLocations) {
            trustedSigningCerts.add(certLocation);
        }
    }
}
