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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jJobStatus;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class Neo4jJobStatusDAO implements JobStatusDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(JobStatusDAO.class);

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public Neo4jJobStatusDAO(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean existsById(final String key) {
        return neo4jTemplate.existsById(key, Neo4jJobStatus.class);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends JobStatus> findById(final String key) {
        return neo4jTemplate.findById(key, Neo4jJobStatus.class);
    }

    @Transactional(readOnly = true)
    @Override
    public long count() {
        return neo4jTemplate.count(Neo4jJobStatus.class);
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends JobStatus> findAll() {
        return neo4jTemplate.findAll(Neo4jJobStatus.class);
    }

    @Override
    public <S extends JobStatus> S save(final S jobStatus) {
        return neo4jTemplate.save(nodeValidator.validate(jobStatus));
    }

    @Override
    public void delete(final JobStatus jobStatus) {
        neo4jTemplate.deleteById(jobStatus.getKey(), Neo4jJobStatus.class);
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
            JobStatus jobStatus = new Neo4jJobStatus();
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
