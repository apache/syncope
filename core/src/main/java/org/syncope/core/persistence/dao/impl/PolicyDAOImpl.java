/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.dao.PolicyDAO;

@Repository
public class PolicyDAOImpl extends AbstractDAOImpl
        implements PolicyDAO {

    @Override
    public Policy find(final Long id) {
        return entityManager.find(Policy.class, id);
    }

    @Override
    public Policy getPasswordPolicy() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Policy e WHERE type='PASSWORD'");

        final List<Policy> policies = query.getResultList();

        if (policies != null && !policies.isEmpty()) {
            return policies.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Policy getAccountPolicy() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Policy e WHERE type='ACCOUNT'");

        final List<Policy> policies = query.getResultList();

        if (policies != null && !policies.isEmpty()) {
            return policies.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<Policy> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM Policy e");
        return query.getResultList();
    }

    @Override
    public Policy save(final Policy policy) {
        return entityManager.merge(policy);
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));
    }
}
