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
package org.apache.syncope.fit.core.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WAClientAppITCase extends AbstractITCase {

    private static final String AUTH_MODULE = "DefaultJDBCAuthModule";

    private static WAClientAppService WA_CLIENT_APP_SERVICE;

    @BeforeAll
    public static void setup() {
        assumeTrue(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.JSON);

        SyncopeClient anonymous = CLIENT_FACTORY.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        WA_CLIENT_APP_SERVICE = anonymous.getService(WAClientAppService.class);
    }

    @Test
    public void list() {
        createClientApp(ClientAppType.OIDCRP, buildOIDCRP());

        List<WAClientApp> list = WA_CLIENT_APP_SERVICE.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void read() {
        OIDCRPClientAppTO oidcrpto = createClientApp(ClientAppType.OIDCRP, buildOIDCRP());
        WAClientApp waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getClientAppId(), null);
        assertNotNull(waClientApp);

        waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getClientAppId(), ClientAppType.OIDCRP);
        assertNotNull(waClientApp);

        waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getName(), null);
        assertNotNull(waClientApp);

        waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getName(), ClientAppType.OIDCRP);
        assertNotNull(waClientApp);

        SAML2SPClientAppTO samlspto = createClientApp(ClientAppType.SAML2SP, buildSAML2SP());
        WAClientApp registeredSamlClientApp = WA_CLIENT_APP_SERVICE.read(samlspto.getClientAppId(), null);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = WA_CLIENT_APP_SERVICE.read(samlspto.getClientAppId(), ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = WA_CLIENT_APP_SERVICE.read(samlspto.getName(), null);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = WA_CLIENT_APP_SERVICE.read(samlspto.getName(), ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);
    }

    @Test
    public void readWithPolicies() {
        OIDCRPClientAppTO oidcrpto = buildOIDCRP();

        AuthPolicyTO authPolicyTO = createPolicy(PolicyType.AUTH, buildAuthPolicyTO(AUTH_MODULE));

        AccessPolicyTO accessPolicyTO = createPolicy(PolicyType.ACCESS, buildAccessPolicyTO());

        AttrReleasePolicyTO attrReleasePolicyTO = createPolicy(PolicyType.ATTR_RELEASE, buildAttrReleasePolicyTO());

        oidcrpto.setAuthPolicy(authPolicyTO.getKey());
        oidcrpto.setAccessPolicy(accessPolicyTO.getKey());
        oidcrpto.setAttrReleasePolicy(attrReleasePolicyTO.getKey());

        oidcrpto = createClientApp(ClientAppType.OIDCRP, oidcrpto);

        WAClientApp waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getClientAppId(), null);
        assertNotNull(waClientApp);
        assertTrue(waClientApp.getReleaseAttrs().isEmpty());

        // add items to the authentication module
        addItems();
        try {
            waClientApp = WA_CLIENT_APP_SERVICE.read(oidcrpto.getClientAppId(), null);
            assertNotNull(waClientApp);
            assertFalse(waClientApp.getReleaseAttrs().isEmpty());
            assertEquals("username", waClientApp.getReleaseAttrs().get("uid"));
            assertEquals("fullname", waClientApp.getReleaseAttrs().get("cn"));
        } finally {
            removeItems();
        }
    }

    private void addItems() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read(AUTH_MODULE);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        authModuleTO.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        authModuleTO.getItems().add(fullnameMapping);

        AUTH_MODULE_SERVICE.update(authModuleTO);

        authModuleTO = AUTH_MODULE_SERVICE.read(AUTH_MODULE);
        assertFalse(authModuleTO.getItems().isEmpty());
    }

    private void removeItems() {
        AuthModuleTO authModuleTO = AUTH_MODULE_SERVICE.read(AUTH_MODULE);
        authModuleTO.getItems().clear();

        AUTH_MODULE_SERVICE.update(authModuleTO);

        authModuleTO = AUTH_MODULE_SERVICE.read(AUTH_MODULE);
        assertTrue(authModuleTO.getItems().isEmpty());
    }
}
