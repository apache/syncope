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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
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
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SAML2ITCase extends AbstractITCase {

    private static SyncopeClient anonymous;

    @BeforeClass
    public static void setupAnonymousClient() {
        anonymous = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).
                create(new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));

        WSSConfig.init();
        OpenSAMLUtil.initSamlEngine();
    }

    @BeforeClass
    public static void importFromIdPMetadata() {
        if (!SAML2SPDetector.isSAML2SPAvailable()) {
            return;
        }

        assertTrue(saml2IdPService.list().isEmpty());

        WebClient.client(saml2IdPService).
                accept(MediaType.APPLICATION_XML_TYPE).
                type(MediaType.APPLICATION_XML_TYPE);
        try {
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/ssocircle.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/testshib-providers.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/fediz.xml"));
        } catch (Exception e) {
            LOG.error("Unexpected error while importing SAML 2.0 IdP metadata", e);
        } finally {
            WebClient.client(saml2IdPService).
                    accept(clientFactory.getContentType().getMediaType()).
                    type(clientFactory.getContentType().getMediaType());
        }

        assertEquals(3, saml2IdPService.list().size());
    }

    @AfterClass
    public static void clearIdPs() {
        if (!SAML2SPDetector.isSAML2SPAvailable()) {
            return;
        }

        for (SAML2IdPTO idp : saml2IdPService.list()) {
            saml2IdPService.delete(idp.getKey());
        }
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
        assertTrue(Base64.isBase64(loginRequest.getContent()));
        assertNotNull(loginRequest.getRelayState());
    }

    @Test
    public void setIdPMapping() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        SAML2IdPTO ssoCircle = IterableUtils.find(saml2IdPService.list(), new Predicate<SAML2IdPTO>() {

            @Override
            public boolean evaluate(final SAML2IdPTO object) {
                return "https://idp.ssocircle.com".equals(object.getEntityID());
            }
        });
        assertNotNull(ssoCircle);
        assertFalse(ssoCircle.getItems().isEmpty());
        assertNotNull(ssoCircle.getConnObjectKeyItem());
        assertNotEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertNotEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());

        ssoCircle.getItems().clear();

        ItemTO keyMapping = new ItemTO();
        keyMapping.setIntAttrName("email");
        keyMapping.setExtAttrName("EmailAddress");
        ssoCircle.setConnObjectKeyItem(keyMapping);

        saml2IdPService.update(ssoCircle);

        ssoCircle = saml2IdPService.read(ssoCircle.getKey());
        assertEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());
    }

    @Test
    public void validateLoginResponse() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest =
            saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        assertEquals("https://localhost:8443/fediz-idp/saml/up", loginRequest.getIdpServiceAddress());
        assertNotNull(loginRequest.getContent());
        assertTrue(Base64.isBase64(loginRequest.getContent()));
        assertNotNull(loginRequest.getRelayState());

        // Check a null relaystate
        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on no Relay State");
        } catch (SyncopeClientException ex) {
            assertTrue(ex.getMessage().contains("No Relay State was provided"));
        }

        // Check a null Response
        response.setRelayState(loginRequest.getRelayState());
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on no SAML Response");
        } catch (SyncopeClientException ex) {
            assertTrue(ex.getMessage().contains("No SAML Response was provided"));
        }

        // Create a SAML Response using WSS4J
        Document doc = DOMUtils.newDocument();
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse = createResponse(doc, inResponseTo);
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(java.util.Base64.getEncoder().encodeToString(responseStr.getBytes()));
        SAML2LoginResponseTO loginResponse = saml2Service.validateLoginResponse(response);
        assertNotNull(loginResponse.getAccessToken());
        assertEquals("puccini", loginResponse.getNameID());
    }

    @Test
    @org.junit.Ignore
    public void testUnsignedAssertionInLoginResponse() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest =
            saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setRelayState(loginRequest.getRelayState());

        // Create a SAML Response using WSS4J
        Document doc = DOMUtils.newDocument();
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse =
            createResponse(doc, inResponseTo, false, SAML2Constants.CONF_SENDER_VOUCHES);
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);
        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(java.util.Base64.getEncoder().encodeToString(responseStr.getBytes()));
        try {
            saml2Service.validateLoginResponse(response);
            fail("Failure expected on an unsigned Assertion");
        } catch (SyncopeClientException ex) {
            // expected
        }
    }

    @Test
    @org.junit.Ignore
    public void testLoginResponseWrappingAttack() throws Exception {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        // Get a valid login request for the Fediz realm
        SAML2SPService saml2Service = anonymous.getService(SAML2SPService.class);
        SAML2RequestTO loginRequest =
            saml2Service.createLoginRequest(ADDRESS, "urn:org:apache:cxf:fediz:idp:realm-A");
        assertNotNull(loginRequest);

        SAML2ReceivedResponseTO response = new SAML2ReceivedResponseTO();
        response.setRelayState(loginRequest.getRelayState());

        // Create a SAML Response using WSS4J
        Document doc = DOMUtils.newDocument();
        JwsJwtCompactConsumer relayState = new JwsJwtCompactConsumer(response.getRelayState());
        String inResponseTo = relayState.getJwtClaims().getSubject();

        org.opensaml.saml.saml2.core.Response samlResponse = createResponse(doc, inResponseTo);
        Element responseElement = OpenSAMLUtil.toDom(samlResponse, doc);

        doc.appendChild(responseElement);
        assertNotNull(responseElement);

        // Get Assertion Element
        Element assertionElement =
            (Element)responseElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion").item(0);
        assertNotNull(assertionElement);

        // Clone it, strip the Signature, modify the Subject, change Subj Conf
        Element clonedAssertion = (Element)assertionElement.cloneNode(true);
        clonedAssertion.setAttributeNS(null, "ID", "_12345623562");
        Element sigElement =
            (Element)clonedAssertion.getElementsByTagNameNS(WSConstants.SIG_NS, "Signature").item(0);
        clonedAssertion.removeChild(sigElement);

        Element subjElement =
            (Element)clonedAssertion.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Subject").item(0);
        Element subjNameIdElement =
            (Element)subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "NameID").item(0);
        subjNameIdElement.setTextContent("verdi");

        Element subjConfElement =
            (Element)subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "SubjectConfirmation").item(0);
        subjConfElement.setAttributeNS(null, "Method", SAML2Constants.CONF_SENDER_VOUCHES);

        // Now insert the modified cloned Assertion into the Response after the other assertion
        responseElement.insertBefore(clonedAssertion, null);

        String responseStr = DOM2Writer.nodeToString(responseElement);

        // Validate the SAML Response
        response.setSamlResponse(java.util.Base64.getEncoder().encodeToString(responseStr.getBytes()));
        SAML2LoginResponseTO loginResponse = saml2Service.validateLoginResponse(response);
        assertNotNull(loginResponse.getAccessToken());
        assertEquals("puccini", loginResponse.getNameID());
    }

    private org.opensaml.saml.saml2.core.Response createResponse(Document doc, String inResponseTo) throws Exception {
        return createResponse(doc, inResponseTo, true, SAML2Constants.CONF_BEARER);
    }

    private org.opensaml.saml.saml2.core.Response createResponse(Document doc, String inResponseTo,
                                                                 boolean signAssertion, String subjectConfMethod) throws Exception {
        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null
            );
        org.opensaml.saml.saml2.core.Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                inResponseTo, "urn:org:apache:cxf:fediz:idp:realm-A", status
            );
        response.setDestination("http://recipient.apache.org");

        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setIssuer("urn:org:apache:cxf:fediz:idp:realm-A");
        callbackHandler.setSubjectName("puccini");
        callbackHandler.setSubjectConfirmationMethod(subjectConfMethod);

        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        if (signAssertion) {
            Crypto issuerCrypto = new Merlin();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            ClassLoader loader = Loader.getClassLoader(SAML2ITCase.class);
            InputStream input = Merlin.loadInputStream(loader, "stsrealm_a.jks");
            keyStore.load(input, "storepass".toCharArray());
            ((Merlin)issuerCrypto).setKeyStore(keyStore);

            assertion.signAssertion("realma", "realma", issuerCrypto, false);
        }

        response.getAssertions().add(assertion.getSaml2());

        return response;
    }


}
