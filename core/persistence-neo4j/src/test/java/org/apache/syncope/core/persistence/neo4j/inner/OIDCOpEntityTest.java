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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class OIDCOpEntityTest extends AbstractTest {

    @Autowired
    private OIDCOpEntityDAO oidcOpEntityDAO;

    @Test
    public void save() throws Exception {
        OIDCOpEntity oidcOpEntity = entityFactory.newEntity(OIDCOpEntity.class);

        RSAKey jwk = new RSAKeyGenerator(2048).
                keyUse(KeyUse.SIGNATURE).
                keyID(UUID.randomUUID().toString()).
                generate();
        oidcOpEntity.setJWKS(new JWKSet(jwk).toString());

        oidcOpEntity.getCustomScopes().put("scope1", Set.of("claim1", "claim2"));
        oidcOpEntity.getCustomScopes().put("scope2", Set.of("claim1", "claim3", "claim4"));

        oidcOpEntity = oidcOpEntityDAO.save(oidcOpEntity);
        assertNotNull(oidcOpEntity);
        assertNotNull(oidcOpEntity.getKey());
        assertEquals(2, oidcOpEntity.getCustomScopes().size());
        assertEquals(Set.of("claim1", "claim2"), oidcOpEntity.getCustomScopes().get("scope1"));
    }
}
