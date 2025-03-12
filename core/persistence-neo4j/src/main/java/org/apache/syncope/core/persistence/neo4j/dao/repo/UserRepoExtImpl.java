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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jLinkedAccount;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jSecurityQuestion;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserRepoExtImpl extends AbstractAnyRepoExt<User, Neo4jUser> implements UserRepoExt {

    protected final RoleDAO roleDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final GroupDAO groupDAO;

    protected final DelegationDAO delegationDAO;

    protected final FIQLQueryDAO fiqlQueryDAO;

    protected final SecurityProperties securityProperties;

    protected final NodeValidator nodeValidator;

    protected final Cache<EntityCacheKey, Neo4jUser> userCache;

    public UserRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final AnyFinder anyFinder,
            final SecurityProperties securityProperties,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jUser> userCache) {

        super(
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                anyFinder,
                anyUtilsFactory.getInstance(AnyTypeKind.USER),
                neo4jTemplate,
                neo4jClient);
        this.roleDAO = roleDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.groupDAO = groupDAO;
        this.delegationDAO = delegationDAO;
        this.fiqlQueryDAO = fiqlQueryDAO;
        this.securityProperties = securityProperties;
        this.nodeValidator = nodeValidator;
        this.userCache = userCache;
    }

    @Override
    protected Cache<EntityCacheKey, Neo4jUser> cache() {
        return userCache;
    }

    @Override
    public Optional<? extends User> findByToken(final String token) {
        return neo4jClient.query(
                "MATCH (n:" + Neo4jUser.NODE + ") WHERE n.token = $token RETURN n.id").
                bindAll(Map.of("token", token)).fetch().one().
                flatMap(toOptional("n.id", Neo4jUser.class, userCache));
    }

    @Override
    public List<User> findBySecurityQuestion(final SecurityQuestion securityQuestion) {
        return findByRelationship(
                Neo4jUser.NODE,
                Neo4jSecurityQuestion.NODE,
                securityQuestion.getKey(),
                Neo4jUser.class,
                userCache);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OffsetDateTime> findLastChange(final String key) {
        return findLastChange(key, Neo4jUser.NODE);
    }

    @Transactional(readOnly = true)
    @Override
    public void securityChecks(
            final Set<String> authRealms,
            final String key,
            final String realm,
            final Collection<String> groups) {

        // 1. check if AuthContextUtils.getUsername() is owner of at least one group of which user is member
        boolean authorized = authRealms.stream().
                map(authRealm -> RealmUtils.parseGroupOwnerRealm(authRealm).orElse(null)).
                filter(Objects::nonNull).
                anyMatch(pair -> groups.contains(pair.getRight()));

        // 2. check if user is in at least one DynRealm for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized && key != null) {
            authorized = findDynRealms(key).stream().anyMatch(authRealms::contains);
        }

        // 3. check if user is in Realm (or descendants) for which AuthContextUtils.getUsername() owns entitlement
        if (!authorized) {
            authorized = authRealms.stream().anyMatch(realm::startsWith);
        }

        if (!authorized) {
            Optional.ofNullable(key).map(EntityCacheKey::of).ifPresent(userCache::remove);
            throw new DelegatedAdministrationException(realm, AnyTypeKind.USER.name(), key);
        }
    }

    @Override
    protected void securityChecks(final User user) {
        // Allows anonymous (during self-registration) and self (during self-update) to read own user,
        // otherwise goes through security checks to see if required entitlements are owned
        if (!AuthContextUtils.getUsername().equals(securityProperties.getAnonymousUser())
                && !AuthContextUtils.getUsername().equals(user.getUsername())) {

            Set<String> authRealms = AuthContextUtils.getAuthorizations().
                    getOrDefault(IdRepoEntitlement.USER_READ, Set.of());

            securityChecks(authRealms, user.getKey(), user.getRealm().getFullPath(), findAllGroupKeys(user));
        }
    }

    @Override
    public Map<String, Long> countByRealm() {
        Collection<Map<String, Object>> result = neo4jClient.query(
                "MATCH (n:" + Neo4jUser.NODE + ")-[]-(r:" + Neo4jRealm.NODE + ") "
                + "RETURN r.fullPath AS realm, COUNT(n) AS counted").fetch().all();

        return result.stream().collect(Collectors.toMap(r -> r.get("realm").toString(), r -> (Long) r.get("counted")));
    }

    @Override
    public Map<String, Long> countByStatus() {
        Collection<Map<String, Object>> result = neo4jClient.query(
                "MATCH (n:" + Neo4jUser.NODE + ") "
                + "RETURN n.status AS status, COUNT(n) AS counted").fetch().all();

        return result.stream().collect(Collectors.toMap(r -> r.get("status").toString(), r -> (Long) r.get("counted")));
    }

    @Override
    public UMembership findMembership(final String key) {
        return neo4jTemplate.findById(key, Neo4jUMembership.class).orElse(null);
    }

    @Override
    public void deleteMembership(final UMembership membership) {
        neo4jTemplate.deleteById(membership.getKey(), Neo4jUMembership.class);
    }

    @Override
    protected <T extends Attributable> void checkBeforeSave(final T user) {
        super.checkBeforeSave(user);
        ((User) user).getLinkedAccounts().forEach(super::checkBeforeSave);
    }

    protected Pair<User, Pair<Set<String>, Set<String>>> doSave(final User user) {
        checkBeforeSave(user);

        // unlink any role, resource, aux class or security question that was unlinked from user
        // delete any membership, relationship or linked account that was removed from user
        neo4jTemplate.findById(user.getKey(), Neo4jUser.class).ifPresent(before -> {
            before.getRoles().stream().filter(role -> !user.getRoles().contains(role)).
                    forEach(role -> deleteRelationship(
                    Neo4jUser.NODE,
                    Neo4jRole.NODE,
                    user.getKey(),
                    role.getKey(),
                    Neo4jUser.ROLE_MEMBERSHIP_REL));
            before.getResources().stream().filter(resource -> !user.getResources().contains(resource)).
                    forEach(resource -> deleteRelationship(
                    Neo4jUser.NODE,
                    Neo4jExternalResource.NODE,
                    user.getKey(),
                    resource.getKey(),
                    Neo4jUser.USER_RESOURCE_REL));
            before.getAuxClasses().stream().filter(auxClass -> !user.getAuxClasses().contains(auxClass)).
                    forEach(auxClass -> deleteRelationship(
                    Neo4jUser.NODE,
                    Neo4jAnyTypeClass.NODE,
                    user.getKey(),
                    auxClass.getKey(),
                    Neo4jUser.USER_AUX_CLASSES_REL));
            if (before.getSecurityQuestion() != null && user.getSecurityQuestion() == null) {
                deleteRelationship(
                        Neo4jUser.NODE,
                        Neo4jSecurityQuestion.NODE,
                        user.getKey(),
                        before.getSecurityQuestion().getKey(),
                        Neo4jUser.USER_SECURITY_QUESTION_REL);
            }

            Set<String> beforeMembs = before.getMemberships().stream().map(UMembership::getKey).
                    collect(Collectors.toSet());
            beforeMembs.removeAll(user.getMemberships().stream().map(UMembership::getKey).toList());
            beforeMembs.forEach(m -> neo4jTemplate.deleteById(m, Neo4jUMembership.class));

            Set<String> beforeRels = before.getRelationships().stream().map(URelationship::getKey).
                    collect(Collectors.toSet());
            beforeRels.removeAll(user.getRelationships().stream().map(URelationship::getKey).toList());
            beforeRels.forEach(r -> neo4jTemplate.deleteById(r, Neo4jURelationship.class));

            Set<String> beforeLAs = before.getLinkedAccounts().stream().map(LinkedAccount::getKey).
                    collect(Collectors.toSet());
            beforeLAs.removeAll(user.getLinkedAccounts().stream().map(LinkedAccount::getKey).toList());
            beforeLAs.forEach(la -> neo4jTemplate.deleteById(la, Neo4jLinkedAccount.class));
        });

        User merged = neo4jTemplate.save(nodeValidator.validate(user));

        userCache.put(EntityCacheKey.of(merged.getKey()), (Neo4jUser) merged);

        roleDAO.refreshDynMemberships(merged);
        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends User> S save(final S user) {
        return (S) doSave(user).getLeft();
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final User user) {
        return doSave(user).getRight();
    }

    @Override
    public void delete(final User user) {
        roleDAO.removeDynMemberships(user.getKey());
        groupDAO.removeDynMemberships(user);
        dynRealmDAO.removeDynMemberships(user.getKey());

        delegationDAO.findByDelegating(user).forEach(delegationDAO::delete);
        delegationDAO.findByDelegated(user).forEach(delegationDAO::delete);

        fiqlQueryDAO.findByOwner(user, null).forEach(fiqlQueryDAO::delete);

        accessTokenDAO.findByOwner(user.getUsername()).ifPresent(accessTokenDAO::delete);

        userCache.remove(EntityCacheKey.of(user.getKey()));

        cascadeDelete(
                Neo4jURelationship.NODE,
                Neo4jUser.NODE,
                user.getKey());

        cascadeDelete(
                Neo4jUMembership.NODE,
                Neo4jUser.NODE,
                user.getKey());

        cascadeDelete(
                Neo4jLinkedAccount.NODE,
                Neo4jUser.NODE,
                user.getKey());

        neo4jTemplate.deleteById(user.getKey(), Neo4jUser.class);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Role> findAllRoles(final User user) {
        Set<Role> result = new HashSet<>();
        result.addAll(user.getRoles());
        result.addAll(findDynRoles(user.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Role> findDynRoles(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jUser.NODE + " {id: $id})-"
                + "[:" + RoleRepoExt.DYN_ROLE_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jRole.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jRole.class,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public List<Group> findDynGroups(final String key) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jUser.NODE + " {id: $id})-"
                + "[:" + GroupRepoExt.DYN_GROUP_USER_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jGroup.NODE + ") "
                + "RETURN p.id").bindAll(Map.of("id", key)).fetch().all(),
                "p.id",
                Neo4jGroup.class,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<Group> findAllGroups(final User user) {
        Set<Group> result = new HashSet<>();
        result.addAll(user.getMemberships().stream().
                map(UMembership::getRightEnd).collect(Collectors.toSet()));
        result.addAll(findDynGroups(user.getKey()));

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupKeys(final User user) {
        return findAllGroups(user).stream().map(Group::getKey).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<String> findAllGroupNames(final User user) {
        return findAllGroups(user).stream().map(Group::getName).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    public Collection<ExternalResource> findAllResources(final User user) {
        Set<ExternalResource> result = new HashSet<>();
        result.addAll(user.getResources());
        findAllGroups(user).forEach(group -> result.addAll(group.getResources()));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<String> findAllResourceKeys(final String key) {
        return findAllResources(authFind(key)).stream().map(ExternalResource::getKey).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public boolean linkedAccountExists(final String userKey, final String connObjectKeyValue) {
        if (connObjectKeyValue == null) {
            return false;
        }

        return neo4jTemplate.count(
                "MATCH (n:" + Neo4jUser.NODE + " {id: $id})-[]-(p:" + Neo4jLinkedAccount.NODE + ") "
                + "WHERE p.connObjectKeyValue = $connObjectKeyValue "
                + "RETURN COUNT(p)",
                Map.of("id", userKey, "connObjectKeyValue", connObjectKeyValue)) > 0;
    }

    @Override
    public Optional<? extends LinkedAccount> findLinkedAccount(
            final ExternalResource resource,
            final String connObjectKeyValue) {

        if (connObjectKeyValue == null) {
            return Optional.empty();
        }

        return neo4jClient.query(
                "MATCH (n:" + Neo4jLinkedAccount.NODE + ")-[]-"
                + "(e:" + Neo4jExternalResource.NODE + " {id: $resource}) "
                + "WHERE n.connObjectKeyValue = $connObjectKeyValue "
                + "RETURN n.id").
                bindAll(Map.of("resource", resource.getKey(), "connObjectKeyValue", connObjectKeyValue)).fetch().one().
                flatMap(toOptional("n.id", Neo4jLinkedAccount.class, null));
    }

    @Override
    public List<LinkedAccount> findLinkedAccounts(final String userKey) {
        return findByRelationship(
                Neo4jLinkedAccount.NODE,
                Neo4jUser.NODE,
                userKey,
                Neo4jLinkedAccount.class,
                null);
    }

    @Override
    public List<LinkedAccount> findLinkedAccountsByResource(final ExternalResource resource) {
        return findByRelationship(
                Neo4jLinkedAccount.NODE,
                Neo4jExternalResource.NODE,
                resource.getKey(),
                Neo4jLinkedAccount.class,
                null);
    }
}
