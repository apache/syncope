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
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAttrRepo;
import org.springframework.transaction.annotation.Transactional;

public class JPAAttrRepoDAO extends AbstractDAO<AttrRepo> implements AttrRepoDAO {

    @Transactional(readOnly = true)
    @Override
    public AttrRepo find(final String key) {
        return entityManager().find(JPAAttrRepo.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AttrRepo> findAll() {
        TypedQuery<AttrRepo> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAttrRepo.class.getSimpleName() + " e", AttrRepo.class);
        return query.getResultList();
    }

    @Override
    public AttrRepo save(final AttrRepo attrRepo) {
        ((JPAAttrRepo) attrRepo).list2json();
        return entityManager().merge(attrRepo);
    }

    @Override
    public void delete(final String key) {
        AttrRepo attrRepo = find(key);
        if (attrRepo == null) {
            return;
        }

        delete(attrRepo);
    }

    @Override
    public void delete(final AttrRepo attrRepo) {
        entityManager().remove(attrRepo);
    }
}
