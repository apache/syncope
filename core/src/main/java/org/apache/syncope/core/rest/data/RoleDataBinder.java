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
package org.apache.syncope.core.rest.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RVirSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = {Throwable.class})
public class RoleDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Transactional(readOnly = true)
    public SyncopeRole getRoleFromId(final Long roleId) {
        if (roleId == null) {
            throw new NotFoundException("Null role id");
        }

        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }
        return role;
    }

    @Transactional(readOnly = true)
    public List<WorkflowResult<Long>> getUsersOnResourcesOnlyBecauseOfRole(final Long roleId) {
        SyncopeRole role = getRoleFromId(roleId);

        List<WorkflowResult<Long>> result = new ArrayList<WorkflowResult<Long>>();

        for (Membership membership : roleDAO.findMemberships(role)) {
            SyncopeUser user = membership.getSyncopeUser();

            PropagationByResource propByRes = new PropagationByResource();
            for (ExternalResource resource : role.getResources()) {
                if (!user.getOwnResources().contains(resource)) {
                    propByRes.add(ResourceOperation.DELETE, resource.getName());
                }

                if (!propByRes.isEmpty()) {
                    result.add(new WorkflowResult<Long>(user.getId(), propByRes, Collections.<String>emptySet()));
                }
            }
        }

        return result;
    }

    private <T extends AbstractAttrTemplate<K>, K extends AbstractSchema> void setAttrTemplates(
            final SyncopeRole role, final List<String> schemaNames,
            final Class<T> templateClass, final Class<K> schemaClass) {

        List<T> toRemove = new ArrayList<T>();
        for (T template : role.getAttrTemplates(templateClass)) {
            if (!schemaNames.contains(template.getSchema().getName())) {
                toRemove.add(template);
            }
        }
        role.getAttrTemplates(templateClass).removeAll(toRemove);

        for (String schemaName : schemaNames) {
            if (role.getAttrTemplate(templateClass, schemaName) == null) {
                K schema = getSchema(schemaName, schemaClass);
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

    public SyncopeRole create(final SyncopeRole role, final RoleTO roleTO) {
        role.setInheritOwner(roleTO.isInheritOwner());

        role.setInheritAttrs(roleTO.isInheritAttrs());
        role.setInheritDerAttrs(roleTO.isInheritDerAttrs());
        role.setInheritVirAttrs(roleTO.isInheritVirAttrs());

        role.setInheritTemplates(roleTO.isInheritTemplates());

        role.setInheritPasswordPolicy(roleTO.isInheritPasswordPolicy());
        role.setInheritAccountPolicy(roleTO.isInheritAccountPolicy());

        SyncopeClientCompositeException scce =
                new SyncopeClientCompositeException(Response.Status.BAD_REQUEST.getStatusCode());

        // name and parent
        SyncopeClientException invalidRoles = new SyncopeClientException(SyncopeClientExceptionType.InvalidRoles);
        if (roleTO.getName() == null) {
            LOG.error("No name specified for this role");

            invalidRoles.addElement("No name specified for this role");
        } else {
            role.setName(roleTO.getName());
        }
        Long parentRoleId = null;
        if (roleTO.getParent() != 0) {
            SyncopeRole parentRole = roleDAO.find(roleTO.getParent());
            if (parentRole == null) {
                LOG.error("Could not find role with id " + roleTO.getParent());

                invalidRoles.addElement(String.valueOf(roleTO.getParent()));
                scce.addException(invalidRoles);
            } else {
                role.setParent(parentRole);
                parentRoleId = role.getParent().getId();
            }
        }

        SyncopeRole otherRole = roleDAO.find(roleTO.getName(), parentRoleId);
        if (otherRole != null) {
            LOG.error("Another role exists with the same name " + "and the same parent role: " + otherRole);

            invalidRoles.addElement(roleTO.getName());
        }

        // attribute templates
        setAttrTemplates(role, roleTO.getRAttrTemplates(), RAttrTemplate.class, RSchema.class);
        setAttrTemplates(role, roleTO.getRDerAttrTemplates(), RDerAttrTemplate.class, RDerSchema.class);
        setAttrTemplates(role, roleTO.getRVirAttrTemplates(), RVirAttrTemplate.class, RVirSchema.class);
        setAttrTemplates(role, roleTO.getMAttrTemplates(), MAttrTemplate.class, MSchema.class);
        setAttrTemplates(role, roleTO.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        setAttrTemplates(role, roleTO.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);

        // attributes, derived attributes, virtual attributes and resources
        fill(role, roleTO, AttributableUtil.getInstance(AttributableType.ROLE), scce);

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
            SyncopeUser owner = userDAO.find(roleTO.getUserOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid user specified as owner: {}", roleTO.getUserOwner());
            } else {
                role.setUserOwner(owner);
            }
        }
        if (roleTO.getRoleOwner() != null) {
            SyncopeRole owner = roleDAO.find(roleTO.getRoleOwner());
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

    public PropagationByResource update(final SyncopeRole role, final RoleMod roleMod) {
        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce =
                new SyncopeClientCompositeException(Response.Status.BAD_REQUEST.getStatusCode());

        Set<String> currentResources = role.getResourceNames();

        // name
        SyncopeClientException invalidRoles = new SyncopeClientException(SyncopeClientExceptionType.InvalidRoles);
        if (roleMod.getName() != null) {
            SyncopeRole otherRole = roleDAO.find(roleMod.getName(),
                    role.getParent() == null ? null : role.getParent().getId());
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

                invalidRoles.addElement(roleMod.getName());
                scce.addException(invalidRoles);
            }
        }

        if (roleMod.getInheritOwner() != null) {
            role.setInheritOwner(roleMod.getInheritOwner());
        }

        if (roleMod.getInheritTemplates() != null) {
            role.setInheritTemplates(roleMod.getInheritTemplates());
        }

        if (roleMod.getInheritAttrs() != null) {
            role.setInheritAttrs(roleMod.getInheritAttrs());
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
            setAttrTemplates(role, roleMod.getRAttrTemplates(), RAttrTemplate.class, RSchema.class);
        }
        if (roleMod.isModRDerAttrTemplates()) {
            setAttrTemplates(role, roleMod.getRDerAttrTemplates(), RDerAttrTemplate.class, RDerSchema.class);
        }
        if (roleMod.isModRVirAttrTemplates()) {
            setAttrTemplates(role, roleMod.getRVirAttrTemplates(), RVirAttrTemplate.class, RVirSchema.class);
        }
        if (roleMod.isModMAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMAttrTemplates(), MAttrTemplate.class, MSchema.class);
        }
        if (roleMod.isModMDerAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        }
        if (roleMod.isModMVirAttrTemplates()) {
            setAttrTemplates(role, roleMod.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);
        }

        // policies
        if (roleMod.getPasswordPolicy() != null) {
            role.setPasswordPolicy(roleMod.getPasswordPolicy().getId() == null
                    ? null
                    : (PasswordPolicy) policyDAO.find(roleMod.getPasswordPolicy().getId()));
        }
        if (roleMod.getAccountPolicy() != null) {
            role.setAccountPolicy(roleMod.getAccountPolicy().getId() == null
                    ? null
                    : (AccountPolicy) policyDAO.find(roleMod.getAccountPolicy().getId()));
        }

        // owner
        if (roleMod.getUserOwner() != null) {
            role.setUserOwner(roleMod.getUserOwner().getId() == null
                    ? null
                    : userDAO.find(roleMod.getUserOwner().getId()));
        }
        if (roleMod.getRoleOwner() != null) {
            role.setRoleOwner(roleMod.getRoleOwner().getId() == null
                    ? null
                    : roleDAO.find(roleMod.getRoleOwner().getId()));
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(role, roleMod, AttributableUtil.getInstance(AttributableType.ROLE), scce));

        return propByRes;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public RoleTO getRoleTO(final SyncopeRole role) {
        connObjectUtil.retrieveVirAttrValues(role, AttributableUtil.getInstance(AttributableType.ROLE));

        RoleTO roleTO = new RoleTO();

        // set sys info
        roleTO.setCreator(role.getCreator());
        roleTO.setCreationDate(role.getCreationDate());
        roleTO.setLastModifier(role.getLastModifier());
        roleTO.setLastChangeDate(role.getLastChangeDate());

        roleTO.setId(role.getId());
        roleTO.setName(role.getName());

        roleTO.setInheritOwner(role.isInheritOwner());

        roleTO.setInheritTemplates(role.isInheritTemplates());

        roleTO.setInheritAttrs(role.isInheritAttrs());
        roleTO.setInheritDerAttrs(role.isInheritDerAttrs());
        roleTO.setInheritVirAttrs(role.isInheritVirAttrs());

        roleTO.setInheritPasswordPolicy(role.isInheritPasswordPolicy());
        roleTO.setInheritAccountPolicy(role.isInheritAccountPolicy());

        if (role.getParent() != null) {
            roleTO.setParent(role.getParent().getId());
        }

        if (role.getUserOwner() != null) {
            roleTO.setUserOwner(role.getUserOwner().getId());
        }
        if (role.getRoleOwner() != null) {
            roleTO.setRoleOwner(role.getRoleOwner().getId());
        }

        // -------------------------
        // Retrieve all [derived/virtual] attributes (inherited and not)
        // -------------------------
        final List<RAttr> allAttributes = role.findInheritedAttrs();
        allAttributes.addAll((List<RAttr>) role.getAttrs());

        final List<RDerAttr> allDerAttributes = role.findInheritedDerAttrs();
        allDerAttributes.addAll((List<RDerAttr>) role.getDerAttrs());

        final List<RVirAttr> allVirAttributes = role.findInheritedVirAttrs();
        allVirAttributes.addAll((List<RVirAttr>) role.getVirAttrs());
        // -------------------------

        fillTO(roleTO, allAttributes, allDerAttributes, allVirAttributes, role.getResources());

        for (Entitlement entitlement : role.getEntitlements()) {
            roleTO.getEntitlements().add(entitlement.getName());
        }

        for (RAttrTemplate template : role.findInheritedTemplates(RAttrTemplate.class)) {
            roleTO.getRAttrTemplates().add(template.getSchema().getName());
        }
        for (RDerAttrTemplate template : role.findInheritedTemplates(RDerAttrTemplate.class)) {
            roleTO.getRDerAttrTemplates().add(template.getSchema().getName());
        }
        for (RVirAttrTemplate template : role.findInheritedTemplates(RVirAttrTemplate.class)) {
            roleTO.getRVirAttrTemplates().add(template.getSchema().getName());
        }
        for (MAttrTemplate template : role.findInheritedTemplates(MAttrTemplate.class)) {
            roleTO.getMAttrTemplates().add(template.getSchema().getName());
        }
        for (MDerAttrTemplate template : role.findInheritedTemplates(MDerAttrTemplate.class)) {
            roleTO.getMDerAttrTemplates().add(template.getSchema().getName());
        }
        for (MVirAttrTemplate template : role.findInheritedTemplates(MVirAttrTemplate.class)) {
            roleTO.getMVirAttrTemplates().add(template.getSchema().getName());
        }

        roleTO.setPasswordPolicy(role.getPasswordPolicy() == null
                ? null
                : role.getPasswordPolicy().getId());
        roleTO.setAccountPolicy(role.getAccountPolicy() == null
                ? null
                : role.getAccountPolicy().getId());

        return roleTO;
    }

    @Transactional(readOnly = true)
    public RoleTO getRoleTO(final Long roleId) {
        return getRoleTO(getRoleFromId(roleId));
    }
}
