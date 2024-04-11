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

import java.util.List;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.query.CypherAdapterUtils;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.support.PageableExecutionUtils;

public class SyncopeNeo4jRepository<T, ID> extends SimpleNeo4jRepository<T, ID> {

    protected static final CypherGenerator CYPHER_GENERATOR = CypherGenerator.INSTANCE;

    protected static QueryFragmentsAndParameters getQueryFragmentsAndParameters(
            final Neo4jPersistentEntity<?> entityMetaData,
            final Pageable pageable) {

        QueryFragments queryFragments = new QueryFragments();
        queryFragments.addMatchOn(CYPHER_GENERATOR.createRootNode(entityMetaData));
        queryFragments.setCondition(null);
        queryFragments.setReturnExpressions(CYPHER_GENERATOR.createReturnStatementForMatch(entityMetaData));

        if (pageable.isPaged()) {
            queryFragments.setSkip(pageable.getOffset());
            queryFragments.setLimit(pageable.getPageSize());
        }
        Sort pageableSort = pageable.getSort();
        if (pageableSort.isSorted()) {
            queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, pageableSort));
        }

        return new QueryFragmentsAndParameters(entityMetaData, queryFragments, null, null);
    }

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

    @Override
    public Page<T> findAll(final Pageable pageable) {
        List<T> allResult = neo4jOperations.toExecutableQuery(
                entityInformation.getJavaType(),
                getQueryFragmentsAndParameters(entityMetaData, pageable)).getResults();

        return PageableExecutionUtils.getPage(allResult, pageable, this::count);
    }
}
