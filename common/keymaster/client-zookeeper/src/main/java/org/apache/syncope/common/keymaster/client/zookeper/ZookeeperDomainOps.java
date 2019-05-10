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
package org.apache.syncope.common.keymaster.client.zookeper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.WatchMode;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements {@link DomainOps} via Apache Curator / Zookeeper.
 */
public class ZookeeperDomainOps implements DomainOps, InitializingBean {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(DomainOps.class);

    private static final String DOMAIN_PATH = "/domains";

    @Autowired
    private CuratorFramework client;

    @Autowired(required = false)
    private DomainWatcher watcher;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (watcher != null) {
            if (client.checkExists().forPath(buildDomainPath()) == null) {
                client.create().creatingParentContainersIfNeeded().forPath(buildDomainPath());
            }

            AsyncCuratorFramework.wrap(client).with(WatchMode.successOnly).watched().getChildren().
                    forPath(buildDomainPath()).event().thenAccept(event -> {
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    try {
                        List<String> children = client.getChildren().
                                forPath(event.getPath()).stream().collect(Collectors.toList());
                        watcher.update(children);
                    } catch (Exception e) {
                        LOG.error("Unexpected exception", e);
                    }
                }
            });
        }
    }

    private String buildDomainPath(final String... parts) {
        String prefix = DOMAIN_PATH;
        String suffix = StringUtils.EMPTY;
        if (parts != null && parts.length > 0) {
            suffix = "/" + String.join("/", parts);
        }
        return prefix + suffix;
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

            client.create().creatingParentContainersIfNeeded().forPath(buildDomainPath(domain.getKey()));
            client.setData().forPath(buildDomainPath(domain.getKey()), MAPPER.writeValueAsBytes(domain));
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
    public void adjustPoolSize(final String key, final int maxPoolSize, final int minIdle) {
        try {
            Domain domain = read(key);

            domain.setMaxPoolSize(maxPoolSize);
            domain.setMinIdle(minIdle);
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
