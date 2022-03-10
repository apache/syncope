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

import java.time.OffsetDateTime;
import javax.persistence.Query;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.jpa.entity.JPABatch;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class JPABatchDAO extends AbstractDAO<Batch> implements BatchDAO {

    @Transactional(readOnly = true)
    @Override
    public Batch find(final String key) {
        return entityManager().find(JPABatch.class, key);
    }

    @Override
    public Batch save(final Batch batch) {
        return entityManager().merge(batch);
    }

    @Override
    public void delete(final String key) {
        Batch batch = find(key);
        if (batch == null) {
            return;
        }

        entityManager().remove(batch);
    }

    @Override
    public int deleteExpired() {
        Query query = entityManager().createQuery(
                "DELETE FROM " + JPABatch.class.getSimpleName() + " e WHERE e.expiryTime < :now");
        query.setParameter("now", OffsetDateTime.now());
        return query.executeUpdate();
    }
}
