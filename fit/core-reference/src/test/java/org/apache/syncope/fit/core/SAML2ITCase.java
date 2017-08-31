/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.SAML2SPDetector;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.xml.security.signature.XMLSignature;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SAML2ITCase extends AbstractITCase {

    private static final Pattern BASE64 = Pattern.compile(
            "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$");

    private static SyncopeClient anonymous;

    private static Path keystorePath;

    private static Path truststorePath;

    @BeforeClass
    public static void setup() {
        anonymous = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).
                create(new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));

        WSSConfig.init();
        OpenSAMLUtil.initSamlEngine(false);
    }

    @BeforeClass
    public static void importFromIdPMetadata() throws Exception {
        if (!SAML2SPDetector.isSAML2SPAvailable()) {
            return;
        }

        assertTrue(saml2IdPService.list().isEmpty());

        createKeystores();

        updateMetadataWithCert();

        WebClient.client(saml2IdPService).
                accept(MediaType.APPLICATION_XML_TYPE).
                type(MediaType.APPLICATION_XML_TYPE);
        try {
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/ssocircle.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/testshib-providers.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/fediz.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/fediz_realmb.xml"));

            // Allow unsolicited responses for the realmb case
            String realmBEntityId = "urn:org:apache:cxf:fediz:idp:realm-B";
            Optional<SAML2IdPTO> realmBIdP =
                saml2IdPService.list().stream().filter(idp -> realmBEntityId.equals(idp.getEntityID())).findFirst();
            realmBIdP.get().setSupportUnsolicited(true);
            saml2IdPService.update(realmBIdP.get());
        } catch (Exception e) {
            LOG.error("Unexpected error while importing SAML 2.0 IdP metadata", e);
        } finally {
            WebClient.client(saml2IdPService).
                    accept(clientFactory.getContentType().getMediaType()).
                    type(clientFactory.getContentType().getMediaType());
        }

        assertEquals(4, saml2IdPService.list().size());
    }

    @AfterClass
    public static void clearIdPs() throws Exception {
        if (!SAML2SPDetector.isSAML2SPAvailable()) {
            return;
        }

        saml2IdPService.list().forEach(idp -> {
            saml2IdPService.delete(idp.getKey());
        });

        Files.delete(keystorePath);
        Files.delete(truststorePath);
    }

    @Test
    public void spMetadata() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        try {
            SAML2SPService service = anonymous.getService(SAML2SPService.class);
            WebClient.client(service).accept(MediaType.APPLICATION_XML_TYPE);
            Response response = service.getMetadata(ADDRESS, "saml2sp");
            assertNotNull(response);

            Document responseDoc = StaxUtils.read(
                    new InputStreamReader((InputStream) response.getEntity(), StandardCharsets.UTF_8));
            assertEquals("EntityDescriptor", responseDoc.getDocumentElement().getLocalName());
            assertEquals("urn:oasis:names:tc:SAML:2.0:metadata", responseDoc.getDocumentElement().getNamespaceURI());

            // Get the signature
            QName signatureQName = new QName(SignatureConstants.XMLSIG_NS, "Signature");
            Element signatureElement =
                    DOMUtils.getFirstChildWithName(responseDoc.getDocumentElement(), signatureQName);
            assertNotNull(signatureElement);

            // Validate the signature
            XMLSignature signature = new XMLSignature(signatureElement, null);
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Loader.getResourceAsStream("keystore"), "changeit".toCharArray());
            assertTrue(signature.checkSignatureValue((X509Certificate) keystore.getCertificate("sp")));
        } catch (Exception e) {
            LOG.error("During SAML 2.0 SP metadata parsing", e);
            fail(e.getMessage());
        }
    }

    @Test
    public void createLoginRequest() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        SAML2RequestTO loginRequest = anonymous.getService(SAML2SPService.class).
                createLoginRequest(ADDRESS, "https://idp.testshib.org/idp/shibboleth");
        assertNotNull(loginRequest);

        assertEquals("https://idp.testshib.org/idp/profile/SAML2/POST/SSO", loginRequest.getIdpServiceAddress());
        assertNotNull(loginRequest.getContent());
        assertTrue(BASE64.matcher(loginRequest.getContent()).matches());
        assertNotNull(loginRequest.getRelayState());
    }

    @Test
    public void setIdPMapping() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        Optional<SAML2IdPTO> ssoCircleOpt =
                saml2IdPService.list().stream().filter(o -> "https://idp.ssocircle.com".equals(o.getEntityID())).
                        findFirst();
        assertTrue(ssoCircleOpt.isPresent());

        SAML2IdPTO ssoCircle = ssoCircleOpt.get();
        assertNotNull(ssoCircle);
        assertFalse(ssoCircle.isCreateUnmatching());
        assertNull(ssoCircle.getUserTemplate());
        assertFalse(ssoCircle.getItems().isEmpty());
        assertNotNull(ssoCircle.getConnObjectKeyItem());
        assertNotEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertNotEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());

        ssoCircle.setCreateUnmatching(true);

        UserTO userTemplate = new UserTO();
        userTemplate.setRealm("'/'");
        ssoCircle.setUserTemplate(userTemplate);

        ssoCircle.getItems().clear();
        ItemTO keyMapping = new ItemTO();
        keyMapping.setIntAttrName("email");
        keyMapping.setExtAttrName("EmailAddress");
        ssoCircle.setConnObjectKeyItem(keyMapping);

        saml2IdPService.update(ssoCircle);

        ssoCircle = saml2IdPService.read(ssoCircle.getKey());
        assertTrue(ssoCircle.isCreateUnmatching());
        assertEquals(userTemplate, ssoCircle.getUserTemplate());
        assertEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());
    }

    @Test
    public void validateLoginResponse() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest = saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        assertEquals("https://localhost:8443/fediz-idp/saml/up", loginRequest.getIdpServiceAddress());
        assertNotNull(loginRequest.getContent());
        assertTrue(BASE64.matcher(loginRequest.getContent()).matches());
        assertNotNull(loginRequest.getRelayState());

        // Check a null relaystate
        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setSpEntityID("http://recipient.apache.org/");
        response.setUrlContext("saml2sp");
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on no Relay State");
        } catch (SyncopeClientException e) {
            assertTrue(e.getMessage().contains("No Relay State was provided"));
        }

        // Check a null Response
        response.setRelayState(loginRequest.getRelayState());
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on no SAML Response");
        } catch (SyncopeClientException e) {
            assertTrue(e.getMessage().contains("No SAML Response was provided"));
        }

        // Create a SAML Response using WSS4J
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse = createResponse(inResponseTo);

        Document doc = DOMUtils.newDocument();
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(Base64.getEncoder().encodeToString(responseStr.getBytes()));
        SAML2LoginResponseTO loginResponse = saml2Service.validateLoginResponse(response);
        assertNotNull(loginResponse.getAccessToken());
        assertEquals("puccini", loginResponse.getNameID());
    }

    @Test
    public void unsignedAssertionInLoginResponse() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest = saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setSpEntityID("http://recipient.apache.org/");
        response.setUrlContext("saml2sp");
        response.setRelayState(loginRequest.getRelayState());

        // Create a SAML Response using WSS4J
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse =
                createResponse(inResponseTo, false, SAML2Constants.CONF_SENDER_VOUCHES,
                               "urn:org:apache:cxf:fediz:idp:realm-A");

        Document doc = DOMUtils.newDocument();
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(Base64.getEncoder().encodeToString(responseStr.getBytes()));
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on an unsigned Assertion");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void loginResponseWrappingAttack() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest = saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setSpEntityID("http://recipient.apache.org/");
        response.setUrlContext("saml2sp");
        response.setRelayState(loginRequest.getRelayState());

        // Create a SAML Response using WSS4J
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse = createResponse(inResponseTo);

        Document doc = DOMUtils.newDocument();
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        assertNotNull(responseElement);
        doc.appendChild(responseElement);

        // Get Assertion Element
        Element assertionElement =
                (Element) responseElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion").item(0);
        assertNotNull(assertionElement);

        // Clone it, strip the Signature, modify the Subject, change Subj Conf
        Element clonedAssertion = (Element) assertionElement.cloneNode(true);
        clonedAssertion.setAttributeNS(null, "ID", "_12345623562");
        Element sigElement =
                (Element) clonedAssertion.getElementsByTagNameNS(WSConstants.SIG_NS, "Signature").item(0);
        clonedAssertion.removeChild(sigElement);

        Element subjElement =
                (Element) clonedAssertion.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Subject").item(0);
        Element subjNameIdElement =
                (Element) subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "NameID").item(0);
        subjNameIdElement.setTextContent("verdi");

        Element subjConfElement =
                (Element) subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "SubjectConfirmation").item(0);
        subjConfElement.setAttributeNS(null, "Method", SAML2Constants.CONF_SENDER_VOUCHES);

        // Now insert the modified cloned Assertion into the Response after the other assertion
        responseElement.insertBefore(clonedAssertion, null);

        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(Base64.getEncoder().encodeToString(responseStr.getBytes()));
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on an unsigned Assertion");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void validateIdpInitiatedLoginResponse() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);

        // Create a SAML Response using WSS4J
        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setSpEntityID("http://recipient.apache.org/");
        response.setUrlContext("saml2sp");

        org.opensaml.saml.saml2.core.Response samlResponse =
            createResponse(null, true, SAML2Constants.CONF_BEARER, "urn:org:apache:cxf:fediz:idp:realm-B");

        Document doc = DOMUtils.newDocument();
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(Base64.getEncoder().encodeToString(responseStr.getBytes()));
        response.setRelayState("idpInitiated");
        SAML2LoginResponseTO loginResponse =
            saml2Service.validateLoginResponse(response);
        assertNotNull(loginResponse.getAccessToken());
        assertEquals("puccini", loginResponse.getNameID());
    }

    // Make sure that the IdP initiated case is only supported when "supportUnsolicited" is true for that IdP
    @Test
    public void validateIdpInitiatedLoginResponseFailure() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);

        // Create a SAML Response using WSS4J
        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setSpEntityID("http://recipient.apache.org/");
        response.setUrlContext("saml2sp");

        org.opensaml.saml.saml2.core.Response samlResponse =
            createResponse(null, true, SAML2Constants.CONF_BEARER, "urn:org:apache:cxf:fediz:idp:realm-A");

        Document doc = DOMUtils.newDocument();
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(Base64.getEncoder().encodeToString(responseStr.getBytes()));
        response.setRelayState("idpInitiated");
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on an unsolicited login");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    private org.opensaml.saml.saml2.core.Response createResponse(final String inResponseTo) throws Exception {
        return createResponse(inResponseTo, true, SAML2Constants.CONF_BEARER, "urn:org:apache:cxf:fediz:idp:realm-A");
    }

    private org.opensaml.saml.saml2.core.Response createResponse(
            final String inResponseTo, final boolean signAssertion, final String subjectConfMethod,
            final String issuer) throws Exception {

        Status status = SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null);
        org.opensaml.saml.saml2.core.Response response = SAML2PResponseComponentBuilder.createSAMLResponse(
                inResponseTo, issuer, status);
        response.setDestination("http://recipient.apache.org");

        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setIssuer(issuer);
        callbackHandler.setSubjectName("puccini");
        callbackHandler.setSubjectConfirmationMethod(subjectConfMethod);

        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo(inResponseTo);
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org/saml2sp/assertion-consumer");
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://recipient.apache.org/"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        if (signAssertion) {
            Crypto issuerCrypto = new Merlin();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            InputStream input = Files.newInputStream(keystorePath);
            keyStore.load(input, "security".toCharArray());
            ((Merlin) issuerCrypto).setKeyStore(keyStore);

            assertion.signAssertion("subject", "security", issuerCrypto, false);
        }

        response.getAssertions().add(assertion.getSaml2());

        return response;
    }

    private static void createKeystores() throws Exception {
        // Create KeyPair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        Date currentDate = new Date();
        Date expiryDate = new Date(currentDate.getTime() + 365L * 24L * 60L * 60L * 1000L);

        // Create X509Certificate
        String issuerName = "CN=Issuer";
        String subjectName = "CN=Subject";
        BigInteger serial = new BigInteger("123456");
        X509v3CertificateBuilder certBuilder =
                new X509v3CertificateBuilder(new X500Name(RFC4519Style.INSTANCE, issuerName), serial, currentDate,
                        expiryDate,
                        new X500Name(RFC4519Style.INSTANCE, subjectName),
                        SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));

        // Store Private Key + Certificate in Keystore
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, "security".toCharArray());
        keystore.setKeyEntry("subject", keyPair.getPrivate(), "security".toCharArray(),
                new Certificate[] { certificate });

        File keystoreFile = File.createTempFile("samlkeystore", ".jks");
        try (OutputStream output = Files.newOutputStream(keystoreFile.toPath())) {
            keystore.store(output, "security".toCharArray());
        }
        keystorePath = keystoreFile.toPath();

        // Now store the Certificate in the truststore
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, "security".toCharArray());

        trustStore.setCertificateEntry("subject", certificate);

        File truststoreFile = File.createTempFile("samltruststore", ".jks");
        try (OutputStream output = Files.newOutputStream(truststoreFile.toPath())) {
            trustStore.store(output, "security".toCharArray());
        }
        truststorePath = truststoreFile.toPath();
    }

    private static void updateMetadataWithCert() throws Exception {
        // Get encoded truststore cert
        KeyStore keyStore = KeyStore.getInstance("JKS");
        InputStream input = Files.newInputStream(truststorePath);
        keyStore.load(input, "security".toCharArray());
        X509Certificate cert = (X509Certificate) keyStore.getCertificate("subject");
        String certEncoded = Base64.getMimeEncoder().encodeToString(cert.getEncoded());

        // Replace the "cert-placeholder" string in the metadata with the actual cert
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        List<String> fileNames = Arrays.asList("fediz.xml", "fediz_realmb.xml");
        for (String fileName : fileNames) {
            Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/" + fileName);
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll("cert-placeholder", certEncoded);

            Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/" + fileName);
            Files.write(path2, content.getBytes());
        }
    }

}
