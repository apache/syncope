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
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.apache.syncope.core.persistence.neo4j.entity.keymaster.Neo4jNetworkService;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class NetworkServiceRepoExtImpl implements NetworkServiceRepoExt {

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public NetworkServiceRepoExtImpl(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Transactional(readOnly = true)
    @Override
    public List<NetworkServiceEntity> findAll(final NetworkService.Type serviceType) {
        return neo4jTemplate.findAll(
                "MATCH (n:" + Neo4jNetworkService.NODE + ") WHERE n.type = $serviceType RETURN n",
                Map.of("serviceType", serviceType.name()),
                Neo4jNetworkService.class).stream().map(NetworkServiceEntity.class::cast).toList();
    }

    @Override
    public <S extends Neo4jNetworkService> S save(final S service) {
        if (findAll(service.getType()).stream().anyMatch(s -> s.getAddress().equals(service.getAddress()))) {
            throw new DuplicateException(
                    "NetworkService with type " + service.getType() + "and address " + service.getAddress());
        }

        return neo4jTemplate.save(nodeValidator.validate(service));
    }

    @Override
    public void deleteAll(final NetworkService service) {
        findAll(service.getType()).stream().
                filter(s -> s.getAddress().equals(service.getAddress())).
                forEach(s -> neo4jTemplate.deleteById(s.getKey(), Neo4jNetworkService.class));
    }
}
