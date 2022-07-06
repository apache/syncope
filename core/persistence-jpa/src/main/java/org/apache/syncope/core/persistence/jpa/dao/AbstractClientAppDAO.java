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
import org.apache.syncope.core.persistence.api.dao.ClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.am.ClientApp;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;

public abstract class AbstractClientAppDAO<C extends ClientApp> extends AbstractDAO<C> implements ClientAppDAO<C> {

    protected StringBuilder getByPolicyQuery(
            final Class<? extends Policy> policyClass,
            final Class<? extends C> clientAppJPAClass) {

        StringBuilder query = new StringBuilder("SELECT e FROM ").
                append(clientAppJPAClass.getSimpleName()).
                append(" e WHERE e.");

        if (AuthPolicy.class.isAssignableFrom(policyClass)) {
            query.append("authPolicy");
        } else if (AccessPolicy.class.isAssignableFrom(policyClass)) {
            query.append("accessPolicy");
        } else if (AttrReleasePolicy.class.isAssignableFrom(policyClass)) {
            query.append("attrReleasePolicy");
        }

        return query;
    }

    protected List<C> findByPolicy(
            final Policy policy,
            final Class<C> reference,
            final Class<? extends C> clientAppJPAClass) {

        TypedQuery<C> query = entityManager().createQuery(
                getByPolicyQuery(policy.getClass(), clientAppJPAClass).append("=:policy").toString(), reference);
        query.setParameter("policy", policy);
        return query.getResultList();
    }

    protected List<C> findByRealm(
            final Realm realm,
            final Class<C> reference,
            final Class<? extends C> clientAppJPAClass) {

        TypedQuery<C> query = entityManager().createQuery(
                "SELECT e FROM " + clientAppJPAClass.getSimpleName() + " e WHERE e.realm=:realm", reference);
        query.setParameter("realm", realm);
        return query.getResultList();
    }
}
