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
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomUtils;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.SelfKeymasterEntityFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.NetworkServiceEntity;

public class NetworkServiceLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final NetworkServiceDAO serviceDAO;

    protected final SelfKeymasterEntityFactory entityFactory;

    public NetworkServiceLogic(final NetworkServiceDAO serviceDAO, final SelfKeymasterEntityFactory entityFactory) {
        this.serviceDAO = serviceDAO;
        this.entityFactory = entityFactory;
    }

    protected NetworkService toNetworkService(
            final NetworkService.Type serviceType,
            final NetworkServiceEntity service) {

        NetworkService ns = new NetworkService();
        ns.setType(serviceType);
        ns.setAddress(service.getAddress());
        return ns;
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    @Transactional(readOnly = true)
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        return serviceDAO.findAll(serviceType).stream().
                map(service -> toNetworkService(serviceType, service)).collect(Collectors.toList());
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    @Transactional(readOnly = true)
    public NetworkService get(final NetworkService.Type serviceType) {
        List<NetworkService> list = list(serviceType);
        if (list.isEmpty()) {
            throw new NotFoundException("No registered services for type " + serviceType);
        }

        return list.size() == 1
                ? list.get(0)
                : list.get(RandomUtils.nextInt(0, list.size()));
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    public void register(final NetworkService networkService) {
        unregister(networkService);

        NetworkServiceEntity service = entityFactory.newNetworkService();
        service.setType(networkService.getType());
        service.setAddress(networkService.getAddress());
        serviceDAO.save(service);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    public void unregister(final NetworkService networkService) {
        serviceDAO.findAll(networkService.getType()).stream().
                filter(service -> service.getAddress().equals(networkService.getAddress())).
                forEach(service -> serviceDAO.delete(service));
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        throw new UnsupportedOperationException();
    }
}
