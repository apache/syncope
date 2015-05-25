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
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.dao.DerAttrDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirAttrDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.java.VirAttrHandler;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractAnyDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAnyDataBinder.class);

    @Autowired
    protected RealmDAO realmDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected UserDAO userDAO;

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
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected VirAttrHandler virtAttrHander;

    protected void setRealm(final Any<?, ?, ?> any, final AnyMod anyMod) {
        if (StringUtils.isNotBlank(anyMod.getRealm())) {
            Realm newRealm = realmDAO.find(anyMod.getRealm());
            if (newRealm == null) {
                LOG.warn("Invalid realm specified: {}, ignoring", anyMod.getRealm());
            } else {
                any.setRealm(newRealm);
            }
        }
    }

    protected PlainSchema getPlainSchema(final String schemaName) {
        PlainSchema schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = plainSchemaDAO.find(schemaName);

            // safely ignore invalid schemas from AttrTO
            if (schema == null) {
                LOG.debug("Ignoring invalid schema {}", schemaName);
            } else if (schema.isReadonly()) {
                schema = null;

                LOG.debug("Ignoring readonly schema {}", schemaName);
            }
        }

        return schema;
    }

    private DerSchema getDerSchema(final String derSchemaName) {
        DerSchema schema = null;
        if (StringUtils.isNotBlank(derSchemaName)) {
            schema = derSchemaDAO.find(derSchemaName);
            if (schema == null) {
                LOG.debug("Ignoring invalid derived schema {}", derSchemaName);
            }
        }

        return schema;
    }

    protected void fillAttribute(final List<String> values, final AnyUtils anyUtils,
            final PlainSchema schema, final PlainAttr<?> attr, final SyncopeClientException invalidValues) {

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
                    attr.add(value, anyUtils);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute " + schema.getKey() + ": " + value, e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        }
    }

    private boolean evaluateMandatoryCondition(final AnyUtils anyUtils, final ExternalResource resource,
            final Any<?, ?, ?> any, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        Collection<MappingItem> mappings = MappingUtils.getMatchingMappingItems(
                anyUtils.getMappingItems(resource.getProvision(any.getType()), MappingPurpose.PROPAGATION),
                intAttrName, intMappingType);
        for (Iterator<MappingItem> itor = mappings.iterator(); itor.hasNext() && !result;) {
            MappingItem mapping = itor.next();
            result |= JexlUtils.evaluateMandatoryCondition(mapping.getMandatoryCondition(), any);
        }

        return result;
    }

    private boolean evaluateMandatoryCondition(final AnyUtils anyUtils,
            final Any<?, ?, ?> any, final String intAttrName, final IntMappingType intMappingType) {

        boolean result = false;

        Iterable<? extends ExternalResource> iterable = any instanceof User
                ? userDAO.findAllResources((User) any)
                : any instanceof Group
                        ? ((Group) any).getResources()
                        : Collections.<ExternalResource>emptySet();

        for (Iterator<? extends ExternalResource> itor = iterable.iterator(); itor.hasNext() && !result;) {
            ExternalResource resource = itor.next();
            if (resource.isEnforceMandatoryCondition()) {
                result |= evaluateMandatoryCondition(
                        anyUtils, resource, any, intAttrName, intMappingType);
            }
        }

        return result;
    }

    private SyncopeClientException checkMandatory(final AnyUtils anyUtils, final Any<?, ?, ?> any) {
        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        // Check if there is some mandatory schema defined for which no value has been provided
        List<PlainSchema> plainSchemas = plainSchemaDAO.findAll();
        for (PlainSchema schema : plainSchemas) {
            if (any.getPlainAttr(schema.getKey()) == null
                    && !schema.isReadonly()
                    && (JexlUtils.evaluateMandatoryCondition(schema.getMandatoryCondition(), any)
                    || evaluateMandatoryCondition(anyUtils, any, schema.getKey(),
                            anyUtils.plainIntMappingType()))) {

                LOG.error("Mandatory schema " + schema.getKey() + " not provided with values");

                reqValMissing.getElements().add(schema.getKey());
            }
        }

        List<DerSchema> derSchemas = derSchemaDAO.findAll();
        for (DerSchema derSchema : derSchemas) {
            if (any.getDerAttr(derSchema.getKey()) == null
                    && evaluateMandatoryCondition(anyUtils, any, derSchema.getKey(),
                            anyUtils.derIntMappingType())) {

                LOG.error("Mandatory derived schema " + derSchema.getKey() + " does not evaluate to any value");

                reqValMissing.getElements().add(derSchema.getKey());
            }
        }

        List<VirSchema> virSchemas = virSchemaDAO.findAll();
        for (VirSchema virSchema : virSchemas) {
            if (any.getVirAttr(virSchema.getKey()) == null
                    && !virSchema.isReadonly()
                    && evaluateMandatoryCondition(anyUtils, any, virSchema.getKey(),
                            anyUtils.virIntMappingType())) {

                LOG.error("Mandatory virtual schema " + virSchema.getKey() + " not provided with values");

                reqValMissing.getElements().add(virSchema.getKey());
            }
        }

        return reqValMissing;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected PropagationByResource fill(final Any any, final AnyMod anyMod, final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // 1. resources to be removed
        for (String resourceToBeRemoved : anyMod.getResourcesToRemove()) {
            ExternalResource resource = resourceDAO.find(resourceToBeRemoved);
            if (resource != null) {
                propByRes.add(ResourceOperation.DELETE, resource.getKey());
                ((Any<?, ?, ?>) any).remove(resource);
            }
        }

        LOG.debug("Resources to be removed:\n{}", propByRes);

        // 2. resources to be added
        for (String resourceToBeAdded : anyMod.getResourcesToAdd()) {
            ExternalResource resource = resourceDAO.find(resourceToBeAdded);
            if (resource != null) {
                propByRes.add(ResourceOperation.CREATE, resource.getKey());
                ((Any<?, ?, ?>) any).add(resource);
            }
        }

        LOG.debug("Resources to be added:\n{}", propByRes);

        Set<ExternalResource> externalResources = new HashSet<>();
        if (any instanceof User) {
            externalResources.addAll(userDAO.findAllResources((User) any));
        } else if (any instanceof Group) {
            externalResources.addAll(((Group) any).getResources());
        } else if (any instanceof AnyObject) {
            externalResources.addAll(anyObjectDAO.findAllResources((AnyObject) any));
        }

        // 3. attributes to be removed
        for (String attributeToBeRemoved : anyMod.getPlainAttrsToRemove()) {
            PlainSchema schema = getPlainSchema(attributeToBeRemoved);
            if (schema != null) {
                PlainAttr<?> attr = any.getPlainAttr(schema.getKey());
                if (attr == null) {
                    LOG.debug("No attribute found for schema {}", schema);
                } else {
                    String newValue = null;
                    for (AttrMod mod : anyMod.getPlainAttrsToUpdate()) {
                        if (schema.getKey().equals(mod.getSchema())) {
                            newValue = mod.getValuesToBeAdded().get(0);
                        }
                    }

                    if (!schema.isUniqueConstraint()
                            || (!attr.getUniqueValue().getStringValue().equals(newValue))) {

                        any.remove(attr);
                        plainAttrDAO.delete(attr.getKey(), anyUtils.plainAttrClass());
                    }
                }

                for (ExternalResource resource : externalResources) {
                    for (MappingItem mapItem : anyUtils.getMappingItems(
                            resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                        if (schema.getKey().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == anyUtils.plainIntMappingType()) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                            if (mapItem.isConnObjectKey() && attr != null && !attr.getValuesAsStrings().isEmpty()) {
                                propByRes.addOldAccountId(resource.getKey(), attr.getValuesAsStrings().get(0));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Attributes to be removed:\n{}", propByRes);

        // 4. attributes to be updated
        for (AttrMod attributeMod : anyMod.getPlainAttrsToUpdate()) {
            PlainSchema schema = getPlainSchema(attributeMod.getSchema());
            PlainAttr attr = null;
            if (schema != null) {
                attr = any.getPlainAttr(schema.getKey());
                if (attr == null) {
                    attr = anyUtils.newPlainAttr();
                    attr.setSchema(schema);
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeMod);
                    } else {
                        attr.setOwner(any);
                        any.add(attr);
                    }
                }
            }

            if (schema != null && attr != null && attr.getSchema() != null) {
                virtAttrHander.updateOnResourcesIfMappingMatches(any, anyUtils, schema.getKey(),
                        externalResources, anyUtils.plainIntMappingType(), propByRes);

                // 1.1 remove values
                Set<Long> valuesToBeRemoved = new HashSet<>();
                for (String valueToBeRemoved : attributeMod.getValuesToBeRemoved()) {
                    if (attr.getSchema().isUniqueConstraint()) {
                        if (attr.getUniqueValue() != null
                                && valueToBeRemoved.equals(attr.getUniqueValue().getValueAsString())) {

                            valuesToBeRemoved.add(attr.getUniqueValue().getKey());
                        }
                    } else {
                        for (PlainAttrValue mav : ((PlainAttr<?>) attr).getValues()) {
                            if (valueToBeRemoved.equals(mav.getValueAsString())) {
                                valuesToBeRemoved.add(mav.getKey());
                            }
                        }
                    }
                }
                for (Long attributeValueId : valuesToBeRemoved) {
                    plainAttrValueDAO.delete(attributeValueId, anyUtils.plainAttrValueClass());
                }

                // 1.2 add values
                List<String> valuesToBeAdded = attributeMod.getValuesToBeAdded();
                if (valuesToBeAdded != null && !valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint() || attr.getUniqueValue() == null
                        || !valuesToBeAdded.iterator().next().equals(attr.getUniqueValue().getValueAsString()))) {

                    fillAttribute(attributeMod.getValuesToBeAdded(), anyUtils, schema, attr, invalidValues);
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
        for (String derAttrToBeRemoved : anyMod.getDerAttrsToRemove()) {
            DerSchema derSchema = getDerSchema(derAttrToBeRemoved);
            if (derSchema != null) {
                DerAttr derAttr = any.getDerAttr(derSchema.getKey());
                if (derAttr == null) {
                    LOG.debug("No derived attribute found for schema {}", derSchema.getKey());
                } else {
                    derAttrDAO.delete(derAttr);
                }

                for (ExternalResource resource : externalResources) {
                    for (MappingItem mapItem : anyUtils.getMappingItems(
                            resource.getProvision(any.getType()), MappingPurpose.PROPAGATION)) {

                        if (derSchema.getKey().equals(mapItem.getIntAttrName())
                                && mapItem.getIntMappingType() == anyUtils.derIntMappingType()) {

                            propByRes.add(ResourceOperation.UPDATE, resource.getKey());

                            if (mapItem.isConnObjectKey() && derAttr != null
                                    && !derAttr.getValue(any.getPlainAttrs()).isEmpty()) {

                                propByRes.addOldAccountId(resource.getKey(),
                                        derAttr.getValue(any.getPlainAttrs()));
                            }
                        }
                    }
                }
            }
        }

        LOG.debug("Derived attributes to be removed:\n{}", propByRes);

        // 6. derived attributes to be added
        for (String derAttrToBeAdded : anyMod.getDerAttrsToAdd()) {
            DerSchema derSchema = getDerSchema(derAttrToBeAdded);
            if (derSchema != null) {
                virtAttrHander.updateOnResourcesIfMappingMatches(any, anyUtils, derSchema.getKey(),
                        externalResources, anyUtils.derIntMappingType(), propByRes);

                DerAttr derAttr = anyUtils.newDerAttr();
                derAttr.setSchema(derSchema);
                if (derAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", derAttrToBeAdded);
                } else {
                    derAttr.setOwner(any);
                    any.add(derAttr);
                }
            }
        }

        LOG.debug("Derived attributes to be added:\n{}", propByRes);

        // Finally, check if mandatory values are missing
        SyncopeClientException requiredValuesMissing = checkMandatory(anyUtils, any);
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
    protected void fill(final Any any, final AnyTO anyTO,
            final AnyUtils anyUtils, final SyncopeClientCompositeException scce) {

        // 1. attributes
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // Only consider attributeTO with values
        for (AttrTO attributeTO : anyTO.getPlainAttrs()) {
            if (attributeTO.getValues() != null && !attributeTO.getValues().isEmpty()) {
                PlainSchema schema = getPlainSchema(attributeTO.getSchema());

                if (schema != null) {
                    PlainAttr attr = any.getPlainAttr(schema.getKey());
                    if (attr == null) {
                        attr = anyUtils.newPlainAttr();
                        attr.setSchema(schema);
                    }
                    if (attr.getSchema() == null) {
                        LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                    } else {
                        fillAttribute(attributeTO.getValues(), anyUtils, schema, attr, invalidValues);

                        if (!attr.getValuesAsStrings().isEmpty()) {
                            any.add(attr);
                            attr.setOwner(any);
                        }
                    }
                }
            }
        }

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // 2. derived attributes
        for (AttrTO attributeTO : anyTO.getDerAttrs()) {
            DerSchema derSchema = getDerSchema(attributeTO.getSchema());

            if (derSchema != null) {
                DerAttr derAttr = anyUtils.newDerAttr();
                derAttr.setSchema(derSchema);
                if (derAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", attributeTO);
                } else {
                    derAttr.setOwner(any);
                    any.add(derAttr);
                }
            }
        }

        // 3. virtual attributes
        for (AttrTO vattrTO : anyTO.getVirAttrs()) {
            VirSchema virSchema = virtAttrHander.getVirSchema(vattrTO.getSchema());

            if (virSchema != null) {
                VirAttr virAttr = anyUtils.newVirAttr();
                virAttr.setSchema(virSchema);
                if (virAttr.getSchema() == null) {
                    LOG.debug("Ignoring {} because no valid schema or template was found", vattrTO);
                } else {
                    virAttr.setOwner(any);
                    any.add(virAttr);
                }
            }
        }

        virtAttrHander.fillVirtual(any, anyTO.getVirAttrs(), anyUtils);

        // 4. realm & resources
        Realm realm = realmDAO.find(anyTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add(
                    "Invalid or null realm specified: " + anyTO.getRealm());
            scce.addException(noRealm);
        }
        ((Any<?, ?, ?>) any).setRealm(realm);

        for (String resourceName : anyTO.getResources()) {
            ExternalResource resource = resourceDAO.find(resourceName);

            if (resource != null) {
                ((Any<?, ?, ?>) any).add(resource);
            }
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(anyUtils, any);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void fillTO(final AnyTO anyTO,
            final String realmFullPath,
            final Collection<? extends PlainAttr<?>> attrs,
            final Collection<? extends DerAttr<?>> derAttrs,
            final Collection<? extends VirAttr<?>> virAttrs,
            final Collection<? extends ExternalResource> resources) {

        AttrTO attributeTO;
        for (PlainAttr<?> attr : attrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(attr.getSchema().getKey());
            attributeTO.getValues().addAll(attr.getValuesAsStrings());
            attributeTO.setReadonly(attr.getSchema().isReadonly());

            anyTO.getPlainAttrs().add(attributeTO);
        }

        for (DerAttr<?> derAttr : derAttrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(derAttr.getSchema().getKey());
            attributeTO.getValues().add(derAttr.getValue(attrs));
            attributeTO.setReadonly(true);

            anyTO.getDerAttrs().add(attributeTO);
        }

        for (VirAttr<?> virAttr : virAttrs) {
            attributeTO = new AttrTO();
            attributeTO.setSchema(virAttr.getSchema().getKey());
            attributeTO.getValues().addAll(virAttr.getValues());
            attributeTO.setReadonly(virAttr.getSchema().isReadonly());

            anyTO.getVirAttrs().add(attributeTO);
        }

        anyTO.setRealm(realmFullPath);
        for (ExternalResource resource : resources) {
            anyTO.getResources().add(resource.getKey());
        }
    }

    protected Map<String, String> getConnObjectKeys(final Any<?, ?, ?> any) {
        Map<String, String> connObjectKeys = new HashMap<>();

        Iterable<? extends ExternalResource> iterable = any instanceof User
                ? userDAO.findAllResources((User) any)
                : any instanceof AnyObject
                        ? anyObjectDAO.findAllResources((AnyObject) any)
                        : ((Group) any).getResources();
        for (ExternalResource resource : iterable) {
            Provision provision = resource.getProvision(any.getType());
            if (provision.getMapping() != null) {
                MappingItem connObjectKeyItem = anyUtilsFactory.getInstance(any).getConnObjectKeyItem(provision);
                if (connObjectKeyItem == null) {
                    throw new NotFoundException(
                            "ConnObjectKey mapping for " + any.getType().getKey() + " " + any.getKey()
                            + " on resource '" + resource.getKey() + "'");
                }

                connObjectKeys.put(resource.getKey(), MappingUtils.getConnObjectKeyValue(any, provision));
            }
        }

        return connObjectKeys;
    }
}
