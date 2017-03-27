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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.SAML2SPDetector;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class SAML2ITCase extends AbstractITCase {

    private static SyncopeClient anonymous;

    @BeforeClass
    public static void setupAnonymousClient() {
        anonymous = new SyncopeClientFactoryBean().
                setAddress(ADDRESS).
                create(new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
    }

    @BeforeClass
    public static void importFromIdPMetadata() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

        assertTrue(saml2IdPService.list().isEmpty());

        WebClient.client(saml2IdPService).
                accept(MediaType.APPLICATION_XML_TYPE).
                type(MediaType.APPLICATION_XML_TYPE);
        try {
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/ssocircle.xml"));
            saml2IdPService.importFromMetadata(SAML2ITCase.class.getResourceAsStream("/testshib-providers.xml"));
        } catch (Exception e) {
            LOG.error("Unexpected error while importing SAML 2.0 IdP metadata", e);
        } finally {
            WebClient.client(saml2IdPService).
                    accept(clientFactory.getContentType().getMediaType()).
                    type(clientFactory.getContentType().getMediaType());
        }

        assertEquals(2, saml2IdPService.list().size());
    }

    @AfterClass
    public static void clearIdPs() {
        Assume.assumeTrue(SAML2SPDetector.isSAML2SPAvailable());

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
            Response response = service.getMetadata(ADDRESS);
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
        SAML2IdPTO ssoCircle = IterableUtils.find(saml2IdPService.list(), new Predicate<SAML2IdPTO>() {

            @Override
            public boolean evaluate(final SAML2IdPTO object) {
                return "https://idp.ssocircle.com".equals(object.getEntityID());
            }
        });
        assertNotNull(ssoCircle);
        assertFalse(ssoCircle.getMappingItems().isEmpty());
        assertNotNull(ssoCircle.getConnObjectKeyItem());
        assertNotEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertNotEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());

        ssoCircle.getMappingItems().clear();

        MappingItemTO keyMapping = new MappingItemTO();
        keyMapping.setIntAttrName("email");
        keyMapping.setExtAttrName("EmailAddress");
        ssoCircle.setConnObjectKeyItem(keyMapping);

        saml2IdPService.update(ssoCircle);

        ssoCircle = saml2IdPService.read(ssoCircle.getKey());
        assertEquals("email", ssoCircle.getConnObjectKeyItem().getIntAttrName());
        assertEquals("EmailAddress", ssoCircle.getConnObjectKeyItem().getExtAttrName());
    }

}
