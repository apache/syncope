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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.DomainDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.DomainEntity;
import org.junit.jupiter.api.Test;

class DomainLogicTest {

    private final DomainDAO domainDAO = mock(DomainDAO.class);

    private final EntityFactory entityFactory = mock(EntityFactory.class);

    private final DomainWatcher domainWatcher = mock(DomainWatcher.class);

    private final DomainLogic logic = new DomainLogic(domainDAO, entityFactory, domainWatcher);

    @Test
    void list() {
        DomainEntity entity = mock(DomainEntity.class);
        Domain domain = mock(Domain.class);
        when(entity.get()).thenReturn(domain);
        doReturn(List.of(entity)).when(domainDAO).findAll();

        List<Domain> result = logic.list();

        assertEquals(List.of(domain), result);
        verify(domainDAO).findAll();
    }

    @Test
    void readWhenNotFound() {
        doReturn(Optional.<DomainEntity>empty()).when(domainDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.read("key"));
    }

    @Test
    void readWhenFound() {
        DomainEntity entity = mock(DomainEntity.class);
        Domain domain = mock(Domain.class);
        when(entity.get()).thenReturn(domain);
        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById("key");

        assertSame(domain, logic.read("key"));
    }

    @Test
    void createMasterDomainRejected() {
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn(SyncopeConstants.MASTER_DOMAIN);

        assertThrows(
                org.apache.syncope.common.keymaster.client.api.KeymasterException.class,
                () -> logic.create(domain));
    }

    @Test
    void createDuplicateRejected() {
        JPADomain domain = new JPADomain.Builder("domain").build();
        when(domainDAO.existsById("domain")).thenReturn(true);

        assertThrows(DuplicateException.class, () -> logic.create(domain));
    }

    @Test
    void createSucceeds() {
        JPADomain domain = new JPADomain.Builder("domain").build();
        DomainEntity entity = mock(DomainEntity.class);
        Domain created = mock(Domain.class);

        when(domainDAO.existsById("domain")).thenReturn(false);
        when(entityFactory.newEntity(eq(DomainEntity.class))).thenReturn(entity);
        when(domainDAO.save(entity)).thenReturn(entity);
        when(entity.get()).thenReturn(created);

        Domain result = logic.create(domain);

        assertSame(created, result);
        verify(entity).setKey("domain");
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
        verify(domainWatcher).added(domain);
    }

    @Test
    void deployedSetsDeployed() {
        String key = "domain";
        DomainEntity entity = mock(DomainEntity.class);
        Domain domain = mock(Domain.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.deployed(key);

        verify(domain).setDeployed(true);
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void deployedWhenNotFound() {
        doReturn(Optional.<DomainEntity>empty()).when(domainDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.deployed("key"));
    }

    @Test
    void changeAdminPassword() {
        String key = "domain";
        String password = "new-password";
        CipherAlgorithm algorithm = CipherAlgorithm.SHA512;

        DomainEntity entity = mock(DomainEntity.class);
        Domain domain = mock(Domain.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.changeAdminPassword(key, password, algorithm);

        verify(domain).setAdminPassword(password);
        verify(domain).setAdminCipherAlgorithm(algorithm);
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void changeAdminPasswordWhenNotFound() {
        doReturn(Optional.<DomainEntity>empty()).when(domainDAO).findById("key");

        assertThrows(NotFoundException.class,
                () -> logic.changeAdminPassword("key", "password", CipherAlgorithm.SHA512));
    }

    @Test
    void setAdminMfaSecret() {
        String key = "domain";
        String secret = "secret";

        DomainEntity entity = mock(DomainEntity.class);
        Domain domain = mock(Domain.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.setAdminMfaSecret(key, secret);

        verify(domain).setAdminMfaSecret(secret);
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void setAdminMfaSecretWhenNotFound() {
        doReturn(Optional.<DomainEntity>empty()).when(domainDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.setAdminMfaSecret("key", "secret"));
    }

    @Test
    void adjustPoolSizeForJPADomain() {
        String key = "domain";
        JPADomain domain = new JPADomain.Builder(key).build();
        DomainEntity entity = mock(DomainEntity.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.adjustPoolSize(key, 20, 5);

        assertEquals(20, domain.getPoolMaxActive());
        assertEquals(5, domain.getPoolMinIdle());
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void adjustPoolSizeForNeo4jDomain() {
        String key = "domain";
        Neo4jDomain domain = new Neo4jDomain.Builder(key).build();
        DomainEntity entity = mock(DomainEntity.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.adjustPoolSize(key, 50, 5);

        assertEquals(50, domain.getMaxConnectionPoolSize());
        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void adjustPoolSizeForUnknownDomainType() {
        String key = "domain";
        Domain domain = mock(Domain.class);
        DomainEntity entity = mock(DomainEntity.class);

        doReturn(Optional.<DomainEntity>of(entity)).when(domainDAO).findById(key);
        when(entity.get()).thenReturn(domain);

        logic.adjustPoolSize(key, 20, 5);

        verify(entity).set(domain);
        verify(domainDAO).save(entity);
    }

    @Test
    void adjustPoolSizeWhenNotFound() {
        doReturn(Optional.<DomainEntity>empty()).when(domainDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.adjustPoolSize("key", 20, 5));
    }

    @Test
    void delete() {
        String key = "domain";

        logic.delete(key);

        verify(domainDAO).deleteById(key);
        verify(domainWatcher).removed(key);
    }

    @Test
    void resolveReferenceThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> logic.resolveReference(null, new Object[0]));
    }

    @SuppressWarnings("unused")
    private static void unused() {
        assertNotNull(SyncopeConstants.MASTER_DOMAIN);
    }
}
