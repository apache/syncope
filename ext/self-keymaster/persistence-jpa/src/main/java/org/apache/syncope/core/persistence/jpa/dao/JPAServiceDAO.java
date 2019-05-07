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
import org.apache.syncope.common.keymaster.client.api.NetworkService;
import org.apache.syncope.core.persistence.api.dao.ServiceDAO;
import org.apache.syncope.core.persistence.api.entity.Service;
import org.apache.syncope.core.persistence.jpa.entity.JPAService;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAServiceDAO extends AbstractDAO<Service> implements ServiceDAO {

    @Transactional(readOnly = true)
    @Override
    public List<Service> findAll(final NetworkService.Type serviceType) {
        TypedQuery<Service> query = entityManager().createQuery(
                "SELECT e FROM " + JPAService.class.getSimpleName() + " e WHERE e.type=:serviceType", Service.class);
        query.setParameter("serviceType", serviceType);
        return query.getResultList();
    }

    @Override
    public Service save(final Service service) {
        return entityManager().merge(service);
    }

    @Override
    public void delete(final Service service) {
        entityManager().remove(service);
    }
}
