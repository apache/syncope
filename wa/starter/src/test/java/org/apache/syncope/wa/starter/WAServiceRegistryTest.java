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
package org.apache.syncope.wa.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.services.AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria;
import org.apereo.cas.services.ChainingAttributeReleasePolicy;
import org.apereo.cas.services.DenyAllAttributeReleasePolicy;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegisteredServiceDelegatedAuthenticationPolicy;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.util.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;

public class WAServiceRegistryTest extends AbstractTest {

    private static OIDCRPClientAppTO buildOIDCRP() {
        OIDCRPClientAppTO oidcrpTO = new OIDCRPClientAppTO();
        oidcrpTO.setName("ExampleRP_" + getUUIDString());
        oidcrpTO.setClientAppId(RandomUtils.nextLong());
        oidcrpTO.setDescription("Example OIDC RP application");
        oidcrpTO.setClientId("clientId_" + getUUIDString());
        oidcrpTO.setClientSecret("secret");
        oidcrpTO.getRedirectUris().addAll(List.of("uri1", "uri2"));
        oidcrpTO.setSubjectType(OIDCSubjectType.PUBLIC);
        oidcrpTO.getSupportedGrantTypes().add(OIDCGrantType.password);
        oidcrpTO.getSupportedResponseTypes().add(OIDCResponseType.CODE);

        return oidcrpTO;
    }

    private static SAML2SPClientAppTO buildSAML2SP() {
        SAML2SPClientAppTO saml2spto = new SAML2SPClientAppTO();
        saml2spto.setName("ExampleSAML2SP_" + getUUIDString());
        saml2spto.setClientAppId(RandomUtils.nextLong());
        saml2spto.setDescription("Example SAML 2.0 service provider");
        saml2spto.setEntityId("SAML2SPEntityId_" + getUUIDString());
        saml2spto.setMetadataLocation("file:./test.xml");
        saml2spto.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        saml2spto.setEncryptionOptional(true);
        saml2spto.setEncryptAssertions(true);

        return saml2spto;
    }

    private static void addPolicies(
            final WAClientApp waClientApp,
            final boolean withReleaseAttributes,
            final boolean withAttrReleasePolicy) {

        DefaultAuthPolicyConf authPolicyConf = new DefaultAuthPolicyConf();
        authPolicyConf.setTryAll(true);
        authPolicyConf.getAuthModules().add("TestAuthModule");
        AuthPolicyTO authPolicy = new AuthPolicyTO();
        authPolicy.setConf(authPolicyConf);

        waClientApp.setAuthPolicy(authPolicy);

        if (withReleaseAttributes) {
            waClientApp.getReleaseAttrs().putAll(Map.of("uid", "username", "cn", "fullname"));
        }

        AccessPolicyTO accessPolicy = new AccessPolicyTO();
        accessPolicy.setEnabled(true);
        DefaultAccessPolicyConf accessPolicyConf = new DefaultAccessPolicyConf();
        accessPolicyConf.getRequiredAttrs().add(new Attr.Builder("cn").values("admin", "Admin", "TheAdmin").build());
        accessPolicy.setConf(accessPolicyConf);
        waClientApp.setAccessPolicy(accessPolicy);

        if (withAttrReleasePolicy) {
            DefaultAttrReleasePolicyConf attrReleasePolicyConf = new DefaultAttrReleasePolicyConf();
            attrReleasePolicyConf.getAllowedAttrs().add("cn");
            attrReleasePolicyConf.getPrincipalAttrRepoConf().getAttrRepos().add("TestAttrRepo");

            AttrReleasePolicyTO attrReleasePolicy = new AttrReleasePolicyTO();
            attrReleasePolicy.setConf(attrReleasePolicyConf);
            waClientApp.setAttrReleasePolicy(attrReleasePolicy);
        }
    }

    @Autowired
    private WARestClient waRestClient;

    @Autowired
    private ServicesManager servicesManager;

    @Autowired
    private ObjectProvider<AuthenticationEventExecutionPlan> authenticationEventExecutionPlan;

    @Autowired
    private ContextRefresher contextRefresher;

    @Test
    public void addClientApp() {
        // 1. start with no client apps defined on mocked Core
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        assertNotNull(syncopeClient);

        SyncopeCoreTestingServer.CLIENT_APPS.clear();

        WAClientAppService service = syncopeClient.getService(WAClientAppService.class);
        assertTrue(service.list().isEmpty());

        // 2. add one client app on mocked Core, nothing on WA yet
        WAClientApp waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildOIDCRP());
        Long clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addPolicies(waClientApp, false, false);

        SyncopeCoreTestingServer.CLIENT_APPS.add(waClientApp);
        List<WAClientApp> apps = service.list();
        assertEquals(1, apps.size());

        assertNotNull(servicesManager.findServiceBy(clientAppId));

        // 3. trigger client app refresh
        Collection<RegisteredService> load = servicesManager.load();
        assertEquals(3, load.size());

