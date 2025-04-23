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
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class ExternalResourceRepoExtImpl implements ExternalResourceRepoExt {

    protected final TaskDAO taskDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final RealmDAO realmDAO;

    protected final EntityManager entityManager;

    public ExternalResourceRepoExtImpl(
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO,
            final EntityManager entityManager) {

        this.taskDAO = taskDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.realmDAO = realmDAO;
        this.entityManager = entityManager;
    }

    @Override
    public ExternalResource authFind(final String key) {
        ExternalResource resource = entityManager.find(JPAExternalResource.class, key);
        if (resource == null) {
            return null;
        }

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_READ);
        if (authRealms == null || authRealms.isEmpty()
                || authRealms.stream().noneMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))) {

            throw new DelegatedAdministrationException(
                    resource.getConnector().getAdminRealm().getFullPath(),
                    ExternalResource.class.getSimpleName(),
                    resource.getKey());
        }

        return resource;
    }

    @Override
    public List<ExternalResource> findByPolicy(final Policy policy) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAExternalResource.class.getSimpleName()).
                append(" e WHERE e.");

        if (AccountPolicy.class.isAssignableFrom(policy.getClass())) {
            queryString.append("accountPolicy");
        } else if (PasswordPolicy.class.isAssignableFrom(policy.getClass())) {
            queryString.append("passwordPolicy");
        } else if (PropagationPolicy.class.isAssignableFrom(policy.getClass())) {
            queryString.append("propagationPolicy");
        } else if (InboundPolicy.class.isAssignableFrom(policy.getClass())) {
            queryString.append("inboundPolicy");
        } else if (PushPolicy.class.isAssignableFrom(policy.getClass())) {
            queryString.append("pushPolicy");
        }

        TypedQuery<ExternalResource> query = entityManager.createQuery(
                queryString.append("=:policy").toString(), ExternalResource.class);
        query.setParameter("policy", policy);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends ExternalResource> findAll() {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_LIST);
        if (CollectionUtils.isEmpty(authRealms)) {
            return List.of();
        }

        TypedQuery<ExternalResource> query = entityManager.createQuery(
                "SELECT e FROM  " + JPAExternalResource.class.getSimpleName() + " e", ExternalResource.class);

        return query.getResultList().stream().filter(resource -> authRealms.stream().
                anyMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))).
                toList();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public ExternalResource save(final ExternalResource resource) {
        ((JPAExternalResource) resource).list2json();
        return entityManager.merge(resource);
    }

    @Override
    public void deleteMapping(final String schemaKey) {
        findAll().forEach(resource -> {
            Mutable<Boolean> removed = new MutableObject<>(false);

            resource.getProvisions().forEach(provision -> removed.setValue(
                    removed.getValue()
                    || (provision.getMapping() != null
                    && provision.getMapping().getItems().removeIf(item -> schemaKey.equals(item.getIntAttrName())))));

            if (removed.getValue()) {
                entityManager.merge(resource);
            }
        });
    }

    @Override
    public void deleteById(final String key) {
        ExternalResource resource = entityManager.find(JPAExternalResource.class, key);
        if (resource == null) {
            return;
        }

        taskDAO.deleteAll(resource, TaskType.PROPAGATION);
        taskDAO.deleteAll(resource, TaskType.LIVE_SYNC);
        taskDAO.deleteAll(resource, TaskType.PULL);
        taskDAO.deleteAll(resource, TaskType.PUSH);

        realmDAO.findByResources(resource).
                forEach(realm -> realm.getResources().remove(resource));
        anyObjectDAO.findByResourcesContaining(resource).
                forEach(anyObject -> anyObject.getResources().remove(resource));
        userDAO.findLinkedAccountsByResource(resource).forEach(account -> {
            account.getOwner().getLinkedAccounts().remove(account);
            account.setOwner(null);
        });
        userDAO.findByResourcesContaining(resource).
                forEach(user -> user.getResources().remove(resource));
        groupDAO.findByResourcesContaining(resource).
                forEach(group -> group.getResources().remove(resource));

        virSchemaDAO.findByResource(resource).forEach(virSchemaDAO::delete);

        if (resource.getConnector() != null
                && resource.getConnector().getResources() != null
                && !resource.getConnector().getResources().isEmpty()) {

            resource.getConnector().getResources().remove(resource);
        }
        resource.setConnector(null);

        entityManager.remove(resource);
    }
}
