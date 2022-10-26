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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class JPAExternalResourceDAO extends AbstractDAO<ExternalResource> implements ExternalResourceDAO {

    protected final TaskDAO taskDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final PolicyDAO policyDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final RealmDAO realmDAO;

    public JPAExternalResourceDAO(
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PolicyDAO policyDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO) {

        this.taskDAO = taskDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.policyDAO = policyDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.realmDAO = realmDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public int count() {
        Query query = entityManager().createQuery(
                "SELECT COUNT(e) FROM  " + JPAExternalResource.class.getSimpleName() + " e");
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public ExternalResource find(final String name) {
        return entityManager().find(JPAExternalResource.class, name);
    }

    @Override
    public ExternalResource authFind(final String key) {
        ExternalResource resource = find(key);
        if (resource == null) {
            return null;
        }

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_READ);
        if (authRealms == null || authRealms.isEmpty()
                || !authRealms.stream().anyMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))) {

            throw new DelegatedAdministrationException(
                    resource.getConnector().getAdminRealm().getFullPath(),
                    ExternalResource.class.getSimpleName(),
                    resource.getKey());
        }

        return resource;
    }

    @Override
    public List<Provision> findProvisionsByAuxClass(final AnyTypeClass anyTypeClass) {
        return findAll().stream().
                flatMap(resource -> resource.getProvisions().stream()).
                filter(provision -> provision.getAuxClasses().contains(anyTypeClass.getKey())).
                collect(Collectors.toList());
    }

    @Override
    public boolean anyItemHaving(final Implementation transformer) {
        return findAll().stream().
                flatMap(resource -> resource.getProvisions().stream()).
                flatMap(provision -> provision.getMapping().getItems().stream()).
                filter(item -> item.getTransformers().contains(transformer.getKey())).
                count() > 0;
    }

    @Override
    public List<ExternalResource> findByPropagationActions(final Implementation propagationActions) {
        TypedQuery<ExternalResource> query = entityManager().createQuery(
                "SELECT e FROM " + JPAExternalResource.class.getSimpleName() + " e "
                + "WHERE :propagationActions MEMBER OF e.propagationActions", ExternalResource.class);
        query.setParameter("propagationActions", propagationActions);

        return query.getResultList();
    }

    @Override
    public List<ExternalResource> findByProvisionSorter(final Implementation provisionSorter) {
        TypedQuery<ExternalResource> query = entityManager().createQuery(
                "SELECT e FROM " + JPAExternalResource.class.getSimpleName() + " e "
                + "WHERE e.provisionSorter=:provisionSorter", ExternalResource.class);
        query.setParameter("provisionSorter", provisionSorter);

        return query.getResultList();
    }

    protected StringBuilder getByPolicyQuery(final Class<? extends Policy> policyClass) {
        StringBuilder query = new StringBuilder("SELECT e FROM ").
                append(JPAExternalResource.class.getSimpleName()).
                append(" e WHERE e.");

        if (AccountPolicy.class.isAssignableFrom(policyClass)) {
            query.append("accountPolicy");
        } else if (PasswordPolicy.class.isAssignableFrom(policyClass)) {
            query.append("passwordPolicy");
        } else if (PropagationPolicy.class.isAssignableFrom(policyClass)) {
            query.append("propagationPolicy");
        } else if (PullPolicy.class.isAssignableFrom(policyClass)) {
            query.append("pullPolicy");
        } else if (PushPolicy.class.isAssignableFrom(policyClass)) {
            query.append("pushPolicy");
        }

        return query;
    }

    @Override
    public List<ExternalResource> findByPolicy(final Policy policy) {
        TypedQuery<ExternalResource> query = entityManager().createQuery(
                getByPolicyQuery(policy.getClass()).append("=:policy").toString(), ExternalResource.class);
        query.setParameter("policy", policy);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ExternalResource> findAll() {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_LIST);
        if (CollectionUtils.isEmpty(authRealms)) {
            return List.of();
        }

        TypedQuery<ExternalResource> query = entityManager().createQuery(
                "SELECT e FROM  " + JPAExternalResource.class.getSimpleName() + " e", ExternalResource.class);

        return query.getResultList().stream().filter(resource -> authRealms.stream().
                anyMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))).
                collect(Collectors.toList());
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public ExternalResource save(final ExternalResource resource) {
        ((JPAExternalResource) resource).list2json();
        return entityManager().merge(resource);
    }

    @Override
    public void deleteMapping(final String intAttrName) {
        findAll().forEach(resource -> {
            AtomicBoolean removed = new AtomicBoolean(false);

            resource.getProvisions().forEach(provision -> removed.set(
                    removed.get()
                    || (provision.getMapping() != null
                    && provision.getMapping().getItems().removeIf(item -> intAttrName.equals(item.getIntAttrName())))));

            if (removed.get()) {
                entityManager().merge(resource);
            }
        });
    }

    @Override
    public void delete(final String name) {
        ExternalResource resource = find(name);
        if (resource == null) {
            return;
        }

        taskDAO.deleteAll(resource, TaskType.PROPAGATION);
        taskDAO.deleteAll(resource, TaskType.PULL);
        taskDAO.deleteAll(resource, TaskType.PUSH);

        realmDAO.findByResource(resource).
                forEach(realm -> realm.getResources().remove(resource));
        anyObjectDAO.findByResource(resource).
                forEach(anyObject -> anyObject.getResources().remove(resource));
        userDAO.findLinkedAccountsByResource(resource).forEach(account -> {
            account.getOwner().getLinkedAccounts().remove(account);
            account.setOwner(null);
        });
        userDAO.findByResource(resource).
                forEach(user -> user.getResources().remove(resource));
        groupDAO.findByResource(resource).
                forEach(group -> group.getResources().remove(resource));
        policyDAO.findByResource(resource).
                forEach(policy -> policy.getResources().remove(resource));

        virSchemaDAO.find(resource).forEach(virSchemaDAO::delete);

        if (resource.getConnector() != null
                && resource.getConnector().getResources() != null
                && !resource.getConnector().getResources().isEmpty()) {

            resource.getConnector().getResources().remove(resource);
        }
        resource.setConnector(null);

        entityManager().remove(resource);
    }
}
