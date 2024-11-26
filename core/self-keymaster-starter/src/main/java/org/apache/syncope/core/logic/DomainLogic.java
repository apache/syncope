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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.DomainDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.DomainEntity;
import org.springframework.security.access.prepost.PreAuthorize;

public class DomainLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final DomainDAO domainDAO;

    protected final EntityFactory entityFactory;

    protected final DomainWatcher domainWatcher;

    public DomainLogic(
            final DomainDAO domainDAO,
            final EntityFactory entityFactory,
            final DomainWatcher domainWatcher) {

        this.domainDAO = domainDAO;
        this.entityFactory = entityFactory;
        this.domainWatcher = domainWatcher;
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public List<Domain> list() {
        return domainDAO.findAll().stream().map(DomainEntity::get).toList();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public Domain read(final String key) {
        DomainEntity domain = domainDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Domain " + key));

        return domain.get();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public Domain create(final Domain domain) {
        if (Objects.equals(domain.getKey(), SyncopeConstants.MASTER_DOMAIN)) {
            throw new KeymasterException("Cannot create domain " + SyncopeConstants.MASTER_DOMAIN);
        }

        if (domainDAO.existsById(domain.getKey())) {
            throw new DuplicateException("Domain " + domain.getKey() + " already existing");
        }

        DomainEntity domainEntity = entityFactory.newEntity(DomainEntity.class);
        domainEntity.setKey(domain.getKey());
        domainEntity.set(domain);
        domainEntity = domainDAO.save(domainEntity);

        domainWatcher.added(domain);

        return domainEntity.get();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void deployed(final String key) {
        DomainEntity domain = domainDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Domain " + key));

        Domain domainObj = domain.get();
        domainObj.setDeployed(true);
        domain.set(domainObj);
        domainDAO.save(domain);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void changeAdminPassword(final String key, final String password, final CipherAlgorithm cipherAlgorithm) {
        DomainEntity domain = domainDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Domain " + key));

        Domain domainObj = domain.get();
        domainObj.setAdminPassword(password);
        domainObj.setAdminCipherAlgorithm(cipherAlgorithm);
        domain.set(domainObj);
        domainDAO.save(domain);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void adjustPoolSize(final String key, final int poolMaxActive, final int poolMinIdle) {
        DomainEntity domainEntity = domainDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Domain " + key));

        Domain domain = domainEntity.get();
        switch (domain) {
            case JPADomain jpaDomain -> {
                jpaDomain.setPoolMaxActive(poolMaxActive);
                jpaDomain.setPoolMinIdle(poolMinIdle);
            }

            case Neo4jDomain neo4jDomain ->
                neo4jDomain.setMaxConnectionPoolSize(poolMaxActive);

            default -> {
            }
        }
        domainEntity.set(domain);

        domainDAO.save(domainEntity);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void delete(final String key) {
        domainDAO.deleteById(key);

        domainWatcher.removed(key);
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) {
        throw new UnsupportedOperationException();
    }
}
