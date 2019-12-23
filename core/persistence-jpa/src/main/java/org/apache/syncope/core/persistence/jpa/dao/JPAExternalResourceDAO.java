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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMapping;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.provisioning.api.ConnectorRegistry;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAExternalResourceDAO extends AbstractDAO<ExternalResource> implements ExternalResourceDAO {

    @Autowired
    private ConnectorRegistry connRegistry;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private RealmDAO realmDAO;

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

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_READ);
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
        TypedQuery<Provision> query = entityManager().createQuery(
                "SELECT e FROM " + JPAProvision.class.getSimpleName()
                + " e WHERE :anyTypeClass MEMBER OF e.auxClasses", Provision.class);
        query.setParameter("anyTypeClass", anyTypeClass);

        return query.getResultList();
    }

    @Override
    public List<MappingItem> findByTransformer(final Implementation transformer) {
        TypedQuery<MappingItem> query = entityManager().createQuery(
                "SELECT e FROM " + JPAMappingItem.class.getSimpleName()
                + " e WHERE :transformer MEMBER OF e.transformers", MappingItem.class);
        query.setParameter("transformer", transformer);

        return query.getResultList();
    }

    @Override
    public List<ExternalResource> findByPropagationActions(final Implementation propagationActions) {
        TypedQuery<ExternalResource> query = entityManager().createQuery(
                "SELECT e FROM " + JPAExternalResource.class.getSimpleName() + " e "
                + "WHERE :propagationActions MEMBER OF e.propagationActions", ExternalResource.class);
        query.setParameter("propagationActions", propagationActions);

        return query.getResultList();
    }

    private StringBuilder getByPolicyQuery(final Class<? extends Policy> policyClass) {
        StringBuilder query = new StringBuilder("SELECT e FROM ").
                append(JPAExternalResource.class.getSimpleName()).
                append(" e WHERE e.");

        if (AccountPolicy.class.isAssignableFrom(policyClass)) {
            query.append("accountPolicy");
        } else if (PasswordPolicy.class.isAssignableFrom(policyClass)) {
            query.append("passwordPolicy");
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

    @Override
    public List<ExternalResource> findAll() {
        final Set<String> authRealms = AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_LIST);
        if (authRealms == null || authRealms.isEmpty()) {
            return Collections.emptyList();
        }

        TypedQuery<ExternalResource> query = entityManager().createQuery(
                "SELECT e FROM  " + JPAExternalResource.class.getSimpleName() + " e", ExternalResource.class);

        return query.getResultList().stream().filter(resource -> authRealms.stream().
                anyMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))).
                collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = { Throwable.class })
    public ExternalResource save(final ExternalResource resource) {
        ExternalResource merged = entityManager().merge(resource);
        try {
            connRegistry.registerConnector(merged);
        } catch (NotFoundException e) {
            LOG.error("While registering connector for resource", e);
        }
        return merged;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deleteMapping(final String intAttrName) {
        TypedQuery<MappingItem> query = entityManager().createQuery(
                "SELECT m FROM " + JPAMappingItem.class.getSimpleName()
                + " m WHERE m.intAttrName=:intAttrName", MappingItem.class);
        query.setParameter("intAttrName", intAttrName);

        query.getResultList().stream().
                map(Entity::getKey).
                map(itemKey -> entityManager().find(JPAMappingItem.class, itemKey)).filter(Objects::nonNull).
                forEach(item -> {
                    item.getMapping().getItems().remove(item);
                    item.setMapping(null);
                    entityManager().remove(item);
                });

        // Make empty query cache for *MappingItem and related *Mapping
        entityManager().getEntityManagerFactory().getCache().evict(JPAMappingItem.class);
        entityManager().getEntityManagerFactory().getCache().evict(JPAMapping.class);
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

        resource.getProvisions().stream().
                peek(provision -> {
                    provision.setUidOnCreate(null);
                    if (provision.getMapping() != null) {
                        provision.getMapping().getItems().forEach(item -> item.setMapping(null));
                        provision.getMapping().getItems().clear();
                    }
                    provision.setMapping(null);
                    provision.setResource(null);
                }).
                forEach(provision -> virSchemaDAO.findByProvision(provision).
                forEach(schema -> virSchemaDAO.delete(schema.getKey())));

        if (resource.getConnector() != null && resource.getConnector().getResources() != null
                && !resource.getConnector().getResources().isEmpty()) {

            resource.getConnector().getResources().remove(resource);
        }
        resource.setConnector(null);

        entityManager().remove(resource);
    }
}
