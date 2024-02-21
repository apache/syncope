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
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jSAML2SPClientApp;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class SAML2SPClientAppRepoExtImpl
        extends AbstractClientRepoExt<SAML2SPClientApp>
        implements SAML2SPClientAppRepoExt {

    protected final NodeValidator nodeValidator;

    public SAML2SPClientAppRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    @Override
    public List<SAML2SPClientApp> findAllByPolicy(final Policy policy) {
        return findAllByPolicy(policy, Neo4jSAML2SPClientApp.NODE, Neo4jSAML2SPClientApp.class);
    }

    @Override
    public List<SAML2SPClientApp> findAllByRealm(final Realm realm) {
        return findAllByRealm(realm, Neo4jSAML2SPClientApp.NODE, Neo4jSAML2SPClientApp.class);
    }

    @Override
    public SAML2SPClientApp save(final SAML2SPClientApp clientApp) {
        ((Neo4jSAML2SPClientApp) clientApp).list2json();
        SAML2SPClientApp saved = neo4jTemplate.save(nodeValidator.validate(clientApp));
        ((Neo4jSAML2SPClientApp) saved).postSave();
        return saved;
    }
}
