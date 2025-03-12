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
package org.springframework.data.neo4j.repository.support;

import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;

public class SyncopeNeo4jRepository<T, ID> extends SimpleNeo4jRepository<T, ID> {

    protected static final CypherGenerator CYPHER_GENERATOR = CypherGenerator.INSTANCE;

    protected final Neo4jOperations neo4jOperations;

    protected final Neo4jEntityInformation<T, ID> entityInformation;

    protected final Neo4jPersistentEntity<T> entityMetaData;

    private NodeValidator nodeValidator;

    public SyncopeNeo4jRepository(
            final Neo4jOperations neo4jOperations,
            final Neo4jEntityInformation<T, ID> entityInformation) {

        super(neo4jOperations, entityInformation);
        this.neo4jOperations = neo4jOperations;
        this.entityInformation = entityInformation;
        this.entityMetaData = entityInformation.getEntityMetaData();
    }

    protected NodeValidator nodeValidator() {
        synchronized (this) {
            if (nodeValidator == null) {
                nodeValidator = ApplicationContextProvider.getApplicationContext().getBean(NodeValidator.class);
            }
        }
        return nodeValidator;
    }

    @Override
    public <S extends T> S save(final S entity) {
        return super.save(nodeValidator().validate(entity));
    }
}
