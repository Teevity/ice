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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.KeyFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.security.PublicKey;
//import org.opensaml.xml.signature.PublicKey;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.security.MetadataCriteria;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;

import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.xml.XMLObject;
import org.opensaml.common.xml.SAMLSchemaBuilder;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.Configuration;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.util.Base64;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.saml2.core.Response;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.security.SAMLSignatureProfileValidator;


/**
 * SAML Plugin
 */
public class Saml extends LoginMethod {

    public final String SAML_PREFIX=propertyPrefix("saml");

    private static final Logger logger = LoggerFactory.getLogger(Saml.class);

    private final SamlConfig config;

    private List<Certificate> trustedSigningCerts=new ArrayList<Certificate>();

    public String propertyName(String name) {
       return SAML_PREFIX + "." + name;
    }

    public Saml(Properties properties) throws LoginMethodException {
        super(properties);
        config = new SamlConfig(properties);
        for(String signingCert : config.trustedSigningCerts) {
            try {
                FileInputStream fis = new FileInputStream(new File(signingCert));
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Certificate cert = certFactory.generateCertificate(fis);
                trustedSigningCerts.add(cert);
            } catch(IOException ioe) {
                logger.error("Error reading public key " + signingCert + ":" + ioe.toString());
            } catch(Exception e) {
                logger.error("Error decoding public key " + signingCert + ":" + e.toString());
            }
        }

        try {
            DefaultBootstrap.bootstrap();
        } catch(ConfigurationException ce) {
            throw new LoginMethodException("Failure to init OpenSAML: " + ce.toString());
        }
    }

    public LoginResponse processLogin(HttpServletRequest request) throws LoginMethodException {
        IceSession iceSession = new IceSession(request.getSession());
        iceSession.voidSession(); //a second login request voids anything previous
        logger.info("Saml::processLogin");
        LoginResponse lr = new LoginResponse();
        String assertion = (String)request.getParameter("SAMLResponse");
        if (assertion == null) {
            lr.redirectTo=config.singleSignOnUrl;
            return lr;
        }
        logger.trace("Received SAML Assertion: " + assertion);
        try
        {
            // 1.1 2.0 schemas
            Schema schema = SAMLSchemaBuilder.getSAML11Schema();
    
            //get parser pool manager
            BasicParserPool parserPoolManager = new BasicParserPool();
            parserPoolManager.setNamespaceAware(true);
            parserPoolManager.setIgnoreElementContentWhitespace(true);
            parserPoolManager.setSchema(schema); 
     
            String data = new String(Base64.decode(assertion));
            logger.info("Decoded SAML Assertion: " + data);

            StringReader reader = new StringReader(data);
            Document document = parserPoolManager.parse(reader);
            Element documentRoot = document.getDocumentElement();

            QName qName= new QName(documentRoot.getNamespaceURI(), documentRoot.getLocalName(), documentRoot.getPrefix());

            //get an unmarshaller
            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(documentRoot);

            //unmarshall using the document root element
            XMLObject xmlObj = unmarshaller.unmarshall(documentRoot);
            Response response = (Response)xmlObj;
            for(Assertion myAssertion : response.getAssertions())
            {
                if (! myAssertion.isSigned()) {
                    logger.error("SAML Assertion not signed" );
                    throw new LoginMethodException("SAML Assertions must be signed by a trusted provider");
                }

                Signature assertionSignature = myAssertion.getSignature();
                SAMLSignatureProfileValidator profVal = new SAMLSignatureProfileValidator();
       
                logger.info("Validating SAML Assertion" );
                // will throw a ValidationException 
                profVal.validate(assertionSignature);

                //Credential signCred = assertionSignature.getSigningCredential();
                boolean goodSignature = false;
                for(Certificate trustedCert : trustedSigningCerts) {
                    BasicCredential cred = new BasicCredential();
                    cred.setPublicKey(trustedCert.getPublicKey());
                    SignatureValidator validator = new SignatureValidator(cred);
                    try {
                        validator.validate(assertionSignature);
                    } catch(ValidationException ve) {
                        /* Not a good key! */ 
                        logger.debug("Not signed by " + trustedCert.toString());
                        continue;
                    }
                    logger.info("Assertion trusted from " + trustedCert.toString());
                    processAssertion(iceSession, myAssertion, lr);
                    goodSignature = true;
                    break;
                }

                if (goodSignature) {
                    lr.loginSuccess=true;
                }
      
            }
        } catch(org.xml.sax.SAXException saxe) {
            logger.error(saxe.toString());
        } catch(org.opensaml.xml.parse.XMLParserException xmlpe) {
            logger.error(xmlpe.toString());
        } catch(org.opensaml.xml.io.UnmarshallingException uee) {
            logger.error(uee.toString());
        } catch(org.opensaml.xml.validation.ValidationException ve) {
            throw new LoginMethodException("SAML Assertion Signature was not usable: " + ve.toString());
        }
        return lr;
    }

    /**
     * Process an assertion and setup our session attributes
     */
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
} 

