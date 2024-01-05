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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.OffsetDateTime;
import org.apache.syncope.core.persistence.jpa.entity.JPABatch;

public class BatchRepoExtImpl implements BatchRepoExt {

    protected final EntityManager entityManager;

    public BatchRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public long deleteExpired() {
        Query query = entityManager.createQuery(
                "DELETE FROM " + JPABatch.class.getSimpleName() + " e WHERE e.expiryTime < :now");
        query.setParameter("now", OffsetDateTime.now());
        return query.executeUpdate();
    }
}
