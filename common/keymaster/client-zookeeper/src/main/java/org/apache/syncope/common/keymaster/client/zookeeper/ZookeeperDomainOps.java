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
package org.apache.syncope.common.keymaster.client.zookeeper;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements {@link DomainOps} via Apache Curator / Zookeeper.
 */
public class ZookeeperDomainOps implements DomainOps, InitializingBean {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainOps.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String DOMAIN_PATH = "/domains";

    protected static String buildDomainPath(final String... parts) {
        String prefix = DOMAIN_PATH;
        String suffix = StringUtils.EMPTY;
        if (parts != null && parts.length > 0) {
            suffix = '/' + String.join("/", parts);
        }
        return prefix + suffix;
    }

    @Autowired
    protected CuratorFramework client;

    @Autowired(required = false)
    protected DomainWatcher watcher;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (watcher == null) {
            LOG.warn("No watcher found, aborting");
            return;
        }

        if (client.checkExists().forPath(buildDomainPath()) == null) {
            client.create().creatingParentContainersIfNeeded().forPath(buildDomainPath());
        }

        CuratorCache cache = CuratorCache.build(client, buildDomainPath());
        cache.listenable().addListener((type, oldData, newData) -> {
            switch (type) {
                case NODE_CREATED -> {
                    LOG.debug("Domain {} added", newData.getPath());
                    try {
                        Domain domain = MAPPER.readValue(newData.getData(), Domain.class);

                        LOG.info("Domain {} created", domain.getKey());
                        watcher.added(domain);
                    } catch (IOException e) {
                        LOG.debug("Could not parse {}", new String(newData.getData()), e);
                    }
                }

                case NODE_CHANGED ->
                    LOG.debug("Domain {} updated", newData.getPath());

                case NODE_DELETED -> {
                    LOG.debug("Domain {} removed", newData.getPath());
                    watcher.removed(StringUtils.substringAfter(newData.getPath(), DOMAIN_PATH + '/'));
                }

                default ->
                    LOG.debug("Event {} received with data {}", type, newData);
            }
        });
        cache.start();
    }

    @Override
    public List<Domain> list() {
        try {
            if (client.checkExists().forPath(buildDomainPath()) == null) {
                client.create().creatingParentContainersIfNeeded().forPath(buildDomainPath());
            }

            List<Domain> list = new ArrayList<>();
            for (String child : client.getChildren().forPath(buildDomainPath())) {
                list.add(MAPPER.readValue(client.getData().forPath(buildDomainPath(child)), Domain.class));
            }

            return list;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public Domain read(final String key) {
        try {
            return MAPPER.readValue(client.getData().forPath(buildDomainPath(key)), Domain.class);
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void create(final Domain domain) {
        if (Objects.equals(domain.getKey(), SyncopeConstants.MASTER_DOMAIN)) {
            throw new KeymasterException("Cannot create domain " + SyncopeConstants.MASTER_DOMAIN);
        }

        try {
            if (client.checkExists().forPath(buildDomainPath(domain.getKey())) != null) {
                throw new KeymasterException("Domain " + domain.getKey() + " existing");
            }

            client.create().creatingParentContainersIfNeeded().
                    forPath(buildDomainPath(domain.getKey()), MAPPER.writeValueAsBytes(domain));
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void deployed(final String key) {
        try {
            Domain domain = read(key);

            domain.setDeployed(true);
            client.setData().forPath(buildDomainPath(key), MAPPER.writeValueAsBytes(domain));
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void changeAdminPassword(
            final String key, final String password, final CipherAlgorithm cipherAlgorithm) {

        try {
            Domain domain = read(key);

            domain.setAdminPassword(password);
            domain.setAdminCipherAlgorithm(cipherAlgorithm);
            client.setData().forPath(buildDomainPath(key), MAPPER.writeValueAsBytes(domain));
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void adjustPoolSize(final String key, final int poolMaxActive, final int poolMinIdle) {
        try {
            Domain domain = read(key);
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

            client.setData().forPath(buildDomainPath(key), MAPPER.writeValueAsBytes(domain));
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void delete(final String key) {
        try {
            client.delete().forPath(buildDomainPath(key));
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }
}
