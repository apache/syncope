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

    protected static NetworkService toNetworkService(
            final NetworkService.Type serviceType,
            final ServiceInstance<String> serviceInstance) {

        NetworkService ns = new NetworkService();
        ns.setType(serviceType);
        ns.setAddress(serviceInstance.getAddress());
        ns.setDomain(serviceInstance.getPayload());
        return ns;
    }

    private final Map<NetworkService.Type, ServiceProvider<String>> providers = new ConcurrentHashMap<>();

    @Autowired
    private CuratorFramework client;

    private ServiceDiscovery<String> discovery;

    @Override
    public void afterPropertiesSet() throws Exception {
        discovery = ServiceDiscoveryBuilder.builder(String.class).
                client(client).
                basePath(SERVICE_PATH).
                build();
        discovery.start();
    }

    private ServiceProvider<String> getProvider(final NetworkService.Type type) {
        return providers.computeIfAbsent(type, t -> {
            try {
                ServiceProvider<String> provider = discovery.
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

            ServiceInstance<String> instance = ServiceInstance.<String>builder().
                    name(service.getType().name()).
                    address(service.getAddress()).
                    payload(service.getDomain()).
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

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        try {
            return discovery.queryForInstances(serviceType.name()).stream().
                    map(serviceInstance -> toNetworkService(serviceType, serviceInstance)).
                    toList();
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType, final String domain) {
        try {
            return discovery.queryForInstances(serviceType.name()).stream().
                    filter(serviceInstance -> domain.equals(serviceInstance.getPayload())).
                    map(serviceInstance -> toNetworkService(serviceType, serviceInstance)).
                    toList();
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        ServiceInstance<String> serviceInstance = null;
        try {
            serviceInstance = discovery.queryForInstances(serviceType.name()).stream().
                    findFirst().
                    orElse(null);
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

    @Override
    public NetworkService get(final NetworkService.Type serviceType, final String domain) {
        ServiceInstance<String> serviceInstance = null;
        try {
            serviceInstance = discovery.queryForInstances(serviceType.name()).stream().
                    filter(s -> domain.equals(s.getPayload())).findFirst().
                    orElse(null);
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
