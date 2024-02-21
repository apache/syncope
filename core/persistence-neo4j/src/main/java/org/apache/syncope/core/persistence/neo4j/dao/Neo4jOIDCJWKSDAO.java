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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jOIDCJWKS;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jOIDCJWKSDAO implements OIDCJWKSDAO {

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public Neo4jOIDCJWKSDAO(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OIDCJWKS> get() {
        return neo4jTemplate.findAll(Neo4jOIDCJWKS.class).stream().findFirst().map(OIDCJWKS.class::cast);
    }

    @Override
    public OIDCJWKS save(final OIDCJWKS jwks) {
        return neo4jTemplate.save(nodeValidator.validate(jwks));
    }

    @Override
    public void delete() {
        neo4jTemplate.deleteAll(Neo4jOIDCJWKS.class);
    }
}
