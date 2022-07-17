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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAuthModule;
import org.springframework.transaction.annotation.Transactional;

public class JPAAuthModuleDAO extends AbstractDAO<AuthModule> implements AuthModuleDAO {

    protected final PolicyDAO policyDAO;

    public JPAAuthModuleDAO(final PolicyDAO policyDAO) {
        this.policyDAO = policyDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public AuthModule find(final String key) {
        return entityManager().find(JPAAuthModule.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuthModule> findAll() {
        TypedQuery<AuthModule> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthModule.class.getSimpleName() + " e", AuthModule.class);
        return query.getResultList();
    }

    @Override
    public AuthModule save(final AuthModule authModule) {
        ((JPAAuthModule) authModule).list2json();
        return entityManager().merge(authModule);
    }

    @Override
    public void delete(final String key) {
        AuthModule authModule = find(key);
        if (authModule == null) {
            return;
        }

        delete(authModule);
    }

    @Override
    public void delete(final AuthModule authModule) {
        policyDAO.find(AuthPolicy.class).stream().
                filter(policy -> policy.getConf() instanceof DefaultAuthPolicyConf).
                forEach(policy -> {
                    DefaultAuthPolicyConf conf = (DefaultAuthPolicyConf) policy.getConf();
                    if (conf.getAuthModules().remove(authModule.getKey())) {
                        policy.setConf(conf);
                        policyDAO.save(policy);
                    }
                });

        entityManager().remove(authModule);
    }
}
