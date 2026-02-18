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
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAuthProfile;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class AuthProfileRepoExtImpl extends AbstractDAO implements AuthProfileRepoExt {

    protected final NodeValidator nodeValidator;

    public AuthProfileRepoExtImpl(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.nodeValidator = nodeValidator;
    }

    protected StringBuilder query(final String owner) {
        return new StringBuilder("MATCH (n:").append(Neo4jAuthProfile.NODE).append(") WHERE ").
                append("n.owner =~ \"").append(AnyRepoExt.escapeForLikeRegex(owner).replace("%", ".*")).append('"');
    }

    @Override
    public List<? extends AuthProfile> findByOwnerLike(final String owner, final Pageable pageable) {
        StringBuilder query = query(owner).append(" RETURN n.id");

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(query.toString()).fetch().all(), "n.id", Neo4jAuthProfile.class, null);
    }

    @Override
    public long countByOwnerLike(final String owner) {
        StringBuilder query = query(owner).append(" RETURN COUNT(n.id)");

        return neo4jTemplate.count(query.toString());
    }

    @Override
    public AuthProfile save(final AuthProfile connector) {
        ((Neo4jAuthProfile) connector).list2json();
        AuthProfile saved = neo4jTemplate.save(nodeValidator.validate(connector));
        ((Neo4jAuthProfile) saved).postSave();
        return saved;
    }
}
