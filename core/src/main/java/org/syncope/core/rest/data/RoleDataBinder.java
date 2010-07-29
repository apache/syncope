/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class RoleDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    public RoleDataBinder(SyncopeRoleDAO syncopeRoleDAO,
            SchemaDAO schemaDAO,
            DerivedSchemaDAO derivedSchemaDAO,
            AttributeValueDAO attributeValueDAO,
            SyncopeUserDAO syncopeUserDAO,
            ResourceDAO resourceDAO) {

        this.syncopeRoleDAO = syncopeRoleDAO;
        this.schemaDAO = schemaDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
        this.attributeValueDAO = attributeValueDAO;
        this.syncopeUserDAO = syncopeUserDAO;
        this.resourceDAO = resourceDAO;
    }

    public SyncopeRole createSyncopeRole(RoleTO roleTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeRole syncopeRole = new SyncopeRole();
        syncopeRole.setInheritAttributes(roleTO.isInheritAttributes());
        syncopeRole.setInheritDerivedAttributes(
                roleTO.isInheritDerivedAttributes());

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // name and parent
        SyncopeClientException invalidRoles =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidRoles);
        if (roleTO.getName() == null) {
            log.error("No name specified for this role");

            invalidRoles.addElement("No name specified for this role");
        } else {
            syncopeRole.setName(roleTO.getName());
        }
        Long parentRoleId = null;
        SyncopeRole parentRole = syncopeRoleDAO.find(roleTO.getParent());
        if (parentRole == null) {
            log.error("Could not find role with id " + roleTO.getParent());

            invalidRoles.addElement(String.valueOf(roleTO.getParent()));
            scce.addException(invalidRoles);
        } else {
            syncopeRole.setParent(parentRole);
            parentRoleId = syncopeRole.getParent().getId();
        }

        SyncopeRole otherRole = syncopeRoleDAO.find(
                roleTO.getName(), parentRoleId);
        if (otherRole != null) {
            log.error("Another role exists with the same name "
                    + "and the same parent role: " + otherRole);

            invalidRoles.addElement(roleTO.getName());
        }

        // attributes, derived attributes and resources
        syncopeRole = fill(syncopeRole, roleTO, AttributableUtil.ROLE, scce);

        return syncopeRole;
    }

    public SyncopeRole updateSyncopeRole(SyncopeRole syncopeRole,
            RoleMod roleMod)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // name
        SyncopeClientException invalidRoles =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidRoles);
        if (roleMod.getName() != null) {
            SyncopeRole otherRole = syncopeRoleDAO.find(
                    roleMod.getName(), syncopeRole.getParent().getId());

            if (otherRole != null) {
                log.error("Another role exists with the same name "
                        + "and the same parent role: " + otherRole);

                invalidRoles.addElement(roleMod.getName());
                scce.addException(invalidRoles);
            } else {
                syncopeRole.setName(roleMod.getName());
            }
        }

        // inherited attributes
        if (roleMod.isChangeInheritAttributes()) {
            syncopeRole.setInheritAttributes(
                    !syncopeRole.isInheritAttributes());
        }

        // inherited derived attributes
        if (roleMod.isChangeInheritDerivedAttributes()) {
            syncopeRole.setInheritDerivedAttributes(
                    !syncopeRole.isInheritDerivedAttributes());
        }

        // attributes, derived attributes and resources
        syncopeRole = fill(syncopeRole, roleMod, AttributableUtil.ROLE, scce);

        return syncopeRole;
    }

    public RoleTO getRoleTO(SyncopeRole role) {
        RoleTO roleTO = new RoleTO();
        roleTO.setId(role.getId());
        roleTO.setName(role.getName());
        roleTO.setInheritAttributes(role.isInheritAttributes());
        roleTO.setInheritDerivedAttributes(role.isInheritDerivedAttributes());
        if (role.getParent() != null) {
            roleTO.setParent(role.getParent().getId());
        }

        roleTO = fillTO(roleTO, role.getAttributes(),
                role.getDerivedAttributes(), role.getResources());

        if (role.isInheritAttributes() || role.isInheritDerivedAttributes()) {
            roleTO = fillTO(roleTO,
                    role.isInheritAttributes()
                    ? syncopeRoleDAO.findInheritedAttributes(role)
                    : Collections.EMPTY_SET,
                    role.isInheritDerivedAttributes()
                    ? syncopeRoleDAO.findInheritedDerivedAttributes(role)
                    : Collections.EMPTY_SET,
                    Collections.EMPTY_SET);
        }

        return roleTO;
    }
}
