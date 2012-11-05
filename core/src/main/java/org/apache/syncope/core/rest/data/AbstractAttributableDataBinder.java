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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.validation.ValidationException;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrValueDAO;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.MembershipDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.dao.VirSchemaDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.mod.AbstractAttributableMod;
import org.apache.syncope.mod.AttributeMod;
import org.apache.syncope.to.AbstractAttributableTO;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.PropagationOperation;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.validation.SyncopeClientException;

public abstract class AbstractAttributableDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAttributableDataBinder.class);

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected SchemaDAO schemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected AttrDAO attrDAO;

    @Autowired
    protected DerAttrDAO derAttrDAO;

    @Autowired
    protected VirAttrDAO virAttrDAO;

    @Autowired
    protected AttrValueDAO attributeValueDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected ResourceDAO resourceDAO;

    @Autowired
    protected MembershipDAO membershipDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    private JexlUtil jexlUtil;

    private <T extends AbstractSchema> T getSchema(final String schemaName, final Class<T> reference) {

        T schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = schemaDAO.find(schemaName, reference);

            // safely ignore invalid schemas from AttributeTO
            // see http://code.google.com/p/syncope/issues/detail?id=17
            if (schema == null) {
                LOG.debug("Ignoring invalid schema {}", schemaName);
            } else if (schema.isReadonly()) {
                schema = null;

                LOG.debug("Ignoring virtual or readonly schema {}", schemaName);
            }
        }

        return schema;
    }

    private <T extends AbstractDerSchema> T getDerivedSchema(final String derSchemaName, final Class<T> reference) {

        T derivedSchema = null;
        if (StringUtils.isNotBlank(derSchemaName)) {
            derivedSchema = derSchemaDAO.find(derSchemaName, reference);
            if (derivedSchema == null) {
                LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
            }
        }

        return derivedSchema;
    }

    private <T extends AbstractVirSchema> T getVirtualSchema(final String virSchemaName, final Class<T> reference) {

        T virtualSchema = null;
        if (StringUtils.isNotBlank(virSchemaName)) {
            virtualSchema = virSchemaDAO.find(virSchemaName, reference);

            if (virtualSchema == null) {
                LOG.debug("Ignoring invalid virtual schema {}", virSchemaName);
            }
        }

        return virtualSchema;
    }

    private ExternalResource getResource(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            LOG.debug("Ignoring invalid resource {} ", resourceName);
        }

        return resource;
    }

    protected void fillAttribute(final List<String> values, final AttributableUtil attributableUtil,
            final AbstractSchema schema, final AbstractAttr attribute, final SyncopeClientException invalidValues) {

        // if the schema is multivalue, all values are considered for
        // addition, otherwise only the fist one - if provided - is
        // considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                ? Collections.EMPTY_LIST
                : Collections.singletonList(values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getName());
            } else {
                try {
                    attribute.addValue(value, attributableUtil);
                } catch (ValidationException e) {
                    LOG.error("Invalid value for attribute " + schema.getName() + ": " + value, e);

                    invalidValues.addElement(schema.getName() + ": " + value 
                            + " - " + e.getMessage());
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(final String mandatoryCondition,
            final AbstractAttributable attributable) {

        JexlContext jexlContext = new MapContext();
        jexlUtil.addAttrsToContext(attributable.getAttributes(), jexlContext);
        jexlUtil.addDerAttrsToContext(attributable.getDerivedAttributes(), attributable.getAttributes(), jexlContext);
        jexlUtil.addVirAttrsToContext(attributable.getVirtualAttributes(), jexlContext);

        return Boolean.parseBoolean(jexlUtil.evaluate(mandatoryCondition, jexlContext));
    }

    private boolean evaluateMandatoryCondition(final ExternalResource resource, final AbstractAttributable attributable,
            final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        final Set<SchemaMapping> mappings = resource.getMappings(intAttrName, intMappingType);
        for (Iterator<SchemaMapping> itor = mappings.iterator(); itor.hasNext() && !result;) {
            final SchemaMapping mapping = itor.next();
            result |= evaluateMandatoryCondition(mapping.getMandatoryCondition(), attributable);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(final AbstractAttributable attributable, final String intAttrName,
            final IntMappingType intMappingType) {

        boolean result = false;

        for (Iterator<ExternalResource> itor = attributable.getResources().iterator(); itor.hasNext() && !result;) {
            final ExternalResource resource = itor.next();
            if (resource.isEnforceMandatoryCondition()) {
                result |= evaluateMandatoryCondition(resource, attributable, intAttrName, intMappingType);
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(final AttributableUtil attributableUtil,
            final AbstractAttributable attributable) {

        SyncopeClientException reqValMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        LOG.debug("Check mandatory constraint among resources {}", attributable.getResources());

        // Check if there is some mandatory schema defined for which no value has been provided
        for (AbstractSchema schema : schemaDAO.findAll(attributableUtil.schemaClass())) {
            if (attributable.getAttribute(schema.getName()) == null
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(schema.getMandatoryCondition(), attributable)
                    || evaluateMandatoryCondition(attributable, schema.getName(), attributableUtil.intMappingType()))) {

                LOG.error("Mandatory schema " + schema.getName() + " not provided with values");

                reqValMissing.addElement(schema.getName());
            }
        }
        for (AbstractDerSchema derSchema : derSchemaDAO.findAll(attributableUtil.derSchemaClass())) {
            if (attributable.getDerivedAttribute(derSchema.getName()) == null
                    && evaluateMandatoryCondition(
                    attributable, derSchema.getName(), attributableUtil.derIntMappingType())) {

                LOG.error("Mandatory derived schema " + derSchema.getName() + " does not evaluate to any value");

                reqValMissing.addElement(derSchema.getName());
            }
        }
        for (AbstractVirSchema virSchema : virSchemaDAO.findAll(attributableUtil.virSchemaClass())) {
            if (attributable.getAttribute(virSchema.getName()) == null
                    && evaluateMandatoryCondition(
                    attributable, virSchema.getName(), attributableUtil.virIntMappingType())) {

                LOG.error("Mandatory virtual schema " + virSchema.getName() + " not provided with values");

                reqValMissing.addElement(virSchema.getName());
            }
        }

        return reqValMissing;
    }

    public PropagationByResource fillVirtual(final AbstractAttributable attributable,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final AttributableUtil attributableUtil) {

        PropagationByResource propByRes = new PropagationByResource();

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            AbstractVirSchema virtualSchema = getVirtualSchema(vAttrToBeRemoved, attributableUtil.virSchemaClass());

            if (virtualSchema != null) {
                AbstractVirAttr virAttr = attributable.getVirtualAttribute(virtualSchema.getName());

                if (virAttr == null) {
                    LOG.debug("No virtual attribute found for schema {}", virtualSchema.getName());
                } else {
                    attributable.removeVirtualAttribute(virAttr);
                    virAttrDAO.delete(virAttr);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (virtualSchema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.virIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());

                        // TODO: using virtual attribute as AccountId must be avoided
                        if (mapping.isAccountid() && virAttr != null && !virAttr.getValues().isEmpty()) {
                            propByRes.addOldAccountId(mapping.getResource().getName(), virAttr.getValues().get(0));
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attributes to be removed:\n{}", propByRes);

        // 2. virtual attributes to be updated
        for (AttributeMod vAttrToBeUpdated : vAttrsToBeUpdated) {
            AbstractVirSchema virtualSchema = getVirtualSchema(vAttrToBeUpdated.getSchema(),
                    attributableUtil.virSchemaClass());

            if (virtualSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (virtualSchema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.virIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());
                    }
                }

                AbstractVirAttr virtualAttribute = attributable.getVirtualAttribute(virtualSchema.getName());

                if (virtualAttribute == null) {
                    virtualAttribute = attributableUtil.newVirAttr();
                    virtualAttribute.setVirtualSchema(virtualSchema);
                    attributable.addVirtualAttribute(virtualAttribute);
                }

                final List<String> values = new ArrayList<String>(virtualAttribute.getValues());
                values.removeAll(vAttrToBeUpdated.getValuesToBeRemoved());
                values.addAll(vAttrToBeUpdated.getValuesToBeAdded());

                virtualAttribute.setValues(values);

                // Owner cannot be specified before otherwise a virtual attribute remove will be invalidated.
                virtualAttribute.setOwner(attributable);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    protected PropagationByResource fill(final AbstractAttributable attributable,
            final AbstractAttributableMod attributableMod, final AttributableUtil attributableUtil,
            final SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientException invalidValues = new SyncopeClientException(SyncopeClientExceptionType.InvalidValues);

        // 1. resources to be removed
        ExternalResource resource;
        for (String resourceToBeRemoved : attributableMod.getResourcesToBeRemoved()) {

            resource = getResource(resourceToBeRemoved);

            if (resource != null) {
                propByRes.add(PropagationOperation.DELETE, resource.getName());
                attributable.removeResource(resource);
            }
        }

        LOG.debug("Resources to be removed:\n{}", propByRes);

        // 2. resources to be added
        for (String resourceToBeAdded : attributableMod.getResourcesToBeAdded()) {

            resource = getResource(resourceToBeAdded);

            if (resource != null) {
                propByRes.add(PropagationOperation.CREATE, resource.getName());
                attributable.addResource(resource);
            }
        }

        LOG.debug("Resources to be added:\n{}", propByRes);

        AbstractSchema schema;
        AbstractAttr attribute;
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;

        // 3. attributes to be removed
        for (String attributeToBeRemoved : attributableMod.getAttributesToBeRemoved()) {

            schema = getSchema(attributeToBeRemoved, attributableUtil.schemaClass());

            if (schema != null) {
                attribute = attributable.getAttribute(schema.getName());

                if (attribute == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttributeMod mod : attributableMod.getAttributesToBeUpdated()) {
                        if (schema.getName().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().get(0);
                        }
                    }

                    if (!schema.isUniqueConstraint() || (!attribute.getUniqueValue().getStringValue().equals(newValue))) {

                        attributable.removeAttribute(attribute);
                        attrDAO.delete(attribute.getId(), attributableUtil.attrClass());
                    }
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (schema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.intMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());

                        if (mapping.isAccountid() && attribute != null && !attribute.getValuesAsStrings().isEmpty()) {

                            propByRes.addOldAccountId(mapping.getResource().getName(), attribute.getValuesAsStrings().
                                    iterator().next());
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        Set<Long> valuesToBeRemoved;
        List<String> valuesToBeAdded;
        for (AttributeMod attributeMod : attributableMod.getAttributesToBeUpdated()) {

            schema = getSchema(attributeMod.getSchema(), attributableUtil.schemaClass());

            if (schema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (schema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.intMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());
                    }
                }

                attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    attribute = attributableUtil.newAttr();
                    attribute.setSchema(schema);
                    attribute.setOwner(attributable);

                    attributable.addAttribute(attribute);
                }

                // 1.1 remove values
                valuesToBeRemoved = new HashSet<Long>();
                for (String valueToBeRemoved : attributeMod.getValuesToBeRemoved()) {

                    if (attribute.getSchema().isUniqueConstraint()) {
                        if (attribute.getUniqueValue() != null
                                && valueToBeRemoved.equals(attribute.getUniqueValue().getValueAsString())) {

                            valuesToBeRemoved.add(attribute.getUniqueValue().getId());
                        }
                    } else {
                        for (AbstractAttrValue mav : attribute.getValues()) {
                            if (valueToBeRemoved.equals(mav.getValueAsString())) {
                                valuesToBeRemoved.add(mav.getId());
                            }
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    attributeValueDAO.delete(attributeValueId, attributableUtil.attrValueClass());
                }

                // 1.2 add values
                valuesToBeAdded = attributeMod.getValuesToBeAdded();
                if (valuesToBeAdded != null
                        && !valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint() || attribute.getUniqueValue() == null || !valuesToBeAdded.
                        iterator().next().equals(attribute.getUniqueValue().getValueAsString()))) {

                    fillAttribute(attributeMod.getValuesToBeAdded(), attributableUtil, schema, attribute, invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attribute.getValuesAsStrings().isEmpty()) {
                    attrDAO.delete(attribute);
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        LOG.debug("Attributes to be updated:\n{}", propByRes);

        // 5. derived attributes to be removed
        for (String derivedAttributeToBeRemoved : attributableMod.getDerivedAttributesToBeRemoved()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeRemoved, attributableUtil.derSchemaClass());

            if (derivedSchema != null) {
                derivedAttribute = attributable.getDerivedAttribute(derivedSchema.getName());

                if (derivedAttribute == null) {
                    LOG.debug("No derived attribute found for schema {}", derivedSchema.getName());
                } else {
                    derAttrDAO.delete(derivedAttribute);
                }

                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (derivedSchema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.derIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());

                        if (mapping.isAccountid() && derivedAttribute != null
                                && !derivedAttribute.getValue(attributable.getAttributes()).isEmpty()) {

                            propByRes.addOldAccountId(mapping.getResource().getName(),
                                    derivedAttribute.getValue(attributable.getAttributes()));
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}", propByRes);

        // 6. derived attributes to be added
        for (String derivedAttributeToBeAdded : attributableMod.getDerivedAttributesToBeAdded()) {

            derivedSchema = getDerivedSchema(derivedAttributeToBeAdded, attributableUtil.derSchemaClass());

            if (derivedSchema != null) {
                for (SchemaMapping mapping : resourceDAO.findAllMappings()) {
                    if (derivedSchema.getName().equals(mapping.getIntAttrName())
                            && mapping.getIntMappingType() == attributableUtil.derIntMappingType()
                            && mapping.getResource() != null
                            && attributable.getResources().contains(mapping.getResource())) {

                        propByRes.add(PropagationOperation.UPDATE, mapping.getResource().getName());
                    }
                }

                derivedAttribute = attributableUtil.newDerAttr();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        LOG.debug("Derived attributes to be added:\n{}", propByRes);

        // 7. virtual attributes: for users this is delegated to PropagationManager
        if (AttributableType.USER != attributableUtil.getType()) {
            fillVirtual(attributable, attributableMod.getVirtualAttributesToBeRemoved(), attributableMod.
                    getVirtualAttributesToBeUpdated(), attributableUtil);
        }

        // Finally, check if mandatory values are missing
        SyncopeClientException requiredValuesMissing = checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return propByRes;
    }

    /**
     * Add virtual attributes and specify values to be propagated.
     *
     * @param attributable attributable.
     * @param vAttrs virtual attributes to be added.
     * @param attributableUtil attributable util.
     */
    public void fillVirtual(final AbstractAttributable attributable, final List<AttributeTO> vAttrs,
            final AttributableUtil attributableUtil) {

        for (AttributeTO attributeTO : vAttrs) {
            AbstractVirAttr virtualAttribute = attributable.getVirtualAttribute(attributeTO.getSchema());

            if (virtualAttribute == null) {
                AbstractVirSchema virtualSchema = getVirtualSchema(attributeTO.getSchema(),
                        attributableUtil.virSchemaClass());

                if (virtualSchema != null) {
                    virtualAttribute = attributableUtil.newVirAttr();
                    virtualAttribute.setVirtualSchema(virtualSchema);
                    virtualAttribute.setOwner(attributable);
                    attributable.addVirtualAttribute(virtualAttribute);
                    virtualAttribute.setValues(attributeTO.getValues());
                }

            } else {
                virtualAttribute.setValues(attributeTO.getValues());
            }

        }
    }

    protected void fill(final AbstractAttributable attributable, final AbstractAttributableTO attributableTO,
            final AttributableUtil attributableUtil, final SyncopeClientCompositeErrorException compositeErrorException)
            throws SyncopeClientCompositeErrorException {

        // 1. attributes
        SyncopeClientException invalidValues = new SyncopeClientException(SyncopeClientExceptionType.InvalidValues);

        AbstractSchema schema;
        AbstractAttr attribute;

        // Only consider attributeTO with values
        for (AttributeTO attributeTO : attributableTO.getAttributes()) {
            if (attributeTO.getValues() != null && !attributeTO.getValues().isEmpty()) {

                schema = getSchema(attributeTO.getSchema(), attributableUtil.schemaClass());

                if (schema != null) {
                    attribute = attributable.getAttribute(schema.getName());
                    if (attribute == null) {
                        attribute = attributableUtil.newAttr();
                        attribute.setSchema(schema);
                    }

                    fillAttribute(attributeTO.getValues(), attributableUtil, schema, attribute, invalidValues);

                    if (!attribute.getValuesAsStrings().isEmpty()) {
                        attributable.addAttribute(attribute);
                        attribute.setOwner(attributable);
                    }
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }

        // 2. derived attributes
        AbstractDerSchema derivedSchema;
        AbstractDerAttr derivedAttribute;
        for (AttributeTO attributeTO : attributableTO.getDerivedAttributes()) {

            derivedSchema = getDerivedSchema(attributeTO.getSchema(), attributableUtil.derSchemaClass());

            if (derivedSchema != null) {
                derivedAttribute = attributableUtil.newDerAttr();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(attributable);
                attributable.addDerivedAttribute(derivedAttribute);
            }
        }

        // 3. user virtual attributes will be valued by the propagation manager only (if needed).
        if (AttributableType.USER == attributableUtil.getType()) {
            for (AttributeTO vattrTO : attributableTO.getVirtualAttributes()) {
                AbstractVirSchema uVirSchema = getVirtualSchema(vattrTO.getSchema(),
                        attributableUtil.virSchemaClass());

                if (uVirSchema != null) {
                    AbstractVirAttr vattr = attributableUtil.newVirAttr();
                    vattr.setVirtualSchema(uVirSchema);
                    vattr.setOwner(attributable);
                    attributable.addVirtualAttribute(vattr);
                }
            }
        }

        fillVirtual(attributable, attributableTO.getVirtualAttributes(), attributableUtil);

        // 4. resources
        ExternalResource resource;
        for (String resourceName : attributableTO.getResources()) {
            resource = getResource(resourceName);

            if (resource != null) {
                attributable.addResource(resource);
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }
    }

    protected void fillTO(final AbstractAttributableTO abstractAttributableTO,
            final Collection<? extends AbstractAttr> attributes,
            final Collection<? extends AbstractDerAttr> derivedAttributes,
            final Collection<? extends AbstractVirAttr> virtualAttributes, final Collection<ExternalResource> resources) {

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
            attributeTO.setSchema(derivedAttribute.getDerivedSchema().getName());
            attributeTO.addValue(derivedAttribute.getValue(attributes));
            attributeTO.setReadonly(true);

            abstractAttributableTO.addDerivedAttribute(attributeTO);
        }

        for (AbstractVirAttr virtualAttribute : virtualAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(virtualAttribute.getVirtualSchema().getName());
            attributeTO.setValues(virtualAttribute.getValues());
            attributeTO.setReadonly(false);

            abstractAttributableTO.addVirtualAttribute(attributeTO);
        }

        for (ExternalResource resource : resources) {
            abstractAttributableTO.addResource(resource.getName());
        }
    }
}
