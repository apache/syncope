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
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.jpa.entity.JPADomain;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPADomainDAO extends AbstractDAO<Domain> implements DomainDAO {

    @Transactional(readOnly = true)
    @Override
    public Domain find(final String key) {
        return entityManager().find(JPADomain.class, key);
    }

    @Override
    public List<Domain> findAll() {
        TypedQuery<Domain> query = entityManager().createQuery(
                "SELECT e FROM " + JPADomain.class.getSimpleName() + " e ", Domain.class);
        return query.getResultList();
    }

    @Override
    public Domain save(final Domain anyTypeClass) {
        return entityManager().merge(anyTypeClass);
    }

    @Override
    public void delete(final String key) {
        Domain domain = find(key);
        if (domain == null) {
            return;
        }

        entityManager().remove(domain);
    }
}
