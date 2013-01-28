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

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public SyncopeRole getRoleFromId(final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

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

    public SyncopeRole create(final SyncopeRole role, final RoleTO roleTO)
            throws SyncopeClientCompositeErrorException {

        role.setInheritOwner(roleTO.isInheritOwner());

        role.setInheritAttributes(roleTO.isInheritAttributes());
        role.setInheritDerivedAttributes(roleTO.isInheritDerivedAttributes());
        role.setInheritVirtualAttributes(roleTO.isInheritVirtualAttributes());

        role.setInheritPasswordPolicy(roleTO.isInheritPasswordPolicy());
        role.setInheritAccountPolicy(roleTO.isInheritAccountPolicy());

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

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

        // attributes, derived attributes, virtual attributes and resources
        fill(role, roleTO, AttributableUtil.getInstance(AttributableType.ROLE), scce);

        // entitlements
        Entitlement entitlement;
        for (String entitlementName : roleTO.getEntitlements()) {
            entitlement = entitlementDAO.find(entitlementName);
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

    public PropagationByResource update(final SyncopeRole role, final RoleMod roleMod)
            throws SyncopeClientCompositeErrorException {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

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

        if (roleMod.getInheritAttributes() != null) {
            role.setInheritAttributes(roleMod.getInheritAttributes());
        }
        if (roleMod.getInheritDerivedAttributes() != null) {
            role.setInheritDerivedAttributes(roleMod.getInheritDerivedAttributes());
        }
        if (roleMod.getInheritVirtualAttributes() != null) {
            role.setInheritVirtualAttributes(roleMod.getInheritVirtualAttributes());
        }

        if (roleMod.getInheritPasswordPolicy() != null) {
            role.setInheritPasswordPolicy(roleMod.getInheritPasswordPolicy());
        }
        if (roleMod.getInheritAccountPolicy() != null) {
            role.setInheritAccountPolicy(roleMod.getInheritAccountPolicy());
        }

        // entitlements
        if (roleMod.getEntitlements() != null) {
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
        roleTO.setId(role.getId());
        roleTO.setName(role.getName());

        roleTO.setInheritOwner(role.isInheritOwner());

        roleTO.setInheritAttributes(role.isInheritAttributes());
        roleTO.setInheritDerivedAttributes(role.isInheritDerivedAttributes());
        roleTO.setInheritVirtualAttributes(role.isInheritVirtualAttributes());

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
        final List<RAttr> allAttributes = role.findInheritedAttributes();
        allAttributes.addAll((List<RAttr>) role.getAttributes());

        final List<RDerAttr> allDerAttributes = role.findInheritedDerivedAttributes();
        allDerAttributes.addAll((List<RDerAttr>) role.getDerivedAttributes());

        final List<RVirAttr> allVirAttributes = role.findInheritedVirtualAttributes();
        allVirAttributes.addAll((List<RVirAttr>) role.getVirtualAttributes());
        // -------------------------

        fillTO(roleTO, allAttributes, allDerAttributes, allVirAttributes, role.getResources());

        for (Entitlement entitlement : role.getEntitlements()) {
            roleTO.addEntitlement(entitlement.getName());
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
    public RoleTO getRoleTO(final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        return getRoleTO(getRoleFromId(roleId));
    }
}
