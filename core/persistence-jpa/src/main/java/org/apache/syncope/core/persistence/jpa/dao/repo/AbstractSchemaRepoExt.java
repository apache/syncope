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
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Schema;

public abstract class AbstractSchemaRepoExt {

    protected final EntityManager entityManager;

    protected AbstractSchemaRepoExt(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected <S extends Schema> List<S> findByAnyTypeClasses(
            final Collection<AnyTypeClass> anyTypeClasses, final String entity, final Class<S> reference) {

        if (anyTypeClasses.isEmpty()) {
            return List.of();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(entity).append(" e WHERE ");

        List<Object> parameters = new ArrayList<>();
        List<String> clauses = new ArrayList<>();
        int clausesIdx = 0;
        for (AnyTypeClass anyTypeClass : anyTypeClasses) {
            clauses.add("e.anyTypeClass.id=?" + (clausesIdx + 1));
            parameters.add(anyTypeClass.getKey());
            clausesIdx++;
        }
        queryString.append(String.join(" OR ", clauses));

        TypedQuery<S> query = entityManager.createQuery(queryString.toString(), reference);
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getResultList();
    }
}
