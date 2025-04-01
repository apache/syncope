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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
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
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class JPARealmDAO extends AbstractDAO<Realm> implements RealmDAO {

    protected static int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected static StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final String keyword,
            final List<Object> parameters) {

        String basesClause = bases.stream().
                map(base -> "e.fullPath=?" + setParameter(parameters, base)
                + " OR e.fullPath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARealm.class.getSimpleName()).append(" e ").
                append("WHERE (").append(basesClause).append(')');

        if (keyword != null) {
            queryString.append(" AND LOWER(e.name) LIKE ?").
                    append(setParameter(parameters, "%" + keyword.replaceAll("_", "\\\\_").toLowerCase() + "%"));
        }

        return queryString;
    }

    protected final RoleDAO roleDAO;

    protected final ApplicationEventPublisher publisher;

    public JPARealmDAO(final RoleDAO roleDAO, final ApplicationEventPublisher publisher) {
        this.roleDAO = roleDAO;
        this.publisher = publisher;
    }

    @Override
    public Realm getRoot() {
        TypedQuery<Realm> query = entityManager().createQuery("SELECT e FROM " + JPARealm.class.getSimpleName() + " e "
                + "WHERE e.parent IS NULL", Realm.class);

        Realm result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Root realm not found", e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Realm find(final String key) {
        return entityManager().find(JPARealm.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public Realm findByFullPath(final String fullPath) {
        if (SyncopeConstants.ROOT_REALM.equals(fullPath)) {
            return getRoot();
        }

        if (StringUtils.isBlank(fullPath) || !PATH_PATTERN.matcher(fullPath).matches()) {
            throw new MalformedPathException(fullPath);
        }

        TypedQuery<Realm> query = entityManager().createQuery("SELECT e FROM " + JPARealm.class.getSimpleName() + " e "
                + "WHERE e.fullPath=:fullPath", Realm.class);
        query.setParameter("fullPath", fullPath);

        Realm result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Root realm not found", e);
        }

        return result;
    }

    @Override
    public List<Realm> findByName(final String name) {
        TypedQuery<Realm> query = entityManager().createQuery("SELECT e FROM " + JPARealm.class.getSimpleName() + " e "
                + "WHERE e.name=:name", Realm.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public List<Realm> findByResource(final ExternalResource resource) {
        TypedQuery<Realm> query = entityManager().createQuery("SELECT e FROM " + JPARealm.class.getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources", Realm.class);
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public int countDescendants(final String base, final String keyword) {
        return countDescendants(Set.of(base), keyword);
    }

    @Override
    public int countDescendants(final Set<String> bases, final String keyword) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters);
        Query query = entityManager().createQuery(StringUtils.replaceOnce(
                queryString.toString(),
                "SELECT e ",
                "SELECT COUNT(e) "));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<Realm> findDescendants(
            final String base,
            final String keyword,
            final int page,
            final int itemsPerPage) {

        return findDescendants(Set.of(base), keyword, page, itemsPerPage);
    }

    @Override
    public List<Realm> findDescendants(
            final Set<String> bases,
            final String keyword,
            final int page,
            final int itemsPerPage) {

        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, keyword, parameters);
        TypedQuery<Realm> query = entityManager().createQuery(
                queryString.append(" ORDER BY e.fullPath").toString(), Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(Set.of(base), null, parameters);
        TypedQuery<Realm> query = entityManager().createQuery(queryString.
                append(" AND (e.fullPath=?").
                append(setParameter(parameters, prefix)).
                append(" OR e.fullPath LIKE ?").
                append(setParameter(parameters, SyncopeConstants.ROOT_REALM.equals(prefix) ? "/%" : prefix + "/%")).
                append(')').
                append(" ORDER BY e.fullPath").toString(),
                Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return query.getResultList().stream().map(Realm::getKey).collect(Collectors.toList());
    }

    protected <T extends Policy> List<Realm> findSamePolicyChildren(final Realm realm, final T policy) {
        List<Realm> result = new ArrayList<>();

        findChildren(realm).stream().
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

        String policyColumn = null;
        if (policy instanceof AccountPolicy) {
            policyColumn = "accountPolicy";
        } else if (policy instanceof PasswordPolicy) {
            policyColumn = "passwordPolicy";
        } else if (policy instanceof AuthPolicy) {
            policyColumn = "authPolicy";
        } else if (policy instanceof AccessPolicy) {
            policyColumn = "accessPolicy";
        } else if (policy instanceof AttrReleasePolicy) {
            policyColumn = "attrReleasePolicy";
        } else if (policy instanceof TicketExpirationPolicy) {
            policyColumn = "ticketExpirationPolicy";
        }

        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e."
                + policyColumn + "=:policy", Realm.class);
        query.setParameter("policy", policy);

        List<Realm> result = new ArrayList<>();
        query.getResultList().forEach(realm -> {
            result.add(realm);
            result.addAll(findSamePolicyChildren(realm, policy));
        });

        return result;
    }

    @Override
    public List<Realm> findByLogicActions(final Implementation logicActions) {
        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e "
                + "WHERE :logicActions MEMBER OF e.actions", Realm.class);
        query.setParameter("logicActions", logicActions);

        return query.getResultList();
    }

    protected void findAncestors(final List<Realm> result, final Realm realm) {
        if (realm.getParent() != null && !result.contains(realm.getParent())) {
            result.add(realm.getParent());
            findAncestors(result, realm.getParent());
        }
    }

    @Override
    public List<Realm> findAncestors(final Realm realm) {
        List<Realm> result = new ArrayList<>();
        result.add(realm);
        findAncestors(result, realm);
        return result;
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        TypedQuery<Realm> query = entityManager().createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.parent=:realm", Realm.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    protected String buildFullPath(final Realm realm) {
        return realm.getParent() == null
                ? SyncopeConstants.ROOT_REALM
                : StringUtils.appendIfMissing(realm.getParent().getFullPath(), "/") + realm.getName();
    }

    @Override
    public int count() {
        Query query = entityManager().createNativeQuery(
                "SELECT COUNT(id) FROM " + JPARealm.TABLE);
        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<String> findAllKeys(final int page, final int itemsPerPage) {
        Query query = entityManager().createNativeQuery("SELECT id FROM " + JPARealm.TABLE + " ORDER BY fullPath");

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        @SuppressWarnings("unchecked")
        List<Object> raw = query.getResultList();
        return raw.stream().map(key -> key instanceof Object[]
                ? (String) ((Object[]) key)[0]
                : ((String) key)).collect(Collectors.toList());
    }

    @Override
    public Realm save(final Realm realm) {
        String fullPathBefore = realm.getFullPath();
        String fullPathAfter = buildFullPath(realm);
        if (!fullPathAfter.equals(fullPathBefore)) {
            ((JPARealm) realm).setFullPath(fullPathAfter);
        }

        Realm merged = entityManager().merge(realm);

        if (!fullPathAfter.equals(fullPathBefore)) {
            findChildren(realm).forEach(this::save);
        }

        publisher.publishEvent(
                new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, merged, AuthContextUtils.getDomain()));

        return merged;
    }

    @Override
    public void delete(final Realm realm) {
        if (realm == null || realm.getParent() == null) {
            return;
        }

        findDescendants(realm.getFullPath(), null, -1, -1).forEach(toBeDeleted -> {
            roleDAO.findByRealm(toBeDeleted).forEach(role -> role.getRealms().remove(toBeDeleted));

            toBeDeleted.setParent(null);

            entityManager().remove(toBeDeleted);

            publisher.publishEvent(
                    new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, toBeDeleted, AuthContextUtils.getDomain()));
        });
    }
}
