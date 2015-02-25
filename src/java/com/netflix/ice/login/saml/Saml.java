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
import com.netflix.ice.common.IceOptions;
import com.netflix.ice.common.IceSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import org.joda.time.DateTime;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.io.StringReader;
import org.apache.commons.io.FileUtils;

import org.pac4j.saml.credentials.Saml2Credentials;
import org.pac4j.saml.profile.Saml2Profile;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.client.BaseClient;
import org.pac4j.saml.client.Saml2Client;
import org.pac4j.core.context.J2ERequestContext;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.opensaml.common.xml.SAMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAML Plugin
 */
public class Saml extends LoginMethod {

    public final String SAML_PREFIX=propertyPrefix("saml");

    private static final Logger logger = LoggerFactory.getLogger(Saml.class);

    private final SamlConfig config;
    private final Saml2Client client = new Saml2Client();

    public String propertyName(String name) {
       return SAML_PREFIX + "." + name;
    }

    public Saml(Properties properties) throws LoginMethodException {
        super(properties);
        config = new SamlConfig(properties);
        if (config.serviceIdentifier != null) {
            client.setSpEntityId(config.serviceIdentifier);
        }
        client.setIdpMetadataPath(config.idpMetadataPath);
        client.setCallbackUrl(config.signInUrl);
        client.setKeystorePath(config.keystore);
        client.setKeystorePassword(config.keystorePassword);
        client.setPrivateKeyPassword(config.keyPassword);
    }

    public LoginResponse processLogin(HttpServletRequest request) throws LoginMethodException {
        IceSession iceSession = new IceSession(request.getSession());
        iceSession.voidSession(); //a second login request voids anything previous
        logger.info("Saml::processLogin");
        LoginResponse lr = new LoginResponse();
        //String assertion = (String)request.getParameter("SAMLResponse");
        final WebContext context = new J2ERequestContext(request);
        client.setCallbackUrl(config.signInUrl);

        //logger.trace("Received SAML Assertion: " + assertion);
        // get SAML2 credentials
        try {
            Saml2Credentials credentials = client.getCredentials(context);
            Saml2Profile saml2Profile = client.getUserProfile(credentials, context);
            logger.info("Credentials: " + credentials.toString());
        } catch (RequiresHttpAction rha) { 
            try {
                lr.redirectTo=client.getRedirectAction(context, false, false).getLocation();
                return lr;
            } catch (RequiresHttpAction rhae) { }

        }
        return lr;
    }


    /**
     * Process an assertion and setup our session attributes
     */
/*
    private void processAssertion(IceSession iceSession, Assertion assertion, LoginResponse lr) throws LoginMethodException {
        boolean foundAnAccount=false;
        iceSession.voidSession();
        for(AttributeStatement as : assertion.getAttributeStatements()) {
            // iterate once to assure we set the username first
            for(Attribute attr : as.getAttributes()) {
                if (attr.getName().equals("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")) {
                    for(XMLObject groupXMLObj : attr.getAttributeValues()) {
                        String username = groupXMLObj.getDOM().getTextContent();
                        iceSession.setUsername(username);
                    }
                }
            }
            // iterate again for everything else
            for(Attribute attr : as.getAttributes()) {
                if (attr.getName().equals("com.netflix.ice.account")) {
                    for(XMLObject groupXMLObj : attr.getAttributeValues()) {
                        String allowedAccount = groupXMLObj.getDOM().getTextContent();
                        if (allowedAccount.equals(config.allAccounts) ) {
                            whitelistAllAccounts(iceSession);
                            foundAnAccount=true;
                            logger.info("Found Allow All Accounts: " + allowedAccount);
                            break;
                        } else {
                            if (whitelistAccount(iceSession, allowedAccount)) {
                                foundAnAccount=true;
                                logger.info("Found Account: " + allowedAccount);
                            }
                        }
                    }
                }
            }
        }

        //require at least one account
        if (! foundAnAccount) {
            lr.loginFailed=true;
            //throw new LoginMethodException("SAML Assertion must give at least one Account as part of the Assertion");
            return;
        }

        //set expiration date
        DateTime startDate = assertion.getConditions().getNotBefore();
        DateTime endDate = assertion.getConditions().getNotOnOrAfter();
        if (startDate == null || endDate == null) {
            throw new LoginMethodException("Assertion must state an expiration date");
        }
        // Clocks may not be synchronized.
        startDate = startDate.minusMinutes(2);
        endDate = endDate.plusMinutes(2);
        logger.info(startDate.toCalendar(null).getTime().toString()); 
        logger.info(endDate.toCalendar(null).getTime().toString()); 
        lr.loginStart = startDate.toCalendar(null).getTime();
        lr.loginEnd = endDate.toCalendar(null).getTime();
    }
*/
} 

