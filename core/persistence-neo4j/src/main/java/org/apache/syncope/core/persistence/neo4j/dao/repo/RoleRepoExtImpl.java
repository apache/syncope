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
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

public class RoleRepoExtImpl extends AbstractDAO implements RoleRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final AnyMatchDAO anyMatchDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final DelegationDAO delegationDAO;

    protected final SearchCondVisitor searchCondVisitor;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jRole> cache;

    public RoleRepoExtImpl(
            final ApplicationEventPublisher publisher,
            final AnyMatchDAO anyMatchDAO,
            final AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jRole> cache) {

        super(neo4jTemplate, neo4jClient);
        this.publisher = publisher;
        this.anyMatchDAO = anyMatchDAO;
        this.anySearchDAO = anySearchDAO;
        this.delegationDAO = delegationDAO;
        this.searchCondVisitor = searchCondVisitor;
        this.nodeValidator = nodeValidator;
        this.cache = cache;
    }

    @Override
    public Optional<? extends Role> findById(final String key) {
        return findById(key, Neo4jRole.class, cache);
    }

    @Override
    public List<Role> findByRealms(final Realm realm) {
        return findByRelationship(Neo4jRole.NODE, Neo4jRealm.NODE, realm.getKey(), Neo4jRole.class, cache);
    }

    @Override
    public Role save(final Role role) {
        ((Neo4jRole) role).list2json();
        Role saved = neo4jTemplate.save(nodeValidator.validate(role));
        ((Neo4jRole) saved).postSave();
        cache.put(EntityCacheKey.of(saved.getKey()), (Neo4jRole) saved);
        return saved;
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void delete(final Role role) {
        List<Neo4jUser> users = findByRelationship(
                Neo4jUser.NODE, Neo4jRole.NODE, role.getKey(), Neo4jUser.ROLE_MEMBERSHIP_REL, Neo4jUser.class, null);

        users.forEach(user -> {
            user.getRoles().remove(role);
            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, user, AuthContextUtils.getDomain()));
        });

        delegationDAO.findByRoles(role).forEach(delegation -> delegation.getRoles().remove(role));

        cache.remove(EntityCacheKey.of(role.getKey()));

        neo4jTemplate.deleteById(role.getKey(), Neo4jRole.class);
    }
}
