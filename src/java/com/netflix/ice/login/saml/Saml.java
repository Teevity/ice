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
import com.netflix.ice.common.IceSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;
import javax.servlet.http.HttpServletRequestWrapper;
import org.opensaml.ws.message.decoder.MessageDecodingException;

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
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAML Plugin
 */
public class Saml extends LoginMethod {

    // Grails getRequestUrl is an internal dispatch url which isn't
    // very friendly to the user
    private class SamlHttpServletRequest extends HttpServletRequestWrapper {
        private final String requestUrl;
        SamlHttpServletRequest(HttpServletRequest request, String requestUrl) {
            super(request);
            this.requestUrl=requestUrl;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer sb = new StringBuffer();
            sb.append(requestUrl);
            return sb;
        }
    }

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
        client.setMaximumAuthenticationLifetime(Integer.parseInt(config.maximumAuthenticationLifetime));
    }

    public LoginResponse processLogin(HttpServletRequest request, HttpServletResponse response) throws LoginMethodException {
        IceSession iceSession = new IceSession(request.getSession());
        iceSession.voidSession(); //a second login request voids anything previous
        logger.info("Saml::processLogin");
        LoginResponse lr = new LoginResponse();

        SamlHttpServletRequest shsr = new SamlHttpServletRequest(request, config.signInUrl);
        final WebContext context = new J2ERequestContext(shsr);
        client.setCallbackUrl(config.signInUrl);
        boolean redirect = false;
        try {
            Saml2Credentials credentials = client.getCredentials(context);
            Saml2Profile saml2Profile = client.getUserProfile(credentials, context);
            processAssertion(iceSession, credentials, lr);
        } catch (NullPointerException npe) {
            redirect = true;
        } catch (RequiresHttpAction rha) {
            redirect = true;
        } catch (Exception e) {
            redirect = true;
        }
        if (redirect) {
            try {
                logger.info("Redirect user to SSO");
                if (config.singleSignOnUrl != null) {
                    //redirect to SSO using a static URL
                    lr.redirectTo=config.singleSignOnUrl;
                } else {
                    //try redirect using Pac4j library.  Not sure if this will work.
                    final WebContext redirect_context = new J2EContext(shsr, response);
                    client.redirect(redirect_context, false, false);
                    lr.responded = true;
                }
            } catch (RequiresHttpAction rhae) {
                logger.error(rhae.toString());
            }
            catch (NullPointerException npe) {
                logger.error(npe.toString());
            }
        }
        logger.debug("Login Response: " + lr.toString());
        return lr;
    }


    /**
     * Process an assertion and setup our session attributes
     */
    private void processAssertion(IceSession iceSession, Saml2Credentials credentials, LoginResponse lr) throws LoginMethodException {
        boolean foundAnAccount=false;
        iceSession.voidSession();

        for(Attribute attr : credentials.getAttributes()) {
            if (attr.getName().equals("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")) {
                for (XMLObject groupXMLObj : attr.getAttributeValues()) {
                    String username = groupXMLObj.getDOM().getTextContent();
                    iceSession.setUsername(username);
                }
            }
        }
        // iterate again for everything else
        for(Attribute attr : credentials.getAttributes()) {
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

        //require at least one account
        if (! foundAnAccount) {
            lr.loginFailed=true;
            return;
        } else {
            lr.loginSuccess=true;
        }

    }
}

