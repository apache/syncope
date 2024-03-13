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
package org.apache.syncope.core.persistence.neo4j;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.DomainRegistry;

public class DummyDomainOps implements DomainOps {

    private final DomainRegistry<Neo4jDomain> domainRegistry;

    public DummyDomainOps(final DomainRegistry<Neo4jDomain> domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    @Override
    public List<Domain> list() {
        return List.of();
    }

    @Override
    public Domain read(final String key) {
        return new Neo4jDomain.Builder(key).build();
    }

    @Override
    public void create(final Domain domain) {
        domainRegistry.register((Neo4jDomain) domain);
    }

    @Override
    public void deployed(final String key) {
        // nothing to do
    }

    @Override
    public void changeAdminPassword(final String key, final String password, final CipherAlgorithm cipherAlgorithm) {
        // nothing to do
    }

    @Override
    public void adjustPoolSize(final String key, final int maxPoolSize, final int minIdle) {
        // nothing to do
    }

    @Override
    public void delete(final String key) {
        // nothing to do
    }
}
