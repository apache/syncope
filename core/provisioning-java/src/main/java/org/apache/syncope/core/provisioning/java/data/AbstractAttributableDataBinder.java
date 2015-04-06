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
package org.apache.syncope.core.provisioning.java.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.AbstractAttributableMod;
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.MembershipDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.java.VirAttrHandler;
import org.apache.syncope.core.misc.MappingUtil;
import org.apache.syncope.core.misc.jexl.JexlUtil;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractAttributableDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAttributableDataBinder.class);

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected PlainAttrDAO plainAttrDAO;

    @Autowired
    protected DerAttrDAO derAttrDAO;

    @Autowired
    protected VirAttrDAO virAttrDAO;

    @Autowired
    protected PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected MembershipDAO membershipDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AttributableUtilFactory attrUtilFactory;

    @Autowired
    protected VirAttrHandler virtAttrHander;

    @SuppressWarnings("unchecked")
    protected <T extends Schema> T getSchema(final String schemaName, final Class<T> reference) {
        T result = null;

        if (PlainSchema.class.isAssignableFrom(reference)) {
            result = (T) getPlainSchema(schemaName, (Class<? extends PlainSchema>) reference);
        } else if (DerSchema.class.isAssignableFrom(reference)) {
            result = (T) getDerSchema(schemaName, (Class<? extends DerSchema>) reference);
        } else if (VirSchema.class.isAssignableFrom(reference)) {
            result = (T) virtAttrHander.getVirSchema(schemaName, (Class<? extends VirSchema>) reference);
        }

        return result;
    }

    protected <T extends PlainSchema> T getPlainSchema(final String schemaName, final Class<T> reference) {
        T schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = plainSchemaDAO.find(schemaName, reference);

            // safely ignore invalid schemas from AttrTO
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

    private <T extends DerSchema> T getDerSchema(final String derSchemaName, final Class<T> reference) {
        T derivedSchema = null;
        if (StringUtils.isNotBlank(derSchemaName)) {
            derivedSchema = derSchemaDAO.find(derSchemaName, reference);
            if (derivedSchema == null) {
                LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
            }
        }

        return derivedSchema;
    }

    protected void fillAttribute(final List<String> values, final AttributableUtil attributableUtil,
            final PlainSchema schema, final PlainAttr attr, final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty()
                        ? Collections.<String>emptyList()
                        : Collections.singletonList(values.iterator().next()));

        for (String value : valuesProvided) {
            if (value == null || value.isEmpty()) {
                LOG.debug("Null value for {}, ignoring", schema.getKey());
            } else {
                try {
                    attr.addValue(value, attributableUtil);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute " + schema.getKey() + ": " + value, e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(final AttributableUtil attrUtil, final ExternalResource resource,
            final Attributable<?, ?, ?> attributable, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        final List<MappingItem> mappings = MappingUtil.getMatchingMappingItems(
                attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION), intAttrName, intMappingType);
        for (Iterator<MappingItem> itor = mappings.iterator(); itor.hasNext() && !result;) {
            final MappingItem mapping = itor.next();
            result |= JexlUtil.evaluateMandatoryCondition(mapping.getMandatoryCondition(), attributable);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(final AttributableUtil attrUtil,
            final Attributable<?, ?, ?> attributable, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        if (attributable instanceof Subject) {
            for (Iterator<? extends ExternalResource> itor = ((Subject<?, ?, ?>) attributable).getResources().iterator();
                    itor.hasNext() && !result;) {

                final ExternalResource resource = itor.next();
                if (resource.isEnforceMandatoryCondition()) {
                    result |= evaluateMandatoryCondition(attrUtil, resource, attributable, intAttrName, intMappingType);
                }
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(final AttributableUtil attrUtil,
            final Attributable<?, ?, ?> attributable) {

        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        // Check if there is some mandatory schema defined for which no value has been provided
        List<? extends PlainSchema> plainSchemas;
        switch (attrUtil.getType()) {
            case GROUP:
                plainSchemas = ((Group) attributable).getAttrTemplateSchemas(GPlainAttrTemplate.class);
                break;

            case MEMBERSHIP:
                plainSchemas = ((Membership) attributable).getGroup().getAttrTemplateSchemas(MPlainAttrTemplate.class);
                break;

            case USER:
            default:
                plainSchemas = plainSchemaDAO.findAll(attrUtil.plainSchemaClass());
        }
        for (PlainSchema schema : plainSchemas) {
            if (attributable.getPlainAttr(schema.getKey()) == null
                    && !schema.isReadonly()
                    && (JexlUtil.evaluateMandatoryCondition(schema.getMandatoryCondition(), attributable)
                    || evaluateMandatoryCondition(attrUtil, attributable, schema.getKey(),
                            attrUtil.plainIntMappingType()))) {

                LOG.error("Mandatory schema " + schema.getKey() + " not provided with values");

                reqValMissing.getElements().add(schema.getKey());
            }
        }

        List<? extends DerSchema> derSchemas;
        switch (attrUtil.getType()) {
            case GROUP:
                derSchemas = ((Group) attributable).getAttrTemplateSchemas(GDerAttrTemplate.class);
                break;

            case MEMBERSHIP:
                derSchemas = ((Membership) attributable).getGroup().getAttrTemplateSchemas(MDerAttrTemplate.class);
                break;

            case USER:
            default:
                derSchemas = derSchemaDAO.findAll(attrUtil.derSchemaClass());
        }
        for (DerSchema derSchema : derSchemas) {
            if (attributable.getDerAttr(derSchema.getKey()) == null
                    && evaluateMandatoryCondition(attrUtil, attributable, derSchema.getKey(),
                            attrUtil.derIntMappingType())) {

                LOG.error("Mandatory derived schema " + derSchema.getKey() + " does not evaluate to any value");

                reqValMissing.getElements().add(derSchema.getKey());
            }
        }

        List<? extends VirSchema> virSchemas;
        switch (attrUtil.getType()) {
            case GROUP:
                virSchemas = ((Group) attributable).getAttrTemplateSchemas(GVirAttrTemplate.class);
                break;

            case MEMBERSHIP:
                virSchemas = ((Membership) attributable).getGroup().getAttrTemplateSchemas(MVirAttrTemplate.class);
                break;

            case USER:
            default:
                virSchemas = virSchemaDAO.findAll(attrUtil.virSchemaClass());
        }
        for (VirSchema virSchema : virSchemas) {
            if (attributable.getVirAttr(virSchema.getKey()) == null
                    && !virSchema.isReadonly()
                    && evaluateMandatoryCondition(attrUtil, attributable, virSchema.getKey(),
                            attrUtil.virIntMappingType())) {

                LOG.error("Mandatory virtual schema " + virSchema.getKey() + " not provided with values");

                reqValMissing.getElements().add(virSchema.getKey());
            }
        }

        return reqValMissing;
    }

    private void setPlainAttrSchema(final Attributable<?, ?, ?> attributable,
            final PlainAttr attr, final PlainSchema schema) {

        if (attr instanceof UPlainAttr) {
            ((UPlainAttr) attr).setSchema((UPlainSchema) schema);
        } else if (attr instanceof GPlainAttr) {
            GPlainAttrTemplate template =
                    ((Group) attributable).getAttrTemplate(GPlainAttrTemplate.class, schema.getKey());
            if (template != null) {
                ((GPlainAttr) attr).setTemplate(template);
            }
        } else if (attr instanceof MPlainAttr) {
            MPlainAttrTemplate template = ((Membership) attributable).getGroup().
                    getAttrTemplate(MPlainAttrTemplate.class, schema.getKey());
            if (template != null) {
                ((MPlainAttr) attr).setTemplate(template);
            }
        }
    }

    private void setDerAttrSchema(final Attributable<?, ?, ?> attributable,
            final DerAttr derAttr, final DerSchema derSchema) {

        if (derAttr instanceof UDerAttr) {
            ((UDerAttr) derAttr).setSchema((UDerSchema) derSchema);
        } else if (derAttr instanceof GDerAttr) {
            GDerAttrTemplate template = ((Group) attributable).
                    getAttrTemplate(GDerAttrTemplate.class, derSchema.getKey());
            if (template != null) {
                ((GDerAttr) derAttr).setTemplate(template);
            }
        } else if (derAttr instanceof MDerAttr) {
            MDerAttrTemplate template = ((Membership) attributable).getGroup().
                    getAttrTemplate(MDerAttrTemplate.class, derSchema.getKey());
            if (template != null) {
                ((MDerAttr) derAttr).setTemplate(template);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected PropagationByResource fill(final Attributable attributable,
            final AbstractAttributableMod attributableMod, final AttributableUtil attrUtil,
            final SyncopeClientCompositeException scce) {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        if (attributable instanceof Subject && attributableMod instanceof AbstractSubjectMod) {
            // 1. resources to be removed
            for (String resourceToBeRemoved : ((AbstractSubjectMod) attributableMod).getResourcesToRemove()) {
                ExternalResource resource = resourceDAO.find(resourceToBeRemoved);
                if (resource != null) {
                    propByRes.add(ResourceOperation.DELETE, resource.getKey());
                    ((Subject<?, ?, ?>) attributable).removeResource(resource);
                }
            }

            LOG.debug("Resources to be removed:\n{}", propByRes);

            // 2. resources to be added
            for (String resourceToBeAdded : ((AbstractSubjectMod) attributableMod).getResourcesToAdd()) {
                ExternalResource resource = resourceDAO.find(resourceToBeAdded);
                if (resource != null) {
                    propByRes.add(ResourceOperation.CREATE, resource.getKey());
                    ((Subject<?, ?, ?>) attributable).addResource(resource);
                }
            }

            LOG.debug("Resources to be added:\n{}", propByRes);
        }

        final Set<ExternalResource> externalResources = new HashSet<>();
        if (attributable instanceof Subject) {
            externalResources.addAll(((Subject<?, ?, ?>) attributable).getResources());
        } else if (attributable instanceof Membership) {
            externalResources.addAll(((Membership) attributable).getUser().getResources());
            externalResources.addAll(((Membership) attributable).getGroup().getResources());
        }

        // 3. attributes to be removed
        for (String attributeToBeRemoved : attributableMod.getPlainAttrsToRemove()) {
            PlainSchema schema = getPlainSchema(attributeToBeRemoved, attrUtil.plainSchemaClass());
            if (schema != null) {
                PlainAttr attr = attributable.getPlainAttr(schema.getKey());
                if (attr == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttrMod mod : attributableMod.getPlainAttrsToUpdate()) {
                        if (schema.getKey().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().get(0);
                        }
                    }

                    if (!schema.isUniqueConstraint()
                            || (!attr.getUniqueValue().getStringValue().equals(newValue))) {

                        attributable.removePlainAttr(attr);
                        plainAttrDAO.delete(attr.getKey(), attrUtil.plainAttrClass());
                    }
                }

                if (attributable instanceof Subject) {
                    for (ExternalResource resource : externalResources) {
                        for (MappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                            if (schema.getKey().equals(mapItem.getIntAttrName())
                                    && mapItem.getIntMappingType() == attrUtil.plainIntMappingType()) {

                                propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                                if (mapItem.isAccountid() && attr != null && !attr.getValuesAsStrings().isEmpty()) {
                                    propByRes.addOldAccountId(resource.getKey(), attr.getValuesAsStrings().get(0));
                                }
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        for (AttrMod attributeMod : attributableMod.getPlainAttrsToUpdate()) {
            PlainSchema schema = getPlainSchema(attributeMod.getSchema(), attrUtil.plainSchemaClass());
            PlainAttr attr = null;
            if (schema != null) {
                attr = attributable.getPlainAttr(schema.getKey());
                if (attr == null) {
                    attr = attrUtil.newPlainAttr();
                    setPlainAttrSchema(attributable, attr, schema);
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeMod);
                    } else {
                        attr.setOwner(attributable);
                        attributable.addPlainAttr(attr);
                    }
                }
            }

            if (schema != null && attr != null && attr.getSchema() != null) {
                if (attributable instanceof Subject) {
                    virtAttrHander.updateOnResourcesIfMappingMatches(attrUtil, schema.getKey(),
                            externalResources, attrUtil.plainIntMappingType(), propByRes);
                } else if (attributable instanceof Membership) {
                    virtAttrHander.updateOnResourcesIfMappingMatches(attrUtil, schema.getKey(),
                            externalResources, IntMappingType.MembershipPlainSchema, propByRes);
                }

                // 1.1 remove values
                Set<Long> valuesToBeRemoved = new HashSet<>();
                for (String valueToBeRemoved : attributeMod.getValuesToBeRemoved()) {
                    if (attr.getSchema().isUniqueConstraint()) {
                        if (attr.getUniqueValue() != null
                                && valueToBeRemoved.equals(attr.getUniqueValue().getValueAsString())) {

                            valuesToBeRemoved.add(attr.getUniqueValue().getKey());
                        }
                    } else {
                        for (PlainAttrValue mav : attr.getValues()) {
                            if (valueToBeRemoved.equals(mav.getValueAsString())) {
                                valuesToBeRemoved.add(mav.getKey());
                            }
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    plainAttrValueDAO.delete(attributeValueId, attrUtil.plainAttrValueClass());
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
                    plainAttrDAO.delete(attr);
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        LOG.debug("Attributes to be updated:\n{}", propByRes);

        // 5. derived attributes to be removed
        for (String derAttrToBeRemoved : attributableMod.getDerAttrsToRemove()) {
            DerSchema derSchema = getDerSchema(derAttrToBeRemoved, attrUtil.derSchemaClass());
            if (derSchema != null) {
                DerAttr derAttr = attributable.getDerAttr(derSchema.getKey());
                if (derAttr == null) {
                    LOG.debug("No derived attribute found for schema {}", derSchema.getKey());
                } else {
                    derAttrDAO.delete(derAttr);
                }

                if (attributable instanceof Subject) {
                    for (ExternalResource resource : externalResources) {
                        for (MappingItem mapItem : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                            if (derSchema.getKey().equals(mapItem.getIntAttrName())
                                    && mapItem.getIntMappingType() == attrUtil.derIntMappingType()) {

                                propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                                if (mapItem.isAccountid() && derAttr != null
                                        && !derAttr.getValue(attributable.getPlainAttrs()).isEmpty()) {

                                    propByRes.addOldAccountId(resource.getKey(),
                                            derAttr.getValue(attributable.getPlainAttrs()));
                                }
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}", propByRes);

        // 6. derived attributes to be added
        for (String derAttrToBeAdded : attributableMod.getDerAttrsToAdd()) {
            DerSchema derSchema = getDerSchema(derAttrToBeAdded, attrUtil.derSchemaClass());
            if (derSchema != null) {
                if (attributable instanceof Subject) {
                    virtAttrHander.updateOnResourcesIfMappingMatches(attrUtil, derSchema.getKey(),
                            externalResources, attrUtil.derIntMappingType(), propByRes);
                } else if (attributable instanceof Membership) {
                    virtAttrHander.updateOnResourcesIfMappingMatches(attrUtil, derSchema.getKey(),
                            externalResources, IntMappingType.MembershipDerivedSchema, propByRes);
                }

                DerAttr derAttr = attrUtil.newDerAttr();
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

        // 7. virtual attributes: for users and groups this is delegated to PropagationManager
        if (AttributableType.USER != attrUtil.getType() && AttributableType.GROUP != attrUtil.getType()) {
            virtAttrHander.fillVirtual(attributable, attributableMod.getVirAttrsToRemove(),
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void fill(final Attributable attributable, final AbstractAttributableTO attributableTO,
            final AttributableUtil attrUtil, final SyncopeClientCompositeException scce) {

        // 1. attributes
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // Only consider attributeTO with values
        for (AttrTO attributeTO : attributableTO.getPlainAttrs()) {
            if (attributeTO.getValues() != null && !attributeTO.getValues().isEmpty()) {
                PlainSchema schema = getPlainSchema(attributeTO.getSchema(), attrUtil.plainSchemaClass());

                if (schema != null) {
                    PlainAttr attr = attributable.getPlainAttr(schema.getKey());
                    if (attr == null) {
                        attr = attrUtil.newPlainAttr();
                        setPlainAttrSchema(attributable, attr, schema);
                    }
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                    } else {
                        fillAttribute(attributeTO.getValues(), attrUtil, schema, attr, invalidValues);

                        if (!attr.getValuesAsStrings().isEmpty()) {
                            attributable.addPlainAttr(attr);
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
        for (AttrTO attributeTO : attributableTO.getDerAttrs()) {
            DerSchema derSchema = getDerSchema(attributeTO.getSchema(), attrUtil.derSchemaClass());

            if (derSchema != null) {
                DerAttr derAttr = attrUtil.newDerAttr();
                setDerAttrSchema(attributable, derAttr, derSchema);
                if (derAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                } else {
                    derAttr.setOwner(attributable);
                    attributable.addDerAttr(derAttr);
                }
            }
        }

        // 3. user and group virtual attributes will be evaluated by the propagation manager only (if needed).
        if (AttributableType.USER == attrUtil.getType()
                || AttributableType.GROUP == attrUtil.getType()) {

            for (AttrTO vattrTO : attributableTO.getVirAttrs()) {
                VirSchema virSchema = virtAttrHander.getVirSchema(vattrTO.getSchema(), attrUtil.virSchemaClass());

                if (virSchema != null) {
                    VirAttr virAttr = attrUtil.newVirAttr();
                    virtAttrHander.setVirAttrSchema(attributable, virAttr, virSchema);
                    if (virAttr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", vattrTO);
                    } else {
                        virAttr.setOwner(attributable);
                        attributable.addVirAttr(virAttr);
                    }
                }
            }
        }

        virtAttrHander.fillVirtual(attributable, attributableTO.getVirAttrs(), attrUtil);

        // 4. resources
        if (attributable instanceof Subject && attributableTO instanceof AbstractSubjectTO) {
            for (String resourceName : ((AbstractSubjectTO) attributableTO).getResources()) {
                ExternalResource resource = resourceDAO.find(resourceName);

                if (resource != null) {
                    ((Subject<?, ?, ?>) attributable).addResource(resource);
                }
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(attrUtil, attributable);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void fillTO(final AbstractAttributableTO attributableTO,
            final Collection<? extends PlainAttr> attrs,
            final Collection<? extends DerAttr> derAttrs,
            final Collection<? extends VirAttr> virAttrs,
            final Collection<? extends ExternalResource> resources) {

        AttrTO attributeTO;
        for (PlainAttr attr : attrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(attr.getSchema().getKey());
            attributeTO.getValues().addAll(attr.getValuesAsStrings());
            attributeTO.setReadonly(attr.getSchema().isReadonly());

            attributableTO.getPlainAttrs().add(attributeTO);
        }

        for (DerAttr derAttr : derAttrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(derAttr.getSchema().getKey());
            attributeTO.getValues().add(derAttr.getValue(attrs));
            attributeTO.setReadonly(true);

            attributableTO.getDerAttrs().add(attributeTO);
        }

        for (VirAttr virAttr : virAttrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(virAttr.getSchema().getKey());
            attributeTO.getValues().addAll(virAttr.getValues());
            attributeTO.setReadonly(virAttr.getSchema().isReadonly());

            attributableTO.getVirAttrs().add(attributeTO);
        }

        if (attributableTO instanceof AbstractSubjectTO) {
            for (ExternalResource resource : resources) {
                ((AbstractSubjectTO) attributableTO).getResources().add(resource.getKey());
            }
        }
    }

    protected Map<String, String> getAccountIds(final Subject<?, ?, ?> subject, final AttributableType type) {
        Map<String, String> accountIds = new HashMap<>();

        for (ExternalResource resource : subject.getResources()) {
            if ((type == AttributableType.USER && resource.getUmapping() != null)
                    || (type == AttributableType.GROUP && resource.getGmapping() != null)) {

                MappingItem accountIdItem =
                        attrUtilFactory.getInstance(type).getAccountIdItem(resource);
                if (accountIdItem == null) {
                    throw new NotFoundException(
                            "AccountId mapping for " + type + " " + subject.getKey()
                            + " on resource '" + resource.getKey() + "'");
                }

                accountIds.put(resource.getKey(), MappingUtil.getAccountIdValue(subject, resource, accountIdItem));
            }
        }

        return accountIds;
    }
}
