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

import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAuthModule;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class AuthModuleRepoExtImpl implements AuthModuleRepoExt {

    protected final PolicyDAO policyDAO;

    protected final Neo4jTemplate neo4jTemplate;

    protected final NodeValidator nodeValidator;

    public AuthModuleRepoExtImpl(
            final PolicyDAO policyDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        this.policyDAO = policyDAO;
        this.neo4jTemplate = neo4jTemplate;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public AuthModule save(final AuthModule authModule) {
        ((Neo4jAuthModule) authModule).list2json();
        AuthModule saved = neo4jTemplate.save(nodeValidator.validate(authModule));
        ((Neo4jAuthModule) saved).postSave();
        return saved;
    }

    @Override
    public void delete(final AuthModule authModule) {
        policyDAO.findAll(AuthPolicy.class).stream().
                filter(policy -> policy.getConf() instanceof DefaultAuthPolicyConf).
                forEach(policy -> {
                    DefaultAuthPolicyConf conf = (DefaultAuthPolicyConf) policy.getConf();
                    if (conf.getAuthModules().remove(authModule.getKey())) {
                        policy.setConf(conf);
                        policyDAO.save(policy);
                    }
                });

        neo4jTemplate.deleteById(authModule.getKey(), Neo4jAuthModule.class);
    }
}
