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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jFIQLQuery;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class FIQLQueryRepoExtImpl extends AbstractDAO implements FIQLQueryRepoExt {

    public FIQLQueryRepoExtImpl(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        super(neo4jTemplate, neo4jClient);
    }

    @Override
    public List<FIQLQuery> findByOwner(final User user, final String target) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", user.getKey());
        StringBuilder queryString = new StringBuilder(
                "MATCH (n:").append(Neo4jFIQLQuery.NODE).append(")-[]-(p:").append(Neo4jUser.NODE).
                append(" {id: $id}) ");
        if (StringUtils.isNotBlank(target)) {
            queryString.append("WHERE n.target = $target ");
            parameters.put("target", target);
        }
        queryString.append("RETURN n.id");

        return toList(
                neo4jClient.query(queryString.toString()).bindAll(parameters).fetch().all(),
                "n.id",
                Neo4jFIQLQuery.class,
                null);
    }
}
