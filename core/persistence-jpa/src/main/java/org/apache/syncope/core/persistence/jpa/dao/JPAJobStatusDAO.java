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

import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.persistence.jpa.entity.JPAJobStatus;
import org.springframework.transaction.annotation.Transactional;

public class JPAJobStatusDAO extends AbstractDAO<JobStatus> implements JobStatusDAO {

    @Transactional(readOnly = true)
    @Override
    public JobStatus find(final String key) {
        return entityManager().find(JPAJobStatus.class, key);
    }

    @Transactional
    @Override
    public JobStatus save(final JobStatus jobStatus) {
        return entityManager().merge(jobStatus);
    }

    @Transactional
    @Override
    public void delete(final String key) {
        JobStatus jobStatus = find(key);
        if (jobStatus != null) {
            entityManager().remove(jobStatus);
        }
    }
}
