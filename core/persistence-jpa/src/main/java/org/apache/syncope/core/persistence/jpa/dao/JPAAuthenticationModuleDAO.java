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
import org.apache.syncope.core.persistence.api.dao.AuthenticationModuleDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationModule;
import org.apache.syncope.core.persistence.jpa.entity.authentication.JPAAuthenticationModule;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAAuthenticationModuleDAO extends AbstractDAO<AuthenticationModule> implements AuthenticationModuleDAO {

    @Override
    public AuthenticationModule find(final String key) {
        return entityManager().find(JPAAuthenticationModule.class, key);
    }

    @Override
    public List<AuthenticationModule> findByConfiguration(final Implementation configuration) {
        TypedQuery<AuthenticationModule> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthenticationModule.class.getSimpleName() + " e "
                + "WHERE :configuration MEMBER OF e.configurations", AuthenticationModule.class);
        query.setParameter("configuration", configuration);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuthenticationModule> findAll() {
        TypedQuery<AuthenticationModule> query = entityManager().createQuery(
                "SELECT e FROM " + JPAAuthenticationModule.class.getSimpleName() + " e", AuthenticationModule.class);

        return query.getResultList();
    }

    @Override
    public AuthenticationModule save(final AuthenticationModule authenticationModule) {
        return entityManager().merge(authenticationModule);
    }

    @Override
    public void delete(final String key) {
        AuthenticationModule authenticationModule = find(key);
        if (authenticationModule == null) {
            return;
        }

        delete(authenticationModule);
    }

    @Override
    public void delete(final AuthenticationModule authenticationModule) {
        entityManager().remove(authenticationModule);
    }

}
