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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
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

    protected final DomainHolder domainHolder;

    protected final PersistenceProperties persistenceProperties;

    protected final ResourceLoader resourceLoader;

    protected final DomainRegistry domainRegistry;

    public StartupDomainLoader(
            final DomainOps domainOps,
            final DomainHolder domainHolder,
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final DomainRegistry domainRegistry) {

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
        Map<String, Domain> keymasterDomains = domainOps.list().stream().
                collect(Collectors.toMap(Domain::getKey, Function.identity()));

        persistenceProperties.getDomain().stream().
                filter(d -> !domainHolder.getDomains().containsKey(d.getKey())).forEach(domainProps -> {

            if (keymasterDomains.containsKey(domainProps.getKey())) {
                LOG.info("Domain {} initialization", domainProps.getKey());

                domainRegistry.register(keymasterDomains.get(domainProps.getKey()));

                LOG.info("Domain {} successfully inited", domainProps.getKey());
            } else {
                Domain.Builder builder = new Domain.Builder(domainProps.getKey());

                builder.adminPassword(domainProps.getAdminPassword());
                builder.adminCipherAlgorithm(domainProps.getAdminCipherAlgorithm());

                builder.jdbcDriver(domainProps.getJdbcDriver());
                builder.jdbcURL(domainProps.getJdbcURL());
                builder.dbSchema(domainProps.getDbSchema());
                builder.dbUsername(domainProps.getDbUsername());
                builder.dbPassword(domainProps.getDbPassword());
                builder.databasePlatform(domainProps.getDatabasePlatform());
                builder.orm(domainProps.getOrm());
                builder.poolMaxActive(domainProps.getPoolMaxActive());
                builder.poolMinIdle(domainProps.getPoolMinIdle());
                builder.auditSql(domainProps.getAuditSql());

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
