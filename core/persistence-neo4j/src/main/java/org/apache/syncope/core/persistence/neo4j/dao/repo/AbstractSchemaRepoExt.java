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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jVirSchema;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public abstract class AbstractSchemaRepoExt extends AbstractDAO {

    protected final NodeValidator nodeValidator;

    public AbstractSchemaRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    protected abstract <S extends Schema> Cache<EntityCacheKey, S> cache();

    protected <S extends Schema> List<S> findByIdLike(
            final String label,
            final Class<? extends Neo4jSchema> domainType,
            final String keyword) {

        return toList(neo4jClient.query(
                "MATCH (n:" + label + ") WHERE n.id =~ $keyword RETURN n.id").
                bindAll(Map.of("keyword", keyword.replace("%", ".*"))).fetch().all(),
                "n.id",
                domainType,
                cache());
    }

    protected <S extends Schema> List<S> findByAnyTypeClasses(
            final Collection<AnyTypeClass> anyTypeClasses,
            final Class<? extends Neo4jSchema> domainType,
            final Class<S> reference) {

        if (anyTypeClasses.isEmpty()) {
            return List.of();
        }

        String label = null;
        String relationship = null;
        if (domainType.equals(Neo4jPlainSchema.class)) {
            label = Neo4jPlainSchema.NODE;
            relationship = Neo4jAnyTypeClass.ANY_TYPE_CLASS_PLAIN_REL;
        } else if (domainType.equals(Neo4jDerSchema.class)) {
            label = Neo4jDerSchema.NODE;
            relationship = Neo4jAnyTypeClass.ANY_TYPE_CLASS_DER_REL;
        } else if (domainType.equals(Neo4jVirSchema.class)) {
            label = Neo4jVirSchema.NODE;
            relationship = Neo4jAnyTypeClass.ANY_TYPE_CLASS_VIR_REL;
        }

        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        int clausesIdx = 0;
        for (AnyTypeClass anyTypeClass : anyTypeClasses) {
            clauses.add("a.id = $id" + (clausesIdx + 1));
            parameters.put("id" + (clausesIdx + 1), anyTypeClass.getKey());
            clausesIdx++;
        }

        return toList(neo4jClient.query(
                "MATCH (n:" + label + ")-[:" + relationship + "]-(a:" + Neo4jAnyTypeClass.NODE + ") "
                + "WHERE (" + String.join(" OR ", clauses) + ") "
                + "RETURN n.id").bindAll(parameters).fetch().all(),
                "n.id",
                domainType,
                cache());
    }
}
