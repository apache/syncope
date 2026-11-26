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
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AccessTokenManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;

public class RoleRepoExtImpl implements RoleRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final AccessTokenManager accessTokenManager;

    protected final DelegationDAO delegationDAO;

    protected final EntityManager entityManager;

    public RoleRepoExtImpl(
            final ApplicationEventPublisher publisher,
            final AccessTokenManager accessTokenManager,
            final DelegationDAO delegationDAO,
            final EntityManager entityManager) {

        this.publisher = publisher;
        this.accessTokenManager = accessTokenManager;
        this.delegationDAO = delegationDAO;
        this.entityManager = entityManager;
    }

    protected List<User> getMembers(final Role role) {
        return entityManager.createQuery(
                "SELECT e FROM " + JPAUser.class.getSimpleName() + " e WHERE :role MEMBER OF e.roles", User.class).
                setParameter("role", role).
                getResultList();
    }

    @Override
    public Role save(final Role role) {
        ((JPARole) role).list2json();
        Role saved = entityManager.merge(role);
        getMembers(saved).forEach(user -> accessTokenManager.evict(user.getUsername()));
        return saved;
    }

    @Override
    public void delete(final Role role) {
        getMembers(role).forEach(user -> {
            user.getRoles().remove(role);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));

            accessTokenManager.evict(user.getUsername());
        });

        delegationDAO.findByRoles(role).forEach(delegation -> delegation.getRoles().remove(role));

        entityManager.remove(role);
    }
}
