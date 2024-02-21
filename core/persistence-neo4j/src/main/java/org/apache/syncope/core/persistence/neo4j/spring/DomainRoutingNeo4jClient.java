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
package org.apache.syncope.core.persistence.neo4j.spring;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.neo4j.driver.QueryRunner;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.UserSelection;

public class DomainRoutingNeo4jClient implements Neo4jClient {

    protected final Map<String, Neo4jClient> delegates = new ConcurrentHashMap<>();

    public void add(final String domain, final Neo4jClient neo4jClient) {
        delegates.put(domain, neo4jClient);
    }

    public void remove(final String domain) {
        delegates.remove(domain);
    }

    protected Neo4jClient delegate() {
        return delegates.computeIfAbsent(AuthContextUtils.getDomain(), domain -> {
            throw new IllegalStateException("Could not find Neo4jClient for domain " + domain);
        });
    }

    @Override
    public QueryRunner getQueryRunner(final DatabaseSelection databaseSelection, final UserSelection asUser) {
        return delegate().getQueryRunner(databaseSelection, asUser);
    }

    @Override
    public UnboundRunnableSpec query(final String cypher) {
        return delegate().query(cypher);
    }

    @Override
    public UnboundRunnableSpec query(final Supplier<String> cypherSupplier) {
        return delegate().query(cypherSupplier);
    }

    @Override
    public <T> OngoingDelegation<T> delegateTo(final Function<QueryRunner, Optional<T>> callback) {
        return delegate().delegateTo(callback);
    }

    @Override
    public DatabaseSelectionProvider getDatabaseSelectionProvider() {
        return delegate().getDatabaseSelectionProvider();
    }
}
