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
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jCASSPClientApp;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class CASSPClientAppRepoExtImpl
        extends AbstractClientRepoExt<CASSPClientApp>
        implements CASSPClientAppRepoExt {

    public CASSPClientAppRepoExtImpl(final Neo4jTemplate neo4jTemplate, final Neo4jClient neo4jClient) {
        super(neo4jTemplate, neo4jClient);
    }

    @Override
    public List<CASSPClientApp> findAllByPolicy(final Policy policy) {
        return findAllByPolicy(policy, Neo4jCASSPClientApp.NODE, Neo4jCASSPClientApp.class);
    }

    @Override
    public List<CASSPClientApp> findAllByRealm(final Realm realm) {
        return findAllByRealm(realm, Neo4jCASSPClientApp.NODE, Neo4jCASSPClientApp.class);
    }
}
