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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.cache.Cache;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jConnInstance;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPropagationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPullPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jLinkedAccount;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class ExternalResourceRepoExtImpl extends AbstractDAO implements ExternalResourceRepoExt {

    protected final TaskDAO taskDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final RealmDAO realmDAO;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jExternalResource> cache;

    public ExternalResourceRepoExtImpl(
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jExternalResource> cache) {

        super(neo4jTemplate, neo4jClient);
        this.taskDAO = taskDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.realmDAO = realmDAO;
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends ExternalResource> findById(final String key) {
        return findById(key, Neo4jExternalResource.class, cache);
    }

    @Override
    public ExternalResource authFind(final String key) {
        ExternalResource resource = findById(key).orElse(null);
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
    public List<ExternalResource> findByConnInstance(final String connInstance) {
        return findByRelationship(
                Neo4jExternalResource.NODE, Neo4jConnInstance.NODE, connInstance, Neo4jExternalResource.class, cache);
    }

    @Override
    public List<ExternalResource> findByProvisionSorter(final Implementation provisionSorter) {
        return findByRelationship(
                Neo4jExternalResource.NODE,
                Neo4jImplementation.NODE,
                provisionSorter.getKey(),
                Neo4jExternalResource.RESOURCE_PROVISION_SORTER_REL,
                Neo4jExternalResource.class,
                cache);
    }

    @Override
    public List<ExternalResource> findByPropagationActionsContaining(final Implementation propagationActions) {
        return findByRelationship(
                Neo4jExternalResource.NODE,
                Neo4jImplementation.NODE,
                propagationActions.getKey(),
                Neo4jExternalResource.RESOURCE_PROPAGATION_ACTIONS_REL,
                Neo4jExternalResource.class,
                cache);
    }

    @Override
    public List<ExternalResource> findByPolicy(final Policy policy) {
        String relationship = null;
        String label = null;
        if (policy instanceof AccountPolicy) {
            relationship = Neo4jExternalResource.RESOURCE_ACCOUNT_POLICY_REL;
            label = Neo4jAccountPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof PasswordPolicy) {
            relationship = Neo4jExternalResource.RESOURCE_PASSWORD_POLICY_REL;
            label = Neo4jPasswordPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof PropagationPolicy) {
            relationship = Neo4jExternalResource.RESOURCE_PROPAGATION_POLICY_REL;
            label = Neo4jPropagationPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof PushPolicy) {
            relationship = Neo4jExternalResource.RESOURCE_PUSH_POLICY_REL;
            label = Neo4jPushPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof PullPolicy) {
            relationship = Neo4jExternalResource.RESOURCE_PULL_POLICY_REL;
            label = Neo4jPullPolicy.NODE + ":" + Neo4jPolicy.NODE;
        }

        return findByRelationship(
                Neo4jExternalResource.NODE,
                label,
                policy.getKey(),
                relationship,
                Neo4jExternalResource.class,
                cache);
    }

    @Transactional(readOnly = true)
    @Override
    public List<? extends ExternalResource> findAll() {
        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_LIST);
        if (CollectionUtils.isEmpty(authRealms)) {
            return List.of();
        }

        List<Neo4jExternalResource> all = toList(neo4jClient.query(
                "MATCH (n:" + Neo4jExternalResource.NODE + ") RETURN n.id").fetch().all(),
                "n.id",
                Neo4jExternalResource.class,
                cache);
        return all.stream().filter(resource -> authRealms.stream().
                anyMatch(realm -> resource.getConnector() != null
                && resource.getConnector().getAdminRealm().getFullPath().startsWith(realm))).
                toList();
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public ExternalResource save(final ExternalResource resource) {
        ((Neo4jExternalResource) resource).list2json();
        ExternalResource saved = neo4jTemplate.save(nodeValidator.validate(resource));
        ((Neo4jExternalResource) saved).postSave();
        cache.put(EntityCacheKey.of(resource.getKey()), (Neo4jExternalResource) saved);
        return saved;
    }

    @Override
    public void deleteMapping(final String schemaKey) {
        findAll().forEach(resource -> {
            AtomicBoolean removed = new AtomicBoolean(false);

            resource.getProvisions().forEach(provision -> removed.set(
                    removed.get()
                    || (provision.getMapping() != null
                    && provision.getMapping().getItems().removeIf(item -> schemaKey.equals(item.getIntAttrName())))));

            if (removed.get()) {
                ((Neo4jExternalResource) resource).list2json();
                ExternalResource saved = neo4jTemplate.save(resource);
                ((Neo4jExternalResource) saved).postSave();
                cache.put(EntityCacheKey.of(resource.getKey()), (Neo4jExternalResource) saved);
            }
        });
    }

    @Override
    public void deleteById(final String key) {
        ExternalResource resource = findById(key).orElse(null);
        if (resource == null) {
            return;
        }

        taskDAO.deleteAll(resource, TaskType.PROPAGATION);
        taskDAO.deleteAll(resource, TaskType.PULL);
        taskDAO.deleteAll(resource, TaskType.PUSH);
        realmDAO.findByResources(resource).
                forEach(realm -> realm.getResources().remove(resource));
        anyObjectDAO.findByResourcesContaining(resource).
                forEach(anyObject -> anyObject.getResources().remove(resource));
        userDAO.findLinkedAccountsByResource(resource).forEach(account -> {
            account.getOwner().getLinkedAccounts().remove(account);
            account.setOwner(null);
            neo4jTemplate.deleteById(account.getKey(), Neo4jLinkedAccount.class);
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

        cache.remove(EntityCacheKey.of(key));

        neo4jTemplate.deleteById(key, Neo4jExternalResource.class);
    }
}
