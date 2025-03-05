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
package org.apache.syncope.core.persistence.jpa;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;

public class StartupDomainLoader implements SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(StartupDomainLoader.class);

    protected final DomainOps domainOps;

    protected final DomainHolder<?> domainHolder;

    protected final PersistenceProperties persistenceProperties;

    protected final ResourceLoader resourceLoader;

    protected final DomainRegistry<JPADomain> domainRegistry;

    public StartupDomainLoader(
            final DomainOps domainOps,
            final DomainHolder<?> domainHolder,
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final DomainRegistry<JPADomain> domainRegistry) {

        this.domainOps = domainOps;
        this.domainHolder = domainHolder;
        this.persistenceProperties = persistenceProperties;
        this.resourceLoader = resourceLoader;
        this.domainRegistry = domainRegistry;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void load() {
        Map<String, JPADomain> keymasterDomains = domainOps.list().stream().
                collect(Collectors.toMap(Domain::getKey, JPADomain.class::cast));

        persistenceProperties.getDomain().stream().
                filter(d -> !SyncopeConstants.MASTER_DOMAIN.equals(d.getKey())
                && !domainHolder.getDomains().containsKey(d.getKey())).forEach(domainProps -> {

            if (keymasterDomains.containsKey(domainProps.getKey())) {
                LOG.info("Domain {} initialization", domainProps.getKey());

                domainRegistry.register(keymasterDomains.get(domainProps.getKey()));

                LOG.info("Domain {} successfully inited", domainProps.getKey());
            } else {
                JPADomain.Builder builder = new JPADomain.Builder(domainProps.getKey()).
                        adminPassword(domainProps.getAdminPassword()).
                        adminCipherAlgorithm(domainProps.getAdminCipherAlgorithm()).
                        jdbcDriver(domainProps.getJdbcDriver()).
                        jdbcURL(domainProps.getJdbcURL()).
                        dbSchema(domainProps.getDbSchema()).
                        dbUsername(domainProps.getDbUsername()).
                        dbPassword(domainProps.getDbPassword()).
                        databasePlatform(domainProps.getDatabasePlatform()).
                        orm(domainProps.getOrm()).
                        poolMaxActive(domainProps.getPoolMaxActive()).
                        poolMinIdle(domainProps.getPoolMinIdle());

                try {
                    builder.content(IOUtils.toString(
                            resourceLoader.getResource(domainProps.getContent()).getInputStream()));
                } catch (IOException e) {
                    LOG.error("While loading {}", domainProps.getContent(), e);
                }

                try {
                    builder.keymasterConfParams(IOUtils.toString(
                            resourceLoader.getResource(domainProps.getKeymasterConfParams()).getInputStream()));
                } catch (IOException e) {
                    LOG.error("While loading {}", domainProps.getKeymasterConfParams(), e);
                }

                domainOps.create(builder.build());
            }
        });
    }
}
