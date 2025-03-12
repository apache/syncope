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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.DropdownValueProvider;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidPlainAttrValueException;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AccountGetter;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PlainAttrGetter;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractAnyDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractAnyDataBinder.class);

    protected static void fillTO(
            final AnyTO anyTO,
            final String realmFullPath,
            final Collection<? extends AnyTypeClass> auxClasses,
            final Collection<PlainAttr> plainAttrs,
            final Map<DerSchema, String> derAttrs,
            final Map<VirSchema, List<String>> virAttrs,
            final Collection<? extends ExternalResource> resources) {

        anyTO.setRealm(realmFullPath);

        anyTO.getAuxClasses().addAll(auxClasses.stream().map(AnyTypeClass::getKey).toList());

        plainAttrs.forEach(plainAttr -> anyTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrs.forEach((schema, value) -> anyTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        virAttrs.forEach((schema, values) -> anyTO.getVirAttrs().
                add(new Attr.Builder(schema.getKey()).values(values).build()));

        anyTO.getResources().addAll(resources.stream().map(ExternalResource::getKey).collect(Collectors.toSet()));
    }

    protected static RelationshipTO getRelationshipTO(
            final String relationshipType,
            final RelationshipTO.End end,
            final Any otherEnd) {

        return new RelationshipTO.Builder(relationshipType, end).otherEnd(
                otherEnd.getType().getKey(),
                otherEnd.getKey(),
                otherEnd instanceof User user
                        ? user.getUsername()
                        : ((AnyObject) otherEnd).getName()).
                build();
    }

    protected static MembershipTO getMembershipTO(
            final Collection<PlainAttr> plainAttrs,
            final Map<DerSchema, String> derAttrs,
            final Map<VirSchema, List<String>> virAttrs,
            final Membership<? extends Any> membership) {

        MembershipTO membershipTO = new MembershipTO.Builder(membership.getRightEnd().getKey()).
                groupName(membership.getRightEnd().getName()).build();

        plainAttrs.forEach(plainAttr -> membershipTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrs.forEach((schema, value) -> membershipTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        virAttrs.forEach((schema, values) -> membershipTO.getVirAttrs().
                add(new Attr.Builder(schema.getKey()).values(values).build()));

        return membershipTO;
    }

    protected final AnyTypeDAO anyTypeDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final EntityFactory entityFactory;

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final DerAttrHandler derAttrHandler;

    protected final VirAttrHandler virAttrHandler;

    protected final MappingManager mappingManager;

    protected final IntAttrNameParser intAttrNameParser;

    protected final OutboundMatcher outboundMatcher;

    protected final PlainAttrValidationManager validator;

    protected final Map<String, DropdownValueProvider> dropdownValueProviders = new ConcurrentHashMap<>();

    protected AbstractAnyDataBinder(
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator) {

        this.anyTypeDAO = anyTypeDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.resourceDAO = resourceDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.entityFactory = entityFactory;
        this.anyUtilsFactory = anyUtilsFactory;
        this.derAttrHandler = derAttrHandler;
        this.virAttrHandler = virAttrHandler;
        this.mappingManager = mappingManager;
        this.intAttrNameParser = intAttrNameParser;
        this.outboundMatcher = outboundMatcher;
        this.validator = validator;
    }

    protected void setRealm(final Any any, final AnyUR anyUR) {
        if (anyUR.getRealm() != null && StringUtils.isNotBlank(anyUR.getRealm().getValue())) {
            realmSearchDAO.findByFullPath(anyUR.getRealm().getValue()).ifPresentOrElse(
                any::setRealm,
                    () -> LOG.debug("Invalid realm specified: {}, ignoring", anyUR.getRealm().getValue()));
        }
    }

    protected Map<String, ConnObject> onResources(
            final Any any,
            final Collection<String> resources,
            final String password,
            final Set<String> changePwdRes) {

        Map<String, ConnObject> onResources = new HashMap<>();

        resources.stream().map(resourceDAO::findById).flatMap(Optional::stream).
                forEach(resource -> resource.getProvisionByAnyType(any.getType().getKey()).
                ifPresent(provision -> MappingUtils.getConnObjectKeyItem(provision).ifPresent(keyItem -> {

            Pair<String, Set<Attribute>> prepared = mappingManager.prepareAttrsFromAny(
                    any, password, changePwdRes.contains(resource.getKey()), true, resource, provision);

            ConnObject connObjectTO;
            if (StringUtils.isBlank(prepared.getLeft())) {
                connObjectTO = ConnObjectUtils.getConnObjectTO(null, prepared.getRight());
            } else {
                ConnectorObject connectorObject = new ConnectorObjectBuilder().
                        addAttributes(prepared.getRight()).
                        addAttribute(new Uid(prepared.getLeft())).
                        addAttribute(AttributeBuilder.build(keyItem.getExtAttrName(), prepared.getLeft())).
                        build();

                connObjectTO = ConnObjectUtils.getConnObjectTO(
                        outboundMatcher.getFIQL(connectorObject, resource, provision),
                        connectorObject.getAttributes());
            }

            onResources.put(resource.getKey(), connObjectTO);
        })));

        return onResources;
    }

    protected Optional<PlainSchema> getPlainSchema(final String schemaName) {
        PlainSchema schema = null;
        if (StringUtils.isNotBlank(schemaName)) {
            schema = plainSchemaDAO.findById(schemaName).orElse(null);

            // safely ignore invalid schemas from Attr
            if (schema == null) {
                LOG.debug("Ignoring invalid schema {}", schemaName);
            } else if (schema.isReadonly()) {
                schema = null;
                LOG.debug("Ignoring readonly schema {}", schemaName);
            }
        }

        return Optional.ofNullable(schema);
    }

    protected void fillAttr(
            final AnyTO anyTO,
            final List<String> values,
            final PlainSchema schema,
            final PlainAttr attr,
            final SyncopeClientException invalidValues) {

        // if schema is multivalue, all values are considered for addition;
        // otherwise only the fist one - if provided - is considered
        List<String> valuesProvided = schema.isMultivalue()
                ? values
                : (values.isEmpty() || values.getFirst() == null
                ? List.of()
                : List.of(values.getFirst()));

        valuesProvided.forEach(value -> {
            if (StringUtils.isBlank(value)) {
                LOG.debug("Null value for {}, ignoring", schema.getKey());
            } else {
                try {
                    switch (schema.getType()) {
                        case Enum -> {
                            if (!schema.getEnumValues().containsKey(value)) {
                                throw new InvalidPlainAttrValueException(
                                        '\'' + value + "' is not one of: " + schema.getEnumValues().keySet());
                            }
                        }

                        case Dropdown -> {
                            List<String> dropdownValues = List.of();
                            try {
                                DropdownValueProvider provider = ImplementationManager.build(
                                        schema.getDropdownValueProvider(),
                                        () -> dropdownValueProviders.get(
                                                schema.getDropdownValueProvider().getKey()),
                                        instance -> dropdownValueProviders.put(
                                                schema.getDropdownValueProvider().getKey(), instance));
                                dropdownValues = provider.getChoices(anyTO);
                            } catch (Exception e) {
                                LOG.error("While getting dropdown values for {}", schema.getKey(), e);
                            }

                            if (!dropdownValues.contains(value)) {
                                throw new InvalidPlainAttrValueException(
                                        '\'' + value + "' is not one of: " + dropdownValues);
                            }
                        }

                        default -> {
                        }
                    }

                    attr.add(validator, value);
                } catch (InvalidPlainAttrValueException e) {
                    LOG.warn("Invalid value for attribute {}: {}",
                            schema.getKey(), StringUtils.abbreviate(value, 20), e);

                    invalidValues.getElements().add(schema.getKey() + ": " + value + " - " + e.getMessage());
                }
            }
        });
    }

    protected List<String> evaluateMandatoryCondition(
            final ExternalResource resource, final Provision provision, final Any any) {

        List<String> missingAttrNames = new ArrayList<>();

        MappingUtils.getPropagationItems(provision.getMapping().getItems().stream()).forEach(mapItem -> {
            IntAttrName intAttrName = null;
            try {
                intAttrName = intAttrNameParser.parse(mapItem.getIntAttrName(), any.getType().getKind());
            } catch (ParseException e) {
                LOG.error("Invalid intAttrName '{}', ignoring", mapItem.getIntAttrName(), e);
            }
            if (intAttrName != null && intAttrName.getSchema() != null) {
                AttrSchemaType schemaType = intAttrName.getSchema() instanceof PlainSchema
                        ? intAttrName.getSchema().getType()
                        : AttrSchemaType.String;

                Pair<AttrSchemaType, List<PlainAttrValue>> intValues = mappingManager.getIntValues(
                        resource,
                        provision,
                        mapItem,
                        intAttrName,
                        schemaType,
                        any,
                        AccountGetter.DEFAULT,
                        PlainAttrGetter.DEFAULT);
                if (intValues.getRight().isEmpty()
                        && JexlUtils.evaluateMandatoryCondition(mapItem.getMandatoryCondition(), any, derAttrHandler)) {

                    missingAttrNames.add(mapItem.getIntAttrName());
                }
            }
        });

        return missingAttrNames;
    }

    private SyncopeClientException checkMandatoryOnResources(
            final Any any, final Collection<? extends ExternalResource> resources) {

        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        resources.stream().filter(ExternalResource::isEnforceMandatoryCondition).
                forEach(resource -> resource.getProvisionByAnyType(any.getType().getKey()).
                ifPresent(provision -> {
                    List<String> missingAttrNames = evaluateMandatoryCondition(resource, provision, any);
                    if (!missingAttrNames.isEmpty()) {
                        LOG.error("Mandatory schemas {} not provided with values", missingAttrNames);

                        reqValMissing.getElements().addAll(missingAttrNames);
                    }
                }));

        return reqValMissing;
    }

    private void checkMandatory(
            final PlainSchema schema,
            final PlainAttr attr,
            final Any any,
            final SyncopeClientException reqValMissing) {

        if (attr == null
                && !schema.isReadonly()
                && JexlUtils.evaluateMandatoryCondition(schema.getMandatoryCondition(), any, derAttrHandler)) {

            LOG.error("Mandatory schema {} not provided with values", schema.getKey());

            reqValMissing.getElements().add(schema.getKey());
        }
    }

    private SyncopeClientException checkMandatory(final Any any, final AnyUtils anyUtils) {
        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        // Check if there is some mandatory schema defined for which no value has been provided
        AllowedSchemas<PlainSchema> allowedPlainSchemas = anyUtils.dao().findAllowedSchemas(any, PlainSchema.class);
        allowedPlainSchemas.getForSelf().forEach(schema -> checkMandatory(
                schema, any.getPlainAttr(schema.getKey()).orElse(null), any, reqValMissing));
        if (any instanceof Groupable<?, ?, ?, ?> groupable) {
            allowedPlainSchemas.getForMemberships().forEach((group, schemas) -> {
                Membership<?> membership = groupable.getMembership(group.getKey()).orElse(null);
                schemas.forEach(schema -> checkMandatory(
                        schema,
                        groupable.getPlainAttr(schema.getKey(), membership).orElse(null),
                        any,
                        reqValMissing));
            });
        }

        return reqValMissing;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void processAttrPatch(
            final AnyTO anyTO,
            final Any any,
            final AttrPatch patch,
            final PlainSchema schema,
            final PlainAttr attr,
            final SyncopeClientException invalidValues) {

        switch (patch.getOperation()) {
            case ADD_REPLACE:
                // 1.1 remove values
                if (schema.isUniqueConstraint()) {
                    if (attr.getUniqueValue() != null
                            && !patch.getAttr().getValues().isEmpty()
                            && !patch.getAttr().getValues().getFirst().equals(
                                attr.getUniqueValue().getValueAsString())) {

                        attr.setUniqueValue(null);
                    }
                } else {
                    attr.getValues().clear();
                }

                // 1.2 add values
                List<String> valuesToBeAdded = patch.getAttr().getValues();
                if (!valuesToBeAdded.isEmpty()
                        && (!schema.isUniqueConstraint() || attr.getUniqueValue() == null
                        || !valuesToBeAdded.getFirst().equals(attr.getUniqueValue().getValueAsString()))) {

                    fillAttr(anyTO, valuesToBeAdded, schema, attr, invalidValues);
                }

                // if no values are in, the attribute can be safely removed
                if (attr.getValuesAsStrings().isEmpty()) {
                    any.remove(attr);
                }
                break;

            case DELETE:
            default:
                any.remove(attr);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void fill(
            final AnyTO anyTO,
            final Any any,
            final AnyUR anyUR,
            final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        // 1. anyTypeClasses
        for (StringPatchItem patch : anyUR.getAuxClasses()) {
            anyTypeClassDAO.findById(patch.getValue()).ifPresentOrElse(
                    auxClass -> {
                        switch (patch.getOperation()) {
                            case ADD_REPLACE:
                                any.add(auxClass);
                                break;

                            case DELETE:
                            default:
                                any.getAuxClasses().remove(auxClass);
                        }
                    },
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            AnyTypeClass.class.getSimpleName(), patch.getValue()));
        }

        // 2. resources
        for (StringPatchItem patch : anyUR.getResources()) {
            resourceDAO.findById(patch.getValue()).ifPresentOrElse(
                    resource -> {
                        switch (patch.getOperation()) {
                            case ADD_REPLACE:
                                any.add(resource);
                                break;

                            case DELETE:
                            default:
                                any.getResources().remove(resource);
                        }
                    },
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            ExternalResource.class.getSimpleName(), patch.getValue()));
        }

        Set<ExternalResource> resources = anyUtils.getAllResources(any);
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // 3. plain attributes
        anyUR.getPlainAttrs().stream().filter(patch -> patch.getAttr() != null).
                forEach(patch -> getPlainSchema(patch.getAttr().getSchema()).ifPresentOrElse(
                schema -> {
                    PlainAttr attr = any.getPlainAttr(schema.getKey()).orElse(null);
                    if (attr == null) {
                        LOG.debug("No plain attribute found for schema {}", schema);

                        if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                            attr = new PlainAttr();
                            attr.setPlainSchema(schema);
                            any.add(attr);
                        }
                    }
                    if (attr != null) {
                        processAttrPatch(anyTO, any, patch, schema, attr, invalidValues);
                    }
                },
                () -> LOG.debug("Invalid {} {}, ignoring...",
                        PlainSchema.class.getSimpleName(), patch.getAttr().getSchema())));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(any, anyUtils);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }
        requiredValuesMissing = checkMandatoryOnResources(any, resources);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }
    }

    protected PropagationByResource<String> propByRes(
            final Map<String, ConnObject> before,
            final Map<String, ConnObject> after) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();

        after.forEach((resource, connObject) -> {
            if (before.containsKey(resource)) {
                ConnObject beforeObject = before.get(resource);
                if (!beforeObject.equals(connObject)) {
                    propByRes.add(ResourceOperation.UPDATE, resource);

                    beforeObject.getAttr(Uid.NAME).map(attr -> attr.getValues().getFirst()).
                            ifPresent(value -> propByRes.addOldConnObjectKey(resource, value));
                }
            } else {
                propByRes.add(ResourceOperation.CREATE, resource);
            }
        });
        propByRes.addAll(
                ResourceOperation.DELETE,
                before.keySet().stream().filter(resource -> !after.containsKey(resource)).collect(Collectors.toSet()));

        return propByRes;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void fill(
            final AnyTO anyTO,
            final Any any,
            final AnyCR anyCR,
            final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        // 0. aux classes
        any.getAuxClasses().clear();
        anyCR.getAuxClasses().stream().
                map(anyTypeClassDAO::findById).
                flatMap(Optional::stream).
                forEach(auxClass -> {
                    if (auxClass == null) {
                        LOG.debug("Invalid {} {}, ignoring...", AnyTypeClass.class.getSimpleName(), auxClass);
                    } else {
                        any.add(auxClass);
                    }
                });

        // 1. attributes
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        anyCR.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = any.getPlainAttr(schema.getKey()).orElseGet(() -> {
                PlainAttr newAttr = new PlainAttr();
                newAttr.setPlainSchema(schema);
                return newAttr;
            });
            fillAttr(anyTO, attrTO.getValues(), schema, attr, invalidValues);

            if (!attr.getValuesAsStrings().isEmpty()) {
                any.add(attr);
            }
        }));

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        SyncopeClientException requiredValuesMissing = checkMandatory(any, anyUtils);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // 2. resources
        anyCR.getResources().forEach(resource -> resourceDAO.findById(resource).ifPresentOrElse(
                any::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", ExternalResource.class.getSimpleName(), resource)));

        requiredValuesMissing = checkMandatoryOnResources(any, anyUtils.getAllResources(any));
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }
    }

    protected void fill(
            final AnyTO anyTO,
            final Any any,
            final Membership<?> membership,
            final MembershipTO membershipTO,
            final SyncopeClientCompositeException scce) {

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        membershipTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = ((Groupable<?, ?, ?, ?>) any).getPlainAttr(schema.getKey(), membership).orElseGet(() -> {
                PlainAttr gpa = new PlainAttr();
                gpa.setMembership(membership.getKey());
                gpa.setPlainSchema(schema);
                return gpa;
            });
            fillAttr(anyTO, attrTO.getValues(), schema, attr, invalidValues);

            if (!attr.getValuesAsStrings().isEmpty()) {
                any.add(attr);
            }
        }));

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }
    }
}
