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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAuthModule;

public class AuthModuleRepoExtImpl implements AuthModuleRepoExt {

    protected final PolicyDAO policyDAO;

    protected final EntityManager entityManager;

    public AuthModuleRepoExtImpl(final PolicyDAO policyDAO, final EntityManager entityManager) {
        this.policyDAO = policyDAO;
        this.entityManager = entityManager;
    }

    @Override
    public AuthModule save(final AuthModule authModule) {
        ((JPAAuthModule) authModule).list2json();
        return entityManager.merge(authModule);
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

        entityManager.remove(authModule);
    }
}
