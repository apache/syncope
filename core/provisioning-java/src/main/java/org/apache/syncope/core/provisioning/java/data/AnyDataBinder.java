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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
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
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AccountGetter;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PlainAttrGetter;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Uid;

abstract class AnyDataBinder extends AttributableDataBinder {

    protected static void fillTO(
            final AnyTO anyTO,
            final String realmFullPath,
            final Collection<? extends AnyTypeClass> auxClasses,
            final Collection<PlainAttr> plainAttrs,
            final Map<DerSchema, String> derAttrs,
            final Collection<? extends ExternalResource> resources) {

        anyTO.setRealm(realmFullPath);

        anyTO.getAuxClasses().addAll(auxClasses.stream().map(AnyTypeClass::getKey).toList());

        plainAttrs.forEach(plainAttr -> anyTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrs.forEach((schema, value) -> anyTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        anyTO.getResources().addAll(resources.stream().map(ExternalResource::getKey).collect(Collectors.toSet()));
    }

    protected static RelationshipTO getRelationshipTO(
            final Collection<PlainAttr> plainAttrs,
            final Map<DerSchema, String> derAttrs,
            final String relationshipType,
            final RelationshipTO.End end,
            final Any otherEnd) {

        RelationshipTO relationshipTO = new RelationshipTO.Builder(relationshipType, end).otherEnd(
                otherEnd.getType().getKey(),
                otherEnd.getKey(),
                otherEnd instanceof User user
                        ? user.getUsername()
                        : otherEnd instanceof Group group
                                ? group.getName()
                                : ((AnyObject) otherEnd).getName()).
                build();

        plainAttrs.forEach(plainAttr -> relationshipTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrs.forEach((schema, value) -> relationshipTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        return relationshipTO;
    }

    protected static MembershipTO getMembershipTO(
            final Collection<PlainAttr> plainAttrs,
            final Map<DerSchema, String> derAttrs,
            final Membership<? extends Any> membership) {

        MembershipTO membershipTO = new MembershipTO.Builder(membership.getRightEnd().getKey()).
                groupName(membership.getRightEnd().getName()).build();

        plainAttrs.forEach(plainAttr -> membershipTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrs.forEach((schema, value) -> membershipTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        return membershipTO;
    }

    protected final AnyTypeDAO anyTypeDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final EntityFactory entityFactory;

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final OutboundMatcher outboundMatcher;

    protected AnyDataBinder(
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
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final JexlTools jexlTools) {

        super(plainSchemaDAO, validator, derAttrHandler, mappingManager, intAttrNameParser, jexlTools);
        this.anyTypeDAO = anyTypeDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.resourceDAO = resourceDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.entityFactory = entityFactory;
        this.anyUtilsFactory = anyUtilsFactory;
        this.outboundMatcher = outboundMatcher;
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

            MappingManager.PreparedAttrs prepared = mappingManager.prepareAttrsFromAny(
                    any, password, changePwdRes.contains(resource.getKey()), true, resource, provision);

            ConnObject connObjectTO;
            if (StringUtils.isBlank(prepared.connObjectLink())) {
                connObjectTO = ConnObjectUtils.getConnObjectTO(null, prepared.attributes());
            } else {
                ConnectorObject connectorObject = new ConnectorObjectBuilder().
                        addAttributes(prepared.attributes()).
                        addAttribute(new Uid(prepared.connObjectLink())).
                        addAttribute(AttributeBuilder.build(keyItem.getExtAttrName(), prepared.connObjectLink())).
                        build();

                connObjectTO = ConnObjectUtils.getConnObjectTO(
                        outboundMatcher.getFIQL(connectorObject, resource, provision),
                        connectorObject.getAttributes());
            }

            onResources.put(resource.getKey(), connObjectTO);
        })));

        return onResources;
    }

    protected List<String> evaluateMandatoryCondition(
            final ExternalResource resource, final Provision provision, final Any any) {

        List<String> missingAttrNames = new ArrayList<>();

        MappingUtils.getPropagationItems(provision.getMapping().getItems().stream()).forEach(item -> {
            IntAttrName intAttrName = null;
            try {
                intAttrName = intAttrNameParser.parse(item.getIntAttrName(), any.getType().getKind());
            } catch (ParseException e) {
                LOG.error("Invalid intAttrName '{}', ignoring", item.getIntAttrName(), e);
            }
            if (intAttrName != null && intAttrName.getSchemaInfo() != null) {
                AttrSchemaType schemaType = intAttrName.getSchemaInfo().schema() instanceof PlainSchema
                        ? intAttrName.getSchemaInfo().schema().getType()
                        : AttrSchemaType.String;

                MappingManager.IntValues intValues = mappingManager.getIntValues(
                        resource,
                        provision,
                        item,
                        intAttrName,
                        schemaType,
                        any,
                        AccountGetter.DEFAULT,
                        PlainAttrGetter.DEFAULT);
                if (intValues.values().isEmpty()
                        && jexlTools.evaluateMandatoryCondition(item.getMandatoryCondition(), any, derAttrHandler)) {

                    missingAttrNames.add(item.getIntAttrName());
                }
            }
        });

        return missingAttrNames;
    }

    protected SyncopeClientException checkMandatoryOnResources(
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

    protected SyncopeClientException checkMandatory(final Any any, final AnyUtils anyUtils) {
        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        // Check if there is some mandatory schema defined for which no value has been provided
        AllowedSchemas<PlainSchema> allowedPlainSchemas = anyUtils.dao().findAllowedSchemas(any, PlainSchema.class);
        allowedPlainSchemas.self().forEach(schema -> checkMandatory(
                schema, any.getPlainAttr(schema.getKey()).orElse(null), any, reqValMissing));
        if (any instanceof Groupable<?, ?, ?> groupable) {
            allowedPlainSchemas.memberships().forEach((group, schemas) -> {
                Membership<?> membership = groupable.getMembership(group.getKey()).orElse(null);
                schemas.forEach(schema -> checkMandatory(
                        schema,
                        groupable.getPlainAttr(schema.getKey(), membership).orElse(null),
                        any,
                        reqValMissing));
            });
        }
        if (any instanceof Relatable<?, ?> relatable) {
            allowedPlainSchemas.relationshipTypes().forEach((relationshipType, schemas) -> {
                List<? extends Relationship<?, ?>> rels = relatable.getRelationships(relationshipType.getKey());
                rels.forEach(rel -> schemas.forEach(schema -> checkMandatory(
                        schema,
                        relatable.getPlainAttr(schema.getKey(), rel).orElse(null),
                        any,
                        reqValMissing)));
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
            final Relatable<?, ?> any,
            final AnyUR anyUR,
            final PropagationByResource<String> propByRes,
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

        // 2. relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        for (RelationshipUR patch : anyUR.getRelationships().stream().
                filter(patch -> patch.getType() != null && patch.getOtherEndKey() != null).toList()) {

            RelationshipType relationshipType = relationshipTypeDAO.findById(patch.getType()).orElse(null);
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getType());
            } else {
                switch (patch.getOperation()) {
                    case DELETE -> {
                        any.getRelationship(relationshipType, patch.getOtherEndKey()).ifPresentOrElse(
                                relationship -> {
                                    anyUtils.remove(any, relationship);

                                    propByRes.addAll(
                                            ResourceOperation.UPDATE,
                                            anyObjectDAO.findAllResourceKeys(relationship.getRightEnd().getKey()));
                                },
                                () -> LOG.debug("No relationship ({},{},{}) was found, nothing to delete",
                                        anyTO.getKey(), relationshipType.getKey(), patch.getOtherEndKey()));
                    }

                    case ADD_REPLACE -> {
                        AnyObject otherEnd = anyObjectDAO.findById(patch.getOtherEndKey()).orElse(null);
                        if (otherEnd == null) {
                            LOG.debug("Ignoring invalid any object {}", patch.getOtherEndKey());
                        } else if (!relationshipType.getRightEndAnyType().equals(otherEnd.getType())) {
                            LOG.debug("Ignoring mismatching anyType {}", otherEnd.getType().getKey());
                        } else if (relationships.contains(Pair.of(relationshipType.getKey(), otherEnd.getKey()))) {
                            SyncopeClientException assigned =
                                    SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                            assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                                    + " in relationship " + relationshipType.getKey());
                            scce.addException(assigned);
                        } else {
                            relationships.add(Pair.of(relationshipType.getKey(), otherEnd.getKey()));

                            Relationship<?, ?> relationship = any.getRelationship(
                                    relationshipType, patch.getOtherEndKey()).orElse(null);
                            if (relationship == null) {
                                relationship = anyUtils.add(any, relationshipType, otherEnd);
                            } else {
                                any.getPlainAttrs(relationship).forEach(any::remove);
                            }

                            // relationship attributes
                            relationshipPlainAttrsOnUpdate(patch.getPlainAttrs(), anyTO, any, relationship, scce);

                            propByRes.addAll(
                                    ResourceOperation.UPDATE, anyObjectDAO.findAllResourceKeys(otherEnd.getKey()));
                        }
                    }

                    default -> {
                    }
                }
            }
        }

        // 3. resources
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

        // 4. attributes
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

        SyncopeClientException reqValMissing = checkMandatory(any, anyUtils);
        if (!reqValMissing.isEmpty()) {
            scce.addException(reqValMissing);
        }
        reqValMissing = checkMandatoryOnResources(any, resources);
        if (!reqValMissing.isEmpty()) {
            scce.addException(reqValMissing);
        }
    }

    protected void memberships(
            final Set<MembershipUR> memberships,
            final AnyTO anyTO,
            final Groupable<?, ?, ?> any,
            final Consumer<Group> groupProcessor,
            final PropagationByResource<String> propByRes,
            final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        Set<String> groups = new HashSet<>();
        memberships.stream().filter(patch -> patch.getGroup() != null).forEach(patch -> {
            switch (patch.getOperation()) {
                case DELETE -> {
                    any.getMembership(patch.getGroup()).ifPresentOrElse(
                            membership -> {
                                anyUtils.remove(any, membership);

                                propByRes.addAll(
                                        ResourceOperation.UPDATE,
                                        groupDAO.findAllResourceKeys(membership.getRightEnd().getKey()));
                            },
                            () -> LOG.debug("No membership ({},{}) was found, nothing to delete",
                                    anyTO.getKey(), patch.getGroup()));
                }

                case ADD_REPLACE -> {
                    Group group = groupDAO.findById(patch.getGroup()).orElse(null);
                    if (group == null) {
                        LOG.debug("Ignoring invalid group {}", patch.getGroup());
                    } else if (groups.contains(group.getKey())) {
                        LOG.error("Multiple patches for group {} of {} were found", group, any);

                        SyncopeClientException assigned =
                                SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                        assigned.getElements().add("Multiple patches for group " + group.getName() + " were found");
                        scce.addException(assigned);
                    } else {
                        groups.add(group.getKey());

                        Membership<?> membership = any.getMembership(patch.getGroup()).orElse(null);
                        if (membership == null) {
                            membership = anyUtils.add(any, group);
                        } else {
                            any.getPlainAttrs(membership).forEach(any::remove);
                        }

                        // membership attributes
                        membershipPlainAttrsOnUpdate(patch.getPlainAttrs(), anyTO, any, membership, scce);

                        propByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));

                        groupProcessor.accept(group);
                    }
                }

                default -> {
                }
            }
        });
    }

