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
package org.apache.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.VirAttrDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RoleDAOImpl extends AbstractAttributableDAOImpl implements RoleDAO {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private DerAttrDAO derAttrDAO;

    @Autowired
    private VirAttrDAO virAttrDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @SuppressWarnings("unchecked")
    @Override
    protected <T extends AbstractAttributable> T findInternal(final Long id) {
        return (T) find(id);
    }

    @Override
    public SyncopeRole find(final Long id) {
        TypedQuery<SyncopeRole> query = entityManager.createQuery("SELECT e FROM SyncopeRole e WHERE e.id = :id",
                SyncopeRole.class);
        query.setParameter("id", id);

        SyncopeRole result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No role found with id {}", id, e);
        }

        return result;
    }

    @Override
    public List<SyncopeRole> find(final String name) {
        TypedQuery<SyncopeRole> query = entityManager.createQuery("SELECT e FROM SyncopeRole e WHERE e.name = :name",
                SyncopeRole.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public SyncopeRole find(final String name, final Long parentId) {
        TypedQuery<SyncopeRole> query;
        if (parentId == null) {
            query = entityManager.createQuery("SELECT r FROM SyncopeRole r WHERE "
                    + "r.name=:name AND r.parent IS NULL", SyncopeRole.class);
        } else {
            query = entityManager.createQuery("SELECT r FROM SyncopeRole r WHERE "
                    + "r.name=:name AND r.parent.id=:parentId", SyncopeRole.class);
            query.setParameter("parentId", parentId);
        }
        query.setParameter("name", name);

        List<SyncopeRole> result = query.getResultList();
        return result.isEmpty()
                ? null
                : result.get(0);
    }

    private void findSameOwnerDescendants(final List<SyncopeRole> result, final SyncopeRole role) {
        List<SyncopeRole> children = findChildren(role);
        if (children != null) {
            for (SyncopeRole child : children) {
                if ((child.getUserOwner() == null && child.getRoleOwner() == null && child.isInheritOwner())
                        || (child.getUserOwner() != null && child.getUserOwner().equals(role.getUserOwner()))
                        || (child.getRoleOwner() != null && child.getRoleOwner().equals(role.getRoleOwner()))) {

                    findDescendants(result, child);
                }
            }
        }
        result.add(role);
    }

    @Override
    public List<SyncopeRole> findOwned(final SyncopeUser owner) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(SyncopeRole.class.getSimpleName()).
                append(" e WHERE e.userOwner=:owner ");
        for (Long roleId : owner.getRoleIds()) {
            queryString.append("OR e.roleOwner.id=").append(roleId).append(' ');
        }

        TypedQuery<SyncopeRole> query = entityManager.createQuery(queryString.toString(), SyncopeRole.class);
        query.setParameter("owner", owner);

        List<SyncopeRole> result = new ArrayList<SyncopeRole>();
        for (SyncopeRole role : query.getResultList()) {
            findSameOwnerDescendants(result, role);
        }

        return result;
    }

    @Override
    public List<SyncopeRole> findOwned(final SyncopeRole owner) {
        StringBuilder queryString = new StringBuilder("SELECT e FROM ").append(SyncopeRole.class.getSimpleName()).
                append(" e WHERE e.roleOwner=:owner ");

        TypedQuery<SyncopeRole> query = entityManager.createQuery(queryString.toString(), SyncopeRole.class);
        query.setParameter("owner", owner);

        List<SyncopeRole> result = new ArrayList<SyncopeRole>();
        for (SyncopeRole role : query.getResultList()) {
            findSameOwnerDescendants(result, role);
        }

        return result;
    }

    @Override
    public List<SyncopeRole> findByEntitlement(final Entitlement entitlement) {
        TypedQuery<SyncopeRole> query = entityManager.createQuery("SELECT e FROM " + SyncopeRole.class.getSimpleName()
                + " e "
                + "WHERE :entitlement MEMBER OF e.entitlements", SyncopeRole.class);
        query.setParameter("entitlement", entitlement);

        return query.getResultList();
    }

    private void findAncestors(final List<SyncopeRole> result, final SyncopeRole role) {
        if (role.getParent() != null && !result.contains(role.getParent())) {
            result.add(role.getParent());
            findAncestors(result, role.getParent());
        }
    }

    @Override
    public List<SyncopeRole> findAncestors(final SyncopeRole role) {
        List<SyncopeRole> result = new ArrayList<SyncopeRole>();
        findAncestors(result, role);
        return result;
    }

    @Override
    public List<SyncopeRole> findChildren(final SyncopeRole role) {
        TypedQuery<SyncopeRole> query = entityManager.createQuery("SELECT r FROM SyncopeRole r WHERE "
                + "r.parent=:role", SyncopeRole.class);
        query.setParameter("role", role);

        return query.getResultList();
    }

    private void findDescendants(final List<SyncopeRole> result, final SyncopeRole role) {
        List<SyncopeRole> children = findChildren(role);
        if (children != null) {
            for (SyncopeRole child : children) {
                findDescendants(result, child);
            }
        }
        result.add(role);
    }

    @Override
    public List<SyncopeRole> findDescendants(final SyncopeRole role) {
        List<SyncopeRole> result = new ArrayList<SyncopeRole>();
        findDescendants(result, role);
        return result;
    }

    @Override
    protected TypedQuery<AbstractAttrValue> findByAttrValueQuery(final String entityName) {
        return entityManager.createQuery("SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.template.schema.name = :schemaName AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue)",
                AbstractAttrValue.class);
    }

    @Override
    public List<SyncopeRole> findByAttrValue(final String schemaName, final RAttrValue attrValue) {
        return findByAttrValue(schemaName, attrValue, AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @Override
    public SyncopeRole findByAttrUniqueValue(final String schemaName, final RAttrValue attrUniqueValue) {
        return (SyncopeRole) findByAttrUniqueValue(schemaName, attrUniqueValue,
                AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @Override
    public List<SyncopeRole> findByDerAttrValue(final String schemaName, final String value)
            throws InvalidSearchConditionException {

        return findByDerAttrValue(schemaName, value, AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @Override
    public List<SyncopeRole> findByResource(final ExternalResource resource) {
        return findByResource(resource, SyncopeRole.class);
    }

    @Override
    public List<SyncopeRole> findAll() {
        TypedQuery<SyncopeRole> query = entityManager.createQuery("SELECT e FROM SyncopeRole e", SyncopeRole.class);
        return query.getResultList();
    }

    @Override
    public List<Membership> findMemberships(final SyncopeRole role) {
        TypedQuery<Membership> query = entityManager.createQuery("SELECT e FROM " + Membership.class.getSimpleName()
                + " e"
                + " WHERE e.syncopeRole=:role", Membership.class);
        query.setParameter("role", role);

        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> unmatched(final Long roleId,
            final Class<?> attrClass, final Class<? extends AbstractAttrTemplate> attrTemplateClass) {

        final Query query = entityManager.createNativeQuery(new StringBuilder().
                append("SELECT ma.id ").
                append("FROM ").append(Membership.class.getSimpleName()).append(" m, ").
                append(attrClass.getSimpleName()).append(" ma ").
                append("WHERE m.syncopeRole_id = ?1 ").
                append("AND ma.owner_id = m.id ").
                append("AND ma.template_id NOT IN (").
                append("SELECT id ").
                append("FROM ").append(attrTemplateClass.getSimpleName()).append(' ').
                append("WHERE owner_id = ?1)").toString());
        query.setParameter(1, roleId);

        return query.getResultList();
    }

    @Override
    public SyncopeRole save(final SyncopeRole role) {
        // reset account policy in case of inheritance
        if (role.isInheritAccountPolicy()) {
            role.setAccountPolicy(null);
        }

        // reset password policy in case of inheritance
        if (role.isInheritPasswordPolicy()) {
            role.setPasswordPolicy(null);
        }

        // remove attributes without a valid template
        List<RAttr> rToBeDeleted = new ArrayList<RAttr>();
        for (AbstractAttr attr : role.getAttrs()) {
            boolean found = false;
            for (RAttrTemplate template : role.findInheritedTemplates(RAttrTemplate.class)) {
                if (template.getSchema().equals(attr.getSchema())) {
                    found = true;
                }
            }
            if (!found) {
                rToBeDeleted.add((RAttr) attr);
            }
        }
        for (RAttr attr : rToBeDeleted) {
            LOG.debug("Removing {} from {} because no template is available for it", attr, role);
            role.removeAttr(attr);
        }

        // remove derived attributes without a valid template
        List<RDerAttr> rDerToBeDeleted = new ArrayList<RDerAttr>();
        for (AbstractDerAttr attr : role.getDerAttrs()) {
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
        for (AbstractVirAttr attr : role.getVirAttrs()) {
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

        SyncopeRole merged = entityManager.merge(role);

        // Now the same process for any exising membership of the role being saved
        if (role.getId() != null) {
            for (Long id : unmatched(role.getId(), MAttr.class, MAttrTemplate.class)) {
                LOG.debug("Removing MAttr[{}] because no template is available for it in {}", id, role);
                attrDAO.delete(id, MAttr.class);
            }
            for (Long id : unmatched(role.getId(), MDerAttr.class, MDerAttrTemplate.class)) {
                LOG.debug("Removing MDerAttr[{}] because no template is available for it in {}", id, role);
                derAttrDAO.delete(id, MDerAttr.class);
            }
            for (Long id : unmatched(role.getId(), MVirAttr.class, MVirAttrTemplate.class)) {
                LOG.debug("Removing MVirAttr[{}] because no template is available for it in {}", id, role);
                virAttrDAO.delete(id, MVirAttr.class);
            }
        }

        merged = entityManager.merge(merged);
        for (AbstractVirAttr attr : merged.getVirAttrs()) {
            attr.setValues(role.getVirAttr(attr.getSchema().getName()).getValues());
        }

        entitlementDAO.saveEntitlementRole(merged);

        return merged;
    }

    @Override
    public void delete(final SyncopeRole role) {
        for (SyncopeRole roleToBeDeleted : findDescendants(role)) {
            for (Membership membership : findMemberships(roleToBeDeleted)) {
                membership.getSyncopeUser().removeMembership(membership);
                userDAO.save(membership.getSyncopeUser());

                entityManager.remove(membership);
            }

            roleToBeDeleted.getEntitlements().clear();

            roleToBeDeleted.setParent(null);
            roleToBeDeleted.setUserOwner(null);
            roleToBeDeleted.setRoleOwner(null);
            entityManager.remove(roleToBeDeleted);

            entitlementDAO.delete(EntitlementUtil.getEntitlementNameFromRoleId(roleToBeDeleted.getId()));
        }
    }

    @Override
    public void delete(final Long id) {
        SyncopeRole role = findInternal(id);
        if (role == null) {
            return;
        }

        delete(role);
    }
}
