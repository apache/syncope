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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAGroupDAO extends AbstractSubjectDAO<GPlainAttr, GDerAttr, GVirAttr> implements GroupDAO {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private AttributableUtilsFactory attrUtilsFactory;

    @Override
    protected Subject<GPlainAttr, GDerAttr, GVirAttr> findInternal(final Long key) {
        return find(key);
    }

    @Override
    public Group find(final Long key) {
        TypedQuery<Group> query = entityManager.createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.id = :id", Group.class);
        query.setParameter("id", key);

        Group result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No group found with id {}", key, e);
        }

        return result;
    }

    @Override
    public Group find(final String name) {
        TypedQuery<Group> query = entityManager.createQuery(
                "SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.name = :name", Group.class);
        query.setParameter("name", name);

        Group result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No group found with name {}", name, e);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByUser(final Long userKey) {
        User owner = userDAO.find(userKey);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAGroup.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (Long groupKey : owner.getGroupKeys()) {
            queryString.append("OR e.groupOwner.id=").append(groupKey).append(' ');
        }

        TypedQuery<Group> query = entityManager.createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Group> findOwnedByGroup(final Long groupId) {
        Group owner = find(groupId);
        if (owner == null) {
            return Collections.<Group>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPAGroup.class.getSimpleName()).
                append(" e WHERE e.groupOwner=:owner ");

        TypedQuery<Group> query = entityManager.createQuery(queryString.toString(), Group.class);
        query.setParameter("owner", owner);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByAttrValue(final String schemaName, final GPlainAttrValue attrValue) {
        return (List<Group>) findByAttrValue(
                schemaName, attrValue, attrUtilsFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Group findByAttrUniqueValue(final String schemaName, final GPlainAttrValue attrUniqueValue) {
        return (Group) findByAttrUniqueValue(
                schemaName, attrUniqueValue, attrUtilsFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByDerAttrValue(final String schemaName, final String value) {
        return (List<Group>) findByDerAttrValue(
                schemaName, value, attrUtilsFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByResource(final ExternalResource resource) {
        return (List<Group>) findByResource(resource, attrUtilsFactory.getInstance(AttributableType.GROUP));
    }

    @Override
    public final List<Group> findAll(final Set<String> adminRealms, final int page, final int itemsPerPage) {
        return findAll(adminRealms, page, itemsPerPage, Collections.<OrderByClause>emptyList());
    }

    @Override
    public List<Group> findAll(final Set<String> adminRealms,
            final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {

        return searchDAO.search(adminRealms, getAllMatchingCond(), page, itemsPerPage, orderBy, SubjectType.GROUP);
    }

    @Override
    public final int count(final Set<String> adminRealms) {
        return searchDAO.count(adminRealms, getAllMatchingCond(), SubjectType.GROUP);
    }

    @Override
    public List<Membership> findMemberships(final Group group) {
        TypedQuery<Membership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAMembership.class.getSimpleName() + " e WHERE e.group=:group", Membership.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> unmatched(final Long groupId,
            final Class<?> attrClass, final Class<? extends AttrTemplate<?>> attrTemplateClass) {

        final Query query = entityManager.createNativeQuery(new StringBuilder().
                append("SELECT ma.id ").
                append("FROM ").append(JPAMembership.TABLE).append(" m, ").
                append(attrClass.getSimpleName()).append(" ma ").
                append("WHERE m.group_id = ?1 ").
                append("AND ma.owner_id = m.id ").
                append("AND ma.template_id NOT IN (").
                append("SELECT id ").
                append("FROM ").append(attrTemplateClass.getSimpleName()).append(' ').
                append("WHERE owner_id = ?1)").toString());
        query.setParameter(1, groupId);

        return query.getResultList();
    }

    @Override
    public Group save(final Group group) {
        // remove plain attributes without a valid template
        List<GPlainAttr> rToBeDeleted = new ArrayList<>();
        for (final PlainAttr attr : group.getPlainAttrs()) {
            boolean found = CollectionUtils.exists(group.getAttrTemplates(GPlainAttrTemplate.class),
                    new Predicate<GPlainAttrTemplate>() {

                        @Override
                        public boolean evaluate(final GPlainAttrTemplate template) {
                            return template.getSchema().equals(attr.getSchema());
                        }
                    });
            if (!found) {
                rToBeDeleted.add((GPlainAttr) attr);
            }
        }
        for (GPlainAttr attr : rToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, group);
            group.removePlainAttr(attr);
        }

        // remove derived attributes without a valid template
        List<GDerAttr> rDerToBeDeleted = new ArrayList<>();
        for (final DerAttr attr : group.getDerAttrs()) {
            boolean found = CollectionUtils.exists(group.getAttrTemplates(GDerAttrTemplate.class),
                    new Predicate<GDerAttrTemplate>() {

                        @Override
                        public boolean evaluate(final GDerAttrTemplate template) {
                            return template.getSchema().equals(attr.getSchema());
                        }
                    });
            if (!found) {
                rDerToBeDeleted.add((GDerAttr) attr);
            }
        }
        for (GDerAttr attr : rDerToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, group);
            group.removeDerAttr(attr);
        }

        // remove virtual attributes without a valid template
        List<GVirAttr> rVirToBeDeleted = new ArrayList<>();
        for (final VirAttr attr : group.getVirAttrs()) {
            boolean found = CollectionUtils.exists(group.getAttrTemplates(GVirAttrTemplate.class),
                    new Predicate<GVirAttrTemplate>() {

                        @Override
                        public boolean evaluate(final GVirAttrTemplate template) {
                            return template.getSchema().equals(attr.getSchema());
                        }
                    });
            if (!found) {
                LOG.debug("Removing {} from {} because no template is available for it", attr, group);
                rVirToBeDeleted.add((GVirAttr) attr);
            }
        }
        for (GVirAttr attr : rVirToBeDeleted) {
            group.removeVirAttr(attr);
        }

        Group merged = entityManager.merge(group);

        // Now the same process for any exising membership of the group being saved
        if (group.getKey() != null) {
            for (Long key : unmatched(group.getKey(), MPlainAttr.class, MPlainAttrTemplate.class)) {
                LOG.debug("Removing MAttr[{}] because no template is available for it in {}", key, group);
                plainAttrDAO.delete(key, MPlainAttr.class);
            }
            for (Long id : unmatched(group.getKey(), MDerAttr.class, MDerAttrTemplate.class)) {
                LOG.debug("Removing MDerAttr[{}] because no template is available for it in {}", id, group);
                derAttrDAO.delete(id, MDerAttr.class);
            }
            for (Long id : unmatched(group.getKey(), MVirAttr.class, MVirAttrTemplate.class)) {
                LOG.debug("Removing MVirAttr[{}] because no template is available for it in {}", id, group);
                virAttrDAO.delete(id, MVirAttr.class);
            }
        }

        merged = entityManager.merge(merged);
        for (VirAttr attr : merged.getVirAttrs()) {
            attr.getValues().clear();
            attr.getValues().addAll(group.getVirAttr(attr.getSchema().getKey()).getValues());
        }

        return merged;
    }

    @Override
    public void delete(final Group group) {
        for (Membership membership : findMemberships(group)) {
            membership.getUser().removeMembership(membership);
            userDAO.save(membership.getUser());

            entityManager.remove(membership);
        }

        entityManager.remove(group);
    }

    @Override
    public void delete(final Long key) {
        Group group = (Group) findInternal(key);
        if (group == null) {
            return;
        }

        delete(group);
    }

    @Override
    public Group authFetch(final Long key) {
        if (key == null) {
            throw new NotFoundException("Null group id");
        }

        final Group group = find(key);
        if (group == null) {
            throw new NotFoundException("Group " + key);
        }

        Set<String> authRealms = AuthContextUtils.getAuthorizations().get(Entitlement.GROUP_READ);
        boolean authorized = CollectionUtils.exists(authRealms, new Predicate<String>() {

            @Override
            public boolean evaluate(final String realm) {
                return group.getRealm().getFullPath().startsWith(realm)
                        || realm.equals(RealmUtils.getGroupOwnerRealm(group.getRealm().getFullPath(), group.getKey()));
            }
        });
        if (authRealms == null || authRealms.isEmpty() || !authorized) {
            throw new UnauthorizedException(SubjectType.GROUP, group.getKey());
        }

        return group;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<Long, PropagationByResource> findUsersWithIndirectResources(final Long groupKey) {
        Group group = authFetch(groupKey);

        Map<Long, PropagationByResource> result = new HashMap<>();

        for (Membership membership : findMemberships(group)) {
            User user = membership.getUser();

            PropagationByResource propByRes = new PropagationByResource();
            for (ExternalResource resource : group.getResources()) {
                if (!user.getOwnResources().contains(resource)) {
                    propByRes.add(ResourceOperation.DELETE, resource.getKey());
                }

                if (!propByRes.isEmpty()) {
                    result.put(user.getKey(), propByRes);
                }
            }
        }

        return result;
    }
}
