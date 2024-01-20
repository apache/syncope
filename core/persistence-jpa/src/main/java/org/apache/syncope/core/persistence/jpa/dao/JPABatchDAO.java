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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.jpa.entity.JPABatch;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class JPABatchDAO implements BatchDAO {

    protected final EntityManager entityManager;

    public JPABatchDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsById(final String key) {
        return findById(key).isPresent();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Batch> findById(final String key) {
        return Optional.ofNullable(entityManager.find(JPABatch.class, key));
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        Query query = entityManager.createQuery(
                "SELECT COUNT(e) FROM " + JPABatch.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends Batch> findAll() {
        TypedQuery<Batch> query = entityManager.createQuery(
                "SELECT e FROM " + JPABatch.class.getSimpleName() + " e", Batch.class);
        return query.getResultList();
    }

    @Override
    public <S extends Batch> S save(final S batch) {
        return entityManager.merge(batch);
    }

    @Override
    public void delete(final Batch batch) {
        entityManager.remove(batch);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public long deleteExpired() {
        Query query = entityManager.createQuery(
                "DELETE FROM " + JPABatch.class.getSimpleName() + " e WHERE e.expiryTime < :now");
        query.setParameter("now", OffsetDateTime.now());
        return query.executeUpdate();
    }
}
