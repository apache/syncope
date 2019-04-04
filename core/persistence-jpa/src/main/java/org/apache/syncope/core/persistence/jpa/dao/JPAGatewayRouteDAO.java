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
import org.apache.syncope.core.persistence.api.dao.GatewayRouteDAO;
import org.apache.syncope.core.persistence.api.entity.GatewayRoute;
import org.apache.syncope.core.persistence.jpa.entity.JPAGatewayRoute;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGatewayRouteDAO extends AbstractDAO<GatewayRoute> implements GatewayRouteDAO {

    @Transactional(readOnly = true)
    @Override
    public GatewayRoute find(final String key) {
        return entityManager().find(JPAGatewayRoute.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<GatewayRoute> findAll() {
        TypedQuery<GatewayRoute> query = entityManager().createQuery(
                "SELECT e FROM " + JPAGatewayRoute.class.getSimpleName() + " e", GatewayRoute.class);

        return query.getResultList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public GatewayRoute save(final GatewayRoute report) {
        return entityManager().merge(report);
    }

    @Override
    public void delete(final String key) {
        GatewayRoute report = find(key);
        if (report == null) {
            return;
        }

        delete(report);
    }

    @Override
    public void delete(final GatewayRoute report) {
        entityManager().remove(report);
    }
}
