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

import java.util.Collection;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jSchema;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class DerSchemaRepoExtImpl extends AbstractSchemaRepoExt implements DerSchemaRepoExt {

    protected final ExternalResourceDAO resourceDAO;

    public DerSchemaRepoExtImpl(
            final ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient, nodeValidator);
        this.resourceDAO = resourceDAO;
    }

    @Override
    public List<? extends DerSchema> findByAnyTypeClasses(final Collection<AnyTypeClass> anyTypeClasses) {
        return findByAnyTypeClasses(anyTypeClasses, Neo4jDerSchema.class, DerSchema.class);
    }

    @Override
    public DerSchema save(final DerSchema schema) {
        ((Neo4jSchema) schema).map2json();
        DerSchema saved = neo4jTemplate.save(nodeValidator.validate(schema));
        ((Neo4jSchema) saved).postSave();
        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jDerSchema.class).ifPresent(schema -> {
            resourceDAO.deleteMapping(key);

            if (schema.getAnyTypeClass() != null) {
                schema.getAnyTypeClass().getDerSchemas().remove(schema);
            }

            neo4jTemplate.deleteById(key, Neo4jDerSchema.class);
        });
    }
}
