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
package org.apache.syncope.persistence.jpa.dao;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.persistence.api.dao.RoleDAO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.persistence.api.entity.AttrTemplate;
import org.apache.syncope.persistence.api.entity.DerAttr;
import org.apache.syncope.persistence.api.entity.Entitlement;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.PlainAttr;
import org.apache.syncope.persistence.api.entity.Policy;
import org.apache.syncope.persistence.api.entity.Subject;
import org.apache.syncope.persistence.api.entity.VirAttr;
import org.apache.syncope.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.entity.JPAAttributableUtil;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.persistence.jpa.entity.role.JPARole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPARoleDAO extends AbstractSubjectDAO<RPlainAttr, RDerAttr, RVirAttr> implements RoleDAO {

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

    @Override
    protected Subject<RPlainAttr, RDerAttr, RVirAttr> findInternal(final Long key) {
        return find(key);
    }

    @Override
    public Role find(final Long key) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE e.id = :id", Role.class);
        query.setParameter("id", key);

        Role result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No role found with id {}", key, e);
        }

        return result;
    }

    @Override
    public List<Role> find(final String name) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e WHERE e.name = :name", Role.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public Role find(final String name, final Long parentId) {
        TypedQuery<Role> query;
        if (parentId == null) {
            query = entityManager.createQuery("SELECT r FROM " + JPARole.class.getSimpleName() + " r WHERE "
                    + "r.name=:name AND r.parent IS NULL", Role.class);
        } else {
            query = entityManager.createQuery("SELECT r FROM " + JPARole.class.getSimpleName() + " r WHERE "
                    + "r.name=:name AND r.parent.id=:parentId", Role.class);
            query.setParameter("parentId", parentId);
        }
        query.setParameter("name", name);

        List<Role> result = query.getResultList();
        return result.isEmpty()
                ? null
                : result.get(0);
    }

    private void findSameOwnerDescendants(final List<Role> result, final Role role) {
        List<Role> children = findChildren(role);
        if (children != null) {
            for (Role child : children) {
                if ((child.getUserOwner() == null && child.getRoleOwner() == null && child.isInheritOwner())
                        || (child.getUserOwner() != null && child.getUserOwner().equals(role.getUserOwner()))
                        || (child.getRoleOwner() != null && child.getRoleOwner().equals(role.getRoleOwner()))) {

                    findDescendants(result, child);
                }
            }
        }
        result.add(role);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Role> findOwnedByUser(final Long userKey) {
        User owner = userDAO.find(userKey);
        if (owner == null) {
            return Collections.<Role>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPARole.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (Long roleId : owner.getRoleIds()) {
            queryString.append("OR e.roleOwner.id=").append(roleId).append(' ');
        }

        TypedQuery<Role> query = entityManager.createQuery(queryString.toString(), Role.class);
        query.setParameter("owner", owner);

        List<Role> result = new ArrayList<>();
        for (Role role : query.getResultList()) {
            findSameOwnerDescendants(result, role);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Role> findOwnedByRole(final Long roleId) {
        Role owner = find(roleId);
        if (owner == null) {
            return Collections.<Role>emptyList();
        }

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(JPARole.class.getSimpleName()).
                append(" e WHERE e.roleOwner=:owner ");

        TypedQuery<Role> query = entityManager.createQuery(queryString.toString(), Role.class);
        query.setParameter("owner", owner);

        List<Role> result = new ArrayList<Role>();
        for (Role role : query.getResultList()) {
            findSameOwnerDescendants(result, role);
        }

        return result;
    }

    @Override
    public List<Role> findByEntitlement(final Entitlement entitlement) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e "
                + "WHERE :entitlement MEMBER OF e.entitlements", Role.class);
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

    private List<Role> findSamePolicyChildren(final Role role, final PolicyType type) {
        List<Role> result = new ArrayList<Role>();

        for (Role child : findChildren(role)) {
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
    public List<Role> findByPolicy(final Policy policy) {
        if (policy.getType() == PolicyType.GLOBAL_SYNC || policy.getType() == PolicyType.SYNC) {
            return Collections.<Role>emptyList();
        }

        Map.Entry<String, String> policyFields = getPolicyFields(policy.getType());
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARole.class.getSimpleName()).append(" e WHERE e.").
                append(policyFields.getKey()).append(" = :policy AND (e.").
                append(policyFields.getValue()).append(" IS NULL OR e.").
                append(policyFields.getValue()).append(" = 0)");

        TypedQuery<Role> query = entityManager.createQuery(queryString.toString(), Role.class);
        query.setParameter("policy", policy);

        List<Role> result = new ArrayList<Role>();
        for (Role role : query.getResultList()) {
            result.add(role);
            result.addAll(findSamePolicyChildren(role, policy.getType()));
        }
        return result;
    }

    @Override
    public List<Role> findWithoutPolicy(final PolicyType type) {
        if (type == PolicyType.GLOBAL_SYNC || type == PolicyType.SYNC) {
            return Collections.<Role>emptyList();
        }

        Map.Entry<String, String> policyFields = getPolicyFields(type);
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARole.class.getSimpleName()).append(" e WHERE e.").
                append(policyFields.getKey()).append(" IS NULL AND (e.").
                append(policyFields.getValue()).append(" IS NULL OR e.").
                append(policyFields.getValue()).append(" = 0)");

        TypedQuery<Role> query = entityManager.createQuery(queryString.toString(), Role.class);
        return query.getResultList();
    }

    private void findAncestors(final List<Role> result, final Role role) {
        if (role.getParent() != null && !result.contains(role.getParent())) {
            result.add(role.getParent());
            findAncestors(result, role.getParent());
        }
    }

    @Override
    public List<Role> findAncestors(final Role role) {
        List<Role> result = new ArrayList<>();
        findAncestors(result, role);
        return result;
    }

    @Override
    public List<Role> findChildren(final Role role) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT r FROM " + JPARole.class.getSimpleName() + " r WHERE r.parent=:role", Role.class);
        query.setParameter("role", role);

        return query.getResultList();
    }

    private void findDescendants(final List<Role> result, final Role role) {
        List<Role> children = findChildren(role);
        if (children != null) {
            for (Role child : children) {
                findDescendants(result, child);
            }
        }
        result.add(role);
    }

    @Override
    public List<Role> findDescendants(final Role role) {
        List<Role> result = new ArrayList<>();
        findDescendants(result, role);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> findByAttrValue(final String schemaName, final RPlainAttrValue attrValue) {
        return (List<Role>) findByAttrValue(
                schemaName, attrValue, JPAAttributableUtil.getInstance(AttributableType.ROLE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Role findByAttrUniqueValue(final String schemaName, final RPlainAttrValue attrUniqueValue) {
        return (Role) findByAttrUniqueValue(schemaName, attrUniqueValue,
                JPAAttributableUtil.getInstance(AttributableType.ROLE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> findByDerAttrValue(final String schemaName, final String value) {
        return (List<Role>) findByDerAttrValue(
                schemaName, value, JPAAttributableUtil.getInstance(AttributableType.ROLE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> findByResource(final ExternalResource resource) {
        return (List<Role>) findByResource(resource, JPAAttributableUtil.getInstance(AttributableType.ROLE));
    }

    @Override
    public List<Role> findAll() {
        return findAll(-1, -1, Collections.<OrderByClause>emptyList());
    }

    @Override
    public List<Role> findAll(final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {
        TypedQuery<Role> query = entityManager.createQuery(
                "SELECT e FROM " + JPARole.class.getSimpleName() + " e "
                + toOrderByStatement(Role.class, "e", orderBy), Role.class);

        query.setFirstResult(itemsPerPage * (page <= 0
                ? 0
                : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public List<Membership> findMemberships(final Role role) {
        TypedQuery<Membership> query = entityManager.createQuery(
                "SELECT e FROM " + JPAMembership.class.getSimpleName() + " e"
                + " WHERE e.role=:role", Membership.class);
        query.setParameter("role", role);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> unmatched(final Long roleId,
            final Class<?> attrClass, final Class<? extends AttrTemplate<?>> attrTemplateClass) {

        final Query query = entityManager.createNativeQuery(new StringBuilder().
                append("SELECT ma.id ").
                append("FROM ").append(JPAMembership.TABLE).append(" m, ").
                append(attrClass.getSimpleName()).append(" ma ").
                append("WHERE m.role_id = ?1 ").
                append("AND ma.owner_id = m.id ").
                append("AND ma.template_id NOT IN (").
                append("SELECT id ").
                append("FROM ").append(attrTemplateClass.getSimpleName()).append(' ').
                append("WHERE owner_id = ?1)").toString());
        query.setParameter(1, roleId);

        return query.getResultList();
    }

    @Override
    public final int count() {
        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(e.id) FROM " + JPARole.TABLE + " e");

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public Role save(final Role role) {
        // reset account policy in case of inheritance
        if (role.isInheritAccountPolicy()) {
            role.setAccountPolicy(null);
        }

        // reset password policy in case of inheritance
        if (role.isInheritPasswordPolicy()) {
            role.setPasswordPolicy(null);
        }

        // remove attributes without a valid template
        List<RPlainAttr> rToBeDeleted = new ArrayList<>();
        for (PlainAttr attr : role.getPlainAttrs()) {
            boolean found = false;
            for (RPlainAttrTemplate template : role.findInheritedTemplates(RPlainAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                rToBeDeleted.add((RPlainAttr) attr);
            }
        }
        for (RPlainAttr attr : rToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, role);
            role.removePlainAttr(attr);
        }

        // remove derived attributes without a valid template
        List<RDerAttr> rDerToBeDeleted = new ArrayList<RDerAttr>();
        for (DerAttr attr : role.getDerAttrs()) {
            boolean found = false;
            for (RDerAttrTemplate template : role.findInheritedTemplates(RDerAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                rDerToBeDeleted.add((RDerAttr) attr);
            }
        }
        for (RDerAttr attr : rDerToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, role);
            role.removeDerAttr(attr);
        }

        // remove virtual attributes without a valid template
        List<RVirAttr> rVirToBeDeleted = new ArrayList<RVirAttr>();
        for (VirAttr attr : role.getVirAttrs()) {
            boolean found = false;
            for (RVirAttrTemplate template : role.findInheritedTemplates(RVirAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                LOG.debug("Removing {} from {} because no template is available for it", attr, role);
                rVirToBeDeleted.add((RVirAttr) attr);
            }
        }
        for (RVirAttr attr : rVirToBeDeleted) {
            role.removeVirAttr(attr);
        }

        Role merged = entityManager.merge(role);

        // Now the same process for any exising membership of the role being saved
        if (role.getKey() != null) {
            for (Long key : unmatched(role.getKey(), MPlainAttr.class, MPlainAttrTemplate.class)) {
                LOG.debug("Removing MAttr[{}] because no template is available for it in {}", key, role);
                plainAttrDAO.delete(key, MPlainAttr.class);
            }
            for (Long id : unmatched(role.getKey(), MDerAttr.class, MDerAttrTemplate.class)) {
                LOG.debug("Removing MDerAttr[{}] because no template is available for it in {}", id, role);
                derAttrDAO.delete(id, MDerAttr.class);
            }
            for (Long id : unmatched(role.getKey(), MVirAttr.class, MVirAttrTemplate.class)) {
                LOG.debug("Removing MVirAttr[{}] because no template is available for it in {}", id, role);
                virAttrDAO.delete(id, MVirAttr.class);
            }
        }

        merged = entityManager.merge(merged);
        for (VirAttr attr : merged.getVirAttrs()) {
            attr.getValues().clear();
            attr.getValues().addAll(role.getVirAttr(attr.getSchema().getKey()).getValues());
        }

        entitlementDAO.saveRoleEntitlement(merged);

        return merged;
    }

    @Override
    public void delete(final Role role) {
        for (Role roleToBeDeleted : findDescendants(role)) {
            for (Membership membership : findMemberships(roleToBeDeleted)) {
                membership.getUser().removeMembership(membership);
                userDAO.save(membership.getUser());

                entityManager.remove(membership);
            }

            roleToBeDeleted.getEntitlements().clear();

            roleToBeDeleted.setParent(null);
            roleToBeDeleted.setUserOwner(null);
            roleToBeDeleted.setRoleOwner(null);
            entityManager.remove(roleToBeDeleted);

            entitlementDAO.delete(RoleEntitlementUtil.getEntitlementNameFromRoleId(roleToBeDeleted.getKey()));
        }
    }

    @Override
    public void delete(final Long key) {
        Role role = (Role) findInternal(key);
        if (role == null) {
            return;
        }

        delete(role);
    }
}
