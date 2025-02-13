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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealmMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;

public class DynRealmRepoExtImpl extends AbstractDAO implements DynRealmRepoExt {

    protected final ApplicationEventPublisher publisher;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnySearchDAO searchDAO;

    protected final AnyMatchDAO anyMatchDAO;

    protected final SearchCondVisitor searchCondVisitor;

    protected final NodeValidator nodeValidator;

    public DynRealmRepoExtImpl(
            final ApplicationEventPublisher publisher,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.publisher = publisher;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.searchDAO = searchDAO;
        this.anyMatchDAO = anyMatchDAO;
        this.searchCondVisitor = searchCondVisitor;
        this.nodeValidator = nodeValidator;
    }

    protected List<String> clearDynMembers(final DynRealm dynRealm) {
        String query = "MATCH (n)-[r:" + DYN_REALM_MEMBERSHIP_REL + "]-(p:" + Neo4jDynRealm.NODE + " {id: $id}) ";
        Map<String, Object> parameters = Map.of("id", dynRealm.getKey());

        List<String> cleared = neo4jClient.query(
                query + "RETURN n.id").
                bindAll(parameters).
                fetch().all().stream().map(found -> found.get("n.id").toString()).collect(Collectors.toList());

        neo4jClient.query(query + "DETACH DELETE r").bindAll(parameters).run();

        return cleared;
    }

    protected void notifyDynMembershipRemoval(final List<String> anyKeys) {
        anyKeys.forEach(key -> {
            Optional<? extends Any> any = userDAO.findById(key);
            if (any.isEmpty()) {
                any = groupDAO.findById(key);
            }
            if (any.isEmpty()) {
                any = anyObjectDAO.findById(key);
            }
            any.ifPresent(entity -> publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, entity, AuthContextUtils.getDomain())));
        });
    }

    protected String node(final Any any) {
        return switch (any.getType().getKind()) {
            case USER ->
                Neo4jUser.NODE;
            case GROUP ->
                Neo4jGroup.NODE;
            case ANY_OBJECT ->
                Neo4jAnyObject.NODE;
            default ->
                "";
        };
    }

    @Override
    public DynRealm saveAndRefreshDynMemberships(final DynRealm dynRealm) {
        DynRealm merged = neo4jTemplate.save(nodeValidator.validate(dynRealm));

        // refresh dynamic memberships
        List<String> cleared = clearDynMembers(merged);

        merged.getDynMemberships().stream().map(memb -> searchDAO.search(
                SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()), memb.getAnyType().getKind())).
                forEach(matching -> matching.forEach(any -> {

            neo4jClient.query(
                    "MATCH (a:" + node(any) + " {id: $aid}), (b:" + Neo4jDynRealm.NODE + "{id: $rid}) "
                    + "CREATE (a)-[:" + DYN_REALM_MEMBERSHIP_REL + "]->(b)").
                    bindAll(Map.of("aid", any.getKey(), "rid", merged.getKey())).run();

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, any, AuthContextUtils.getDomain()));
            cleared.remove(any.getKey());
        }));

        notifyDynMembershipRemoval(cleared);

        return merged;
    }

    @Override
    public void deleteById(final String key) {
        neo4jTemplate.findById(key, Neo4jDynRealm.class).ifPresent(dynRealm -> {
            cascadeDelete(
                    Neo4jDynRealmMembership.NODE,
                    Neo4jDynRealm.NODE,
                    key);

            notifyDynMembershipRemoval(clearDynMembers(dynRealm));

            neo4jTemplate.deleteById(key, Neo4jDynRealm.class);
        });
    }

    @Transactional
    @Override
    public void refreshDynMemberships(final Any any) {
        neo4jTemplate.findAll(Neo4jDynRealm.class).
                forEach(dynRealm -> dynRealm.getDynMembership(any.getType()).ifPresent(memb -> {

            boolean matches = anyMatchDAO.matches(
                    any, SearchCondConverter.convert(searchCondVisitor, memb.getFIQLCond()));

            boolean existing = neo4jTemplate.count(
                    "MATCH (n {id: $aid})-[:" + DYN_REALM_MEMBERSHIP_REL + "]-(p:" + Neo4jDynRealm.NODE + "{id: $pid}) "
                    + "RETURN COUNT(p)",
                    Map.of("aid", any.getKey(), "pid", dynRealm.getKey())) > 0;

            if (matches && !existing) {
                neo4jClient.query(
                        "MATCH (a:" + node(any) + " {id: $aid}), (b:" + Neo4jDynRealm.NODE + "{id: $rid}) "
                        + "CREATE (a)-[:" + DYN_REALM_MEMBERSHIP_REL + "]->(b)").
                        bindAll(Map.of("aid", any.getKey(), "rid", dynRealm.getKey())).run();
            } else if (!matches && existing) {
                neo4jClient.query(
                        "MATCH (n {id: $aid})-"
                        + "[r:" + DYN_REALM_MEMBERSHIP_REL + "]-"
                        + "(p:" + Neo4jDynRealm.NODE + " {id: $rid}) "
                        + "DETACH DELETE r").bindAll(Map.of("aid", any.getKey(), "rid", dynRealm.getKey())).run();
            }
        }));
    }

    @Override
    public void removeDynMemberships(final String anyKey) {
        neo4jClient.query(
                "MATCH (n {id: $id})-[r:" + DYN_REALM_MEMBERSHIP_REL + "]-(p:" + Neo4jDynRealm.NODE + ") "
                + "DETACH DELETE r").bindAll(Map.of("id", anyKey)).run();
    }
}
