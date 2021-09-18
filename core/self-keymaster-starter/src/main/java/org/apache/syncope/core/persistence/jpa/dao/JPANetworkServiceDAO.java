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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.persistence.jpa.entity.JPANetworkService;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.dao.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.NetworkServiceEntity;

public class JPANetworkServiceDAO extends AbstractDAO<NetworkServiceEntity> implements NetworkServiceDAO {

    @Transactional(readOnly = true)
    @Override
    public List<NetworkServiceEntity> findAll(final NetworkService.Type serviceType) {
        TypedQuery<NetworkServiceEntity> query = entityManager().createQuery(
                "SELECT e FROM " + JPANetworkService.class.getSimpleName()
                + " e WHERE e.type=:serviceType", NetworkServiceEntity.class);
        query.setParameter("serviceType", serviceType);
        return query.getResultList();
    }

    @Override
    public NetworkServiceEntity save(final NetworkServiceEntity service) {
        return entityManager().merge(service);
    }

    @Override
    public void delete(final NetworkServiceEntity service) {
        entityManager().remove(service);
    }
}