    protected void fill(
            final AnyTO anyTO,
            final Relatable<?, ?> any,
            final AnyCR anyCR,
            final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        // 0. aux classes
        any.getAuxClasses().clear();
        anyCR.getAuxClasses().stream().
                map(anyTypeClassDAO::findById).
                flatMap(Optional::stream).
                forEach(any::add);

        // 1. relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        anyCR.getRelationships().forEach(relationshipTO -> {
            RelationshipType relationshipType = relationshipTypeDAO.findById(relationshipTO.getType()).orElse(null);
            AnyObject otherEnd = anyObjectDAO.findById(relationshipTO.getOtherEndKey()).orElse(null);
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType());
            } else if (otherEnd == null) {
                LOG.debug("Ignoring invalid anyObject {}", relationshipTO.getOtherEndKey());
            } else if (!relationshipType.getRightEndAnyType().equals(otherEnd.getType())) {
                LOG.debug("Ignoring mismatching anyType {}", relationshipTO.getOtherEndType());
            } else if (relationshipTO.getEnd() == RelationshipTO.End.RIGHT) {
                SyncopeClientException noRight =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                noRight.getElements().add(
                        "Relationships shall be created or updated only from their left end");
                scce.addException(noRight);
            } else if (relationships.contains(Pair.of(otherEnd.getKey(), relationshipType.getKey()))) {
                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " in relationship " + relationshipTO.getType());
                scce.addException(assigned);
            } else {
                relationships.add(Pair.of(otherEnd.getKey(), relationshipType.getKey()));

                Relationship<?, ?> relationship = anyUtils.add(any, relationshipType, otherEnd);

                // relationship attributes
                relationshipPlainAttrsOnCreate(anyTO, any, relationship, relationshipTO, scce);
            }
        });

        // 2. attributes
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

        // 3. resources
        anyCR.getResources().forEach(resource -> resourceDAO.findById(resource).ifPresentOrElse(
                any::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", ExternalResource.class.getSimpleName(), resource)));

        requiredValuesMissing = checkMandatoryOnResources(any, anyUtils.getAllResources(any));
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }
    }

    protected void memberships(
            final List<MembershipTO> memberships,
            final AnyTO anyTO,
            final Groupable<?, ?, ?> any,
            final AnyUtils anyUtils,
            final SyncopeClientCompositeException scce) {

        Set<String> groups = new HashSet<>();
        memberships.forEach(membershipTO -> {
            Group group = membershipTO.getGroupKey() == null
                    ? groupDAO.findByName(membershipTO.getGroupName()).orElse(null)
                    : groupDAO.findById(membershipTO.getGroupKey()).orElse(null);
            if (group == null) {
                LOG.debug("Ignoring invalid group {}",
                        membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
            } else if (groups.contains(group.getKey())) {
                LOG.error("{} was already assigned to {}", group, any);

                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                assigned.getElements().add("Group " + group.getName() + " was already assigned");
                scce.addException(assigned);
            } else {
                groups.add(group.getKey());

                Membership<?> newMembership = anyUtils.add(any, group);

                // membership attributes
                membershipPlainAttrsOnCreate(anyTO, any, newMembership, membershipTO, scce);
            }
        });
    }

    protected void relationshipPlainAttrsOnCreate(
            final AnyTO anyTO,
            final Relatable<?, ?> any,
            final Relationship<?, ?> relationship,
            final RelationshipTO relationshipTO,
            final SyncopeClientCompositeException scce) {

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        relationshipTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = any.getPlainAttr(schema.getKey(), relationship).orElseGet(() -> {
                PlainAttr gpa = new PlainAttr();
                gpa.setRelationship(relationship.getKey());
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

    protected void membershipPlainAttrsOnCreate(
            final AnyTO anyTO,
            final Groupable<?, ?, ?> any,
            final Membership<?> membership,
            final MembershipTO membershipTO,
            final SyncopeClientCompositeException scce) {

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        membershipTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = any.getPlainAttr(schema.getKey(), membership).orElseGet(() -> {
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

    protected void relationshipPlainAttrsOnUpdate(
            final Set<Attr> plainAttrs,
            final AnyTO anyTO,
            final Relatable<?, ?> any,
            final Relationship<?, ?> relationship,
            final SyncopeClientCompositeException scce) {

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        plainAttrs.forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresentOrElse(
                schema -> any.getPlainAttr(schema.getKey(), relationship).ifPresentOrElse(
                        attr -> LOG.debug(
                                "Plain attribute found for {} and relationship {} with {}, nothing to do",
                                schema, relationship.getType().getKey(), relationship.getRightEnd()),
                        () -> {
                            LOG.debug("No plain attribute found for {} and relationship {} with {}",
                                    schema, relationship.getType().getKey(), relationship.getRightEnd());

                            PlainAttr newAttr = new PlainAttr();
                            newAttr.setRelationship(relationship.getKey());
                            newAttr.setPlainSchema(schema);
                            any.add(newAttr);

                            processAttrPatch(
                                    anyTO,
                                    any,
                                    new AttrPatch.Builder(attrTO).build(),
                                    schema,
                                    newAttr,
                                    invalidValues);
                        }),
                () -> LOG.debug("Invalid {}{}, ignoring...", PlainSchema.class.getSimpleName(), attrTO.getSchema())));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }
    }

    protected void membershipPlainAttrsOnUpdate(
            final Set<Attr> plainAttrs,
            final AnyTO anyTO,
            final Groupable<?, ?, ?> any,
            final Membership<?> membership,
            final SyncopeClientCompositeException scce) {

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        plainAttrs.forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresentOrElse(
                schema -> any.getPlainAttr(schema.getKey(), membership).ifPresentOrElse(
                        attr -> LOG.debug(
                                "Plain attribute found for {} and membership of {}, nothing to do",
                                schema, membership.getRightEnd()),
                        () -> {
                            LOG.debug("No plain attribute found for {} and membership of {}",
                                    schema, membership.getRightEnd());

                            PlainAttr newAttr = new PlainAttr();
                            newAttr.setMembership(membership.getKey());
                            newAttr.setPlainSchema(schema);
                            any.add(newAttr);

                            processAttrPatch(
                                    anyTO,
                                    any,
                                    new AttrPatch.Builder(attrTO).build(),
                                    schema,
                                    newAttr,
                                    invalidValues);
                        }),
                () -> LOG.debug("Invalid {}{}, ignoring...", PlainSchema.class.getSimpleName(), attrTO.getSchema())));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }
    }
}
