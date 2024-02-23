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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.apache.syncope.core.persistence.jpa.entity.keymaster.JPANetworkService;
import org.springframework.transaction.annotation.Transactional;

public class NetworkServiceRepoExtImpl implements NetworkServiceRepoExt {

    protected final EntityManager entityManager;

    public NetworkServiceRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public List<NetworkServiceEntity> findAll(final NetworkService.Type serviceType) {
        TypedQuery<NetworkServiceEntity> query = entityManager.createQuery(
                "SELECT e FROM " + JPANetworkService.class.getSimpleName()
                + " e WHERE e.type=:serviceType", NetworkServiceEntity.class);
        query.setParameter("serviceType", serviceType);
        return query.getResultList();
    }

    @Override
    public void deleteAll(final NetworkService service) {
        Query query = entityManager.createQuery(
                "DELETE FROM " + JPANetworkService.class.getSimpleName()
                + " e WHERE e.type=:serviceType AND e.address=:address");
        query.setParameter("serviceType", service.getType());
        query.setParameter("address", service.getAddress());

        query.executeUpdate();
    }
}
