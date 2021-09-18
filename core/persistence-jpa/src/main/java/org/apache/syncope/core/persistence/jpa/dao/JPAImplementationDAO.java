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
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.spring.ImplementationManager;
import org.springframework.transaction.annotation.Transactional;

public class JPAImplementationDAO extends AbstractDAO<Implementation> implements ImplementationDAO {

    @Transactional(readOnly = true)
    @Override
    public Implementation find(final String key) {
        return entityManager().find(JPAImplementation.class, key);
    }

    @Override
    public List<Implementation> findByType(final String type) {
        TypedQuery<Implementation> query = entityManager().createQuery(
                "SELECT e FROM " + JPAImplementation.class.getSimpleName() + " e WHERE e.type=:type",
                Implementation.class);
        query.setParameter("type", type);
        return query.getResultList();
    }

    @Override
    public List<Implementation> findAll() {
        TypedQuery<Implementation> query = entityManager().createQuery(
                "SELECT e FROM " + JPAImplementation.class.getSimpleName() + " e", Implementation.class);
        return query.getResultList();
    }

    @Override
    public Implementation save(final Implementation implementation) {
        Implementation merged = entityManager().merge(implementation);

        ImplementationManager.purge(merged.getKey());

        return merged;
    }

    @Override
    public void delete(final String key) {
        Implementation implementation = find(key);
        if (implementation == null) {
            return;
        }

        entityManager().remove(implementation);
        ImplementationManager.purge(key);
    }

}
