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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements {@link ServiceOps} via Apache Curator / Zookeeper.
 */
public class ZookeeperServiceOps implements ServiceOps {

    protected static final Logger LOG = LoggerFactory.getLogger(ServiceOps.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String SERVICE_PATH = "/services";

    @Autowired
    protected CuratorFramework client;

    protected static String buildServicePath(final NetworkService.Type serviceType, final String... parts) {
        String prefix = SERVICE_PATH + '/' + serviceType.name();
        String suffix = StringUtils.EMPTY;
        if (parts != null && parts.length > 0) {
            suffix = '/' + String.join("/", parts);
        }
        return prefix + suffix;
    }

    @Override
    public void register(final NetworkService service) {
        String id = UUID.randomUUID().toString();
        try {
            unregister(service);

            if (client.checkExists().forPath(buildServicePath(service.getType(), id)) == null) {
                client.create().creatingParentContainersIfNeeded().withMode(CreateMode.EPHEMERAL).
                        forPath(buildServicePath(service.getType(), id));
            }

            client.setData().forPath(
                    buildServicePath(service.getType(), id), MAPPER.writeValueAsBytes(service));
        } catch (Exception e) {
            LOG.error("While writing {}", buildServicePath(service.getType(), id), e);
            throw new KeymasterException(e);
        }
    }

    @Override
    public void unregister(final NetworkService service) {
        try {
            if (client.checkExists().forPath(buildServicePath(service.getType())) != null) {
                client.getChildren().forPath(buildServicePath(service.getType())).stream().
                        filter(child -> {
                            try {
                                return MAPPER.readValue(client.getData().forPath(
                                        buildServicePath(service.getType(), child)), NetworkService.class).
                                        equals(service);
                            } catch (Exception e) {
                                LOG.error("While deregistering {}", service, e);
                                throw new KeymasterException(e);
                            }
                        }).
                        findFirst().ifPresent(child -> {
                            try {
                                client.delete().forPath(buildServicePath(service.getType(), child));
                            } catch (Exception e) {
                                LOG.error("While deregistering {}", service, e);
                                throw new KeymasterException(e);
                            }
                        });
            }
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        try {
            if (client.checkExists().forPath(buildServicePath(serviceType)) == null) {
                client.create().creatingParentContainersIfNeeded().forPath(buildServicePath(serviceType));
            }

            List<NetworkService> list = new ArrayList<>();
            for (String child : client.getChildren().forPath(buildServicePath(serviceType))) {
                list.add(MAPPER.readValue(client.getData().forPath(buildServicePath(serviceType, child)),
                        NetworkService.class));
            }

            return list;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        List<NetworkService> list = list(serviceType);
        if (list.isEmpty()) {
            throw new KeymasterException("No registered services for type " + serviceType);
        }

        // always returns first instance, can be improved
        return list.getFirst();
    }
}
