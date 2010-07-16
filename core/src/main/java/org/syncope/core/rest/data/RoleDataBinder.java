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
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.RoleAttribute;
import org.syncope.core.persistence.beans.role.RoleAttributeValue;
import org.syncope.core.persistence.beans.role.RoleDerivedAttribute;
import org.syncope.core.persistence.beans.role.RoleDerivedSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class RoleDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            RoleDataBinder.class);
    private SyncopeRoleDAO syncopeRoleDAO;
    private SchemaDAO schemaDAO;
    private DerivedSchemaDAO derivedSchemaDAO;
    private AttributeValueDAO attributeValueDAO;
    private SyncopeUserDAO syncopeUserDAO;
    private ResourceDAO resourceDAO;

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

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);
        SyncopeClientException invalidRoles =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidRoles);
        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);
        SyncopeClientException invalidUniques = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidUniques);

        SyncopeRole syncopeRole = new SyncopeRole();

        // 0. name and parent
        if (roleTO.getName() == null) {
            log.error("No name specified for this role");

            invalidRoles.addElement(null);
        } else {
            syncopeRole.setName(roleTO.getName());
        }
        Long parentRoleId = null;
        if (roleTO.getParent() != null) {
            SyncopeRole parentRole = syncopeRoleDAO.find(roleTO.getParent());
            if (parentRole == null) {
                log.error("Could not find role with id " + roleTO.getParent());

                invalidRoles.addElement(String.valueOf(roleTO.getParent()));
            } else {
                syncopeRole.setParent(parentRole);
                parentRoleId = syncopeRole.getParent().getId();
            }
        }

        SyncopeRole otherRole = syncopeRoleDAO.find(
                roleTO.getName(), parentRoleId);
        if (otherRole != null) {
            log.error("Another role exists with the same name "
                    + "and the same parent role: " + otherRole);

            invalidRoles.addElement(roleTO.getName());
        }

        // 1. attributes
        RoleSchema schema = null;
        RoleAttribute attribute = null;
        Set<String> valuesProvided = null;
        RoleAttributeValue attributeValue = null;
        for (AttributeTO attributeTO : roleTO.getAttributes()) {
            schema = schemaDAO.find(attributeTO.getSchema(), RoleSchema.class);

            // safely ignore invalid schemas from AttributeTO
            // see http://code.google.com/p/syncope/issues/detail?id=17
            if (schema == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid schema "
                            + attributeTO.getSchema());
                }
            } else {
                if (schema.isVirtual()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring virtual schema" + schema.getName());
                    }
                } else {
                    attribute = new RoleAttribute();
                    attribute.setSchema(schema);
                    attribute.setOwner(syncopeRole);

                    // if the schema is multivale, all values are considered for
                    // addition, otherwise only the fist one - if provided - is
                    // considered
                    valuesProvided = schema.isMultivalue()
                            ? attributeTO.getValues()
                            : (attributeTO.getValues().isEmpty()
                            ? Collections.EMPTY_SET
                            : Collections.singleton(
                            attributeTO.getValues().iterator().next()));
                    for (String value : valuesProvided) {
                        attributeValue = new RoleAttributeValue();

                        try {
                            attributeValue = attribute.addValue(value,
                                    attributeValue);
                        } catch (ValidationException e) {
                            log.error("Invalid value for attribute "
                                    + schema.getName() + ": " + value, e);

                            invalidValues.addElement(schema.getName());
                        }

                        // if the schema is uniquevalue, check the uniqueness
                        if (schema.isUniquevalue()
                                && attributeValueDAO.existingAttributeValue(
                                attributeValue)) {

                            log.error("Unique value schema " + schema.getName()
                                    + " with no unique value: "
                                    + attributeValue.getValueAsString());

                            invalidUniques.addElement(schema.getName());
                            attribute.setAttributeValues(Collections.EMPTY_SET);
                        }
                    }

                    if (!attribute.getAttributeValues().isEmpty()) {
                        syncopeRole.addAttribute(attribute);
                    }
                }
            }
        }

        // 2. derived attributes
        RoleDerivedSchema derivedSchema = null;
        RoleDerivedAttribute derivedAttribute = null;
        for (AttributeTO attributeTO : roleTO.getDerivedAttributes()) {
            derivedSchema = derivedSchemaDAO.find(attributeTO.getSchema(),
                    RoleDerivedSchema.class);

            if (derivedSchema == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid derivedschema "
                            + attributeTO.getSchema());
                }
            } else {
                derivedAttribute = new RoleDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(syncopeRole);
                syncopeRole.addDerivedAttribute(derivedAttribute);
            }
        }

        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<RoleSchema> allRoleSchemas = schemaDAO.findAll(RoleSchema.class);
        for (RoleSchema roleSchema : allRoleSchemas) {
            if (syncopeRole.getAttribute(roleSchema.getName()) == null
                    && roleSchema.isMandatory()) {

                log.error("Mandatory schema " + roleSchema.getName()
                        + " not provided with values");

                requiredValuesMissing.addElement(roleSchema.getName());
            }
        }

        // 3. users
        SyncopeUser user = null;
        for (Long userId : roleTO.getUsers()) {
            user = syncopeUserDAO.find(userId);

            if (user == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid user " + userId);
                }
            } else {
                syncopeRole.addUser(user);
                user.addRole(syncopeRole);
            }
        }

        // 4. resources
        Resource resource = null;
        for (String resourceName : roleTO.getResources()) {
            resource = resourceDAO.find(resourceName);

            if (resource == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid resource " + resourceName);
                }
            } else {
                syncopeRole.addResource(resource);
                resource.addRole(syncopeRole);
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!invalidRoles.getElements().isEmpty()) {
            compositeErrorException.addException(invalidRoles);
        }
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }
        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }
        if (!invalidUniques.getElements().isEmpty()) {
            compositeErrorException.addException(invalidUniques);
        }
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return syncopeRole;
    }

    public RoleTO getRoleTO(SyncopeRole role) {
        RoleTO roleTO = new RoleTO();
        roleTO.setId(role.getId());
        roleTO.setName(role.getName());
        if (role.getParent() != null) {
            roleTO.setParent(role.getParent().getId());
        }

        AttributeTO attributeTO = null;
        for (AbstractAttribute attribute : role.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getAttributeValuesAsStrings());

            roleTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute :
                role.getDerivedAttributes()) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.setValues(Collections.singleton(
                    derivedAttribute.getValue(role.getAttributes())));

            roleTO.addDerivedAttribute(attributeTO);
        }

        for (SyncopeUser user : role.getUsers()) {
            roleTO.addUser(user.getId());
        }

        for (Resource resource : role.getResources()) {
            roleTO.addResource(resource.getName());
        }

        return roleTO;
    }
}
