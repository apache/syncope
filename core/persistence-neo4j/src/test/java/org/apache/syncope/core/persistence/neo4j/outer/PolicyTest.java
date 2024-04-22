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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.inner.AbstractClientAppTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PolicyTest extends AbstractClientAppTest {

    @Autowired
    private OIDCRPClientAppDAO oidcRelyingPartyDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void authPolicyCanBeNull() {
        Realm realm = realmSearchDAO.findByFullPath("/odd").orElseThrow();

        // Create new client app and assign policy
        OIDCRPClientApp rp = entityFactory.newEntity(OIDCRPClientApp.class);
        rp.setName("OIDC");
        rp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        rp.setDescription("This is a sample OIDC RP");
        rp.setClientId(UUID.randomUUID().toString());
        rp.setClientSecret("secret");
        rp.setRealm(realm);

        assertDoesNotThrow(() -> oidcRelyingPartyDAO.save(rp));
    }

    @Test
    public void removePolicyFromRealm() {
        AuthPolicy authPolicy = buildAndSaveAuthPolicy();
        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        AttrReleasePolicy attrPolicy = buildAndSaveAttrRelPolicy();
        TicketExpirationPolicy ticketExpirationPolicy = buildAndSaveTicketExpirationPolicy();

        Realm realm = realmDAO.getRoot();
        realm.setAuthPolicy(authPolicy);
        realm.setAccessPolicy(accessPolicy);
        realm.setAttrReleasePolicy(attrPolicy);
        realm.setTicketExpirationPolicy(ticketExpirationPolicy);

        realm = realmDAO.save(realm);
        assertNotNull(realm.getAuthPolicy());
        assertNotNull(realm.getAccessPolicy());
        assertNotNull(realm.getAttrReleasePolicy());

        policyDAO.delete(authPolicy);
        policyDAO.delete(accessPolicy);
        policyDAO.delete(attrPolicy);
        policyDAO.delete(ticketExpirationPolicy);

        realm = realmDAO.getRoot();
        assertNull(realm.getAuthPolicy());
        assertNull(realm.getAccessPolicy());
        assertNull(realm.getAttrReleasePolicy());
        assertNull(realm.getTicketExpirationPolicy());
    }

    @Test
    public void removePolicyFromApps() {
        // Create new policy
        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        AuthPolicy authPolicy = buildAndSaveAuthPolicy();
        TicketExpirationPolicy ticketExpirationPolicy = buildAndSaveTicketExpirationPolicy();

        // Create new client app and assign policy
        OIDCRPClientApp rp = entityFactory.newEntity(OIDCRPClientApp.class);
        rp.setName("OIDC");
        rp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        rp.setDescription("This is a sample OIDC RP");
        rp.setClientId(UUID.randomUUID().toString());
        rp.setClientSecret("secret");
        rp.setAccessPolicy(accessPolicy);
        rp.setAuthPolicy(authPolicy);
        rp.setTicketExpirationPolicy(ticketExpirationPolicy);

        rp = oidcRelyingPartyDAO.save(rp);
        assertNotNull(rp.getAuthPolicy());
        assertNotNull(rp.getAccessPolicy());
        assertNotNull(rp.getTicketExpirationPolicy());

        policyDAO.delete(accessPolicy);
        policyDAO.delete(authPolicy);
        policyDAO.delete(ticketExpirationPolicy);

        rp = oidcRelyingPartyDAO.findById(rp.getKey()).orElseThrow();
        assertNull(rp.getAuthPolicy());
        assertNull(rp.getAccessPolicy());
        assertNull(rp.getTicketExpirationPolicy());
    }

    @Test
    public void addAndRemoveAccountPolicyRule() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdRepoImplementationType.ACCOUNT_RULE);
        implementation.setBody("TestAccountPolicyRule");
        implementation = implementationDAO.save(implementation);

        AccountPolicy policy = policyDAO.findById("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", AccountPolicy.class).
                orElseThrow();
        assertEquals(1, policy.getRules().size());

        policy.add(implementation);
        policyDAO.save(policy);

        policy = policyDAO.findById("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", AccountPolicy.class).orElseThrow();
        assertEquals(2, policy.getRules().size());

        policy.getRules().clear();
        policy = policyDAO.save(policy);
        assertTrue(policy.getRules().isEmpty());

        policy = policyDAO.findById("20ab5a8c-4b0c-432c-b957-f7fb9784d9f7", AccountPolicy.class).orElseThrow();
        assertTrue(policy.getRules().isEmpty());
    }
}
