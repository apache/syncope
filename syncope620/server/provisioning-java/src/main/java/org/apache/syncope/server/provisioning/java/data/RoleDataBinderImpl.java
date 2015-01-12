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
package org.apache.syncope.server.provisioning.java.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.server.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.entity.AccountPolicy;
import org.apache.syncope.server.persistence.api.entity.AttrTemplate;
import org.apache.syncope.server.persistence.api.entity.Entitlement;
import org.apache.syncope.server.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.server.persistence.api.entity.Schema;
import org.apache.syncope.server.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.server.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.server.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.server.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.server.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RDerSchema;
import org.apache.syncope.server.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.server.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RPlainSchema;
import org.apache.syncope.server.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.server.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.server.persistence.api.entity.role.RVirSchema;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.server.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.server.misc.ConnObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class RoleDataBinderImpl extends AbstractAttributableDataBinder implements RoleDataBinder {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private EntitlementDAO entitlementDAO;

    private <T extends AttrTemplate<S>, S extends Schema> void setAttrTemplates(
            final Role role, final List<String> schemaNames,
            final Class<T> templateClass, final Class<S> schemaClass) {

        List<T> toRemove = new ArrayList<T>();
        for (T template : role.getAttrTemplates(templateClass)) {
            if (!schemaNames.contains(template.getSchema().getKey())) {
                toRemove.add(template);
            }
        }
        role.getAttrTemplates(templateClass).removeAll(toRemove);

        for (String schemaName : schemaNames) {
            if (role.getAttrTemplate(templateClass, schemaName) == null) {
                S schema = getSchema(schemaName, schemaClass);
                if (schema != null) {
                    try {
                        T template = templateClass.newInstance();
                        template.setSchema(schema);
                        template.setOwner(role);
                        role.getAttrTemplates(templateClass).add(template);
                    } catch (Exception e) {
                        LOG.error("Could not create template for {}", templateClass, e);
                    }
                }
            }
        }
    }

    @Override
    public Role create(final Role role, final RoleTO roleTO) {
        role.setInheritOwner(roleTO.isInheritOwner());

        role.setInheritPlainAttrs(roleTO.isInheritAttrs());
        role.setInheritDerAttrs(roleTO.isInheritDerAttrs());
        role.setInheritVirAttrs(roleTO.isInheritVirAttrs());

        role.setInheritTemplates(roleTO.isInheritTemplates());

        role.setInheritPasswordPolicy(roleTO.isInheritPasswordPolicy());
        role.setInheritAccountPolicy(roleTO.isInheritAccountPolicy());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name and parent
        SyncopeClientException invalidRoles = SyncopeClientException.build(ClientExceptionType.InvalidRoles);
        if (roleTO.getName() == null) {
            LOG.error("No name specified for this role");

            invalidRoles.getElements().add("No name specified for this role");
        } else {
            role.setName(roleTO.getName());
        }
        Long parentRoleKey = null;
        if (roleTO.getParent() != 0) {
            Role parentRole = roleDAO.find(roleTO.getParent());
            if (parentRole == null) {
                LOG.error("Could not find role with id " + roleTO.getParent());

                invalidRoles.getElements().add(String.valueOf(roleTO.getParent()));
                scce.addException(invalidRoles);
            } else {
                role.setParent(parentRole);
                parentRoleKey = role.getParent().getKey();
            }
        }

        Role otherRole = roleDAO.find(roleTO.getName(), parentRoleKey);
        if (otherRole != null) {
            LOG.error("Another role exists with the same name " + "and the same parent role: " + otherRole);

            invalidRoles.getElements().add(roleTO.getName());
        }

        // attribute templates
        setAttrTemplates(role, roleTO.getRAttrTemplates(), RPlainAttrTemplate.class, RPlainSchema.class);
        setAttrTemplates(role, roleTO.getRDerAttrTemplates(), RDerAttrTemplate.class, RDerSchema.class);
        setAttrTemplates(role, roleTO.getRVirAttrTemplates(), RVirAttrTemplate.class, RVirSchema.class);
        setAttrTemplates(role, roleTO.getMAttrTemplates(), MPlainAttrTemplate.class, MPlainSchema.class);
        setAttrTemplates(role, roleTO.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        setAttrTemplates(role, roleTO.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);

        // attributes, derived attributes, virtual attributes and resources
        fill(role, roleTO, attrUtilFactory.getInstance(AttributableType.ROLE), scce);

        // entitlements
        for (String entitlementName : roleTO.getEntitlements()) {
            Entitlement entitlement = entitlementDAO.find(entitlementName);
            if (entitlement == null) {
                LOG.warn("Ignoring invalid entitlement {}", entitlementName);
            } else {
                role.addEntitlement(entitlement);
            }
        }

        // owner
        if (roleTO.getUserOwner() != null) {
            User owner = userDAO.find(roleTO.getUserOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid user specified as owner: {}", roleTO.getUserOwner());
            } else {
                role.setUserOwner(owner);
            }
        }
        if (roleTO.getRoleOwner() != null) {
            Role owner = roleDAO.find(roleTO.getRoleOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid role specified as owner: {}", roleTO.getRoleOwner());
            } else {
                role.setRoleOwner(owner);
            }
        }

        // policies
        if (roleTO.getPasswordPolicy() != null) {
            role.setPasswordPolicy((PasswordPolicy) policyDAO.find(roleTO.getPasswordPolicy()));
        }
        if (roleTO.getAccountPolicy() != null) {
            role.setAccountPolicy((AccountPolicy) policyDAO.find(roleTO.getAccountPolicy()));
        }

        return role;
    }

    @Override
    public PropagationByResource update(final Role role, final RoleMod roleMod) {
        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Set<String> currentResources = role.getResourceNames();

        // name
        SyncopeClientException invalidRoles = SyncopeClientException.build(ClientExceptionType.InvalidRoles);
        if (roleMod.getName() != null) {
            Role otherRole = roleDAO.find(roleMod.getName(),
                    role.getParent() == null ? null : role.getParent().getKey());
            if (otherRole == null || role.equals(otherRole)) {
                if (!roleMod.getName().equals(role.getName())) {
                    propByRes.addAll(ResourceOperation.UPDATE, currentResources);
                    for (String resource : currentResources) {
                        propByRes.addOldAccountId(resource, role.getName());
                    }

                    role.setName(roleMod.getName());
                }
            } else {
                LOG.error("Another role exists with the same name and the same parent role: " + otherRole);

                invalidRoles.getElements().add(roleMod.getName());
                scce.addException(invalidRoles);
            }
        }

        if (roleMod.getInheritOwner() != null) {
            role.setInheritOwner(roleMod.getInheritOwner());
        }

        if (roleMod.getInheritTemplates() != null) {
            role.setInheritTemplates(roleMod.getInheritTemplates());
        }

        if (roleMod.getInheritPlainAttrs() != null) {
            role.setInheritPlainAttrs(roleMod.getInheritPlainAttrs());
        }
        if (roleMod.getInheritDerAttrs() != null) {
            role.setInheritDerAttrs(roleMod.getInheritDerAttrs());
        }
        if (roleMod.getInheritVirAttrs() != null) {
            role.setInheritVirAttrs(roleMod.getInheritVirAttrs());
        }

        if (roleMod.getInheritPasswordPolicy() != null) {
            role.setInheritPasswordPolicy(roleMod.getInheritPasswordPolicy());
        }
        if (roleMod.getInheritAccountPolicy() != null) {
            role.setInheritAccountPolicy(roleMod.getInheritAccountPolicy());
        }

        // entitlements
        if (roleMod.isModEntitlements()) {
            role.getEntitlements().clear();
            for (String entitlementName : roleMod.getEntitlements()) {
                Entitlement entitlement = entitlementDAO.find(entitlementName);
                if (entitlement == null) {
                    LOG.warn("Ignoring invalid entitlement {}", entitlementName);
                } else {
                    role.addEntitlement(entitlement);
                }
            }
        }

        // attribute templates
        if (roleMod.isModRAttrTemplates()) {
            setAttrTemplates(role, roleMod.getRPlainAttrTemplates(), RPlainAttrTemplate.class, RPlainSchema.class);
        }
        if (roleMod.isModRDerAttrTemplates()) {
            setAttrTemplates(role, roleMod.getRDerAttrTemplates(), RDerAttrTemplate.class, RDerSchema.class);
        }
        if (roleMod.isModRVirAttrTemplates()) {
            setAttrTemplates(role, roleMod.getRVirAttrTemplates(), RVirAttrTemplate.class, RVirSchema.class);
        }
        if (roleMod.isModMAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMPlainAttrTemplates(), MPlainAttrTemplate.class, MPlainSchema.class);
        }
        if (roleMod.isModMDerAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        }
        if (roleMod.isModMVirAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);
        }

        // policies
        if (roleMod.getPasswordPolicy() != null) {
            role.setPasswordPolicy(roleMod.getPasswordPolicy().getKey() == null
                    ? null
                    : (PasswordPolicy) policyDAO.find(roleMod.getPasswordPolicy().getKey()));
        }
        if (roleMod.getAccountPolicy() != null) {
            role.setAccountPolicy(roleMod.getAccountPolicy().getKey() == null
                    ? null
                    : (AccountPolicy) policyDAO.find(roleMod.getAccountPolicy().getKey()));
        }

        // owner
        if (roleMod.getUserOwner() != null) {
            role.setUserOwner(roleMod.getUserOwner().getKey() == null
                    ? null
                    : userDAO.find(roleMod.getUserOwner().getKey()));
        }
        if (roleMod.getRoleOwner() != null) {
            role.setRoleOwner(roleMod.getRoleOwner().getKey() == null
                    ? null
                    : roleDAO.find(roleMod.getRoleOwner().getKey()));
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(role, roleMod, attrUtilFactory.getInstance(AttributableType.ROLE), scce));

        return propByRes;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public RoleTO getRoleTO(final Role role) {
        connObjectUtil.retrieveVirAttrValues(role, attrUtilFactory.getInstance(AttributableType.ROLE));

        RoleTO roleTO = new RoleTO();

        // set sys info
        roleTO.setCreator(role.getCreator());
        roleTO.setCreationDate(role.getCreationDate());
        roleTO.setLastModifier(role.getLastModifier());
        roleTO.setLastChangeDate(role.getLastChangeDate());

        roleTO.setKey(role.getKey());
        roleTO.setName(role.getName());

        roleTO.setInheritOwner(role.isInheritOwner());

        roleTO.setInheritTemplates(role.isInheritTemplates());

        roleTO.setInheritAttrs(role.isInheritPlainAttrs());
        roleTO.setInheritDerAttrs(role.isInheritDerAttrs());
        roleTO.setInheritVirAttrs(role.isInheritVirAttrs());

        roleTO.setInheritPasswordPolicy(role.isInheritPasswordPolicy());
        roleTO.setInheritAccountPolicy(role.isInheritAccountPolicy());

        if (role.getParent() != null) {
            roleTO.setParent(role.getParent().getKey());
        }

        if (role.getUserOwner() != null) {
            roleTO.setUserOwner(role.getUserOwner().getKey());
        }
        if (role.getRoleOwner() != null) {
            roleTO.setRoleOwner(role.getRoleOwner().getKey());
        }

        // -------------------------
        // Retrieve all [derived/virtual] attributes (inherited and not)
        // -------------------------        
        final List<? extends RPlainAttr> allAttributes = role.findLastInheritedAncestorPlainAttrs();

        final List<? extends RDerAttr> allDerAttributes = role.findLastInheritedAncestorDerAttrs();

        final List<? extends RVirAttr> allVirAttributes = role.findLastInheritedAncestorVirAttrs();
        // -------------------------

        fillTO(roleTO, allAttributes, allDerAttributes, allVirAttributes, role.getResources());

        for (Entitlement entitlement : role.getEntitlements()) {
            roleTO.getEntitlements().add(entitlement.getKey());
        }

        for (RPlainAttrTemplate template : role.findInheritedTemplates(RPlainAttrTemplate.class)) {
            roleTO.getRAttrTemplates().add(template.getSchema().getKey());
        }
        for (RDerAttrTemplate template : role.findInheritedTemplates(RDerAttrTemplate.class)) {
            roleTO.getRDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (RVirAttrTemplate template : role.findInheritedTemplates(RVirAttrTemplate.class)) {
            roleTO.getRVirAttrTemplates().add(template.getSchema().getKey());
        }
        for (MPlainAttrTemplate template : role.findInheritedTemplates(MPlainAttrTemplate.class)) {
            roleTO.getMAttrTemplates().add(template.getSchema().getKey());
        }
        for (MDerAttrTemplate template : role.findInheritedTemplates(MDerAttrTemplate.class)) {
            roleTO.getMDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (MVirAttrTemplate template : role.findInheritedTemplates(MVirAttrTemplate.class)) {
            roleTO.getMVirAttrTemplates().add(template.getSchema().getKey());
        }

        roleTO.setPasswordPolicy(role.getPasswordPolicy() == null
                ? null
                : role.getPasswordPolicy().getKey());
        roleTO.setAccountPolicy(role.getAccountPolicy() == null
                ? null
                : role.getAccountPolicy().getKey());

        return roleTO;
    }

    @Transactional(readOnly = true)
    @Override
    public RoleTO getRoleTO(final Long key) {
        return getRoleTO(roleDAO.authFetch(key));
    }
}
