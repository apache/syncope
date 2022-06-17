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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class ClientAppITCase extends AbstractITCase {

    @Test
    public void createSAML2SP() {
        createClientApp(ClientAppType.SAML2SP, buildSAML2SP());
    }

    @Test
    public void readSAML2SP() {
        SAML2SPClientAppTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        SAML2SPClientAppTO found = CLIENT_APP_SERVICE.read(ClientAppType.SAML2SP, samlSpTO.getKey());
        assertNotNull(found);
        assertFalse(StringUtils.isBlank(found.getEntityId()));
        assertFalse(StringUtils.isBlank(found.getMetadataLocation()));
        assertTrue(found.isEncryptAssertions());
        assertTrue(found.isEncryptionOptional());
        assertNotNull(found.getRequiredNameIdFormat());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void updateSAML2SP() {
        SAML2SPClientAppTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("NewAccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setName("New Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        samlSpTO.setEntityId("newEntityId");
        samlSpTO.setAccessPolicy(accessPolicyTO.getKey());

        CLIENT_APP_SERVICE.update(ClientAppType.SAML2SP, samlSpTO);
        SAML2SPClientAppTO updated = CLIENT_APP_SERVICE.read(ClientAppType.SAML2SP, samlSpTO.getKey());

        assertNotNull(updated);
        assertEquals("newEntityId", updated.getEntityId());
        assertNotNull(updated.getAccessPolicy());
    }

    @Test
    public void deleteSAML2SP() {
        SAML2SPClientAppTO samlSpTO = buildSAML2SP();
        samlSpTO = createClientApp(ClientAppType.SAML2SP, samlSpTO);

        CLIENT_APP_SERVICE.delete(ClientAppType.SAML2SP, samlSpTO.getKey());

        try {
            CLIENT_APP_SERVICE.read(ClientAppType.SAML2SP, samlSpTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void createOIDCRP() {
        createClientApp(ClientAppType.OIDCRP, buildOIDCRP());
    }

    @Test
    public void createCASSP() {
        createClientApp(ClientAppType.CASSP, buildCASSP());
    }

    @Test
    public void readOIDCRP() {
        OIDCRPClientAppTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        OIDCRPClientAppTO found = CLIENT_APP_SERVICE.read(ClientAppType.OIDCRP, oidcrpTO.getKey());
        assertNotNull(found);
        assertFalse(StringUtils.isBlank(found.getClientId()));
        assertFalse(StringUtils.isBlank(found.getClientSecret()));
        assertNotNull(found.getSubjectType());
        assertFalse(found.getSupportedGrantTypes().isEmpty());
        assertFalse(found.getSupportedResponseTypes().isEmpty());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void readCASSP() {
        CASSPClientAppTO casspTO = buildCASSP();
        casspTO = createClientApp(ClientAppType.CASSP, casspTO);
        CASSPClientAppTO found = CLIENT_APP_SERVICE.read(ClientAppType.CASSP, casspTO.getKey());
        assertNotNull(found);
        assertNotNull(found.getServiceId());
        assertNotNull(found.getAccessPolicy());
        assertNotNull(found.getAuthPolicy());
    }

    @Test
    public void updateOIDCRP() {
        OIDCRPClientAppTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("NewAccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setName("New Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        oidcrpTO.setClientId("newClientId");
        oidcrpTO.setAccessPolicy(accessPolicyTO.getKey());

        CLIENT_APP_SERVICE.update(ClientAppType.OIDCRP, oidcrpTO);
        OIDCRPClientAppTO updated = CLIENT_APP_SERVICE.read(ClientAppType.OIDCRP, oidcrpTO.getKey());

        assertNotNull(updated);
        assertEquals("newClientId", updated.getClientId());
        assertNotNull(updated.getAccessPolicy());
    }

    @Test
    public void deleteOIDCRP() {
        OIDCRPClientAppTO oidcrpTO = buildOIDCRP();
        oidcrpTO = createClientApp(ClientAppType.OIDCRP, oidcrpTO);

        CLIENT_APP_SERVICE.delete(ClientAppType.OIDCRP, oidcrpTO.getKey());

        try {
            CLIENT_APP_SERVICE.read(ClientAppType.OIDCRP, oidcrpTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void deleteCASSP() {
        CASSPClientAppTO casspTO = buildCASSP();
        casspTO = createClientApp(ClientAppType.CASSP, casspTO);

        CLIENT_APP_SERVICE.delete(ClientAppType.CASSP, casspTO.getKey());

        try {
            CLIENT_APP_SERVICE.read(ClientAppType.CASSP, casspTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    private CASSPClientAppTO buildCASSP() {
        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        authPolicyTO.setKey("AuthPolicyTest_" + getUUIDString());
        authPolicyTO.setName("Authentication Policy");
        authPolicyTO = createPolicy(PolicyType.AUTH, authPolicyTO);
        assertNotNull(authPolicyTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("AccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setName("Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        CASSPClientAppTO casspTO = new CASSPClientAppTO();
        casspTO.setName("ExampleRP_" + getUUIDString());
        casspTO.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        casspTO.setDescription("Example OIDC RP application");
        casspTO.setServiceId("https://cassp.example.org/" + UUID.randomUUID().getMostSignificantBits());

        casspTO.setAuthPolicy(authPolicyTO.getKey());
        casspTO.setAccessPolicy(accessPolicyTO.getKey());
        return casspTO;
    }
}
