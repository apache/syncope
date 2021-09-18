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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class AnyObjectDataBinderImpl extends AbstractAnyDataBinder implements AnyObjectDataBinder {

    public AnyObjectDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final RealmDAO realmDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final PlainAttrDAO plainAttrDAO,
            final PlainAttrValueDAO plainAttrValueDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher) {

        super(anyTypeDAO,
                realmDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                plainAttrDAO,
                plainAttrValueDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final String key) {
        return getAnyObjectTO(anyObjectDAO.authFind(key), true);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final AnyObject anyObject, final boolean details) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();

        anyObjectTO.setCreator(anyObject.getCreator());
        anyObjectTO.setCreationDate(anyObject.getCreationDate());
        anyObjectTO.setCreationContext(anyObject.getCreationContext());
        anyObjectTO.setLastModifier(anyObject.getLastModifier());
        anyObjectTO.setLastChangeDate(anyObject.getLastChangeDate());
        anyObjectTO.setLastChangeContext(anyObject.getLastChangeContext());

        anyObjectTO.setKey(anyObject.getKey());
        anyObjectTO.setName(anyObject.getName());
        anyObjectTO.setType(anyObject.getType().getKey());
        anyObjectTO.setStatus(anyObject.getStatus());

        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHandler.getValues(anyObject)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(anyObjectTO, anyObject.getRealm().getFullPath(),
                anyObject.getAuxClasses(),
                anyObject.getPlainAttrs(),
                derAttrHandler.getValues(anyObject),
                virAttrValues,
                anyObjectDAO.findAllResources(anyObject));

        // dynamic realms
        anyObjectTO.getDynRealms().addAll(anyObjectDAO.findDynRealms(anyObject.getKey()));

        if (details) {
            // relationships
            anyObjectTO.getRelationships().addAll(
                    anyObjectDAO.findAllRelationships(anyObject).stream().
                            map(relationship -> getRelationshipTO(
                            relationship.getType().getKey(),
                            relationship.getLeftEnd().getKey().equals(anyObject.getKey())
                            ? relationship.getRightEnd()
                            : anyObject)).
                            collect(Collectors.toList()));

            // memberships
            anyObjectTO.getMemberships().addAll(
                    anyObject.getMemberships().stream().map(membership -> getMembershipTO(
                    anyObject.getPlainAttrs(membership),
                    derAttrHandler.getValues(anyObject, membership),
                    virAttrHandler.getValues(anyObject, membership),
                    membership)).collect(Collectors.toList()));

            // dynamic memberships
            anyObjectTO.getDynMemberships().addAll(
                    anyObjectDAO.findDynGroups(anyObject.getKey()).stream().
                            map(group -> new MembershipTO.Builder(group.getKey()).groupName(group.getName()).build()).
                            collect(Collectors.toList()));
        }

        return anyObjectTO;
    }

    @Override
    public void create(final AnyObject anyObject, final AnyObjectCR anyObjectCR) {
        AnyType type = anyTypeDAO.find(anyObjectCR.getType());
        if (type == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(anyObjectCR.getType());
            throw sce;
        }
        anyObject.setType(type);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (anyObjectCR.getName() == null) {
            LOG.error("No name specified for this anyObject");

            invalidGroups.getElements().add("No name specified for this anyObject");
        } else {
            anyObject.setName(anyObjectCR.getName());
        }

        // realm
        Realm realm = realmDAO.findByFullPath(anyObjectCR.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + anyObjectCR.getRealm());
            scce.addException(noRealm);
        }
        anyObject.setRealm(realm);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        anyObjectCR.getRelationships().forEach(relationshipTO -> {
            if (StringUtils.isBlank(relationshipTO.getOtherEndType())
                    || AnyTypeKind.USER.name().equals(relationshipTO.getOtherEndType())
                    || AnyTypeKind.GROUP.name().equals(relationshipTO.getOtherEndType())) {

                SyncopeClientException invalidAnyType =
                        SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
                invalidAnyType.getElements().add(AnyType.class.getSimpleName()
                        + " not allowed for relationship: " + relationshipTO.getOtherEndType());
                scce.addException(invalidAnyType);
            } else {
                AnyObject otherEnd = anyObjectDAO.find(relationshipTO.getOtherEndKey());
                if (otherEnd == null) {
                    LOG.debug("Ignoring invalid anyObject " + relationshipTO.getOtherEndKey());
                } else if (relationships.contains(Pair.of(otherEnd.getKey(), relationshipTO.getType()))) {
                    LOG.error("{} was already in relationship {} with {}",
                            otherEnd, relationshipTO.getType(), anyObject);

                    SyncopeClientException assigned =
                            SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                    assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                            + " in relationship " + relationshipTO.getType());
                    scce.addException(assigned);
                } else if (anyObject.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                    relationships.add(Pair.of(otherEnd.getKey(), relationshipTO.getType()));

                    RelationshipType relationshipType = relationshipTypeDAO.find(relationshipTO.getType());
                    if (relationshipType == null) {
                        LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType());
                    } else {
                        ARelationship relationship = entityFactory.newEntity(ARelationship.class);
                        relationship.setType(relationshipType);
                        relationship.setRightEnd(otherEnd);
                        relationship.setLeftEnd(anyObject);

                        anyObject.add(relationship);
                    }
                } else {
                    LOG.error("{} cannot be related to {}", otherEnd, anyObject);

                    SyncopeClientException unrelatable =
                            SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                    unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                            + " cannot be related");
                    scce.addException(unrelatable);
                }
            }
        });

        // memberships
        Set<String> groups = new HashSet<>();
        anyObjectCR.getMemberships().forEach(membershipTO -> {
            Group group = membershipTO.getGroupKey() == null
                    ? groupDAO.findByName(membershipTO.getGroupName())
                    : groupDAO.find(membershipTO.getGroupKey());
            if (group == null) {
                LOG.debug("Ignoring invalid group "
                        + membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
            } else if (groups.contains(group.getKey())) {
                LOG.error("{} was already assigned to {}", group, anyObject);

                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                assigned.getElements().add("Group " + group.getName() + " was already assigned");
                scce.addException(assigned);
            } else if (anyObject.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                groups.add(group.getKey());

                AMembership membership = entityFactory.newEntity(AMembership.class);
                membership.setRightEnd(group);
                membership.setLeftEnd(anyObject);

                anyObject.add(membership);

                // membership attributes
                fill(anyObject, membership, membershipTO, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce);
            } else {
                LOG.error("{} cannot be assigned to {}", group, anyObject);

                SyncopeClientException unassignable =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                unassignable.getElements().add("Group " + group.getName() + " cannot be assigned");
                scce.addException(unassignable);
            }
        });

        // attributes and resources
        fill(anyObject, anyObjectCR, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource<String> update(final AnyObject toBeUpdated, final AnyObjectUR anyObjectUR) {
        // Re-merge any pending change from workflow tasks
        AnyObject anyObject = anyObjectDAO.save(toBeUpdated);

        PropagationByResource<String> propByRes = new PropagationByResource<>();

        // Save projection on Resources (before update)
        Map<String, ConnObjectTO> beforeOnResources =
                onResources(anyObject, anyObjectDAO.findAllResourceKeys(anyObject.getKey()), null, false);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);

        // realm
        setRealm(anyObject, anyObjectUR);

        // name
        if (anyObjectUR.getName() != null && StringUtils.isNotBlank(anyObjectUR.getName().getValue())) {
            anyObject.setName(anyObjectUR.getName().getValue());
        }

        // attributes and resources
        fill(anyObject, anyObjectUR, anyUtils, scce);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        anyObjectUR.getRelationships().stream().
                filter(patch -> patch.getRelationshipTO() != null).forEach(patch -> {

            RelationshipType relationshipType = relationshipTypeDAO.find(patch.getRelationshipTO().getType());
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
            } else {
                anyObject.getRelationship(relationshipType, patch.getRelationshipTO().getOtherEndKey()).
                        ifPresent(relationship -> {
                            anyObject.getRelationships().remove(relationship);
                            relationship.setLeftEnd(null);
                        });

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    if (StringUtils.isBlank(patch.getRelationshipTO().getOtherEndType())
                            || AnyTypeKind.USER.name().equals(patch.getRelationshipTO().getOtherEndType())
                            || AnyTypeKind.GROUP.name().equals(patch.getRelationshipTO().getOtherEndType())) {

                        SyncopeClientException invalidAnyType =
                                SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
                        invalidAnyType.getElements().add(AnyType.class.getSimpleName()
                                + " not allowed for relationship: " + patch.getRelationshipTO().getOtherEndType());
                        scce.addException(invalidAnyType);
                    } else {
                        AnyObject otherEnd = anyObjectDAO.find(patch.getRelationshipTO().getOtherEndKey());
                        if (otherEnd == null) {
                            LOG.debug("Ignoring invalid any object {}", patch.getRelationshipTO().getOtherEndKey());
                        } else if (relationships.contains(
                                Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()))) {

                            LOG.error("{} was already in relationship {} with {}",
                                    anyObject, patch.getRelationshipTO().getType(), otherEnd);

                            SyncopeClientException assigned =
                                    SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                            assigned.getElements().add("AnyObject was already in relationship "
                                    + patch.getRelationshipTO().getType() + " with "
                                    + otherEnd.getType().getKey() + " " + otherEnd.getName());
                            scce.addException(assigned);
                        } else if (anyObject.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                            relationships.add(Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()));

                            ARelationship newRelationship = entityFactory.newEntity(ARelationship.class);
                            newRelationship.setType(relationshipType);
                            newRelationship.setRightEnd(otherEnd);
                            newRelationship.setLeftEnd(anyObject);

                            anyObject.add(newRelationship);
                        } else {
                            LOG.error("{} cannot be related to {}", otherEnd, anyObject);

                            SyncopeClientException unrelatable =
                                    SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                            unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                                    + " cannot be related");
                            scce.addException(unrelatable);
                        }
                    }
                }
            }
        });

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // memberships
        Set<String> groups = new HashSet<>();
        anyObjectUR.getMemberships().stream().filter(patch -> patch.getGroup() != null).forEach(patch -> {
            anyObject.getMembership(patch.getGroup()).ifPresent(membership -> {
                anyObject.remove(membership);
                membership.setLeftEnd(null);
                anyObject.getPlainAttrs(membership).forEach(attr -> {
                    anyObject.remove(attr);
                    attr.setOwner(null);
                    attr.setMembership(null);
                    plainAttrValueDAO.deleteAll(attr, anyUtils);
                });

                if (patch.getOperation() == PatchOperation.DELETE) {
                    propByRes.addAll(
                            ResourceOperation.UPDATE,
                            groupDAO.findAllResourceKeys((membership.getRightEnd().getKey())));
                }
            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.find(patch.getGroup());
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", patch.getGroup());
                } else if (groups.contains(group.getKey())) {
                    LOG.error("Multiple patches for group {} of {} were found", group, anyObject);

                    SyncopeClientException assigned =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    assigned.getElements().add("Multiple patches for group " + group.getName() + " were found");
                    scce.addException(assigned);
                } else if (anyObject.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    groups.add(group.getKey());

                    AMembership newMembership = entityFactory.newEntity(AMembership.class);
                    newMembership.setRightEnd(group);
                    newMembership.setLeftEnd(anyObject);

                    anyObject.add(newMembership);

                    patch.getPlainAttrs().forEach(attrTO -> {
                        PlainSchema schema = getPlainSchema(attrTO.getSchema());
                        if (schema == null) {
                            LOG.debug("Invalid " + PlainSchema.class.getSimpleName()
                                    + "{}, ignoring...", attrTO.getSchema());
                        } else {
                            Optional<? extends APlainAttr> attr =
                                    anyObject.getPlainAttr(schema.getKey(), newMembership);
                            if (attr.isEmpty()) {
                                LOG.debug("No plain attribute found for {} and membership of {}",
                                        schema, newMembership.getRightEnd());

                                APlainAttr newAttr = anyUtils.newPlainAttr();
                                newAttr.setOwner(anyObject);
                                newAttr.setMembership(newMembership);
                                newAttr.setSchema(schema);
                                anyObject.add(newAttr);

                                processAttrPatch(
                                        anyObject,
                                        new AttrPatch.Builder(attrTO).build(),
                                        schema,
                                        newAttr,
                                        anyUtils,
                                        invalidValues);
                            }
                        }
                    });
                    if (!invalidValues.isEmpty()) {
                        scce.addException(invalidValues);
                    }

                    propByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));
                } else {
                    LOG.error("{} cannot be assigned to {}", group, anyObject);

                    SyncopeClientException unassignable =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignable.getElements().add("Group " + group.getName() + " cannot be assigned");
                    scce.addException(unassignable);
                }
            }
        });

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        AnyObject saved = anyObjectDAO.save(anyObject);

        // Build final information for next stage (propagation)
        propByRes.merge(propByRes(
                beforeOnResources,
                onResources(saved, anyObjectDAO.findAllResourceKeys(anyObject.getKey()), null, false)));
        return propByRes;
    }
}
