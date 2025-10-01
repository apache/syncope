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

    @Override
    public boolean lock(final String key) {
        if (neo4jTemplate.existsById(key, Neo4jJobStatus.class)) {
            LOG.debug("Job {} already locked", key);
            return false;
        }

        try {
            JobStatus jobStatus = new Neo4jJobStatus();
            jobStatus.setKey(key);
            jobStatus.setStatus(JOB_FIRED_STATUS);
            neo4jTemplate.save(nodeValidator.validate(jobStatus));

            return true;
        } catch (Exception e) {
            LOG.debug("Could not lock job {}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(final String key) {
        neo4jTemplate.deleteById(key, Neo4jJobStatus.class);
    }

    @Override
    public void set(final String key, final String status) {
        if (neo4jTemplate.existsById(key, Neo4jJobStatus.class)) {
            JobStatus jobStatus = new Neo4jJobStatus();
            jobStatus.setKey(key);
            jobStatus.setStatus(status);
            neo4jTemplate.save(nodeValidator.validate(jobStatus));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public String get(final String key) {
        return neo4jTemplate.findById(key, Neo4jJobStatus.class).map(JobStatus::getStatus).orElse(UNKNOWN_STATUS);
    }
}
