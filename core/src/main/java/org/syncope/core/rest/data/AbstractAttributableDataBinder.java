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

import org.syncope.core.util.AttributableUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.validation.ValidationException;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.AbstractAttributableMod;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttrDAO;
import org.syncope.core.persistence.dao.AttrValueDAO;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.DerAttrDAO;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.VirAttrDAO;
import org.syncope.core.persistence.dao.VirSchemaDAO;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.ResourceOperationType;
import org.syncope.types.SyncopeClientExceptionType;

public abstract class AbstractAttributableDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractAttributableDataBinder.class);

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected SchemaDAO schemaDAO;

    @Autowired
    protected DerSchemaDAO derivedSchemaDAO;

    @Autowired
    protected VirSchemaDAO virtualSchemaDAO;

    @Autowired
    protected AttrDAO attributeDAO;

    @Autowired
    protected DerAttrDAO derivedAttributeDAO;

    @Autowired
    protected VirAttrDAO virtualAttributeDAO;

    @Autowired
    protected AttrValueDAO attributeValueDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected ResourceDAO resourceDAO;

    @Autowired
    protected MembershipDAO membershipDAO;

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractSchema> T getSchema(
            final String schemaName, final Class<T> reference) {

        T schema = schemaDAO.find(schemaName, reference);

        // safely ignore invalid schemas from AttributeTO
        // see http://code.google.com/p/syncope/issues/detail?id=17
        if (schema == null) {
            LOG.debug("Ignoring invalid schema {}", schemaName);
        } else if (schema.isReadonly()) {
            schema = null;

            LOG.debug("Ignoring virtual or readonly schema {}", schemaName);
        }

        return schema;
    }

    private <T extends AbstractDerSchema> T getDerivedSchema(
            final String derSchemaName, final Class<T> reference) {

        T derivedSchema = derivedSchemaDAO.find(derSchemaName, reference);

        if (derivedSchema == null) {
            LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
        }

        return derivedSchema;
    }

    private <T extends AbstractVirSchema> T getVirtualSchema(
            final String virSchemaName, final Class<T> reference) {

        T virtualSchema = virtualSchemaDAO.find(virSchemaName, reference);

        if (virtualSchema == null) {
            LOG.debug("Ignoring invalid virtual schema {}", virSchemaName);
        }

        return virtualSchema;
    }

    private TargetResource getResource(final String resourceName) {
        TargetResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            LOG.debug("Ignoring invalid resource {} ", resourceName);
        }

        return resource;
    }

    protected void fillAttribute(final List<String> values,
            final AttributableUtil attributableUtil,
            final AbstractSchema schema,
            final AbstractAttr attribute,
            final SyncopeClientException invalidValues) {

        // if the schema is multivalue, all values are considered for
        // addition, otherwise only the fist one - if provided - is
        // considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.EMPTY_LIST
                : Collections.singletonList(
                values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getName());
            } else {
                try {
                    attribute.addValue(value, attributableUtil);
                } catch (ValidationException e) {
                    LOG.error("Invalid value for attribute "
                            + schema.getName() + ": " + value, e);

                    invalidValues.addElement(schema.getName() + ": " + value);
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(
            final String mandatoryCondition,
            final List<? extends AbstractAttr> attributes) {

        JexlContext jexlContext = new MapContext();
        jexlUtil.addAttributesToContext(attributes, jexlContext);

        return Boolean.parseBoolean(
                jexlUtil.evaluateWithAttributes(
                mandatoryCondition, jexlContext));
    }

    private boolean evaluateMandatoryCondition(
            final TargetResource resource,
            final List<? extends AbstractAttr> attributes,
            final String sourceAttrName,
            final AttributableUtil attributableUtil) {

        List<SchemaMapping> mappings = resource.getMappings(sourceAttrName,
                attributableUtil.sourceMappingType());

        boolean result = false;

        SchemaMapping mapping;
        for (Iterator<SchemaMapping> itor = mappings.iterator();
                itor.hasNext() && !result;) {

            mapping = itor.next();
            result |= evaluateMandatoryCondition(
                    mapping.getMandatoryCondition(),
                    attributes);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(
            final Set<TargetResource> resources,
            final List<? extends AbstractAttr> attributes,
            final String sourceAttrName,
            final AttributableUtil attributableUtil) {

        boolean result = false;

        TargetResource resource;
        for (Iterator<TargetResource> itor = resources.iterator();
                itor.hasNext() && !result;) {

            resource = itor.next();
            if (resource.isForceMandatoryConstraint()) {
                result |= evaluateMandatoryCondition(resource,
                        attributes, sourceAttrName, attributableUtil);
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(
            final AttributableUtil attributableUtil,
            final AbstractAttributable attributable) {

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        Set<TargetResource> resources = new HashSet<TargetResource>();
        resources.addAll(attributable.getTargetResources());
        resources.addAll(attributable.getInheritedTargetResources());

        LOG.debug("Check mandatory constraint among resources {}", resources);
        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<AbstractSchema> allSchemas =
                schemaDAO.findAll(attributableUtil.schemaClass());

        for (AbstractSchema schema : allSchemas) {
            if (attributable.getAttribute(schema.getName()) == null
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(
                    schema.getMandatoryCondition(),
                    attributable.getAttributes())
                    || evaluateMandatoryCondition(resources,
                    attributable.getAttributes(),
                    schema.getName(),
                    attributableUtil))) {

                LOG.error("Mandatory schema " + schema.getName()
                        + " not provided with values");

                requiredValuesMissing.addElement(schema.getName());
            }
        }

        return requiredValuesMissing;
    }

    protected ResourceOperations fill(
            final AbstractAttributable attributable,
            final AbstractAttributableMod attributableMod,
            final AttributableUtil attributableUtil,
            final SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        Set<TargetResource> resources = new HashSet<TargetResource>();
        resources.addAll(attributable.getTargetResources());
        resources.addAll(attributable.getInheritedTargetResources());

        ResourceOperations resourceOperations = new ResourceOperations();

        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema;
        AbstractAttr attribute;
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;
        AbstractVirSchema virtualSchema;
        AbstractVirAttr virtualAttribute;

        // 1. attributes to be removed
        for (String attributeToBeRemoved :
                attributableMod.getAttributesToBeRemoved()) {

            schema = getSchema(
                    attributeToBeRemoved, attributableUtil.schemaClass());

            if (schema != null) {
                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    attributable.removeAttribute(attribute);

                    attributeDAO.delete(attribute.getId(),
                            attributableUtil.attributeClass());
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(schema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.sourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && attribute != null
                                && !attribute.getValuesAsStrings().isEmpty()) {

                            resourceOperations.addOldAccountId(
                                    mapping.getResource().getName(),
                                    attribute.getValuesAsStrings().
                                    iterator().next());
                        }
                    }
                }
            }
        }

        LOG.debug("About attributes to be removed:\n{}", resourceOperations);

        // 2. attributes to be updated
        Set<Long> valuesToBeRemoved;
        for (AttributeMod attributeMod :
                attributableMod.getAttributesToBeUpdated()) {

            schema = getSchema(attributeMod.getSchema(),
                    attributableUtil.schemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(schema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.sourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
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

                    if (attribute.getSchema().isUniqueConstraint()) {
                        if (valueToBeRemoved.equals(attribute.getUniqueValue().
                                getValueAsString())) {

                            valuesToBeRemoved.add(
                                    attribute.getUniqueValue().getId());
                        }
                    } else {
                        for (AbstractAttrValue mav : attribute.getValues()) {
                            if (valueToBeRemoved.equals(
                                    mav.getValueAsString())) {

                                valuesToBeRemoved.add(mav.getId());
                            }
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    attributeValueDAO.delete(attributeValueId,
                            attributableUtil.attributeValueClass());
                }

                // 1.2 add values
                fillAttribute(attributeMod.getValuesToBeAdded(),
                        attributableUtil, schema, attribute, invalidValues);

                // if no values are in, the attribute can be safely removed
                if (attribute.getValuesAsStrings().isEmpty()) {
                    attributeDAO.delete(attribute);
                }
            }
        }

        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing =
                checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        LOG.debug("About attributes to be updated:\n{}", resourceOperations);

        // 3. derived attributes to be removed
        for (String derivedAttributeToBeRemoved :
                attributableMod.getDerivedAttributesToBeRemoved()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeRemoved,
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {

                derivedAttribute = attributable.getDerivedAttribute(
                        derivedSchema.getName());

                if (derivedAttribute == null) {
                    LOG.debug("No derived attribute found for schema {}",
                            derivedSchema.getName());
                } else {
                    derivedAttributeDAO.delete(derivedAttribute);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(
                            derivedSchema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.derivedSourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && derivedAttribute != null
                                && !derivedAttribute.getValue(
                                attributable.getAttributes()).isEmpty()) {

                            resourceOperations.addOldAccountId(
                                    mapping.getResource().getName(),
                                    derivedAttribute.getValue(
                                    attributable.getAttributes()));
                        }
                    }
                }
            }
        }

        LOG.debug("About derived attributes to be removed:\n{}",
                resourceOperations);

        // 4. virtual attributes to be removed
        for (String virtualAttributeToBeRemoved :
                attributableMod.getVirtualAttributesToBeRemoved()) {

            virtualSchema = getVirtualSchema(virtualAttributeToBeRemoved,
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {

                virtualAttribute = attributable.getVirtualAttribute(
                        virtualSchema.getName());

                if (virtualAttribute == null) {
                    LOG.debug("No virtual attribute found for schema {}",
                            virtualSchema.getName());
                } else {
                    virtualAttributeDAO.delete(virtualAttribute);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(
                            virtualSchema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.virtualSourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
                                mapping.getResource());

                        if (mapping.isAccountid() && virtualAttribute != null
                                && !virtualAttribute.getValues().isEmpty()) {

                            resourceOperations.addOldAccountId(
                                    mapping.getResource().getName(),
                                    virtualAttribute.getValues().get(0));
                        }
                    }
                }
            }
        }

        LOG.debug("About virtual attributes to be removed:\n{}",
                resourceOperations);

        // 5. derived attributes to be added
        for (String derivedAttributeToBeAdded :
                attributableMod.getDerivedAttributesToBeAdded()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeAdded,
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(
                            derivedSchema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.derivedSourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
                                mapping.getResource());
                    }
                }

                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        LOG.debug("About derived attributes to be added:\n{}",
                resourceOperations);

        // 6. virtual attributes to be added
        for (String virtualAttributeToBeAdded :
                attributableMod.getVirtualAttributesToBeAdded()) {

            virtualSchema = getVirtualSchema(virtualAttributeToBeAdded,
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (mapping.getSourceAttrName().equals(
                            virtualSchema.getName())
                            && mapping.getSourceMappingType()
                            == attributableUtil.virtualSourceMappingType()
                            && mapping.getResource() != null
                            && resources.contains(mapping.getResource())) {

                        resourceOperations.add(ResourceOperationType.UPDATE,
                                mapping.getResource());
                    }
                }

                virtualAttribute = attributableUtil.newVirtualAttribute();
                virtualAttribute.setVirtualSchema(virtualSchema);
                virtualAttribute.setOwner(attributable);
                attributable.addVirtualAttribute(virtualAttribute);
            }
        }

        LOG.debug("About virtual attributes to be added:\n{}",
                resourceOperations);

        // 7. resources to be removed
        TargetResource resource;
        for (String resourceToBeRemoved :
                attributableMod.getResourcesToBeRemoved()) {

            resource = getResource(resourceToBeRemoved);

            if (resource != null) {
                resourceOperations.add(ResourceOperationType.DELETE, resource);

                attributable.removeTargetResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.removeUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.removeRole((SyncopeRole) attributable);
                }
            }
        }

        LOG.debug("About resources to be removed:\n{}", resourceOperations);

        // 6. resources to be added
        for (String resourceToBeAdded :
                attributableMod.getResourcesToBeAdded()) {

            resource = getResource(resourceToBeAdded);

            if (resource != null) {
                resourceOperations.add(ResourceOperationType.CREATE, resource);

                attributable.addTargetResource(resource);

                if (attributableUtil == attributableUtil.USER) {
                    resource.addUser((SyncopeUser) attributable);
                }
                if (attributableUtil == attributableUtil.ROLE) {
                    resource.addRole((SyncopeRole) attributable);
                }
            }
        }

        LOG.debug("About resources to be added:\n{}", resourceOperations);

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return resourceOperations;
    }

    protected void fill(AbstractAttributable attributable,
            AbstractAttributableTO attributableTO,
            AttributableUtil attributableUtil,
            SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        // 1. attributes
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema;
        AbstractAttr attribute;

        // Only consider attributeTO with values
        for (AttributeTO attributeTO : attributableTO.getAttributes()) {
            if (attributeTO.getValues() != null
                    && !attributeTO.getValues().isEmpty()) {

                schema = getSchema(attributeTO.getSchema(),
                        attributableUtil.schemaClass());

                if (schema != null) {
                    attribute = attributable.getAttribute(schema.getName());
                    if (attribute == null) {
                        attribute = attributableUtil.newAttribute();
                        attribute.setSchema(schema);
                    }

                    fillAttribute(attributeTO.getValues(),
                            attributableUtil,
                            schema,
                            attribute,
                            invalidValues);

                    if (!attribute.getValuesAsStrings().isEmpty()) {
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
                checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // 2. derived attributes
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;
        for (AttributeTO attributeTO : attributableTO.getDerivedAttributes()) {

            derivedSchema = getDerivedSchema(attributeTO.getSchema(),
                    attributableUtil.derivedSchemaClass());

            if (derivedSchema != null) {
                derivedAttribute = attributableUtil.newDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        // 3. virtual attributes
        AbstractVirSchema virtualSchema;
        AbstractVirAttr virtualAttribute;
        for (AttributeTO attributeTO : attributableTO.getVirtualAttributes()) {

            virtualSchema = getVirtualSchema(attributeTO.getSchema(),
                    attributableUtil.virtualSchemaClass());

            if (virtualSchema != null) {
                virtualAttribute = attributableUtil.newVirtualAttribute();
                virtualAttribute.setVirtualSchema(virtualSchema);
                virtualAttribute.setOwner(attributable);
                virtualAttribute.setValues(attributeTO.getValues());
                attributable.addVirtualAttribute(virtualAttribute);
            }
        }

        // 3. resources
        TargetResource resource;
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
    }

    public void fillTO(
            AbstractAttributableTO abstractAttributableTO,
            Collection<? extends AbstractAttr> attributes,
            Collection<? extends AbstractDerAttr> derivedAttributes,
            Collection<? extends AbstractVirAttr> virtualAttributes,
            Collection<TargetResource> resources) {

        AttributeTO attributeTO;
        for (AbstractAttr attribute : attributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getValuesAsStrings());
            attributeTO.setReadonly(attribute.getSchema().isReadonly());

            abstractAttributableTO.addAttribute(attributeTO);
        }

        for (AbstractDerAttr derivedAttribute : derivedAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(attributes));
            attributeTO.setReadonly(true);

            abstractAttributableTO.addDerivedAttribute(attributeTO);
        }

        for (AbstractVirAttr virtualAttribute : virtualAttributes) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    virtualAttribute.getVirtualSchema().getName());
            attributeTO.setValues(virtualAttribute.getValues());
            attributeTO.setReadonly(false);

            abstractAttributableTO.addVirtualAttribute(attributeTO);
        }

        for (TargetResource resource : resources) {
            abstractAttributableTO.addResource(resource.getName());
        }
    }
}
