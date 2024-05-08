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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.ProvisioningPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTemplateRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jRealmDAO extends AbstractDAO implements RealmDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmDAO.class);

    public static final String CACHE = "realmCache";

    protected final RoleDAO roleDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final ApplicationEventPublisher publisher;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jRealm> cache;

    public Neo4jRealmDAO(
            final RoleDAO roleDAO,
            final RealmSearchDAO realmSearchDAO,
            final ApplicationEventPublisher publisher,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jRealm> cache) {

        super(neo4jTemplate, neo4jClient);
        this.roleDAO = roleDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.publisher = publisher;
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Override
    public Realm getRoot() {
        Realm root = cache.get(EntityCacheKey.of(SyncopeConstants.ROOT_REALM));
        if (root == null) {
            root = neo4jClient.query(
                    "MATCH (n:" + Neo4jRealm.NODE + ") WHERE n.fullPath = '/' RETURN n.id").fetch().one().
                    flatMap(super.<Realm, Neo4jRealm>toOptional("n.id", Neo4jRealm.class, cache)).orElseGet(() -> {
                LOG.debug("Root realm not found");
                return null;
            });
        }

        return root;
    }

    @Override
    public boolean existsById(final String key) {
        return neo4jTemplate.existsById(key, Neo4jRealm.class);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends Realm> findById(final String key) {
        return findById(key, Neo4jRealm.class, cache);
    }

    protected <T extends Policy> List<Realm> findSamePolicyChildren(final Realm realm, final T policy) {
        List<Realm> result = new ArrayList<>();

        realmSearchDAO.findChildren(realm).stream().
                filter(child -> (policy instanceof AccountPolicy
                && child.getAccountPolicy() == null || policy.equals(child.getAccountPolicy()))
                || (policy instanceof PasswordPolicy
                && child.getPasswordPolicy() == null || policy.equals(child.getPasswordPolicy()))).
                forEach(child -> {
                    result.add(child);
                    result.addAll(findSamePolicyChildren(child, policy));
                });

        return result;
    }

    @Override
    public <T extends Policy> List<Realm> findByPolicy(final T policy) {
        if (policy instanceof PropagationPolicy || policy instanceof ProvisioningPolicy) {
            return List.of();
        }

        String relationship = null;
        String label = null;
        if (policy instanceof AccountPolicy) {
            relationship = Neo4jRealm.REALM_ACCOUNT_POLICY_REL;
            label = Neo4jAccountPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof PasswordPolicy) {
            relationship = Neo4jRealm.REALM_PASSWORD_POLICY_REL;
            label = Neo4jPasswordPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof AuthPolicy) {
            relationship = Neo4jRealm.REALM_AUTH_POLICY_REL;
            label = Neo4jAuthPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof AccessPolicy) {
            relationship = Neo4jRealm.REALM_ACCESS_POLICY_REL;
            label = Neo4jAccessPolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof AttrReleasePolicy) {
            relationship = Neo4jRealm.REALM_ATTR_RELEASE_POLICY_REL;
            label = Neo4jAttrReleasePolicy.NODE + ":" + Neo4jPolicy.NODE;
        } else if (policy instanceof TicketExpirationPolicy) {
            relationship = Neo4jRealm.REALM_TICKET_EXPIRATION_POLICY_REL;
            label = Neo4jTicketExpirationPolicy.NODE + ":" + Neo4jPolicy.NODE;
        }

        List<Realm> found = findByRelationship(
                Neo4jRealm.NODE,
                label,
                policy.getKey(),
                relationship,
                Neo4jRealm.class,
                cache);

        List<Realm> result = new ArrayList<>();
        found.forEach(realm -> {
            result.add(realm);
            result.addAll(findSamePolicyChildren(realm, policy));
        });

        return result;
    }

    @Override
    public List<Realm> findByActionsContaining(final Implementation logicActions) {
        return findByRelationship(
                Neo4jRealm.NODE, Neo4jImplementation.NODE, logicActions.getKey(), Neo4jRealm.class, cache);
    }

    @Override
    public List<Realm> findByResources(final ExternalResource resource) {
        return findByRelationship(
                Neo4jRealm.NODE, Neo4jExternalResource.NODE, resource.getKey(), Neo4jRealm.class, cache);
    }

    @Override
    public long count() {
        return neo4jTemplate.count(Neo4jRealm.class);
    }

    @Override
    public List<? extends Realm> findAll() {
        return findAll(Pageable.unpaged()).getContent();
    }

    @Override
    public Page<? extends Realm> findAll(final Pageable pageable) {
        StringBuilder query = new StringBuilder("MATCH (n:" + Neo4jRealm.NODE + ") RETURN n.id ORDER BY n.fullPath");

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        List<? extends Realm> result = toList(
                neo4jClient.query(query.toString()).fetch().all(),
                "n.id",
                Neo4jRealm.class,
                cache);
        return new SyncopePage<>(result, pageable, count());
    }

    @Override
    public <S extends Realm> S save(final S realm) {
        String fullPathBefore = realm.getFullPath();
        String fullPathAfter = realm.getParent() == null
                ? SyncopeConstants.ROOT_REALM
                : StringUtils.appendIfMissing(realm.getParent().getFullPath(), "/") + realm.getName();
        if (!fullPathAfter.equals(fullPathBefore)) {
            ((Neo4jRealm) realm).setFullPath(fullPathAfter);
        }

        S merged = neo4jTemplate.save(nodeValidator.validate(realm));

        if (!fullPathAfter.equals(fullPathBefore)) {
            realmSearchDAO.findChildren(merged).forEach(this::save);
        }

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, merged, AuthContextUtils.getDomain()));

        cache.put(EntityCacheKey.of(realm.getKey()), (Neo4jRealm) merged);

        return merged;
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void delete(final Realm realm) {
        if (realm == null || realm.getParent() == null) {
            return;
        }

        realmSearchDAO.findDescendants(realm.getFullPath(), null, Pageable.unpaged()).forEach(toBeDeleted -> {
            roleDAO.findByRealms(toBeDeleted).forEach(role -> role.getRealms().remove(toBeDeleted));

            cascadeDelete(
                    Neo4jAnyTemplateRealm.NODE,
                    Neo4jRealm.NODE,
                    toBeDeleted.getKey());

            toBeDeleted.setParent(null);

            cache.remove(EntityCacheKey.of(realm.getKey()));

            neo4jTemplate.deleteById(toBeDeleted.getKey(), Neo4jRealm.class);

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, toBeDeleted, AuthContextUtils.getDomain()));
        });
    }
}
