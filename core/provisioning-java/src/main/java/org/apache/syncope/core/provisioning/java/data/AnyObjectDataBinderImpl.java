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
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.RelationshipPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class AnyObjectDataBinderImpl extends AbstractAnyDataBinder implements AnyObjectDataBinder {

    private static final String[] IGNORE_PROPERTIES = {
        "type", "realm", "auxClasses", "relationships", "memberships", "dynGroups",
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

        Map<DerSchema, String> derAttrValues = derAttrHandler.getValues(anyObject);
        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHander.getValues(anyObject)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(anyObjectTO, anyObject.getRealm().getFullPath(), anyObject.getAuxClasses(),
                anyObject.getPlainAttrs(), derAttrValues, virAttrValues, anyObjectDAO.findAllResources(anyObject));

        if (details) {
            // relationships
            CollectionUtils.collect(anyObject.getRelationships(), new Transformer<ARelationship, RelationshipTO>() {

                @Override
                public RelationshipTO transform(final ARelationship relationship) {
                    return AnyObjectDataBinderImpl.this.getRelationshipTO(relationship);
                }

            }, anyObjectTO.getRelationships());

            // memberships
            CollectionUtils.collect(anyObject.getMemberships(), new Transformer<AMembership, MembershipTO>() {

                @Override
                public MembershipTO transform(final AMembership membership) {
                    return AnyObjectDataBinderImpl.this.getMembershipTO(membership);
                }
            }, anyObjectTO.getMemberships());

            // dynamic memberships
            CollectionUtils.collect(anyObjectDAO.findDynGroupMemberships(anyObject),
                    EntityUtils.<Group>keyTransformer(), anyObjectTO.getDynGroups());
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

        // realm
        Realm realm = realmDAO.findByFullPath(anyObjectTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + anyObjectTO.getRealm());
            scce.addException(noRealm);
        }
        anyObject.setRealm(realm);

        if (anyObject.getRealm() != null) {
            AssignableCond assignableCond = new AssignableCond();
            assignableCond.setRealmFullPath(anyObject.getRealm().getFullPath());

            // relationships
            List<AnyObject> assignableAnyObjects =
                    searchDAO.search(SearchCond.getLeafCond(assignableCond), AnyTypeKind.ANY_OBJECT);

            for (RelationshipTO relationshipTO : anyObjectTO.getRelationships()) {
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
                    } else if (assignableAnyObjects.contains(otherEnd)) {
                        RelationshipType relationshipType = relationshipTypeDAO.find(relationshipTO.getType());
                        if (relationshipType == null) {
                            LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType());
                        } else {
                            ARelationship relationship = entityFactory.newEntity(ARelationship.class);
                            relationship.setType(relationshipType);
                            relationship.setRightEnd(anyObject);
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
            }

            // memberships
            List<Group> assignableGroups =
                    searchDAO.search(SearchCond.getLeafCond(assignableCond), AnyTypeKind.GROUP);

            for (MembershipTO membershipTO : anyObjectTO.getMemberships()) {
                Group group = groupDAO.find(membershipTO.getRightKey());
                if (group == null) {
                    LOG.debug("Ignoring invalid group " + membershipTO.getGroupName());
                } else if (assignableGroups.contains(group)) {
                    AMembership membership = entityFactory.newEntity(AMembership.class);
                    membership.setRightEnd(group);
                    membership.setLeftEnd(anyObject);

                    anyObject.add(membership);
                } else {
                    LOG.error("{} cannot be assigned to {}", group, anyObject);

                    SyncopeClientException unassignabled =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignabled.getElements().add("Cannot be assigned: " + group);
                    scce.addException(unassignabled);
                }
            }
        }

        // attributes, derived attributes, virtual attributes and resources
        fill(anyObject, anyObjectTO, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource update(final AnyObject toBeUpdated, final AnyObjectPatch anyObjectPatch) {
        // Re-merge any pending change from workflow tasks
        final AnyObject anyObject = anyObjectDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Collection<String> currentResources = anyObjectDAO.findAllResourceNames(anyObject);

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(anyObject);

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(anyObject, anyObjectPatch, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce));

        Set<String> toBeDeprovisioned = new HashSet<>();
        Set<String> toBeProvisioned = new HashSet<>();

        // relationships
        List<AnyObject> assignableAnyObjects =
                searchDAO.searchAssignable(anyObject.getRealm().getFullPath(), AnyTypeKind.ANY_OBJECT);

        for (RelationshipPatch patch : anyObjectPatch.getRelationships()) {
            if (patch.getRelationshipTO() != null) {
                RelationshipType relationshipType = relationshipTypeDAO.find(patch.getRelationshipTO().getType());
                if (relationshipType == null) {
                    LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
                } else {
                    ARelationship relationship =
                            anyObject.getRelationship(relationshipType, patch.getRelationshipTO().getRightKey());
                    if (relationship != null) {
                        anyObject.getRelationships().remove(relationship);
                        toBeDeprovisioned.addAll(relationship.getRightEnd().getResourceNames());
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
                            } else if (assignableAnyObjects.contains(otherEnd)) {
                                relationship = entityFactory.newEntity(ARelationship.class);
                                relationship.setType(relationshipType);
                                relationship.setRightEnd(otherEnd);
                                relationship.setLeftEnd(anyObject);

                                anyObject.add(relationship);

                                toBeProvisioned.addAll(otherEnd.getResourceNames());
                            } else {
                                LOG.error("{} cannot be assigned to {}", otherEnd, anyObject);

                                SyncopeClientException unassignabled =
                                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                                unassignabled.getElements().add("Cannot be assigned: " + otherEnd);
                                scce.addException(unassignabled);
                            }
                        }
                    }
                }
            }
        }

        // memberships
        List<Group> assignableGroups =
                searchDAO.searchAssignable(anyObject.getRealm().getFullPath(), AnyTypeKind.GROUP);

        for (MembershipPatch patch : anyObjectPatch.getMemberships()) {
            if (patch.getMembershipTO() != null) {
                AMembership membership = anyObject.getMembership(patch.getMembershipTO().getRightKey());
                if (membership != null) {
                    anyObject.getMemberships().remove(membership);
                    toBeDeprovisioned.addAll(membership.getRightEnd().getResourceNames());
                }

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    Group group = groupDAO.find(patch.getMembershipTO().getRightKey());
                    if (group == null) {
                        LOG.debug("Ignoring invalid group {}", patch.getMembershipTO().getRightKey());
                    } else if (assignableGroups.contains(group)) {
                        membership = entityFactory.newEntity(AMembership.class);
                        membership.setRightEnd(group);
                        membership.setLeftEnd(anyObject);

                        anyObject.add(membership);

                        toBeProvisioned.addAll(group.getResourceNames());
                    } else {
                        LOG.error("{} cannot be assigned to {}", group, anyObject);

                        SyncopeClientException unassignabled =
                                SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                        unassignabled.getElements().add("Cannot be assigned: " + group);
                        scce.addException(unassignabled);
                    }
                }
            }
        }

        propByRes.addAll(ResourceOperation.DELETE, toBeDeprovisioned);
        propByRes.addAll(ResourceOperation.UPDATE, toBeProvisioned);

        /**
         * In case of new memberships all the current resources have to be updated in order to propagate new group and
         * membership attribute values.
         */
        if (!toBeDeprovisioned.isEmpty() || !toBeProvisioned.isEmpty()) {
            currentResources.removeAll(toBeDeprovisioned);
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // check if some connObjectKey was changed by the update above
        Map<String, String> newcCnnObjectKeys = getConnObjectKeys(anyObject);
        for (Map.Entry<String, String> entry : oldConnObjectKeys.entrySet()) {
            if (newcCnnObjectKeys.containsKey(entry.getKey())
                    && !entry.getValue().equals(newcCnnObjectKeys.get(entry.getKey()))) {

                propByRes.addOldConnObjectKey(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return propByRes;
    }

}