        // 4. look for the service created above
        RegisteredService found = servicesManager.findServiceBy(clientAppId);
        assertNotNull(found);
        assertTrue(found instanceof OidcRegisteredService);
        OidcRegisteredService oidc = OidcRegisteredService.class.cast(found);
        OIDCRPClientAppTO oidcrpto = OIDCRPClientAppTO.class.cast(waClientApp.getClientAppTO());
        assertEquals("uri1|uri2", oidc.getServiceId());
        assertEquals(oidcrpto.getClientId(), oidc.getClientId());
        assertEquals(oidcrpto.getClientSecret(), oidc.getClientSecret());
        assertTrue(oidc.getAuthenticationPolicy().getRequiredAuthenticationHandlers().contains("TestAuthModule"));
        assertTrue(((AnyAuthenticationHandlerRegisteredServiceAuthenticationPolicyCriteria) oidc.
                getAuthenticationPolicy().getCriteria()).isTryAll());
        assertTrue(oidc.getAttributeReleasePolicy() instanceof ChainingAttributeReleasePolicy);

        // 5. more client with different attributes 
        waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildSAML2SP());
        clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addPolicies(waClientApp, false, true);

        SyncopeCoreTestingServer.CLIENT_APPS.add(waClientApp);
        apps = service.list();
        assertEquals(2, apps.size());

        load = servicesManager.load();
        assertEquals(4, load.size());

        found = servicesManager.findServiceBy(clientAppId);
        assertTrue(found instanceof SamlRegisteredService);
        SamlRegisteredService saml = SamlRegisteredService.class.cast(found);
        SAML2SPClientAppTO samlspto = SAML2SPClientAppTO.class.cast(waClientApp.getClientAppTO());
        assertEquals(samlspto.getMetadataLocation(), saml.getMetadataLocation());
        assertEquals(samlspto.getEntityId(), saml.getServiceId());
        assertTrue(saml.getAuthenticationPolicy().getRequiredAuthenticationHandlers().contains("TestAuthModule"));
        assertNotNull(found.getAccessStrategy());
        assertTrue(saml.getAttributeReleasePolicy() instanceof ReturnAllowedAttributeReleasePolicy);

        waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildSAML2SP());
        clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addPolicies(waClientApp, false, false);

        SyncopeCoreTestingServer.CLIENT_APPS.add(waClientApp);
        apps = service.list();
        assertEquals(3, apps.size());

        load = servicesManager.load();
        assertEquals(5, load.size());

        found = servicesManager.findServiceBy(clientAppId);
        assertTrue(found.getAttributeReleasePolicy() instanceof DenyAllAttributeReleasePolicy);
    }

    @Test
    public void delegatedAuthentication() {
        // 1. start with 1 client app and 1 auth module defined on mocked Core
        SyncopeClient syncopeClient = waRestClient.getSyncopeClient();
        assertNotNull(syncopeClient);

        OIDCAuthModuleConf oidcAuthModuleConf = new OIDCAuthModuleConf();
        oidcAuthModuleConf.setClientId("clientId");
        oidcAuthModuleConf.setClientSecret("clientSecret");
        AuthModuleTO authModuleTO = new AuthModuleTO();
        authModuleTO.setKey("keycloack");
        authModuleTO.setConf(oidcAuthModuleConf);

        SyncopeCoreTestingServer.AUTH_MODULES.clear();
        SyncopeCoreTestingServer.AUTH_MODULES.add(authModuleTO);
        AuthModuleService authModuleService = syncopeClient.getService(AuthModuleService.class);
        assertEquals(1, authModuleService.list().size());

        SyncopeCoreTestingServer.CLIENT_APPS.clear();
        WAClientAppService waClientAppService = syncopeClient.getService(WAClientAppService.class);
        assertTrue(waClientAppService.list().isEmpty());

        WAClientApp waClientApp = new WAClientApp();
        waClientApp.setClientAppTO(buildOIDCRP());
        waClientApp.getAuthModules().add(0, authModuleTO);
        Long clientAppId = waClientApp.getClientAppTO().getClientAppId();
        addPolicies(waClientApp, false, false);
        DefaultAuthPolicyConf authPolicyConf = (DefaultAuthPolicyConf) waClientApp.getAuthPolicy().getConf();
        authPolicyConf.getAuthModules().clear();
        authPolicyConf.getAuthModules().add(authModuleTO.getKey());
        SyncopeCoreTestingServer.CLIENT_APPS.add(waClientApp);

        // 2. trigger refresh
        int before = authenticationEventExecutionPlan.getObject().getAuthenticationHandlers().size();

        contextRefresher.refresh();

        int after = authenticationEventExecutionPlan.getObject().getAuthenticationHandlers().size();
        assertEquals(before + 1, after);

        // 3. check service
        RegisteredService service = servicesManager.findServiceBy(clientAppId);
        assertNotNull(service);

        assertEquals(
                Set.of("DelegatedClientAuthenticationHandler"),
                service.getAuthenticationPolicy().getRequiredAuthenticationHandlers());

        RegisteredServiceAccessStrategy accessStrategy = service.getAccessStrategy();
        assertNotNull(accessStrategy);
        RegisteredServiceDelegatedAuthenticationPolicy delegatedAuthPolicy =
                accessStrategy.getDelegatedAuthenticationPolicy();
        assertNotNull(delegatedAuthPolicy);
        assertEquals(1, delegatedAuthPolicy.getAllowedProviders().size());
        assertTrue(delegatedAuthPolicy.getAllowedProviders().contains(authModuleTO.getKey()));
    }
}
