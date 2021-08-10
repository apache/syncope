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
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.entity.DomainEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPADomain;
import org.springframework.stereotype.Repository;

@Repository
public class JPADomainDAO extends AbstractDAO<DomainEntity> implements DomainDAO {

    @Override
    public List<DomainEntity> findAll() {
        TypedQuery<DomainEntity> query = entityManager().createQuery(
                "SELECT e FROM " + JPADomain.class.getSimpleName() + " e", DomainEntity.class);
        return query.getResultList();
    }

    @Override
    public DomainEntity find(final String key) {
        return entityManager().find(JPADomain.class, key);
    }

    @Override
    public DomainEntity save(final DomainEntity domain) {
        return entityManager().merge(domain);
    }

    @Override
    public void delete(final String key) {
        DomainEntity domain = find(key);
        if (domain != null) {
            entityManager().remove(domain);
        }
    }
}
