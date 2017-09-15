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
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class AnyObjectDataBinderImpl extends AbstractAnyDataBinder implements AnyObjectDataBinder {

    private static final String[] IGNORE_PROPERTIES = {
        "type", "realm", "auxClasses", "relationships", "memberships", "dynMemberships",
        "plainAttrs", "derAttrs", "virAttrs", "resources"
    };

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final String key) {
        return getAnyObjectTO(anyObjectDAO.authFind(key), true);
    }

    @Override
    public AnyObjectTO getAnyObjectTO(final AnyObject anyObject, final boolean details) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setType(anyObject.getType().getKey());

        BeanUtils.copyProperties(anyObject, anyObjectTO, IGNORE_PROPERTIES);

        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHandler.getValues(anyObject)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(anyObjectTO, anyObject.getRealm().getFullPath(),
                anyObject.getAuxClasses(),
                anyObject.getPlainAttrs(),
                derAttrHandler.getValues(anyObject),
                virAttrValues,
                anyObjectDAO.findAllResources(anyObject),
                details);

        if (details) {
            // dynamic realms
            anyObjectTO.getDynRealms().addAll(userDAO.findDynRealms(anyObject.getKey()));

            // relationships
            anyObjectTO.getRelationships().addAll(
                    anyObject.getRelationships().stream().map(relationship -> getRelationshipTO(relationship)).
                            collect(Collectors.toList()));

            // memberships
            anyObjectTO.getMemberships().addAll(
                    anyObject.getMemberships().stream().map(membership -> {
                        return getMembershipTO(
                                anyObject.getPlainAttrs(membership),
                                derAttrHandler.getValues(anyObject, membership),
                                virAttrHandler.getValues(anyObject, membership),
                                membership);
                    }).collect(Collectors.toList()));

            // dynamic memberships
            anyObjectTO.getDynMemberships().addAll(
                    anyObjectDAO.findDynGroups(anyObject.getKey()).stream().map(group -> {
                        return new MembershipTO.Builder().
                                group(group.getKey(), group.getName()).
                                build();
                    }).collect(Collectors.toList()));
        }

        return anyObjectTO;
    }

    @Override
    public void create(final AnyObject anyObject, final AnyObjectTO anyObjectTO) {
        AnyType type = anyTypeDAO.find(anyObjectTO.getType());
        if (type == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(anyObjectTO.getType());
            throw sce;
        }
        anyObject.setType(type);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (anyObjectTO.getName() == null) {
            LOG.error("No name specified for this anyObject");

            invalidGroups.getElements().add("No name specified for this anyObject");
        } else {
            anyObject.setName(anyObjectTO.getName());
        }

        // realm
        Realm realm = realmDAO.findByFullPath(anyObjectTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + anyObjectTO.getRealm());
            scce.addException(noRealm);
        }
        anyObject.setRealm(realm);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);
        if (anyObject.getRealm() != null) {
            // relationships
            anyObjectTO.getRelationships().forEach(relationshipTO -> {
                if (StringUtils.isBlank(relationshipTO.getRightType())
                        || AnyTypeKind.USER.name().equals(relationshipTO.getRightType())
                        || AnyTypeKind.GROUP.name().equals(relationshipTO.getRightType())) {

                    SyncopeClientException invalidAnyType =
                            SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
                    invalidAnyType.getElements().add(AnyType.class.getSimpleName()
                            + " not allowed for relationship: " + relationshipTO.getRightType());
                    scce.addException(invalidAnyType);
                } else {
                    AnyObject otherEnd = anyObjectDAO.find(relationshipTO.getRightKey());
                    if (otherEnd == null) {
                        LOG.debug("Ignoring invalid anyObject " + relationshipTO.getRightKey());
                    } else if (anyObject.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
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
                        LOG.error("{} cannot be assigned to {}", otherEnd, anyObject);

                        SyncopeClientException unassignabled =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        unassignabled.getElements().add("Cannot be assigned: " + otherEnd);
                        scce.addException(unassignabled);
                    }
                }
            });

            // memberships
            anyObjectTO.getMemberships().forEach(membershipTO -> {
                Group group = membershipTO.getRightKey() == null
                        ? groupDAO.findByName(membershipTO.getGroupName())
                        : groupDAO.find(membershipTO.getRightKey());
                if (group == null) {
                    LOG.debug("Ignoring invalid group "
                            + membershipTO.getRightKey() + " / " + membershipTO.getGroupName());
                } else if (anyObject.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    AMembership membership = entityFactory.newEntity(AMembership.class);
                    membership.setRightEnd(group);
                    membership.setLeftEnd(anyObject);

                    anyObject.add(membership);

                    // membership attributes
                    fill(anyObject, membership, membershipTO, anyUtils, scce);
                } else {
                    LOG.error("{} cannot be assigned to {}", group, anyObject);

                    SyncopeClientException unassignable =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignable.getElements().add("Cannot be assigned: " + group);
                    scce.addException(unassignable);
                }
            });
        }

        // attributes and resources
        fill(anyObject, anyObjectTO, anyUtils, scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource update(final AnyObject toBeUpdated, final AnyObjectPatch anyObjectPatch) {
        // Re-merge any pending change from workflow tasks
        AnyObject anyObject = anyObjectDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);

        Collection<String> currentResources = anyObjectDAO.findAllResourceKeys(anyObject.getKey());

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(anyObject, anyUtils);

        // realm
        setRealm(anyObject, anyObjectPatch);

        // name
        if (anyObjectPatch.getName() != null && StringUtils.isNotBlank(anyObjectPatch.getName().getValue())) {
            propByRes.addAll(ResourceOperation.UPDATE, anyObjectDAO.findAllResourceKeys(anyObject.getKey()));

            anyObject.setName(anyObjectPatch.getName().getValue());
        }

        // attributes and resources
        propByRes.merge(fill(anyObject, anyObjectPatch, anyUtils, scce));

        Set<String> toBeDeprovisioned = new HashSet<>();
        Set<String> toBeProvisioned = new HashSet<>();

        // relationships
        anyObjectPatch.getRelationships().stream().
                filter(patch -> patch.getRelationshipTO() != null).forEachOrdered((patch) -> {
            RelationshipType relationshipType = relationshipTypeDAO.find(patch.getRelationshipTO().getType());
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
            } else {
                Optional<? extends ARelationship> relationship =
                        anyObject.getRelationship(relationshipType, patch.getRelationshipTO().getRightKey());
                if (relationship.isPresent()) {
                    anyObject.getRelationships().remove(relationship.get());
                    relationship.get().setLeftEnd(null);

                    toBeDeprovisioned.addAll(
                            anyObjectDAO.findAllResourceKeys(relationship.get().getRightEnd().getKey()));
                }

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    if (StringUtils.isBlank(patch.getRelationshipTO().getRightType())
                            || AnyTypeKind.USER.name().equals(patch.getRelationshipTO().getRightType())
                            || AnyTypeKind.GROUP.name().equals(patch.getRelationshipTO().getRightType())) {

                        SyncopeClientException invalidAnyType =
                                SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
                        invalidAnyType.getElements().add(AnyType.class.getSimpleName()
                                + " not allowed for relationship: " + patch.getRelationshipTO().getRightType());
                        scce.addException(invalidAnyType);
                    } else {
                        AnyObject otherEnd = anyObjectDAO.find(patch.getRelationshipTO().getRightKey());
                        if (otherEnd == null) {
                            LOG.debug("Ignoring invalid any object {}", patch.getRelationshipTO().getRightKey());
                        } else if (anyObject.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                            ARelationship newRelationship = entityFactory.newEntity(ARelationship.class);
                            newRelationship.setType(relationshipType);
                            newRelationship.setRightEnd(otherEnd);
                            newRelationship.setLeftEnd(anyObject);

                            anyObject.add(newRelationship);

                            toBeProvisioned.addAll(anyObjectDAO.findAllResourceKeys(otherEnd.getKey()));
                        } else {
                            LOG.error("{} cannot be assigned to {}", otherEnd, anyObject);

                            SyncopeClientException unassignable =
                                    SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                            unassignable.getElements().add("Cannot be assigned: " + otherEnd);
                            scce.addException(unassignable);
                        }
                    }
                }
            }
        });

        Collection<ExternalResource> resources = anyObjectDAO.findAllResources(anyObject);
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // memberships
        anyObjectPatch.getMemberships().stream().
                filter((membPatch) -> (membPatch.getGroup() != null)).forEachOrdered(membPatch -> {
            Optional<? extends AMembership> membership = anyObject.getMembership(membPatch.getGroup());
            if (membership.isPresent()) {
                anyObject.getMemberships().remove(membership.get());
                membership.get().setLeftEnd(null);
                anyObject.getPlainAttrs(membership.get()).forEach(attr -> {
                    anyObject.remove(attr);
                    attr.setOwner(null);
                });

                if (membPatch.getOperation() == PatchOperation.DELETE) {
                    toBeDeprovisioned.addAll(groupDAO.findAllResourceKeys(membership.get().getRightEnd().getKey()));
                }
            }
            if (membPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.find(membPatch.getGroup());
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", membPatch.getGroup());
                } else if (anyObject.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    AMembership newMembership = entityFactory.newEntity(AMembership.class);
                    newMembership.setRightEnd(group);
                    newMembership.setLeftEnd(anyObject);

                    anyObject.add(newMembership);

                    membPatch.getPlainAttrs().forEach(attrTO -> {
                        PlainSchema schema = getPlainSchema(attrTO.getSchema());
                        if (schema == null) {
                            LOG.debug("Invalid " + PlainSchema.class.getSimpleName()
                                    + "{}, ignoring...", attrTO.getSchema());
                        } else {
                            Optional<? extends APlainAttr> attr =
                                    anyObject.getPlainAttr(schema.getKey(), newMembership);
                            if (!attr.isPresent()) {
                                LOG.debug("No plain attribute found for {} and membership of {}",
                                        schema, newMembership.getRightEnd());

                                APlainAttr newAttr = anyUtils.newPlainAttr();
                                newAttr.setOwner(anyObject);
                                newAttr.setMembership(newMembership);
                                newAttr.setSchema(schema);
                                anyObject.add(newAttr);

                                AttrPatch patch = new AttrPatch.Builder().attrTO(attrTO).build();
                                processAttrPatch(
                                        anyObject, patch, schema, newAttr, anyUtils,
                                        resources, propByRes, invalidValues);
                            }
                        }
                    });
                    if (!invalidValues.isEmpty()) {
                        scce.addException(invalidValues);
                    }

                    toBeProvisioned.addAll(groupDAO.findAllResourceKeys(group.getKey()));
                } else {
                    LOG.error("{} cannot be assigned to {}", group, anyObject);

                    SyncopeClientException unassignabled =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignabled.getElements().add("Cannot be assigned: " + group);
                    scce.addException(unassignabled);
                }
            }
        });

        propByRes.addAll(ResourceOperation.DELETE, toBeDeprovisioned);
        propByRes.addAll(ResourceOperation.UPDATE, toBeProvisioned);

        // In case of new memberships all current resources need to be updated in order to propagate new group
        // attribute values.
        if (!toBeDeprovisioned.isEmpty() || !toBeProvisioned.isEmpty()) {
            currentResources.removeAll(toBeDeprovisioned);
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // check if some connObjectKey was changed by the update above
        Map<String, String> newcCnnObjectKeys = getConnObjectKeys(anyObject, anyUtils);
        oldConnObjectKeys.entrySet().stream().
                filter(entry -> newcCnnObjectKeys.containsKey(entry.getKey())
                && !entry.getValue().equals(newcCnnObjectKeys.get(entry.getKey()))).
                forEach(entry -> {
                    propByRes.addOldConnObjectKey(entry.getKey(), entry.getValue());
                    propByRes.add(ResourceOperation.UPDATE, entry.getKey());
                });

        Pair<Set<String>, Set<String>> dynGroupMembs = anyObjectDAO.saveAndGetDynGroupMembs(anyObject);

        // finally check if any resource assignment is to be processed due to dynamic group membership change
        dynGroupMembs.getLeft().stream().
                filter(group -> !dynGroupMembs.getRight().contains(group)).
                forEach(delete -> {
                    groupDAO.find(delete).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.DELETE, resource.getKey());
                            });
                });
        dynGroupMembs.getLeft().stream().
                filter(group -> dynGroupMembs.getRight().contains(group)).
                forEach(update -> {
                    groupDAO.find(update).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.UPDATE, resource.getKey());
                            });
                });
        dynGroupMembs.getRight().stream().
                filter(group -> !dynGroupMembs.getLeft().contains(group)).
                forEach(create -> {
                    groupDAO.find(create).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.CREATE, resource.getKey());
                            });
                });

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return propByRes;
    }

}
