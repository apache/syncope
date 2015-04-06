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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Policy;
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
import org.apache.syncope.core.misc.security.AuthContextUtil;
import org.apache.syncope.core.misc.security.UnauthorizedGroupException;
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
    private EntitlementDAO entitlementDAO;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    @Override
    protected Subject<GPlainAttr, GDerAttr, GVirAttr> findInternal(final Long key) {
        return find(key);
    }

    @Override
    public Group find(final Long key) {
        TypedQuery<Group> query = entityManager.createQuery("SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.id = :id", Group.class);
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
    public List<Group> find(final String name) {
        TypedQuery<Group> query = entityManager.createQuery("SELECT e FROM " + JPAGroup.class.getSimpleName() + " e WHERE e.name = :name", Group.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public Group find(final String name, final Long parentId) {
        TypedQuery<Group> query;
        if (parentId == null) {
            query = entityManager.createQuery("SELECT r FROM " + JPAGroup.class.getSimpleName() + " r WHERE "
                    + "r.name=:name AND r.parent IS NULL", Group.class);
        } else {
            query = entityManager.createQuery("SELECT r FROM " + JPAGroup.class.getSimpleName() + " r WHERE "
                    + "r.name=:name AND r.parent.id=:parentId", Group.class);
            query.setParameter("parentId", parentId);
        }
        query.setParameter("name", name);

        List<Group> result = query.getResultList();
        return result.isEmpty()
                ? null
                : result.get(0);
    }

    private void findSameOwnerDescendants(final List<Group> result, final Group group) {
        List<Group> children = findChildren(group);
        if (children != null) {
            for (Group child : children) {
                if ((child.getUserOwner() == null && child.getGroupOwner() == null && child.isInheritOwner())
                        || (child.getUserOwner() != null && child.getUserOwner().equals(group.getUserOwner()))
                        || (child.getGroupOwner() != null && child.getGroupOwner().equals(group.getGroupOwner()))) {

                    findDescendants(result, child);
                }
            }
        }
        result.add(group);
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

        List<Group> result = new ArrayList<>();
        for (Group group : query.getResultList()) {
            findSameOwnerDescendants(result, group);
        }

        return result;
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

        List<Group> result = new ArrayList<Group>();
        for (Group group : query.getResultList()) {
            findSameOwnerDescendants(result, group);
        }

        return result;
    }

    @Override
    public List<Group> findByEntitlement(final Entitlement entitlement) {
        TypedQuery<Group> query = entityManager.createQuery("SELECT e FROM " + JPAGroup.class.getSimpleName() + " e "
                + "WHERE :entitlement MEMBER OF e.entitlements", Group.class);
        query.setParameter("entitlement", entitlement);

        return query.getResultList();
    }

    private Map.Entry<String, String> getPolicyFields(final PolicyType type) {
        String policyField;
        String inheritPolicyField;
        if (type == PolicyType.GLOBAL_ACCOUNT || type == PolicyType.ACCOUNT) {
            policyField = "accountPolicy";
            inheritPolicyField = "inheritAccountPolicy";
        } else {
            policyField = "passwordPolicy";
            inheritPolicyField = "inheritPasswordPolicy";
        }

        return new AbstractMap.SimpleEntry<>(policyField, inheritPolicyField);
    }

    private List<Group> findSamePolicyChildren(final Group group, final PolicyType type) {
        List<Group> result = new ArrayList<>();

        for (Group child : findChildren(group)) {
            boolean inherit = type == PolicyType.GLOBAL_ACCOUNT || type == PolicyType.ACCOUNT
                    ? child.isInheritAccountPolicy()
                    : child.isInheritPasswordPolicy();
            if (inherit) {
                result.add(child);
                result.addAll(findSamePolicyChildren(child, type));
            }
        }

        return result;
    }

    @Override
    public List<Group> findByPolicy(final Policy policy) {
        if (policy.getType() == PolicyType.GLOBAL_SYNC || policy.getType() == PolicyType.SYNC) {
            return Collections.<Group>emptyList();
        }

        Map.Entry<String, String> policyFields = getPolicyFields(policy.getType());
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAGroup.class.getSimpleName()).append(" e WHERE e.").
                append(policyFields.getKey()).append(" = :policy AND (e.").
                append(policyFields.getValue()).append(" IS NULL OR e.").
                append(policyFields.getValue()).append(" = 0)");

        TypedQuery<Group> query = entityManager.createQuery(queryString.toString(), Group.class);
        query.setParameter("policy", policy);

        List<Group> result = new ArrayList<>();
        for (Group group : query.getResultList()) {
            result.add(group);
            result.addAll(findSamePolicyChildren(group, policy.getType()));
        }
        return result;
    }

    @Override
    public List<Group> findWithoutPolicy(final PolicyType type) {
        if (type == PolicyType.GLOBAL_SYNC || type == PolicyType.SYNC) {
            return Collections.<Group>emptyList();
        }

        Map.Entry<String, String> policyFields = getPolicyFields(type);
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPAGroup.class.getSimpleName()).append(" e WHERE e.").
                append(policyFields.getKey()).append(" IS NULL AND (e.").
                append(policyFields.getValue()).append(" IS NULL OR e.").
                append(policyFields.getValue()).append(" = 0)");

        TypedQuery<Group> query = entityManager.createQuery(queryString.toString(), Group.class);
        return query.getResultList();
    }

    private void findAncestors(final List<Group> result, final Group group) {
        if (group.getParent() != null && !result.contains(group.getParent())) {
            result.add(group.getParent());
            findAncestors(result, group.getParent());
        }
    }

    @Override
    public List<Group> findAncestors(final Group group) {
        List<Group> result = new ArrayList<>();
        findAncestors(result, group);
        return result;
    }

    @Override
    public List<Group> findChildren(final Group group) {
        TypedQuery<Group> query = entityManager.createQuery(
                "SELECT g FROM " + JPAGroup.class.getSimpleName() + " g WHERE g.parent=:group", Group.class);
        query.setParameter("group", group);

        return query.getResultList();
    }

    private void findDescendants(final List<Group> result, final Group group) {
        List<Group> children = findChildren(group);
        if (children != null) {
            for (Group child : children) {
                findDescendants(result, child);
            }
        }
        result.add(group);
    }

    @Override
    public List<Group> findDescendants(final Group group) {
        List<Group> result = new ArrayList<>();
        findDescendants(result, group);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByAttrValue(final String schemaName, final GPlainAttrValue attrValue) {
        return (List<Group>) findByAttrValue(
                schemaName, attrValue, attrUtilFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Group findByAttrUniqueValue(final String schemaName, final GPlainAttrValue attrUniqueValue) {
        return (Group) findByAttrUniqueValue(schemaName, attrUniqueValue,
                attrUtilFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByDerAttrValue(final String schemaName, final String value) {
        return (List<Group>) findByDerAttrValue(
                schemaName, value, attrUtilFactory.getInstance(AttributableType.GROUP));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Group> findByResource(final ExternalResource resource) {
        return (List<Group>) findByResource(resource, attrUtilFactory.getInstance(AttributableType.GROUP));
    }

    @Override
    public List<Group> findAll() {
        return findAll(-1, -1, Collections.<OrderByClause>emptyList());
    }

    @Override
    public List<Group> findAll(final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {
        TypedQuery<Group> query = entityManager.createQuery("SELECT e FROM " + JPAGroup.class.getSimpleName() + " e "
                + toOrderByStatement(Group.class, "e", orderBy), Group.class);

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public List<Membership> findMemberships(final Group group) {
        TypedQuery<Membership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAMembership.class.getSimpleName() + " e"
                + " WHERE e.group=:group", Membership.class);
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
    public final int count() {
        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(e.id) FROM " + JPAGroup.TABLE + " e");

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public Group save(final Group group) {
        // reset account policy in case of inheritance
        if (group.isInheritAccountPolicy()) {
            group.setAccountPolicy(null);
        }

        // reset password policy in case of inheritance
        if (group.isInheritPasswordPolicy()) {
            group.setPasswordPolicy(null);
        }

        // remove attributes without a valid template
        List<GPlainAttr> rToBeDeleted = new ArrayList<>();
        for (PlainAttr attr : group.getPlainAttrs()) {
            boolean found = false;
            for (GPlainAttrTemplate template : group.findInheritedTemplates(GPlainAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                rToBeDeleted.add((GPlainAttr) attr);
            }
        }
        for (GPlainAttr attr : rToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, group);
            group.removePlainAttr(attr);
        }

        // remove derived attributes without a valid template
        List<GDerAttr> rDerToBeDeleted = new ArrayList<GDerAttr>();
        for (DerAttr attr : group.getDerAttrs()) {
            boolean found = false;
            for (GDerAttrTemplate template : group.findInheritedTemplates(GDerAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                rDerToBeDeleted.add((GDerAttr) attr);
            }
        }
        for (GDerAttr attr : rDerToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, group);
            group.removeDerAttr(attr);
        }

        // remove virtual attributes without a valid template
        List<GVirAttr> rVirToBeDeleted = new ArrayList<GVirAttr>();
        for (VirAttr attr : group.getVirAttrs()) {
            boolean found = false;
            for (GVirAttrTemplate template : group.findInheritedTemplates(GVirAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
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

        entitlementDAO.saveGroupEntitlement(merged);

        return merged;
    }

    @Override
    public void delete(final Group group) {
        for (Group groupToBeDeleted : findDescendants(group)) {
            for (Membership membership : findMemberships(groupToBeDeleted)) {
                membership.getUser().removeMembership(membership);
                userDAO.save(membership.getUser());

                entityManager.remove(membership);
            }

            groupToBeDeleted.getEntitlements().clear();

            groupToBeDeleted.setParent(null);
            groupToBeDeleted.setUserOwner(null);
            groupToBeDeleted.setGroupOwner(null);
            entityManager.remove(groupToBeDeleted);

            entitlementDAO.delete(GroupEntitlementUtil.getEntitlementNameFromGroupKey(groupToBeDeleted.getKey()));
        }
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
    public Group authFetch(Long key) {
        if (key == null) {
            throw new NotFoundException("Null group id");
        }

        Group group = find(key);
        if (group == null) {
            throw new NotFoundException("Group " + key);
        }

        Set<Long> allowedGroupKeys = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        if (!allowedGroupKeys.contains(group.getKey())) {
            throw new UnauthorizedGroupException(group.getKey());
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
