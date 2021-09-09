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
package org.apache.syncope.core.persistence.jpa.dao.auth;

import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.persistence.jpa.dao.AbstractDAO;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAWAConfigEntry;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.TypedQuery;
import java.util.List;

public class JPAWAConfigDAO extends AbstractDAO<WAConfigEntry> implements WAConfigDAO {

    @Transactional(readOnly = true)
    @Override
    public WAConfigEntry find(final String key) {
        return entityManager().find(JPAWAConfigEntry.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<WAConfigEntry> findAll() {
        TypedQuery<WAConfigEntry> query = entityManager().createQuery(
                "SELECT e FROM " + JPAWAConfigEntry.class.getSimpleName() + " e", WAConfigEntry.class);
        return query.getResultList();
    }

    @Override
    public WAConfigEntry save(final WAConfigEntry configEntry) {
        return entityManager().merge(configEntry);
    }

    @Override
    public void delete(final String key) {
        WAConfigEntry entry = find(key);
        if (entry == null) {
            return;
        }
        entityManager().remove(entry);
    }
}
