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

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.jpa.inner.AbstractClientAppTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.PersistenceException;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.core.persistence.api.entity.auth.OIDCRPClientApp;

@Transactional("Master")
public class PolicyTest extends AbstractClientAppTest {

    @Autowired
    private OIDCRPDAO oidcRelyingPartyDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Test
    public void policyCannotBeRemovedForApps() {
        // Create new policy
        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        AuthPolicy authPolicy = buildAndSaveAuthPolicy();

        // Create new client app and assign policy
        OIDCRPClientApp rp = entityFactory.newEntity(OIDCRPClientApp.class);
        rp.setName("OIDC");
        rp.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        rp.setDescription("This is a sample OIDC RP");
        rp.setClientId(UUID.randomUUID().toString());
        rp.setClientSecret("secret");
        rp.setAccessPolicy(accessPolicy);
        rp.setAuthPolicy(authPolicy);

        rp = oidcRelyingPartyDAO.save(rp);
        assertNotNull(rp);

        assertThrows(PersistenceException.class, () -> {
            this.policyDAO.delete(accessPolicy);
            entityManager().flush();
        });
        assertThrows(PersistenceException.class, () -> {
            this.policyDAO.delete(authPolicy);
            entityManager().flush();
        });
    }

    @Test
    public void authPolicyCanBeNull() {
        Realm realm = realmDAO.findByFullPath("/odd");
        assertNotNull(realm);

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
            entityManager().flush();
        });
    }

    @Test
    public void policyForRealmsCanBeRemoved() {
        AuthPolicy authPolicy = buildAndSaveAuthPolicy();
        AccessPolicy accessPolicy = buildAndSaveAccessPolicy();
        AttrReleasePolicy attrPolicy = buildAndSaveAttrRelPolicy();

        Realm realm = realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM);
        assertNotNull(realm);
        realm.setAuthPolicy(authPolicy);
        realm.setAccessPolicy(accessPolicy);
        realm.setAttrReleasePolicy(attrPolicy);
        realm = realmDAO.save(realm);

        assertNotNull(realm);

        this.policyDAO.delete(authPolicy);
        this.policyDAO.delete(accessPolicy);
        this.policyDAO.delete(attrPolicy);
        entityManager().flush();
        assertNull(this.policyDAO.find(authPolicy.getKey()));
        assertNull(this.policyDAO.find(accessPolicy.getKey()));
        assertNull(this.policyDAO.find(attrPolicy.getKey()));
    }
}
