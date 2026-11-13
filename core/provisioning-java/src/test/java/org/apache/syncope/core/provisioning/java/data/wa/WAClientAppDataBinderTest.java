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
package org.apache.syncope.core.provisioning.java.data.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.to.CASSPClientAppTO;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.apache.syncope.core.provisioning.java.data.AbstractDataBinderTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WAClientAppDataBinderTest extends AbstractDataBinderTest {

    private static final String TWO_KEY = "0679e069-7355-4b20-bd11-a5a0a5453c7c";

    @Autowired
    private WAClientAppDataBinder binder;

    @Autowired
    private ClientAppDataBinder clientAppDataBinder;

    @Autowired
    private CASSPClientAppDAO casspClientAppDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private EntityFactory entityFactory;

    protected AuthPolicy buildAuthPolicy(final String... authModules) {
        AuthPolicy authPolicy = entityFactory.newEntity(AuthPolicy.class);
        authPolicy.setName("wa auth policy");
        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.getAuthModules().addAll(List.of(authModules));
        authPolicy.setConf(conf);
        return policyDAO.save(authPolicy);
    }

    protected AccessPolicy buildAccessPolicy() {
        AccessPolicy accessPolicy = entityFactory.newEntity(AccessPolicy.class);
        accessPolicy.setName("wa access policy");
        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setEnabled(true);
        conf.setSsoEnabled(true);
        accessPolicy.setConf(conf);
        return policyDAO.save(accessPolicy);
    }

    protected AttrReleasePolicy buildAttrReleasePolicy() {
        AttrReleasePolicy attrReleasePolicy = entityFactory.newEntity(AttrReleasePolicy.class);
        attrReleasePolicy.setName("wa attr release policy");
        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getAllowedAttrs().add("cn");
        attrReleasePolicy.setConf(conf);
        return policyDAO.save(attrReleasePolicy);
    }

    protected TicketExpirationPolicy buildTicketExpirationPolicy() {
        TicketExpirationPolicy ticketExpirationPolicy = entityFactory.newEntity(TicketExpirationPolicy.class);
        ticketExpirationPolicy.setName("wa ticket expiration policy");
        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(600);
        conf.setTgtConf(tgtConf);
        ticketExpirationPolicy.setConf(conf);
        return policyDAO.save(ticketExpirationPolicy);
    }

    protected CASSPClientAppTO casspTO(final String name, final long clientAppId) {
        CASSPClientAppTO clientAppTO = new CASSPClientAppTO();
        clientAppTO.setName(name);
        clientAppTO.setClientAppId(clientAppId);
        clientAppTO.setServiceId("https://" + name + ".example.com");
        clientAppTO.setRealm("/even/two");
        return clientAppTO;
    }

    @Test
    void explicitPolicies() {
        AuthPolicy authPolicy = buildAuthPolicy("DefaultLDAPAuthModule", "not-existing");
        AccessPolicy accessPolicy = buildAccessPolicy();
        AttrReleasePolicy attrReleasePolicy = buildAttrReleasePolicy();
        TicketExpirationPolicy ticketExpirationPolicy = buildTicketExpirationPolicy();

        CASSPClientAppTO clientAppTO = casspTO("explicit", 1L);
        clientAppTO.setAuthPolicy(authPolicy.getKey());
        clientAppTO.setAccessPolicy(accessPolicy.getKey());
        clientAppTO.setAttrReleasePolicy(attrReleasePolicy.getKey());
        clientAppTO.setTicketExpirationPolicy(ticketExpirationPolicy.getKey());
        CASSPClientApp clientApp = casspClientAppDAO.save(clientAppDataBinder.create(clientAppTO));
        entityManager.flush();

        WAClientApp waClientApp = binder.getWAClientApp(clientApp);
        assertNotNull(waClientApp.getClientAppTO());
        assertEquals(clientApp.getKey(), waClientApp.getClientAppTO().getKey());

        assertNotNull(waClientApp.getAuthPolicy());
        assertEquals(authPolicy.getKey(), waClientApp.getAuthPolicy().getKey());
        assertEquals(1, waClientApp.getAuthModules().size());
        assertEquals("DefaultLDAPAuthModule", waClientApp.getAuthModules().getFirst().getKey());

        assertNotNull(waClientApp.getAccessPolicy());
        assertEquals(accessPolicy.getKey(), waClientApp.getAccessPolicy().getKey());
        assertNotNull(waClientApp.getAttrReleasePolicy());
        assertEquals(attrReleasePolicy.getKey(), waClientApp.getAttrReleasePolicy().getKey());
        assertNotNull(waClientApp.getTicketExpirationPolicy());
        assertEquals(ticketExpirationPolicy.getKey(), waClientApp.getTicketExpirationPolicy().getKey());
    }

    @Test
    void realmPolicies() {
        AuthPolicy authPolicy = buildAuthPolicy("DefaultJDBCAuthModule");
        AccessPolicy accessPolicy = buildAccessPolicy();
        AttrReleasePolicy attrReleasePolicy = buildAttrReleasePolicy();
        TicketExpirationPolicy ticketExpirationPolicy = buildTicketExpirationPolicy();

        Realm realm = realmDAO.findById(TWO_KEY).orElseThrow();
        realm.setAuthPolicy(authPolicy);
        realm.setAccessPolicy(accessPolicy);
        realm.setAttrReleasePolicy(attrReleasePolicy);
        realm.setTicketExpirationPolicy(ticketExpirationPolicy);
        realmDAO.save(realm);
        entityManager.flush();

        CASSPClientApp clientApp = casspClientAppDAO.save(
                clientAppDataBinder.create(casspTO("from-realm", 2L)));
        entityManager.flush();

        WAClientApp waClientApp = binder.getWAClientApp(clientApp);
        assertNotNull(waClientApp.getAuthPolicy());
        assertEquals(authPolicy.getKey(), waClientApp.getAuthPolicy().getKey());
        assertEquals(1, waClientApp.getAuthModules().size());
        assertEquals("DefaultJDBCAuthModule", waClientApp.getAuthModules().getFirst().getKey());

        assertNotNull(waClientApp.getAccessPolicy());
        assertEquals(accessPolicy.getKey(), waClientApp.getAccessPolicy().getKey());
        assertNotNull(waClientApp.getAttrReleasePolicy());
        assertEquals(attrReleasePolicy.getKey(), waClientApp.getAttrReleasePolicy().getKey());
        assertNotNull(waClientApp.getTicketExpirationPolicy());
        assertEquals(ticketExpirationPolicy.getKey(), waClientApp.getTicketExpirationPolicy().getKey());
    }

    @Test
    void noPolicies() {
        CASSPClientAppTO clientAppTO = casspTO("no-policies", 3L);
        clientAppTO.setRealm(null);
        CASSPClientApp clientApp = casspClientAppDAO.save(clientAppDataBinder.create(clientAppTO));
        entityManager.flush();

        WAClientApp waClientApp = binder.getWAClientApp(clientApp);
        assertNotNull(waClientApp.getClientAppTO());
        assertEquals(clientApp.getKey(), waClientApp.getClientAppTO().getKey());
        assertNull(waClientApp.getAuthPolicy());
        assertTrue(waClientApp.getAuthModules().isEmpty());
        assertNull(waClientApp.getAccessPolicy());
        assertNull(waClientApp.getAttrReleasePolicy());
        assertNull(waClientApp.getTicketExpirationPolicy());
    }
}
