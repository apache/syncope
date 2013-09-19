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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
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
import org.apache.syncope.core.util.MappingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

                LOG.debug("Ignoring readonly schema {}", schemaName);
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
            } else if (virtualSchema.isReadonly()) {
                virtualSchema = null;

                LOG.debug("Ignoring readonly virtual schema {}", virtualSchema);
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
                ? Collections.<String>emptyList()
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

    private boolean evaluateMandatoryCondition(final AttributableUtil attrUtil, final ExternalResource resource,
            final AbstractAttributable attributable, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        final List<AbstractMappingItem> mappings = MappingUtil.getMatchingMappingItems(
                attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION), intAttrName, intMappingType);
        for (Iterator<AbstractMappingItem> itor = mappings.iterator(); itor.hasNext() && !result;) {
            final AbstractMappingItem mapping = itor.next();
            result |= evaluateMandatoryCondition(mapping.getMandatoryCondition(), attributable);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(final AttributableUtil attrUtil,
            final AbstractAttributable attributable, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        for (Iterator<ExternalResource> itor = attributable.getResources().iterator(); itor.hasNext() && !result;) {
            final ExternalResource resource = itor.next();
            if (resource.isEnforceMandatoryCondition()) {
                result |= evaluateMandatoryCondition(attrUtil, resource, attributable, intAttrName, intMappingType);
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(final AttributableUtil attrUtil,
            final AbstractAttributable attributable) {

        SyncopeClientException reqValMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        LOG.debug("Check mandatory constraint among resources {}", attributable.getResources());

        // Check if there is some mandatory schema defined for which no value has been provided
        for (AbstractSchema schema : schemaDAO.findAll(attrUtil.schemaClass())) {
            if (attributable.getAttribute(schema.getName()) == null
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(schema.getMandatoryCondition(), attributable)
                    || evaluateMandatoryCondition(attrUtil, attributable, schema.getName(),
                    attrUtil.intMappingType()))) {

                LOG.error("Mandatory schema " + schema.getName() + " not provided with values");

                reqValMissing.addElement(schema.getName());
            }
        }
        for (AbstractDerSchema derSchema : derSchemaDAO.findAll(attrUtil.derSchemaClass())) {
            if (attributable.getDerivedAttribute(derSchema.getName()) == null
                    && evaluateMandatoryCondition(attrUtil, attributable, derSchema.getName(),
                    attrUtil.derIntMappingType())) {

                LOG.error("Mandatory derived schema " + derSchema.getName() + " does not evaluate to any value");

                reqValMissing.addElement(derSchema.getName());
            }
        }
        for (AbstractVirSchema virSchema : virSchemaDAO.findAll(attrUtil.virSchemaClass())) {
            if (attributable.getVirtualAttribute(virSchema.getName()) == null
                    && !virSchema.isReadonly()
                    && evaluateMandatoryCondition(attrUtil, attributable, virSchema.getName(),
                    attrUtil.virIntMappingType())) {

                LOG.error("Mandatory virtual schema " + virSchema.getName() + " not provided with values");

                reqValMissing.addElement(virSchema.getName());
            }
        }

        return reqValMissing;
    }

    public PropagationByResource fillVirtual(final AbstractAttributable attributable,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final AttributableUtil attrUtil) {

        PropagationByResource propByRes = new PropagationByResource();

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            AbstractVirSchema virSchema = getVirtualSchema(vAttrToBeRemoved, attrUtil.virSchemaClass());
            if (virSchema != null) {
                AbstractVirAttr virAttr = attributable.getVirtualAttribute(virSchema.getName());
                if (virAttr == null) {
                    LOG.debug("No virtual attribute found for schema {}", virSchema.getName());
                } else {
                    attributable.removeVirtualAttribute(virAttr);
                    virAttrDAO.delete(virAttr);
                }

                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (virSchema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.virIntMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());

                            // Using virtual attribute as AccountId must be avoided
                            if (mapItem.isAccountid() && virAttr != null && !virAttr.getValues().isEmpty()) {
                                propByRes.addOldAccountId(resource.getName(), virAttr.getValues().get(0));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Virtual attributes to be removed:\n{}", propByRes);

        // 2. virtual attributes to be updated
        for (AttributeMod vAttrToBeUpdated : vAttrsToBeUpdated) {
            AbstractVirSchema virSchema = getVirtualSchema(vAttrToBeUpdated.getSchema(), attrUtil.virSchemaClass());
            if (virSchema != null) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (virSchema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.virIntMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());
                        }
                    }
                }

                AbstractVirAttr virAttr = attributable.getVirtualAttribute(virSchema.getName());
                if (virAttr == null) {
                    virAttr = attrUtil.newVirAttr();
                    virAttr.setVirtualSchema(virSchema);
                    attributable.addVirtualAttribute(virAttr);
                }

                final List<String> values = new ArrayList<String>(virAttr.getValues());
                values.removeAll(vAttrToBeUpdated.getValuesToBeRemoved());
                values.addAll(vAttrToBeUpdated.getValuesToBeAdded());

                virAttr.setValues(values);

                // Owner cannot be specified before otherwise a virtual attribute remove will be invalidated.
                virAttr.setOwner(attributable);
            }
        }

        LOG.debug("Virtual attributes to be added:\n{}", propByRes);

        return propByRes;
    }

    protected PropagationByResource fill(final AbstractAttributable attributable,
            final AbstractAttributableMod attributableMod, final AttributableUtil attrUtil,
            final SyncopeClientCompositeException scce) {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientException invalidValues = new SyncopeClientException(SyncopeClientExceptionType.InvalidValues);

        // 1. resources to be removed
        for (String resourceToBeRemoved : attributableMod.getResourcesToBeRemoved()) {
            ExternalResource resource = getResource(resourceToBeRemoved);
            if (resource != null) {
                propByRes.add(ResourceOperation.DELETE, resource.getName());
                attributable.removeResource(resource);
            }
        }

        LOG.debug("Resources to be removed:\n{}", propByRes);

        // 2. resources to be added
        for (String resourceToBeAdded : attributableMod.getResourcesToBeAdded()) {
            ExternalResource resource = getResource(resourceToBeAdded);
            if (resource != null) {
                propByRes.add(ResourceOperation.CREATE, resource.getName());
                attributable.addResource(resource);
            }
        }

        LOG.debug("Resources to be added:\n{}", propByRes);

        // 3. attributes to be removed
        for (String attributeToBeRemoved : attributableMod.getAttributesToBeRemoved()) {
            AbstractSchema schema = getSchema(attributeToBeRemoved, attrUtil.schemaClass());
            if (schema != null) {
                AbstractAttr attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttributeMod mod : attributableMod.getAttributesToBeUpdated()) {
                        if (schema.getName().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().get(0);
                        }
                    }

                    if (!schema.isUniqueConstraint()
                            || (!attribute.getUniqueValue().getStringValue().equals(newValue))) {

                        attributable.removeAttribute(attribute);
                        attrDAO.delete(attribute.getId(), attrUtil.attrClass());
                    }
                }

                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (schema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.intMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());

                            if (mapItem.isAccountid() && attribute != null
                                    && !attribute.getValuesAsStrings().isEmpty()) {

                                propByRes.addOldAccountId(resource.getName(),
                                        attribute.getValuesAsStrings().iterator().next());
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        for (AttributeMod attributeMod : attributableMod.getAttributesToBeUpdated()) {
            AbstractSchema schema = getSchema(attributeMod.getSchema(), attrUtil.schemaClass());
            if (schema != null) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (schema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.intMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());
                        }
                    }
                }

                AbstractAttr attribute = attributable.getAttribute(schema.getName());
                if (attribute == null) {
                    attribute = attrUtil.newAttr();
                    attribute.setSchema(schema);
                    attribute.setOwner(attributable);

                    attributable.addAttribute(attribute);
                }

                // 1.1 remove values
                Set<Long> valuesToBeRemoved = new HashSet<Long>();
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
                    attributeValueDAO.delete(attributeValueId, attrUtil.attrValueClass());
                }

                // 1.2 add values
                List<String> valuesToBeAdded = attributeMod.getValuesToBeAdded();
                if (valuesToBeAdded != null && !valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint() || attribute.getUniqueValue() == null
                        || !valuesToBeAdded.iterator().next().equals(attribute.getUniqueValue().getValueAsString()))) {

                    fillAttribute(attributeMod.getValuesToBeAdded(), attrUtil, schema, attribute, invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attribute.getValuesAsStrings().isEmpty()) {
                    attrDAO.delete(attribute);
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        LOG.debug("Attributes to be updated:\n{}", propByRes);

        // 5. derived attributes to be removed
        for (String derAttrToBeRemoved : attributableMod.getDerivedAttributesToBeRemoved()) {
            AbstractDerSchema derSchema = getDerivedSchema(derAttrToBeRemoved, attrUtil.derSchemaClass());
            if (derSchema != null) {
                AbstractDerAttr derAttr = attributable.getDerivedAttribute(derSchema.getName());
                if (derAttr == null) {
                    LOG.debug("No derived attribute found for schema {}", derSchema.getName());
                } else {
                    derAttrDAO.delete(derAttr);
                }

                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (derSchema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.derIntMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());

                            if (mapItem.isAccountid() && derAttr != null
                                    && !derAttr.getValue(attributable.getAttributes()).isEmpty()) {

                                propByRes.addOldAccountId(resource.getName(),
                                        derAttr.getValue(attributable.getAttributes()));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}", propByRes);

        // 6. derived attributes to be added
        for (String derAttrToBeAdded : attributableMod.getDerivedAttributesToBeAdded()) {
            AbstractDerSchema derSchema = getDerivedSchema(derAttrToBeAdded, attrUtil.derSchemaClass());
            if (derSchema != null) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (derSchema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.derIntMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());
                        }
                    }
                }

                AbstractDerAttr derAttr = attrUtil.newDerAttr();
                derAttr.setDerivedSchema(derSchema);
                derAttr.setOwner(attributable);
                attributable.addDerivedAttribute(derAttr);
            }
        }

        LOG.debug("Derived attributes to be added:\n{}", propByRes);

        // 7. virtual attributes: for users and roles this is delegated to PropagationManager
        if (AttributableType.USER != attrUtil.getType() && AttributableType.ROLE != attrUtil.getType()) {
            fillVirtual(attributable, attributableMod.getVirtualAttributesToBeRemoved(),
                    attributableMod.getVirtualAttributesToBeUpdated(), attrUtil);
        }

        // Finally, check if mandatory values are missing
        SyncopeClientException requiredValuesMissing = checkMandatory(attrUtil, attributable);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return propByRes;
    }

    /**
     * Add virtual attributes and specify values to be propagated.
     *
     * @param attributable attributable.
     * @param vAttrs virtual attributes to be added.
     * @param attrUtil attributable util.
     */
    public void fillVirtual(final AbstractAttributable attributable, final List<AttributeTO> vAttrs,
            final AttributableUtil attrUtil) {

        for (AttributeTO attributeTO : vAttrs) {
            AbstractVirAttr virAttr = attributable.getVirtualAttribute(attributeTO.getSchema());
            if (virAttr == null) {
                AbstractVirSchema virSchema = getVirtualSchema(attributeTO.getSchema(), attrUtil.virSchemaClass());
                if (virSchema != null) {
                    virAttr = attrUtil.newVirAttr();
                    virAttr.setVirtualSchema(virSchema);
                    virAttr.setOwner(attributable);
                    attributable.addVirtualAttribute(virAttr);
                    virAttr.setValues(attributeTO.getValues());
                }
            } else {
                virAttr.setValues(attributeTO.getValues());
            }
        }
    }

    protected void fill(final AbstractAttributable attributable, final AbstractAttributableTO attributableTO,
            final AttributableUtil attributableUtil, final SyncopeClientCompositeException scce) {

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
            scce.addException(invalidValues);
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

        // 3. user and role virtual attributes will be evaluated by the propagation manager only (if needed).
        if (AttributableType.USER == attributableUtil.getType()
                || AttributableType.ROLE == attributableUtil.getType()) {

            for (AttributeTO vattrTO : attributableTO.getVirtualAttributes()) {
                AbstractVirSchema uVirSchema = getVirtualSchema(vattrTO.getSchema(), attributableUtil.virSchemaClass());

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
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void fillTO(final AbstractAttributableTO abstractAttributableTO,
            final Collection<? extends AbstractAttr> attributes,
            final Collection<? extends AbstractDerAttr> derivedAttributes,
            final Collection<? extends AbstractVirAttr> virtualAttributes,
            final Collection<ExternalResource> resources) {

        AttributeTO attributeTO;
        for (AbstractAttr attribute : attributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.getValues().addAll(attribute.getValuesAsStrings());
            attributeTO.setReadonly(attribute.getSchema().isReadonly());

            abstractAttributableTO.getAttributes().add(attributeTO);
        }

        for (AbstractDerAttr derivedAttribute : derivedAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(derivedAttribute.getDerivedSchema().getName());
            attributeTO.getValues().add(derivedAttribute.getValue(attributes));
            attributeTO.setReadonly(true);

            abstractAttributableTO.getDerivedAttributes().add(attributeTO);
        }

        for (AbstractVirAttr virtualAttribute : virtualAttributes) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(virtualAttribute.getVirtualSchema().getName());
            attributeTO.getValues().addAll(virtualAttribute.getValues());
            attributeTO.setReadonly(virtualAttribute.getVirtualSchema().isReadonly());

            abstractAttributableTO.getVirtualAttributes().add(attributeTO);
        }

        for (ExternalResource resource : resources) {
            abstractAttributableTO.getResources().add(resource.getName());
        }
    }
}
