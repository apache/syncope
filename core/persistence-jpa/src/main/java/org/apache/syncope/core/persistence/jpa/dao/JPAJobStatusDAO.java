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
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.persistence.jpa.entity.JPAJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class JPAJobStatusDAO implements JobStatusDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(JobStatusDAO.class);

    protected final EntityManager entityManager;

    public JPAJobStatusDAO(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsById(final String key) {
        Query query = entityManager.createQuery(
                "SELECT COUNT(e) FROM " + JPAJobStatus.class.getSimpleName() + " e WHERE e.id = :key");
        query.setParameter("key", key);
        return ((Number) query.getSingleResult()).longValue() > 0;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends JobStatus> findById(final String key) {
        return Optional.ofNullable(entityManager.find(JPAJobStatus.class, key));
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        Query query = entityManager.createQuery(
                "SELECT COUNT(e) FROM " + JPAJobStatus.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).longValue();
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends JobStatus> findAll() {
        TypedQuery<JobStatus> query = entityManager.createQuery(
                "SELECT e FROM " + JPAJobStatus.class.getSimpleName() + " e", JobStatus.class);
        return query.getResultList();
    }

    @Override
    public <S extends JobStatus> S save(final S jobStatus) {
        return entityManager.merge(jobStatus);
    }

    @Override
    public void delete(final JobStatus jobStatus) {
        entityManager.remove(jobStatus);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public boolean lock(final String key) {
        if (existsById(key)) {
            return false;
        }

        try {
            JobStatus jobStatus = new JPAJobStatus();
            jobStatus.setKey(key);
            jobStatus.setStatus(JOB_FIRED_STATUS);
            save(jobStatus);

            return true;
        } catch (Exception e) {
            LOG.debug("Could not lock job {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(final String key) {
        deleteById(key);
    }
}
