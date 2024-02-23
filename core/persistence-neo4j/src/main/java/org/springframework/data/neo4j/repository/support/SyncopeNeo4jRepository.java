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
package org.springframework.data.neo4j.repository.support;

import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.data.neo4j.core.Neo4jOperations;

public class SyncopeNeo4jRepository<T, ID> extends SimpleNeo4jRepository<T, ID> {

    public SyncopeNeo4jRepository(
            final Neo4jOperations neo4jOperations,
            final Neo4jEntityInformation<T, ID> entityInformation) {

        super(neo4jOperations, entityInformation);
    }

    @Override
    public <S extends T> S save(final S entity) {
        return super.save(
                ApplicationContextProvider.getApplicationContext().getBean(NodeValidator.class).validate(entity));
    }
}
