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

import java.util.concurrent.CompletionStage;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.BaseSession;
import org.neo4j.driver.BookmarkManager;
import org.neo4j.driver.Driver;
import org.neo4j.driver.ExecutableQuery;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.types.TypeSystem;

public class DomainRoutingDriver implements Driver {

    protected final DomainHolder<Driver> domainHolder;

    public DomainRoutingDriver(final DomainHolder<Driver> domainHolder) {
        this.domainHolder = domainHolder;
    }

    protected Driver delegate() {
        return domainHolder.getDomains().computeIfAbsent(AuthContextUtils.getDomain(), domain -> {
            throw new IllegalStateException("Could not find Driver for domain " + domain);
        });
    }

    @Override
    public ExecutableQuery executableQuery(final String query) {
        return delegate().executableQuery(query);
    }

    @Override
    public BookmarkManager executableQueryBookmarkManager() {
        return delegate().executableQueryBookmarkManager();
    }

    @Override
    public boolean isEncrypted() {
        return delegate().isEncrypted();
    }

    @Override
    public <T extends BaseSession> T session(
            final Class<T> sessionClass,
            final SessionConfig sessionConfig,
            final AuthToken sessionAuthToken) {

        return delegate().session(sessionClass, sessionConfig, sessionAuthToken);
    }

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        return delegate().closeAsync();
    }

    @Override
    public Metrics metrics() {
        return delegate().metrics();
    }

    @Override
    public boolean isMetricsEnabled() {
        return delegate().isMetricsEnabled();
    }

    @SuppressWarnings("deprecation")
    @Override
    public TypeSystem defaultTypeSystem() {
        return delegate().defaultTypeSystem();
    }

    @Override
    public void verifyConnectivity() {
        delegate().verifyConnectivity();
    }

    @Override
    public CompletionStage<Void> verifyConnectivityAsync() {
        return delegate().verifyConnectivityAsync();
    }

    @Override
    public boolean verifyAuthentication(final AuthToken sessionAuthToken) {
        return delegate().verifyAuthentication(sessionAuthToken);
    }

    @Override
    public boolean supportsSessionAuth() {
        return delegate().supportsSessionAuth();
    }

    @Override
    public boolean supportsMultiDb() {
        return delegate().supportsMultiDb();
    }

    @Override
    public CompletionStage<Boolean> supportsMultiDbAsync() {
        return delegate().supportsMultiDbAsync();
    }
}
