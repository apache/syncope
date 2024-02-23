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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.jpa.inner.AbstractClientAppTest;
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

        assertDoesNotThrow(() -> {
            oidcRelyingPartyDAO.save(rp);
            entityManager.flush();
        });
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
        entityManager.flush();

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
        entityManager.flush();

        rp = oidcRelyingPartyDAO.findById(rp.getKey()).orElseThrow();
        assertNull(rp.getAuthPolicy());
        assertNull(rp.getAccessPolicy());
        assertNull(rp.getTicketExpirationPolicy());
    }
}
