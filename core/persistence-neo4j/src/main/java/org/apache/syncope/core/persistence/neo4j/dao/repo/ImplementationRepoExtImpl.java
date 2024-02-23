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
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class ImplementationRepoExtImpl implements ImplementationRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public ImplementationRepoExtImpl(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public List<Implementation> findByTypeAndKeyword(final String type, final String keyword) {
        StringBuilder queryString = new StringBuilder("MATCH (n) WHERE n.type = $type ");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", type);

        if (StringUtils.isNotBlank(keyword)) {
            queryString.append("AND n.keyword =~ $keyword ");
            parameters.put("keyword", keyword);
        }

        queryString.append("RETURN n");

        return neo4jTemplate.findAll(queryString.toString(), parameters, Implementation.class);
    }

    @Override
    public Implementation save(final Implementation implementation) {
        Implementation saved = neo4jTemplate.save(nodeValidator.validate(implementation));

        ImplementationManager.purge(saved.getKey());

        return saved;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jImplementation.class).ifPresent(implementation -> {
            neo4jTemplate.deleteById(key, Neo4jImplementation.class);
            ImplementationManager.purge(key);
        });
    }
}
