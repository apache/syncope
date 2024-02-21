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
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class DomainRoutingNeo4jTransactionManager implements PlatformTransactionManager {

    protected final Map<String, Neo4jTransactionManager> delegates = new ConcurrentHashMap<>();

    public void add(final String domain, final Neo4jTransactionManager neo4jTransactionManager) {
        delegates.put(domain, neo4jTransactionManager);
    }

    public void remove(final String domain) {
        delegates.remove(domain);
    }

    protected Neo4jTransactionManager delegate() {
        return delegates.computeIfAbsent(AuthContextUtils.getDomain(), domain -> {
            throw new IllegalStateException("Could not find Neo4jTransactionManager for domain " + domain);
        });
    }

    @Override
    public TransactionStatus getTransaction(final TransactionDefinition definition) throws TransactionException {
        return delegate().getTransaction(definition);
    }

    @Override
    public void commit(final TransactionStatus status) throws TransactionException {
        delegate().commit(status);
    }

    @Override
    public void rollback(final TransactionStatus status) throws TransactionException {
        delegate().rollback(status);
    }
}
