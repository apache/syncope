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

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jOIDCRPClientApp;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class OIDCRPClientAppRepoExtImpl
        extends AbstractClientRepoExt<OIDCRPClientApp>
        implements OIDCRPClientAppRepoExt {

    protected final NodeValidator nodeValidator;

    public OIDCRPClientAppRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    @Override
    public List<OIDCRPClientApp> findAllByPolicy(final Policy policy) {
        return findAllByPolicy(policy, Neo4jOIDCRPClientApp.NODE, Neo4jOIDCRPClientApp.class);
    }

    @Override
    public List<OIDCRPClientApp> findAllByRealm(final Realm realm) {
        return findAllByRealm(realm, Neo4jOIDCRPClientApp.NODE, Neo4jOIDCRPClientApp.class);
    }

    @Override
    public OIDCRPClientApp save(final OIDCRPClientApp clientApp) {
        ((Neo4jOIDCRPClientApp) clientApp).list2json();
        OIDCRPClientApp saved = neo4jTemplate.save(nodeValidator.validate(clientApp));
        ((Neo4jOIDCRPClientApp) saved).postSave();
        return saved;
    }
}
