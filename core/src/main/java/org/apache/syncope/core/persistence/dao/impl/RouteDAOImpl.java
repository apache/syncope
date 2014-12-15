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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RouteDAOImpl extends AbstractDAOImpl implements RouteDAO {

    @Override
    public CamelRoute find(final Long id) {
        return entityManager.find(CamelRoute.class, id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CamelRoute> findAll() {
        TypedQuery<CamelRoute> query = entityManager.createQuery("SELECT e FROM " + CamelRoute.class.getSimpleName() + " e", CamelRoute.class);
        return query.getResultList();
    }

    @Override
    public CamelRoute save(final CamelRoute route) throws InvalidEntityException {
        return entityManager.merge(route);
    }

    @Override
    public void delete(Long id) {
        CamelRoute route = find(id);
        if (route != null) {
            entityManager.remove(route);
        }

    }

}
