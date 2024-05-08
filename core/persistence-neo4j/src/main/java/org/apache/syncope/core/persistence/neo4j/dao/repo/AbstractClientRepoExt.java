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
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.am.AbstractClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jCASSPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jOIDCRPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jSAML2SPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public abstract class AbstractClientRepoExt<C extends ClientApp>
        extends AbstractDAO
        implements ClientAppRepoExt<C> {

    protected static Class<? extends AbstractClientApp> toDomainType(final Class<? extends ClientApp> clientAppClass) {
        if (CASSPClientApp.class.isAssignableFrom(clientAppClass)) {
            return Neo4jCASSPClientApp.class;
        }
        if (OIDCRPClientApp.class.isAssignableFrom(clientAppClass)) {
            return Neo4jOIDCRPClientApp.class;
        }
        if (SAML2SPClientApp.class.isAssignableFrom(clientAppClass)) {
            return Neo4jSAML2SPClientApp.class;
        }
        throw new IllegalArgumentException("Unexpected client app class: " + clientAppClass.getName());
    }

    protected AbstractClientRepoExt(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        super(neo4jTemplate, neo4jClient);
    }

    protected List<C> findAllByPolicy(
            final Policy policy,
            final String clientAppNode,
            final Class<? extends C> clientAppClass) {

        return findByRelationship(clientAppNode, Neo4jPolicy.NODE, policy.getKey(), toDomainType(clientAppClass), null);
    }

    protected List<C> findAllByRealm(
            final Realm realm,
            final String clientAppNode,
            final Class<? extends C> clientAppClass) {

        return findByRelationship(clientAppNode, Neo4jRealm.NODE, realm.getKey(), toDomainType(clientAppClass), null);
    }
}
