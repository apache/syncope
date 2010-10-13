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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.MapContext;
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
import org.syncope.types.SchemaType;
import org.syncope.types.SyncopeClientExceptionType;

@Transactional(rollbackFor = {
    Throwable.class
})
public abstract class AbstractAttributableDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
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
    @Autowired
    private JexlEngine jexlEngine;

    private <T extends AbstractSchema> T getSchema(
            final String schemaName, final Class<T> reference) {

        T schema = schemaDAO.find(schemaName, reference);

        // safely ignore invalid schemas from AttributeTO
        // see http://code.google.com/p/syncope/issues/detail?id=17
        if (schema == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring invalid schema " + schemaName);
            }
        } else if (schema.isVirtual() || schema.isReadonly()) {
            schema = null;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring virtual or readonly schema " + schemaName);
            }
        }

        return schema;
    }

    private <T extends AbstractDerivedSchema> T getDerivedSchema(
            final String derivedSchemaName, final Class<T> reference) {

        T derivedSchema = derivedSchemaDAO.find(derivedSchemaName, reference);

        if (derivedSchema == null && LOG.isDebugEnabled()) {
            LOG.debug("Ignoring invalid derivedschema "
                    + derivedSchemaName);
        }

        return derivedSchema;
    }

    private TargetResource getResource(String resourceName) {
        TargetResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring invalid resource " + resourceName);
            }
        }

        return resource;
    }

    private void fillAttribute(Set<String> values,
            AttributableUtil attributableUtil,
            AbstractSchema schema,
            AbstractAttribute attribute,
            SyncopeClientException invalidValues) {

        // if the schema is multivalue, all values are considered for
        // addition, otherwise only the fist one - if provided - is
        // considered
        Set<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.EMPTY_SET
                : Collections.singleton(
                values.iterator().next()));

        AbstractAttributeValue attributeValue;
        for (String value : valuesProvided) {
            if (value == null || value.length() == 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Null value for " + schema.getName()
                            + ", ignoring");
                }
            } else {
                attributeValue = attributableUtil.newAttributeValue();

                try {
                    attributeValue = attribute.addValue(value,
                            attributeValue);
                } catch (ValidationException e) {
                    LOG.error("Invalid value for attribute "
                            + schema.getName() + ": " + value, e);

                    invalidValues.addElement(schema.getName() + ": " + value);
                }
            }
        }
    }

    private <T extends AbstractSchema> boolean evaluateMandatoryCondition(
            final String mandatoryCondition,
            final List<? extends AbstractAttribute> attributes,
            final Class<T> referenceSchema) {

        JexlContext jexlContext = new MapContext();

        List<T> allSchemas = schemaDAO.findAll(referenceSchema);
        for (AbstractAttribute attribute : attributes) {
            jexlContext.set(attribute.getSchema().getName(),
                    attribute.getValues().isEmpty()
                    ? null
                    : (!attribute.getSchema().isMultivalue()
                    ? attribute.getValuesAsStrings().iterator().next()
                    : attribute.getValuesAsStrings()));

            allSchemas.remove((T) attribute.getSchema());
        }
        for (T schema : allSchemas) {
            jexlContext.set(schema.getName(), null);
        }

        boolean result = false;

        try {
            Expression jexlExpression = jexlEngine.createExpression(
                    mandatoryCondition);
            result = Boolean.parseBoolean(
                    jexlExpression.evaluate(jexlContext).toString());
        } catch (JexlException e) {
            LOG.error("Invalid jexl expression: " + mandatoryCondition, e);
        }

        return result;
    }

    private <T extends AbstractSchema> boolean evaluateMandatoryCondition(
            final String resourceName,
            final List<? extends AbstractAttribute> attributes,
            final String schemaName,
            final Class<T> referenceSchema) {

        List<SchemaMapping> mappings = resourceDAO.getMappings(schemaName,
                SchemaType.byClass(referenceSchema), resourceName);

        boolean result = mappings == null || mappings.isEmpty()
                ? false : true;

        SchemaMapping mapping;
        for (Iterator<SchemaMapping> itor = mappings.iterator();
                itor.hasNext() && result;) {

            mapping = itor.next();
            result &= evaluateMandatoryCondition(
                    mapping.getMandatoryCondition(),
                    attributes,
                    referenceSchema);
        }

        return result;
    }

    private <T extends AbstractSchema> boolean evaluateMandatoryCondition(
            final Set<TargetResource> resources,
            final List<? extends AbstractAttribute> attributes,
            final String schemaName,
            final Class<T> referenceSchema) {

        boolean result = resources == null || resources.isEmpty()
                ? false : true;

        TargetResource resource;
        for (Iterator<TargetResource> itor = resources.iterator();
                itor.hasNext() && result;) {

            resource = itor.next();
            result &= evaluateMandatoryCondition(resource.getName(),
                    attributes, schemaName, referenceSchema);
        }

        return result;
    }

    private <T extends AbstractSchema> SyncopeClientException checkMandatory(
            final Class<T> referenceSchema,
            final AbstractAttributable attributable) {

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        Set<TargetResource> resources = new HashSet<TargetResource>();
        resources.addAll(attributable.getTargetResources());
        resources.addAll(attributable.getInheritedTargetResources());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Check mandatory constraint among resources "
                    + resources);
        }

        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<T> allSchemas = schemaDAO.findAll(referenceSchema);

        for (T schema : allSchemas) {
            if (attributable.getAttribute(schema.getName()) == null
                    && !schema.isVirtual()
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(
                    schema.getMandatoryCondition(),
                    attributable.getAttributes(),
                    referenceSchema)
                    || evaluateMandatoryCondition(resources,
                    attributable.getAttributes(),
                    schema.getName(),
                    referenceSchema))) {

                LOG.error("Mandatory schema " + schema.getName()
                        + " not provided with values");

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

        Set<TargetResource> resources = new HashSet<TargetResource>();
        resources.addAll(attributable.getTargetResources());
        resources.addAll(attributable.getInheritedTargetResources());

        ResourceOperations resourceOperations = new ResourceOperations();

        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema = null;
        AbstractAttribute attribute = null;
        AbstractDerivedSchema derivedSchema = null;
        AbstractDerivedAttribute derivedAttribute = null;

        // 1. attributes to be removed
        for (String attributeToBeRemoved :
                attributableMod.getAttributesToBeRemoved()) {

            schema = getSchema(
                    attributeToBeRemoved, attributableUtil.getSchemaClass());

            if (schema != null) {
                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No attribute found for schema " + schema);
                    }
                } else {
                    attributable.removeAttribute(attribute);

                    attributeDAO.delete(attribute.getId(),
                            attributableUtil.getAttributeClass());
                }

                for (SchemaMapping mapping : resourceDAO.getMappings(
                        schema.getName(),
                        SchemaType.byClass(
                        attributableUtil.getSchemaClass()))) {

                    if (mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && attribute != null
                                && !attribute.getValuesAsStrings().isEmpty()) {

                            resourceOperations.setOldAccountId(
                                    attribute.getValuesAsStrings().
                                    iterator().next());
                        }
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("About attributes to be removed:\n" + resourceOperations);
        }

        // 2. attributes to be updated
        Set<Long> valuesToBeRemoved = null;
        for (AttributeMod attributeMod :
                attributableMod.getAttributesToBeUpdated()) {

            schema = getSchema(attributeMod.getSchema(),
                    attributableUtil.getSchemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : resourceDAO.getMappings(
                        schema.getName(),
                        SchemaType.byClass(
                        attributableUtil.getSchemaClass()))) {

                    if (mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

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

                    for (AbstractAttributeValue mav : attribute.getValues()) {

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
                        attributableUtil, schema, attribute, invalidValues);

                // if no values are in, the attribute can be safely removed
                if (attribute.getValues().isEmpty()) {
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("About attributes to be updated:\n" + resourceOperations);
        }

        // 3. derived attributes to be removed
        for (String derivedAttributeToBeRemoved :
                attributableMod.getDerivedAttributesToBeRemoved()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeRemoved,
                    attributableUtil.getDerivedSchemaClass());
            if (derivedSchema != null) {
                for (SchemaMapping mapping : derivedSchema.getMappings()) {
                    if (mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {
                        resourceOperations.add(Type.UPDATE,
                                mapping.getResource());
                    }
                }

                derivedAttribute = attributable.getDerivedAttribute(
                        derivedSchema.getName());
                if (derivedAttribute == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No derived attribute found for schema "
                                + derivedSchema.getName());
                    }
                } else {
                    derivedAttributeDAO.delete(derivedAttribute);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("About derived attributes to be removed:\n"
                    + resourceOperations);
        }

        // 4. derived attributes to be added
        for (String derivedAttributeToBeAdded :
                attributableMod.getDerivedAttributesToBeAdded()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeAdded,
                    attributableUtil.getDerivedSchemaClass());
            if (derivedSchema != null) {
                for (SchemaMapping mapping : derivedSchema.getMappings()) {
                    if (mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("About derived attributes to be added:\n"
                    + resourceOperations);
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("About resources to be removed:\n" + resourceOperations);
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("About resources to be added:\n" + resourceOperations);
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
        // Only consider attributeTO with values
        for (AttributeTO attributeTO : attributableTO.getAttributes()) {
            if (attributeTO.getValues() != null
                    && !attributeTO.getValues().isEmpty()) {

                schema = getSchema(attributeTO.getSchema(),
                        attributableUtil.getSchemaClass());

                if (schema != null) {
                    attribute =
                            attributable.getAttribute(schema.getName()) == null
                            ? attributableUtil.newAttribute()
                            : attributable.getAttribute(schema.getName());
                    attribute.setSchema(schema);

                    fillAttribute(attributeTO.getValues(),
                            attributableUtil, schema, attribute, invalidValues);

                    if (!attribute.getValues().isEmpty()) {
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
            attributeTO.setValues(new HashSet(attribute.getValuesAsStrings()));
            attributeTO.setReadonly(attribute.getSchema().isReadonly());

            abstractAttributableTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute : derivedAttributes) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(attributes));
            attributeTO.setReadonly(true);

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
                    attribute.getValues()) {

                if (attribute.getSchema().isUniquevalue()
                        && attributeValueDAO.nonUniqueAttributeValue(
                        attributeValue)) {

                    LOG.error("Unique value schema "
                            + attribute.getSchema().getName()
                            + " with no unique value: "
                            + attributeValue.getValueAsString());

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
