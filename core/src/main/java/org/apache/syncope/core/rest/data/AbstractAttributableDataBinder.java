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
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RDerAttr;
import org.apache.syncope.core.persistence.beans.role.RDerAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.RVirAttr;
import org.apache.syncope.core.persistence.beans.role.RVirAttrTemplate;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.UDerAttr;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.beans.user.UVirAttr;
import org.apache.syncope.core.persistence.beans.user.UVirSchema;
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
import org.apache.syncope.core.persistence.validation.attrvalue.InvalidAttrValueException;
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

    @SuppressWarnings("unchecked")
    protected <T extends AbstractSchema> T getSchema(final String schemaName, final Class<T> reference) {
        T result = null;

        if (AbstractNormalSchema.class.isAssignableFrom(reference)) {
            result = (T) getNormalSchema(schemaName, (Class<? extends AbstractNormalSchema>) reference);
        } else if (AbstractDerSchema.class.isAssignableFrom(reference)) {
            result = (T) getDerSchema(schemaName, (Class<? extends AbstractDerSchema>) reference);
        } else if (AbstractVirSchema.class.isAssignableFrom(reference)) {
            result = (T) getVirSchema(schemaName, (Class<? extends AbstractVirSchema>) reference);
        }

        return result;
    }

    private <T extends AbstractNormalSchema> T getNormalSchema(final String schemaName, final Class<T> reference) {
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

    private <T extends AbstractDerSchema> T getDerSchema(final String derSchemaName, final Class<T> reference) {
        T derivedSchema = null;
        if (StringUtils.isNotBlank(derSchemaName)) {
            derivedSchema = derSchemaDAO.find(derSchemaName, reference);
            if (derivedSchema == null) {
                LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
            }
        }

        return derivedSchema;
    }

    private <T extends AbstractVirSchema> T getVirSchema(final String virSchemaName, final Class<T> reference) {
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
            final AbstractNormalSchema schema, final AbstractAttr attr, final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
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
                    attr.addValue(value, attributableUtil);
                } catch (InvalidAttrValueException e) {
                    LOG.error("Invalid value for attribute " + schema.getName() + ": " + value, e);

                    invalidValues.getElements().add(schema.getName() + ": " + value + " - " + e.getMessage());
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(final String mandatoryCondition,
            final AbstractAttributable attributable) {

        JexlContext jexlContext = new MapContext();
        jexlUtil.addAttrsToContext(attributable.getAttrs(), jexlContext);
        jexlUtil.addDerAttrsToContext(attributable.getDerAttrs(), attributable.getAttrs(), jexlContext);
        jexlUtil.addVirAttrsToContext(attributable.getVirAttrs(), jexlContext);

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

        SyncopeClientException reqValMissing = SyncopeClientException.build(
                ClientExceptionType.RequiredValuesMissing);

        LOG.debug("Check mandatory constraint among resources {}", attributable.getResources());

        // Check if there is some mandatory schema defined for which no value has been provided
        List<? extends AbstractNormalSchema> normalSchemas;
        switch (attrUtil.getType()) {
            case ROLE:
                normalSchemas = ((SyncopeRole) attributable).getAttrTemplateSchemas(RAttrTemplate.class);
                break;

            case MEMBERSHIP:
                normalSchemas = ((Membership) attributable).getSyncopeRole().
                        getAttrTemplateSchemas(MAttrTemplate.class);
                break;

            case USER:
            default:
                normalSchemas = schemaDAO.findAll(attrUtil.schemaClass());
        }
        for (AbstractNormalSchema schema : normalSchemas) {
            if (attributable.getAttr(schema.getName()) == null
                    && !schema.isReadonly()
                    && (evaluateMandatoryCondition(schema.getMandatoryCondition(), attributable)
                    || evaluateMandatoryCondition(attrUtil, attributable, schema.getName(),
                    attrUtil.intMappingType()))) {

                LOG.error("Mandatory schema " + schema.getName() + " not provided with values");

                reqValMissing.getElements().add(schema.getName());
            }
        }

        List<? extends AbstractDerSchema> derSchemas;
        switch (attrUtil.getType()) {
            case ROLE:
                derSchemas = ((SyncopeRole) attributable).getAttrTemplateSchemas(RDerAttrTemplate.class);
                break;

            case MEMBERSHIP:
                derSchemas = ((Membership) attributable).getSyncopeRole().
                        getAttrTemplateSchemas(MDerAttrTemplate.class);
                break;

            case USER:
            default:
                derSchemas = derSchemaDAO.findAll(attrUtil.derSchemaClass());
        }
        for (AbstractDerSchema derSchema : derSchemas) {
            if (attributable.getDerAttr(derSchema.getName()) == null
                    && evaluateMandatoryCondition(attrUtil, attributable, derSchema.getName(),
                    attrUtil.derIntMappingType())) {

                LOG.error("Mandatory derived schema " + derSchema.getName() + " does not evaluate to any value");

                reqValMissing.getElements().add(derSchema.getName());
            }
        }

        List<? extends AbstractVirSchema> virSchemas;
        switch (attrUtil.getType()) {
            case ROLE:
                virSchemas = ((SyncopeRole) attributable).getAttrTemplateSchemas(RVirAttrTemplate.class);
                break;

            case MEMBERSHIP:
                virSchemas = ((Membership) attributable).getSyncopeRole().
                        getAttrTemplateSchemas(MVirAttrTemplate.class);
                break;

            case USER:
            default:
                virSchemas = virSchemaDAO.findAll(attrUtil.virSchemaClass());
        }
        for (AbstractVirSchema virSchema : virSchemas) {
            if (attributable.getVirAttr(virSchema.getName()) == null
                    && !virSchema.isReadonly()
                    && evaluateMandatoryCondition(attrUtil, attributable, virSchema.getName(),
                    attrUtil.virIntMappingType())) {

                LOG.error("Mandatory virtual schema " + virSchema.getName() + " not provided with values");

                reqValMissing.getElements().add(virSchema.getName());
            }
        }

        return reqValMissing;
    }

    private void setAttrSchema(final AbstractAttributable attributable,
            final AbstractAttr attr, final AbstractNormalSchema schema) {

        if (attr instanceof UAttr) {
            ((UAttr) attr).setSchema((USchema) schema);
        } else if (attr instanceof RAttr) {
            RAttrTemplate template = ((SyncopeRole) attributable).
                    getAttrTemplate(RAttrTemplate.class, schema.getName());
            if (template != null) {
                ((RAttr) attr).setTemplate(template);
            }
        } else if (attr instanceof MAttr) {
            MAttrTemplate template = ((Membership) attributable).getSyncopeRole().
                    getAttrTemplate(MAttrTemplate.class, schema.getName());
            if (template != null) {
                ((MAttr) attr).setTemplate(template);
            }
        }
    }

    private void setDerAttrSchema(final AbstractAttributable attributable,
            final AbstractDerAttr derAttr, final AbstractDerSchema derSchema) {

        if (derAttr instanceof UDerAttr) {
            ((UDerAttr) derAttr).setSchema((UDerSchema) derSchema);
        } else if (derAttr instanceof RDerAttr) {
            RDerAttrTemplate template = ((SyncopeRole) attributable).
                    getAttrTemplate(RDerAttrTemplate.class, derSchema.getName());
            if (template != null) {
                ((RDerAttr) derAttr).setTemplate(template);
            }
        } else if (derAttr instanceof MDerAttr) {
            MDerAttrTemplate template = ((Membership) attributable).getSyncopeRole().
                    getAttrTemplate(MDerAttrTemplate.class, derSchema.getName());
            if (template != null) {
                ((MDerAttr) derAttr).setTemplate(template);
            }
        }
    }

    private void setVirAttrSchema(final AbstractAttributable attributable,
            final AbstractVirAttr virAttr, final AbstractVirSchema virSchema) {

        if (virAttr instanceof UVirAttr) {
            ((UVirAttr) virAttr).setSchema((UVirSchema) virSchema);
        } else if (virAttr instanceof RVirAttr) {
            RVirAttrTemplate template = ((SyncopeRole) attributable).
                    getAttrTemplate(RVirAttrTemplate.class, virSchema.getName());
            if (template != null) {
                ((RVirAttr) virAttr).setTemplate(template);
            }
        } else if (virAttr instanceof MVirAttr) {
            MVirAttrTemplate template =
                    ((Membership) attributable).getSyncopeRole().
                    getAttrTemplate(MVirAttrTemplate.class, virSchema.getName());
            if (template != null) {
                ((MVirAttr) virAttr).setTemplate(template);
            }
        }
    }

    public PropagationByResource fillVirtual(final AbstractAttributable attributable,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final AttributableUtil attrUtil) {

        PropagationByResource propByRes = new PropagationByResource();

        // 1. virtual attributes to be removed
        for (String vAttrToBeRemoved : vAttrsToBeRemoved) {
            AbstractVirSchema virSchema = getVirSchema(vAttrToBeRemoved, attrUtil.virSchemaClass());
            if (virSchema != null) {
                AbstractVirAttr virAttr = attributable.getVirAttr(virSchema.getName());
                if (virAttr == null) {
                    LOG.debug("No virtual attribute found for schema {}", virSchema.getName());
                } else {
                    attributable.removeVirAttr(virAttr);
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
            AbstractVirSchema virSchema = getVirSchema(vAttrToBeUpdated.getSchema(), attrUtil.virSchemaClass());
            AbstractVirAttr virAttr = null;
            if (virSchema != null) {
                virAttr = attributable.getVirAttr(virSchema.getName());
                if (virAttr == null) {
                    virAttr = attrUtil.newVirAttr();
                    setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", vAttrToBeUpdated);
                    } else {
                        attributable.addVirAttr(virAttr);
                    }
                }
            }

            if (virSchema != null && virAttr != null && virAttr.getSchema() != null) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (virSchema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.virIntMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());
                        }
                    }
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

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // 1. resources to be removed
        for (String resourceToBeRemoved : attributableMod.getResourcesToRemove()) {
            ExternalResource resource = getResource(resourceToBeRemoved);
            if (resource != null) {
                propByRes.add(ResourceOperation.DELETE, resource.getName());
                attributable.removeResource(resource);
            }
        }

        LOG.debug("Resources to be removed:\n{}", propByRes);

        // 2. resources to be added
        for (String resourceToBeAdded : attributableMod.getResourcesToAdd()) {
            ExternalResource resource = getResource(resourceToBeAdded);
            if (resource != null) {
                propByRes.add(ResourceOperation.CREATE, resource.getName());
                attributable.addResource(resource);
            }
        }

        LOG.debug("Resources to be added:\n{}", propByRes);

        // 3. attributes to be removed
        for (String attributeToBeRemoved : attributableMod.getAttrsToRemove()) {
            AbstractNormalSchema schema = getNormalSchema(attributeToBeRemoved, attrUtil.schemaClass());
            if (schema != null) {
                AbstractAttr attr = attributable.getAttr(schema.getName());
                if (attr == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttributeMod mod : attributableMod.getAttrsToUpdate()) {
                        if (schema.getName().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().get(0);
                        }
                    }

                    if (!schema.isUniqueConstraint()
                            || (!attr.getUniqueValue().getStringValue().equals(newValue))) {

                        attributable.removeAttr(attr);
                        attrDAO.delete(attr.getId(), attrUtil.attrClass());
                    }
                }

                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (schema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.intMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());

                            if (mapItem.isAccountid() && attr != null
                                    && !attr.getValuesAsStrings().isEmpty()) {

                                propByRes.addOldAccountId(resource.getName(),
                                        attr.getValuesAsStrings().iterator().next());
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        for (AttributeMod attributeMod : attributableMod.getAttrsToUpdate()) {
            AbstractNormalSchema schema = getNormalSchema(attributeMod.getSchema(), attrUtil.schemaClass());
            AbstractAttr attr = null;
            if (schema != null) {
                attr = attributable.getAttr(schema.getName());
                if (attr == null) {
                    attr = attrUtil.newAttr();
                    setAttrSchema(attributable, attr, schema);
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeMod);
                    } else {
                        attr.setOwner(attributable);
                        attributable.addAttr(attr);
                    }
                }
            }

            if (schema != null && attr != null && attr.getSchema() != null) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    for (AbstractMappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (schema.getName().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == attrUtil.intMappingType()
                                && attributable.getResources().contains(resource)) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getName());
                        }
                    }
                }


                // 1.1 remove values
                Set<Long> valuesToBeRemoved = new HashSet<Long>();
                for (String valueToBeRemoved : attributeMod.getValuesToBeRemoved()) {
                    if (attr.getSchema().isUniqueConstraint()) {
                        if (attr.getUniqueValue() != null
                                && valueToBeRemoved.equals(attr.getUniqueValue().getValueAsString())) {

                            valuesToBeRemoved.add(attr.getUniqueValue().getId());
                        }
                    } else {
                        for (AbstractAttrValue mav : attr.getValues()) {
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
                        && (!schema.isUniqueConstraint() || attr.getUniqueValue() == null
                        || !valuesToBeAdded.iterator().next().equals(attr.getUniqueValue().getValueAsString()))) {

                    fillAttribute(attributeMod.getValuesToBeAdded(), attrUtil, schema, attr, invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attr.getValuesAsStrings().isEmpty()) {
                    attrDAO.delete(attr);
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        LOG.debug("Attributes to be updated:\n{}", propByRes);

        // 5. derived attributes to be removed
        for (String derAttrToBeRemoved : attributableMod.getDerAttrsToRemove()) {
            AbstractDerSchema derSchema = getDerSchema(derAttrToBeRemoved, attrUtil.derSchemaClass());
            if (derSchema != null) {
                AbstractDerAttr derAttr = attributable.getDerAttr(derSchema.getName());
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
                                    && !derAttr.getValue(attributable.getAttrs()).isEmpty()) {

                                propByRes.addOldAccountId(resource.getName(),
                                        derAttr.getValue(attributable.getAttrs()));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}", propByRes);

        // 6. derived attributes to be added
        for (String derAttrToBeAdded : attributableMod.getDerAttrsToAdd()) {
            AbstractDerSchema derSchema = getDerSchema(derAttrToBeAdded, attrUtil.derSchemaClass());
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
                setDerAttrSchema(attributable, derAttr, derSchema);
                if (derAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", derAttrToBeAdded);
                } else {
                    derAttr.setOwner(attributable);
                    attributable.addDerAttr(derAttr);
                }
            }
        }

        LOG.debug("Derived attributes to be added:\n{}", propByRes);

        // 7. virtual attributes: for users and roles this is delegated to PropagationManager
        if (AttributableType.USER != attrUtil.getType() && AttributableType.ROLE != attrUtil.getType()) {
            fillVirtual(attributable, attributableMod.getVirAttrsToRemove(),
                    attributableMod.getVirAttrsToUpdate(), attrUtil);
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
    public void fillVirtual(final AbstractAttributable attributable, final Collection<AttributeTO> vAttrs,
            final AttributableUtil attrUtil) {

        for (AttributeTO attributeTO : vAttrs) {
            AbstractVirAttr virAttr = attributable.getVirAttr(attributeTO.getSchema());
            if (virAttr == null) {
                AbstractVirSchema virSchema = getVirSchema(attributeTO.getSchema(), attrUtil.virSchemaClass());
                if (virSchema != null) {
                    virAttr = attrUtil.newVirAttr();
                    setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                    } else {
                        virAttr.setOwner(attributable);
                        attributable.addVirAttr(virAttr);
                        virAttr.setValues(attributeTO.getValues());
                    }
                }
            } else {
                virAttr.setValues(attributeTO.getValues());
            }
        }
    }

    protected void fill(final AbstractAttributable attributable, final AbstractAttributableTO attributableTO,
            final AttributableUtil attributableUtil, final SyncopeClientCompositeException scce) {

        // 1. attributes
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // Only consider attributeTO with values
        for (AttributeTO attributeTO : attributableTO.getAttrs()) {
            if (attributeTO.getValues() != null && !attributeTO.getValues().isEmpty()) {
                AbstractNormalSchema schema = getNormalSchema(attributeTO.getSchema(), attributableUtil.schemaClass());

                if (schema != null) {
                    AbstractAttr attr = attributable.getAttr(schema.getName());
                    if (attr == null) {
                        attr = attributableUtil.newAttr();
                        setAttrSchema(attributable, attr, schema);
                    }
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                    } else {
                        fillAttribute(attributeTO.getValues(), attributableUtil, schema, attr, invalidValues);

                        if (!attr.getValuesAsStrings().isEmpty()) {
                            attributable.addAttr(attr);
                            attr.setOwner(attributable);
                        }
                    }
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // 2. derived attributes
        for (AttributeTO attributeTO : attributableTO.getDerAttrs()) {
            AbstractDerSchema derSchema = getDerSchema(attributeTO.getSchema(), attributableUtil.derSchemaClass());

            if (derSchema != null) {
                AbstractDerAttr derAttr = attributableUtil.newDerAttr();
                setDerAttrSchema(attributable, derAttr, derSchema);
                if (derAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                } else {
                    derAttr.setOwner(attributable);
                    attributable.addDerAttr(derAttr);
                }
            }
        }

        // 3. user and role virtual attributes will be evaluated by the propagation manager only (if needed).
        if (AttributableType.USER == attributableUtil.getType()
                || AttributableType.ROLE == attributableUtil.getType()) {

            for (AttributeTO vattrTO : attributableTO.getVirAttrs()) {
                AbstractVirSchema virSchema = getVirSchema(vattrTO.getSchema(), attributableUtil.virSchemaClass());

                if (virSchema != null) {
                    AbstractVirAttr virAttr = attributableUtil.newVirAttr();
                    setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", vattrTO);
                    } else {
                        virAttr.setOwner(attributable);
                        attributable.addVirAttr(virAttr);
                    }
                }
            }
        }

        fillVirtual(attributable, attributableTO.getVirAttrs(), attributableUtil);

        // 4. resources
        for (String resourceName : attributableTO.getResources()) {
            ExternalResource resource = getResource(resourceName);

            if (resource != null) {
                attributable.addResource(resource);
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(attributableUtil, attributable);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void fillTO(final AbstractAttributableTO attributableTO,
            final Collection<? extends AbstractAttr> attrs,
            final Collection<? extends AbstractDerAttr> derAttrs,
            final Collection<? extends AbstractVirAttr> virAttrs,
            final Collection<ExternalResource> resources) {

        AttributeTO attributeTO;
        for (AbstractAttr attr : attrs) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attr.getSchema().getName());
            attributeTO.getValues().addAll(attr.getValuesAsStrings());
            attributeTO.setReadonly(attr.getSchema().isReadonly());

            attributableTO.getAttrs().add(attributeTO);
        }

        for (AbstractDerAttr derAttr : derAttrs) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(derAttr.getSchema().getName());
            attributeTO.getValues().add(derAttr.getValue(attrs));
            attributeTO.setReadonly(true);

            attributableTO.getDerAttrs().add(attributeTO);
        }

        for (AbstractVirAttr virAttr : virAttrs) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(virAttr.getSchema().getName());
            attributeTO.getValues().addAll(virAttr.getValues());
            attributeTO.setReadonly(virAttr.getSchema().isReadonly());

            attributableTO.getVirAttrs().add(attributeTO);
        }

        for (ExternalResource resource : resources) {
            attributableTO.getResources().add(resource.getName());
        }
    }
}
