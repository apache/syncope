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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.provisioning.api.data.OIDCOpEntityDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OIDCOpEntityDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private OIDCOpEntityDataBinder binder;

    @Autowired
    private OIDCOpEntityDAO oidcOpEntityDAO;

    @Autowired
    private WAConfigDAO waConfigDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    void providedJWKSAndScopes() {
        OIDCOpEntity entity = entityFactory.newEntity(OIDCOpEntity.class);

        OIDCOpEntityTO entityTO = new OIDCOpEntityTO();
        entityTO.setJWKS(Base64.getEncoder().encodeToString("custom-jwks".getBytes(StandardCharsets.UTF_8)));
        entityTO.getCustomScopes().put("scope", Set.of("first", "second"));

        binder.update(entity, entityTO);
        oidcOpEntityDAO.save(entity);
        entityManager.flush();

        OIDCOpEntityTO actual = binder.getOIDCOpEntityTO(oidcOpEntityDAO.get().orElseThrow());
        assertNotNull(actual.getKey());
        assertEquals(entityTO.getJWKS(), actual.getJWKS());
        assertEquals(Set.of("first", "second"), actual.getCustomScopes().get("scope"));
    }

    @Test
    void generatedJWKS() {
        OIDCOpEntity entity = entityFactory.newEntity(OIDCOpEntity.class);

        OIDCOpEntityTO entityTO = new OIDCOpEntityTO();
        entityTO.getCustomScopes().put("scope", Set.of("value"));

        binder.update(entity, entityTO);
        oidcOpEntityDAO.save(entity);
        entityManager.flush();

        OIDCOpEntity actual = oidcOpEntityDAO.get().orElseThrow();
        assertNotNull(actual.getJWKS());
        assertTrue(actual.getJWKS().length > 0);
        assertEquals(Set.of("value"), actual.getCustomScopes().get("scope"));

        OIDCOpEntityTO actualTO = binder.getOIDCOpEntityTO(actual);
        assertFalse(actualTO.getJWKS().isEmpty());

        assertEquals(List.of("syncope"),
                waConfigDAO.findById("cas.authn.oidc.jwks.core.jwks-key-id").orElseThrow().getValues());
        assertEquals(List.of("RSA"),
                waConfigDAO.findById("cas.authn.oidc.jwks.core.jwks-type").orElseThrow().getValues());
        assertEquals(List.of("2048"),
                waConfigDAO.findById("cas.authn.oidc.jwks.core.jwks-key-size").orElseThrow().getValues());
    }

    @Test
    void generateJWKSVariants() {
        assertFalse(binder.generateJWKS("ec384", "EC", 384).length == 0);
        assertFalse(binder.generateJWKS("ec512", "ec", 512).length == 0);
        assertFalse(binder.generateJWKS("ecDefault", "EC", 256).length == 0);
        assertFalse(binder.generateJWKS("rsa", "RSA", 1024).length == 0);
        assertFalse(binder.generateJWKS("rsaDefault", "unknown", 512).length == 0);
    }
}
