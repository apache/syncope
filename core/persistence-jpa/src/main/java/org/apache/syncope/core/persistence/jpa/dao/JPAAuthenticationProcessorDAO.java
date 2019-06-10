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
import org.apache.syncope.core.persistence.api.dao.AuthenticationProcessorDAO;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPostProcessor;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPreProcessor;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationProcessor;
import org.apache.syncope.core.persistence.api.entity.policy.AuthenticationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.authentication.AbstractAuthenticationProcessor;
import org.apache.syncope.core.persistence.jpa.entity.authentication.JPAAuthenticationPostProcessor;
import org.apache.syncope.core.persistence.jpa.entity.authentication.JPAAuthenticationPreProcessor;
import org.springframework.stereotype.Repository;

@Repository
public class JPAAuthenticationProcessorDAO extends AbstractDAO<AuthenticationProcessor> implements
        AuthenticationProcessorDAO {

    private <T extends AuthenticationProcessor> Class<? extends AbstractAuthenticationProcessor> getEntityReference(
            final Class<T> reference) {
        return AuthenticationPreProcessor.class.isAssignableFrom(reference)
                ? JPAAuthenticationPreProcessor.class
                : AuthenticationPostProcessor.class.isAssignableFrom(reference)
                ? JPAAuthenticationPostProcessor.class
                : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AuthenticationProcessor> T find(final String key) {
        return (T) entityManager().find(AbstractAuthenticationProcessor.class, key);
    }

    @Override
    public <T extends AuthenticationProcessor> List<T> find(final Class<T> reference) {
        TypedQuery<T> query = entityManager().createQuery(
                "SELECT e FROM " + getEntityReference(reference).getSimpleName() + " e", reference);

        return query.getResultList();
    }

    public List<AuthenticationProcessor> findAll() {
        TypedQuery<AuthenticationProcessor> query = entityManager().createQuery(
                "SELECT e FROM " + AbstractAuthenticationProcessor.class.getSimpleName()
                + " e", AuthenticationProcessor.class);
        return query.getResultList();
    }

    @Override
    public <T extends AuthenticationProcessor> T save(final T authenticationProcessor) {
        return entityManager().merge(authenticationProcessor);
    }

    @Override
    public <T extends AuthenticationProcessor> void delete(final T authenticationProcessor) {
        AuthenticationPolicy policy = authenticationProcessor.getAuthenticationPolicy();
        if (authenticationProcessor instanceof AuthenticationPreProcessor) {
            policy.setAuthenticationPreProcessor(null);
        } else {
            policy.setAuthenticationPostProcessor(null);
        }
        entityManager().remove(authenticationProcessor);
    }
}
