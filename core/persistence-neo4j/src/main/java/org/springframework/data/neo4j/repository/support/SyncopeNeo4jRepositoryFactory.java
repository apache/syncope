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

import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;

public class SyncopeNeo4jRepositoryFactory extends RepositoryFactorySupport {

    private final Neo4jRepositoryFactory delegate;

    public SyncopeNeo4jRepositoryFactory(
            final Neo4jOperations neo4jOperations,
            final Neo4jMappingContext mappingContext) {

        this.delegate = new Neo4jRepositoryFactory(neo4jOperations, mappingContext);
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(final Class<T> domainClass) {
        return delegate.getEntityInformation(domainClass);
    }

    @Override
    protected Object getTargetRepository(final RepositoryInformation metadata) {
        return delegate.getTargetRepository(metadata);
    }

    @Override
    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(final RepositoryMetadata metadata) {
        return delegate.getRepositoryFragments(metadata);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(final RepositoryMetadata metadata) {
        return SyncopeNeo4jRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
            final @Nullable QueryLookupStrategy.Key key, final ValueExpressionDelegate valueExpressionDelegate) {

        return delegate.getQueryLookupStrategy(key, valueExpressionDelegate);
    }

    @Override
    protected ProjectionFactory getProjectionFactory() {
        return delegate.getProjectionFactory();
    }
}
