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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements {@link ServiceOps} via Apache Curator / Zookeeper via Curator's {@link ServiceDiscovery}.
 */
public class ZookeeperServiceDiscoveryOps implements ServiceOps, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceOps.class);

    private static final String SERVICE_PATH = "/services";

    private final Map<NetworkService.Type, ServiceProvider<Void>> providers = new ConcurrentHashMap<>();

    @Autowired
    private CuratorFramework client;

    private ServiceDiscovery<Void> discovery;

    @Override
    public void afterPropertiesSet() throws Exception {
        discovery = ServiceDiscoveryBuilder.builder(Void.class).
                client(client).
                basePath(SERVICE_PATH).
                build();
        discovery.start();
    }

    private ServiceProvider<Void> getProvider(final NetworkService.Type type) {
        return providers.computeIfAbsent(type, t -> {
            try {
                ServiceProvider<Void> provider = discovery.
                        serviceProviderBuilder().
                        serviceName(t.name()).build();
                provider.start();
                return provider;
            } catch (KeymasterException e) {
                throw e;
            } catch (Exception e) {
                throw new KeymasterException("While preparing ServiceProvider for " + type, e);
            }
        });
    }

    @Override
    public void register(final NetworkService service) {
        try {
            unregister(service);

            ServiceInstance<Void> instance = ServiceInstance.<Void>builder().
                    name(service.getType().name()).
                    address(service.getAddress()).
                    build();
            discovery.registerService(instance);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("While registering {}", service, e);
            throw new KeymasterException(e);
        }
    }

    @Override
    public void unregister(final NetworkService service) {
        try {
            discovery.queryForInstances(service.getType().name()).stream().
                    filter(instance -> instance.getName().equals(service.getType().name())
                    && instance.getAddress().equals(service.getAddress())).findFirst().
                    ifPresent(instance -> {
                        try {
                            discovery.unregisterService(instance);
                        } catch (Exception e) {
                            LOG.error("While deregistering {}", service, e);
                            throw new KeymasterException(e);
                        }
                    });
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("While registering {}", service, e);
            throw new KeymasterException(e);
        }
    }

    private static NetworkService toNetworkService(
        final NetworkService.Type serviceType,
        final ServiceInstance<Void> serviceInstance) {

        NetworkService ns = new NetworkService();
        ns.setType(serviceType);
        ns.setAddress(serviceInstance.getAddress());
        return ns;
    }

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        try {
            return discovery.queryForInstances(serviceType.name()).stream().
                    map(serviceInstance -> toNetworkService(serviceType, serviceInstance)).
                    collect(Collectors.toList());
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        ServiceInstance<Void> serviceInstance = null;
        try {
            if (!discovery.queryForInstances(serviceType.name()).isEmpty()) {
                serviceInstance = getProvider(serviceType).getInstance();
            }
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }

        if (serviceInstance == null) {
            throw new KeymasterException("No services found for " + serviceType);
        }
        return toNetworkService(serviceType, serviceInstance);
    }
}
