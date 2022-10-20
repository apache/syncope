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
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.LiveSyncDAO;
import org.apache.syncope.core.persistence.api.entity.LiveSync;
import org.apache.syncope.core.persistence.jpa.entity.JPALiveSync;
import org.springframework.transaction.annotation.Transactional;

public class JPALiveSyncDAO extends AbstractDAO<LiveSync> implements LiveSyncDAO {

    @Transactional(readOnly = true)
    @Override
    public LiveSync find(final String key) {
        return entityManager().find(LiveSync.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public LiveSync findByEntityID(final String entityID) {
        TypedQuery<LiveSync> query = entityManager().createQuery(
                "SELECT e FROM " + JPALiveSync.class.getSimpleName()
                        + " e WHERE e.entityID = :entityID", LiveSync.class);
        query.setParameter("entityID", entityID);

        LiveSync result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No livesync found with entityID {}", entityID, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<LiveSync> findAll() {
        TypedQuery<LiveSync> query = entityManager().createQuery(
                "SELECT e FROM " + JPALiveSync.class.getSimpleName() + " e", LiveSync.class);
        return query.getResultList();
    }

    @Override
    public LiveSync save(final LiveSync livesync) {
        ((JPALiveSync) livesync).list2json();
        return entityManager().merge(livesync);
    }

    @Override
    public void delete(final String key) {
        LiveSync liveSync = find(key);
        if (liveSync != null) {
            entityManager().remove(key);
        }
    }
}
