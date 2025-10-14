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
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
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

    @Override
    public boolean lock(final String key) {
        if (!UNKNOWN_STATUS.equals(get(key))) {
            LOG.debug("Job {} already locked", key);
            return false;
        }

        try {
            Query query = entityManager.createNativeQuery(
                    "INSERT INTO " + JPAJobStatus.TABLE + "(id, jobStatus) VALUES (?,?)");
            query.setParameter(1, key);
            query.setParameter(2, JOB_FIRED_STATUS);
            query.executeUpdate();

            LOG.debug("Job {} locked", key);
            return true;
        } catch (Exception e) {
            LOG.debug("Could not lock job {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(final String key) {
        Query query = entityManager.createNativeQuery("DELETE FROM " + JPAJobStatus.TABLE + " WHERE id=?");
        query.setParameter(1, key);
        query.executeUpdate();
        LOG.debug("Job {} unlocked", key);
    }

    @Override
    public void set(final String key, final String status) {
        Query query = entityManager.createNativeQuery("UPDATE " + JPAJobStatus.TABLE + " SET jobStatus=? WHERE id=?");
        query.setParameter(1, UNKNOWN_STATUS.equals(status) ? "Status " + UNKNOWN_STATUS : status);
        query.setParameter(2, key);
        query.executeUpdate();
    }

    @Transactional(readOnly = true)
    @Override
    public String get(final String key) {
        Query query = entityManager.createNativeQuery("SELECT jobStatus FROM " + JPAJobStatus.TABLE + " WHERE id=?");
        query.setParameter(1, key);

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.isEmpty() ? UNKNOWN_STATUS : result.getFirst().toString();
    }
}
