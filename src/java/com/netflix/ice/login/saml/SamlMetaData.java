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

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.net.URL;
import java.io.StringWriter;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.*;
import javax.xml.parsers.*;
import javax.xml.parsers.ParserConfigurationException;

import org.opensaml.saml2.metadata.*;
import org.opensaml.xml.security.keyinfo.*;
import org.opensaml.xml.security.credential.*;
import org.opensaml.xml.security.x509.*;
import org.opensaml.xml.*;

import groovy.text.SimpleTemplateEngine;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.security.MetadataCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.signature.impl.ExplicitKeySignatureTrustEngine;
import org.opensaml.Configuration;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.util.Base64;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.xml.security.SecurityException;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAML MetaData Plugin.  Provides MetaData for idPs
 */
public class SamlMetaData extends LoginMethod {

    public final String SAML_PREFIX=propertyPrefix("saml");
    Logger logger = LoggerFactory.getLogger(getClass());

    public String metadataXML = null;
    public final String keystore;
    public final String keystorePassword;
    public final String keyAlias;
    public final String keyPassword;
    public final String orgName;
    public final String orgDisplayName;
    public final String orgUrl;
    public final String signinUrl;
    public final String serviceName;

    public String propertyName(String name) {
        return SAML_PREFIX + "." + name;
    }

    public SamlMetaData(Properties properties) throws LoginMethodException {
        super(properties);
        keystore=propertyValue("keystore");
        keystorePassword=propertyValue("keystore_password");
        keyAlias=propertyValue("key_alias");
        keyPassword=propertyValue("key_password");
        orgName=propertyValue("org_name");
        orgDisplayName=propertyValue("org_display_name");
        orgUrl=propertyValue("org_url");
        signinUrl=propertyValue("signin_url");
        serviceName=propertyValue("service_name");
    }

    public LoginResponse processLogin(HttpServletRequest request) throws LoginMethodException {
        LoginResponse lr = new LoginResponse();
        if (metadataXML == null) {
            generateMetadata();
        }
        lr.renderData = metadataXML;
        lr.contentType = "application/samlmetadata+xml";
        return lr;
    }

