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
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.entity.JPARemediation;
import org.springframework.stereotype.Repository;

@Repository
public class JPARemediationDAO extends AbstractDAO<Remediation> implements RemediationDAO {

    @Override
    public Remediation find(final String key) {
        return entityManager().find(JPARemediation.class, key);
    }

    @Override
    public List<Remediation> findByPullTask(final PullTask pullTask) {
        TypedQuery<Remediation> query = entityManager().createQuery(
                "SELECT e FROM " + JPARemediation.class.getSimpleName() + " e WHERE e.pullTask=:pullTask",
                Remediation.class);
        query.setParameter("pullTask", pullTask);
        return query.getResultList();
    }

    @Override
    public List<Remediation> findAll() {
        TypedQuery<Remediation> query = entityManager().createQuery(
                "SELECT e FROM " + JPARemediation.class.getSimpleName() + " e ", Remediation.class);
        return query.getResultList();
    }

    @Override
    public Remediation save(final Remediation remediation) {
        return entityManager().merge(remediation);
    }

    @Override
    public void delete(final Remediation remediation) {
        entityManager().remove(remediation);
    }

    @Override
    public void delete(final String key) {
        Remediation remediation = find(key);
        if (remediation == null) {
            return;
        }

        delete(remediation);
    }

}
