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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.AbstractAttributableMod;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.persistence.propagation.ResourceOperations.Type;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.SyncopeClientExceptionType;

@Transactional(rollbackFor = {Throwable.class})
public abstract class AbstractAttributableDataBinder {

    protected static final Logger log = LoggerFactory.getLogger(
            AbstractAttributableDataBinder.class);

    @Autowired
    protected SyncopeRoleDAO syncopeRoleDAO;

    @Autowired
    protected SchemaDAO schemaDAO;

    @Autowired
    protected DerivedSchemaDAO derivedSchemaDAO;

    @Autowired
    protected AttributeDAO attributeDAO;

    @Autowired
    protected DerivedAttributeDAO derivedAttributeDAO;

    @Autowired
    protected AttributeValueDAO attributeValueDAO;

    @Autowired
    protected SyncopeUserDAO syncopeUserDAO;

    @Autowired
    protected ResourceDAO resourceDAO;

    @Autowired
    protected MembershipDAO membershipDAO;

    private <T extends AbstractSchema> AbstractSchema getSchema(
            String schemaName, Class<T> reference) {

        T schema = schemaDAO.find(schemaName, reference);

        // safely ignore invalid schemas from AttributeTO
        // see http://code.google.com/p/syncope/issues/detail?id=17
        if (schema == null) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring invalid schema " + schemaName);
            }
        } else if (schema.isVirtual()) {
                schema = null;

                if (log.isDebugEnabled()) {
                    log.debug("Ignoring virtual schema " + schemaName);
                }
            }

        return schema;
    }

    private <T extends AbstractDerivedSchema> AbstractDerivedSchema getDerivedSchema(
            String derivedSchemaName, Class<T> reference) {

        T derivedSchema = derivedSchemaDAO.find(derivedSchemaName, reference);

        if (derivedSchema == null) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring invalid derivedschema " + derivedSchemaName);
            }
        }

        return derivedSchema;
    }

    private TargetResource getResource(String resourceName) {
        TargetResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring invalid resource " + resourceName);
            }
        }

        return resource;
    }

    private void fillAttribute(Set<String> values,
            AttributableUtil attributableUtil,
            AbstractSchema schema,
            AbstractAttribute attribute,
            AbstractAttributeValue attributeValue,
            SyncopeClientException invalidValues) {

        // if the schema is multivale, all values are considered for
        // addition, otherwise only the fist one - if provided - is
        // considered
        Set<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.EMPTY_SET
                : Collections.singleton(
                values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.length() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Null value for " + schema.getName() + ", ignoring");
                }
            } else {
                attributeValue = attributableUtil.newAttributeValue();

                try {
                    attributeValue = attribute.addValue(value,
                            attributeValue);
                } catch (ValidationException e) {
                    log.error("Invalid value for attribute " + schema.getName() + ": " + value, e);

                    invalidValues.addElement(value);
                }
            }
        }
    }

    private <T extends AbstractSchema> SyncopeClientException checkMandatory(
            Class<T> referenceSchema,
            AbstractAttributable attributable) {

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);


        Set<TargetResource> resources = new HashSet<TargetResource>();
        resources.addAll(attributable.getTargetResources());
        resources.addAll(attributable.getInheritedTargetResources());

        if (log.isDebugEnabled()) {
            log.debug("Check mandatory constraint among resources " + resources);
        }

        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<T> allSchemas = schemaDAO.findAll(referenceSchema);

        for (T schema : allSchemas) {
            if (attributable.getAttribute(schema.getName()) == null && (schema.isMandatory() || schemaDAO.isMandatoryOnResource(schema, resources))) {

                log.error("Mandatory schema " + schema.getName() + " not provided with values");

                requiredValuesMissing.addElement(schema.getName());
            }
        }


        return requiredValuesMissing;
    }

    protected ResourceOperations fill(
            AbstractAttributable attributable,
            AbstractAttributableMod attributableMod,
            AttributableUtil attributableUtil,
            SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        ResourceOperations resourceOperations = new ResourceOperations();

        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema = null;
        AbstractAttribute attribute = null;
        AbstractAttributeValue attributeValue = null;
        AbstractDerivedSchema derivedSchema = null;
        AbstractDerivedAttribute derivedAttribute = null;

        // 1. attributes to be removed
        for (String attributeToBeRemoved :
                attributableMod.getAttributesToBeRemoved()) {

            schema = getSchema(
                    attributeToBeRemoved, attributableUtil.getSchemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : schema.getMappings()) {
                    if (mapping.getResource() != null) {
                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());
                    }
                }

                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No attribute found for schema " + schema.getName());
                    }
                } else {
                    attributable.removeAttribute(attribute);

                    attributeDAO.delete(attribute.getId(),
                            attributableUtil.getAttributeClass());
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("About attributes to be removed:\n" + resourceOperations);
        }

        // 2. attributes to be updated
        Set<Long> valuesToBeRemoved = null;
        for (AttributeMod attributeMod :
                attributableMod.getAttributesToBeUpdated()) {

            schema = getSchema(attributeMod.getSchema(),
                    attributableUtil.getSchemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : schema.getMappings()) {
                    if (mapping.getResource() != null) {
                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());
                    }
                }

                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    attribute = attributableUtil.newAttribute();
                    attribute.setSchema(schema);
                    attribute.setOwner(attributable);

                    attributable.addAttribute(attribute);
                }

                // 1.1 remove values
                valuesToBeRemoved = new HashSet<Long>();
                for (String valueToBeRemoved :
                        attributeMod.getValuesToBeRemoved()) {

                    for (AbstractAttributeValue mav :
                            attribute.getAttributeValues()) {

                        if (valueToBeRemoved.equals(mav.getValueAsString())) {
                            valuesToBeRemoved.add(mav.getId());
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    attributeValueDAO.delete(attributeValueId,
                            attributableUtil.getAttributeValueClass());
                }

                // 1.2 add values
                fillAttribute(attributeMod.getValuesToBeAdded(),
                        attributableUtil, schema, attribute,
                        attributeValue, invalidValues);

                // if no values are in, the attribute can be safely removed
                if (attribute.getAttributeValues().isEmpty()) {
                    attributeDAO.delete(attribute);
                }
            }
        }

        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing =
                checkMandatory(attributableUtil.getSchemaClass(), attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (log.isDebugEnabled()) {
            log.debug("About attributes to be updated:\n" + resourceOperations);
        }

        // 3. derived attributes to be removed
        for (String derivedAttributeToBeRemoved :
                attributableMod.getDerivedAttributesToBeRemoved()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeRemoved,
                    attributableUtil.getDerivedSchemaClass());
            if (derivedSchema != null) {
                for (SchemaMapping mapping : derivedSchema.getMappings()) {
                    if (mapping.getResource() != null) {
                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());
                    }
                }

                derivedAttribute = attributable.getDerivedAttribute(
                        derivedSchema.getName());
                if (derivedAttribute == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("No derived attribute found for schema " + derivedSchema.getName());
                    }
                } else {
                    derivedAttributeDAO.delete(derivedAttribute);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("About derived attributes to be removed:\n" +
                    resourceOperations);
        }

        // 4. derived attributes to be added
        for (String derivedAttributeToBeAdded :
                attributableMod.getDerivedAttributesToBeAdded()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeAdded,
                    attributableUtil.getDerivedSchemaClass());
            if (derivedSchema != null) {
                for (SchemaMapping mapping : derivedSchema.getMappings()) {
                    if (mapping.getResource() != null) {
                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());
                    }
                }

                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("About derived attributes to be added:\n" +
                    resourceOperations);
        }

        // 5. resources to be removed
        TargetResource resource = null;
        for (String resourceToBeRemoved :
                attributableMod.getResourcesToBeRemoved()) {

            resource = getResource(resourceToBeRemoved);

            if (resource != null) {
                resourceOperations.add(Type.DELETE, resource);

                attributable.removeTargetResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.removeUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.removeRole((SyncopeRole) attributable);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("About resources to be removed:\n" + resourceOperations);
        }

        // 6. resources to be added
        for (String resourceToBeAdded :
                attributableMod.getResourcesToBeAdded()) {

            resource = getResource(resourceToBeAdded);

            if (resource != null) {
                resourceOperations.add(Type.CREATE, resource);

                attributable.addTargetResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.addUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.addRole((SyncopeRole) attributable);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("About resources to be added:\n" + resourceOperations);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return resourceOperations;
    }

    protected AbstractAttributable fill(AbstractAttributable attributable,
            AbstractAttributableTO attributableTO,
            AttributableUtil attributableUtil,
            SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        // 1. attributes
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema = null;
        AbstractAttribute attribute = null;
        AbstractAttributeValue attributeValue = null;
        for (AttributeTO attributeTO : attributableTO.getAttributes()) {
            // Only consider attributeTO with values
            if (attributeTO.getValues() != null && !attributeTO.getValues().isEmpty()) {

                schema = getSchema(attributeTO.getSchema(),
                        attributableUtil.getSchemaClass());

                if (schema != null) {
                    attribute = attributableUtil.newAttribute();
                    attribute.setSchema(schema);

                    fillAttribute(attributeTO.getValues(),
                            attributableUtil, schema, attribute,
                            attributeValue, invalidValues);

                    if (!attribute.getAttributeValues().isEmpty()) {
                        attributable.addAttribute(attribute);
                        attribute.setOwner(attributable);
                    }
                }
            }
        }

        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing =
                checkMandatory(attributableUtil.getSchemaClass(), attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // 2. derived attributes
        AbstractDerivedSchema derivedSchema = null;
        AbstractDerivedAttribute derivedAttribute = null;
        for (AttributeTO attributeTO : attributableTO.getDerivedAttributes()) {

            derivedSchema = getDerivedSchema(attributeTO.getSchema(),
                    attributableUtil.getDerivedSchemaClass());

            if (derivedSchema != null) {
                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        // 3. resources
        TargetResource resource = null;
        for (String resourceName : attributableTO.getResources()) {
            resource = getResource(resourceName);

            if (resource != null) {
                attributable.addTargetResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.addUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.addRole((SyncopeRole) attributable);
                }
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return attributable;
    }

    protected AbstractAttributableTO fillTO(
            AbstractAttributableTO abstractAttributableTO,
            Collection<? extends AbstractAttribute> attributes,
            Collection<? extends AbstractDerivedAttribute> derivedAttributes,
            Collection<TargetResource> resources) {

        AttributeTO attributeTO = null;
        for (AbstractAttribute attribute : attributes) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(new HashSet(
                    attribute.getAttributeValuesAsStrings()));

            abstractAttributableTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute : derivedAttributes) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(attributes));

            abstractAttributableTO.addDerivedAttribute(attributeTO);
        }

        for (TargetResource resource : resources) {
            abstractAttributableTO.addResource(resource.getName());
        }

        return abstractAttributableTO;
    }

    public void checkUniqueness(AbstractAttributable attributable)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientException invalidUniques = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidUniques);

        for (AbstractAttribute attribute : attributable.getAttributes()) {

            for (AbstractAttributeValue attributeValue :
                    attribute.getAttributeValues()) {

                if (attribute.getSchema().isUniquevalue() && attributeValueDAO.nonUniqueAttributeValue(
                        attributeValue)) {

                    log.error("Unique value schema " + attribute.getSchema().getName() + " with no unique value: " + attributeValue.getValueAsString());

                    invalidUniques.addElement(attribute.getSchema().getName());
                }
            }
        }

        if (!invalidUniques.getElements().isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(invalidUniques);

            throw scce;
        }
    }
}