    /**
    * Wow!
    * from: http://mylifewithjava.blogspot.com/2012/02/generating-metadata-with-opensaml.html
    */
    private synchronized void generateMetadata() {
        if (metadataXML != null) { return; }

        EntityDescriptor spEntityDescriptor = createSAMLObject(EntityDescriptor.class);
        spEntityDescriptor.setEntityID("netflix ice");

        Organization organization = createSAMLObject(Organization.class);

        OrganizationDisplayName samlOrgDisplayName = createSAMLObject(OrganizationDisplayName.class);
        samlOrgDisplayName.setName(new LocalizedString(orgDisplayName, "en"));
        organization.getDisplayNames().add(samlOrgDisplayName);

        OrganizationName samlOrgName = createSAMLObject(OrganizationName.class);
        samlOrgName.setName(new LocalizedString(orgName, "en"));
        organization.getOrganizationNames().add(samlOrgName);

        OrganizationURL samlOrgUrl = createSAMLObject(OrganizationURL.class);
        samlOrgUrl.setURL(new LocalizedString(orgUrl,"en"));
        organization.getURLs().add(samlOrgUrl);

        spEntityDescriptor.setOrganization(organization);

        SPSSODescriptor spSSODescriptor = createSAMLObject(SPSSODescriptor.class);
        spSSODescriptor.setWantAssertionsSigned(true);

        X509KeyInfoGeneratorFactory keyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        keyInfoGeneratorFactory.setEmitEntityCertificate(true);
        KeyInfoGenerator keyInfoGenerator = keyInfoGeneratorFactory.newInstance();

        //spSSODescriptor.setAuthnRequestsSigned(true); 
        //KeyDescriptor encKeyDescriptor = createSAMLObject(KeyDescriptor.class);
 
        //encKeyDescriptor.setUse(UsageType.ENCRYPTION); //Set usage
 
        //try {
        //    encKeyDescriptor.setKeyInfo(keyInfoGenerator.generate(null));
        //} catch (SecurityException e) {
        //    logger.error(e.getMessage(), e);
        //}
 
        //spSSODescriptor.getKeyDescriptors().add(encKeyDescriptor);
   
        KeyDescriptor signKeyDescriptor = createSAMLObject(KeyDescriptor.class);
 
        signKeyDescriptor.setUse(UsageType.SIGNING);  //Set usage
 
        try {
            BasicX509Credential creds = new BasicX509Credential();
            creds.setEntityCertificate(signingKey());
            signKeyDescriptor.setKeyInfo(keyInfoGenerator.generate(creds));
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
 
        spSSODescriptor.getKeyDescriptors().add(signKeyDescriptor);
        NameIDFormat nameIDFormat = createSAMLObject(NameIDFormat.class);
        nameIDFormat.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
        spSSODescriptor.getNameIDFormats().add(nameIDFormat);
        AssertionConsumerService assertionConsumerService = createSAMLObject(AssertionConsumerService.class);
        assertionConsumerService.setIndex(0);
        //assertionConsumerService.setBinding(SAMLConstants.SAML2_ARTIFACT_BINDING_URI);
        assertionConsumerService.setBinding(SAMLConstants.SAML2_POST_BINDING_URI);
 
        assertionConsumerService.setLocation(signinUrl);
        spSSODescriptor.getAssertionConsumerServices().add(assertionConsumerService);
        spSSODescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
 
        spEntityDescriptor.getRoleDescriptors().add(spSSODescriptor);

        AttributeConsumingService service = createSAMLObject(AttributeConsumingService.class);
        ServiceName name = createSAMLObject(ServiceName.class);
        name.setName(new LocalizedString(serviceName, "en"));
        service.getNames().add(name);
        service.setIndex(0);
        service.setIsDefault(true);

        RequestedAttribute accountAttr = createSAMLObject(RequestedAttribute.class);
        accountAttr.setFriendlyName("AccountID");
        accountAttr.setName("com.netflix.ice.account");

        service.getRequestAttributes().add(accountAttr);

        spSSODescriptor.getAttributeConsumingServices().add(service);
 
        DocumentBuilder builder = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try { 
            builder = factory.newDocumentBuilder();
        } catch(javax.xml.parsers.ParserConfigurationException pce) {
            logger.error(pce.toString());
        }
        Document document = builder.newDocument();
        Marshaller out = Configuration.getMarshallerFactory().getMarshaller(spEntityDescriptor);

        try {
            out.marshall(spEntityDescriptor, document);
        } catch(org.opensaml.xml.io.MarshallingException me) {
            logger.error(me.toString());
        }
        Transformer transformer = null;
        try { 
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch(javax.xml.transform.TransformerConfigurationException tce) {
            logger.error(tce.toString());
        }

        StringWriter stringWriter = new StringWriter();

        try {
            StreamResult streamResult = new StreamResult(stringWriter);
       
            DOMSource source = new DOMSource(document);
            transformer.transform(source, streamResult);
            stringWriter.close();
        } catch(IOException ioe) {
            logger.error(ioe.toString());
            return;
        } catch(javax.xml.transform.TransformerException te) {
            logger.error(te.toString());
            return;
        }
        metadataXML = stringWriter.toString();
    }

    /**
    * from: http://mylifewithjava.blogspot.no/2011/04/convenience-methods-for-opensaml.html
    */
    public static <T> T createSAMLObject(final Class<T> clazz) {
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        QName defaultElementName = null;
        try {
            defaultElementName = (QName)clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
        } catch(IllegalAccessException iae) {
            System.out.println(iae.toString());
            return null;
        } catch(java.lang.NoSuchFieldException nsfe) {
            System.out.println(nsfe.toString());
            return null;
        }
        T object = (T)builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
        return object;
    }
   

    private X509Certificate signingKey() throws Exception {
        FileInputStream is = new FileInputStream(keystore);

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, keystorePassword.toCharArray());

        Key key = keystore.getKey(keyAlias, keyPassword.toCharArray());
        System.out.println(key.toString());
        if (key instanceof PrivateKey) {
            System.out.println("Got Here");
            // Get certificate of public key
            X509Certificate cert = (X509Certificate)keystore.getCertificate(keyAlias);
            return cert;
        }
        return null;
    }
} 

