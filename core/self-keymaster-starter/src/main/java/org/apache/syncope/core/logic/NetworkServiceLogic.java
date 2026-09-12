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
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class NetworkServiceLogic extends AbstractTransactionalLogic<EntityTO> {

    protected static NetworkService toNetworkService(final NetworkServiceEntity service) {
        NetworkService ns = new NetworkService();
        ns.setType(service.getType());
        ns.setAddress(service.getAddress());
        ns.setDomain(service.getDomain());
        return ns;
    }

    protected final NetworkServiceDAO serviceDAO;

    protected final EntityFactory entityFactory;

    public NetworkServiceLogic(final NetworkServiceDAO serviceDAO, final EntityFactory entityFactory) {
        this.serviceDAO = serviceDAO;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        return serviceDAO.findAll(serviceType).stream().map(NetworkServiceLogic::toNetworkService).toList();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public List<NetworkService> list(final NetworkService.Type serviceType, final String domain) {
        return serviceDAO.findAll(serviceType, domain).stream().map(NetworkServiceLogic::toNetworkService).toList();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public NetworkService get(final NetworkService.Type serviceType) {
        List<NetworkService> list = list(serviceType);
        if (list.isEmpty()) {
            throw new NotFoundException("No registered services for type " + serviceType);
        }

        return list.size() == 1
                ? list.getFirst()
                : list.get(SecureRandomUtils.generateRandomInt(0, list.size()));
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public NetworkService get(final NetworkService.Type serviceType, final String domain) {
        List<NetworkService> list = list(serviceType, domain);
        if (list.isEmpty()) {
            throw new NotFoundException("No registered services for type " + serviceType + " and domain " + domain);
        }

        return list.size() == 1
                ? list.getFirst()
                : list.get(SecureRandomUtils.generateRandomInt(0, list.size()));
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void register(final NetworkService networkService) {
        if (serviceDAO.findAll(networkService.getType()).stream().
                noneMatch(s -> s.getAddress().equals(networkService.getAddress()))) {

            NetworkServiceEntity service = entityFactory.newEntity(NetworkServiceEntity.class);
            service.setType(networkService.getType());
            service.setAddress(networkService.getAddress());
            service.setDomain(networkService.getDomain());
            serviceDAO.save(service);
        }
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void unregister(final NetworkService networkService) {
        serviceDAO.deleteAll(networkService);
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) {
        throw new UnsupportedOperationException();
    }
}
