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

import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoleDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    private EntitlementDAO entitlementDAO;

    public SyncopeRole getSyncopeRole(final Long roleId) throws NotFoundException {
        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        return role;
    }

    public SyncopeRole create(final RoleTO roleTO) throws SyncopeClientCompositeErrorException {
        SyncopeRole role = new SyncopeRole();

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

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        // name
        SyncopeClientException invalidRoles = new SyncopeClientException(SyncopeClientExceptionType.InvalidRoles);
        if (roleMod.getName() != null) {
            SyncopeRole otherRole = roleDAO.find(roleMod.getName(),
                    role.getParent() == null ? null : role.getParent().getId());
            if (otherRole == null || role.equals(otherRole)) {
                role.setName(roleMod.getName());
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
        if (roleMod.getEntitlementsToBeAdded().size() > 0) {
            for (String entitlementName : roleMod.getEntitlementsToBeAdded()) {
                Entitlement entitlement = entitlementDAO.find(entitlementName);
                if (entitlement == null) {
                    LOG.warn("Ignoring invalid entitlement {}", entitlementName);
                } else {
                    role.addEntitlement(entitlement);
                }
            }
        }
        if (roleMod.getEntitlementsToBeRemoved().size() > 0) {
            for (String entitlementName : roleMod.getEntitlementsToBeRemoved()) {
                Entitlement entitlement = entitlementDAO.find(entitlementName);
                if (entitlement == null) {
                    LOG.warn("Ignoring invalid entitlement {}", entitlementName);
                } else {
                    role.removeEntitlement(entitlement);
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
        return fill(role, roleMod, AttributableUtil.getInstance(AttributableType.ROLE), scce);
    }

    public RoleTO getRoleTO(final SyncopeRole role) {
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
}
