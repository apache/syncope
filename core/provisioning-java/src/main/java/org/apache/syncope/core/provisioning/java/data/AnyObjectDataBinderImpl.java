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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
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
    public AnyObjectTO getAnyObjectTO(final Long key) {
        return getAnyObjectTO(anyObjectDAO.authFind(key), true);
    }

    @Override
    public AnyObjectTO getAnyObjectTO(final AnyObject anyObject, final boolean details) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setType(anyObject.getType().getKey());

        BeanUtils.copyProperties(anyObject, anyObjectTO, IGNORE_PROPERTIES);

        if (details) {
            virAttrHander.retrieveVirAttrValues(anyObject);
        }

        fillTO(anyObjectTO, anyObject.getRealm().getFullPath(), anyObject.getAuxClasses(),
                anyObject.getPlainAttrs(), anyObject.getDerAttrs(), anyObject.getVirAttrs(),
                anyObjectDAO.findAllResources(anyObject));

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
            CollectionUtils.collect(anyObjectDAO.findDynGroupMemberships(anyObject), new Transformer<Group, Long>() {

                @Override
                public Long transform(final Group group) {
                    return group.getKey();
                }
            }, anyObjectTO.getDynGroups());
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

        // relationships
        for (RelationshipTO relationshipTO : anyObjectTO.getRelationships()) {
            AnyObject otherEnd = anyObjectDAO.find(relationshipTO.getRightKey());

            if (otherEnd == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid anyObject " + relationshipTO.getRightKey());
                }
            } else {
                ARelationship relationship = null;
                if (anyObject.getKey() != null) {
                    relationship = anyObject.getRelationship(otherEnd.getKey());
                }
                if (relationship == null) {
                    relationship = entityFactory.newEntity(ARelationship.class);
                    relationship.setRightEnd(otherEnd);
                    relationship.setLeftEnd(anyObject);

                    anyObject.add(relationship);
                }
            }
        }

        // memberships
        for (MembershipTO membershipTO : anyObjectTO.getMemberships()) {
            Group group = groupDAO.find(membershipTO.getRightKey());

            if (group == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid group " + membershipTO.getGroupName());
                }
            } else {
                AMembership membership = null;
                if (anyObject.getKey() != null) {
                    membership = anyObject.getMembership(group.getKey());
                }
                if (membership == null) {
                    membership = entityFactory.newEntity(AMembership.class);
                    membership.setRightEnd(group);
                    membership.setLeftEnd(anyObject);

                    anyObject.add(membership);
                }
            }
        }

        // realm, attributes, derived attributes, virtual attributes and resources
        fill(anyObject, anyObjectTO, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce);
    }

    @Override
    public PropagationByResource update(final AnyObject toBeUpdated, final AnyObjectMod anyObjectMod) {
        // Re-merge any pending change from workflow tasks
        final AnyObject anyObject = anyObjectDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Collection<String> currentResources = anyObjectDAO.findAllResourceNames(anyObject);

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(anyObject);

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(anyObject, anyObjectMod, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce));

        Set<String> toBeDeprovisioned = new HashSet<>();
        Set<String> toBeProvisioned = new HashSet<>();

        // relationships to be removed
        for (Long anyObjectKey : anyObjectMod.getRelationshipsToRemove()) {
            LOG.debug("Relationship to be removed for any object {}", anyObjectKey);

            ARelationship relationship = anyObject.getRelationship(anyObjectKey);
            if (relationship == null) {
                LOG.warn("Invalid anyObject key specified for relationship to be removed: {}", anyObjectKey);
            } else {
                if (!anyObjectMod.getRelationshipsToAdd().contains(anyObjectKey)) {
                    anyObject.remove(relationship);
                    toBeDeprovisioned.addAll(relationship.getRightEnd().getResourceNames());
                }
            }
        }

        // relationships to be added
        for (Long anyObjectKey : anyObjectMod.getRelationshipsToAdd()) {
            LOG.debug("Relationship to be added for any object {}", anyObjectKey);

            AnyObject otherEnd = anyObjectDAO.find(anyObjectKey);
            if (otherEnd == null) {
                LOG.debug("Ignoring invalid any object {}", anyObjectKey);
            } else {
                ARelationship relationship = anyObject.getRelationship(otherEnd.getKey());
                if (relationship == null) {
                    relationship = entityFactory.newEntity(ARelationship.class);
                    relationship.setRightEnd(otherEnd);
                    relationship.setLeftEnd(anyObject);

                    anyObject.add(relationship);

                    toBeProvisioned.addAll(otherEnd.getResourceNames());
                }
            }
        }

        // memberships to be removed
        for (Long groupKey : anyObjectMod.getMembershipsToRemove()) {
            LOG.debug("Membership to be removed for group {}", groupKey);

            AMembership membership = anyObject.getMembership(groupKey);
            if (membership == null) {
                LOG.warn("Invalid group key specified for membership to be removed: {}", groupKey);
            } else {
                if (!anyObjectMod.getMembershipsToAdd().contains(groupKey)) {
                    anyObject.remove(membership);
                    toBeDeprovisioned.addAll(membership.getRightEnd().getResourceNames());
                }
            }
        }

        // memberships to be added
        for (Long groupKey : anyObjectMod.getMembershipsToAdd()) {
            LOG.debug("Membership to be added for group {}", groupKey);

            Group group = groupDAO.find(groupKey);
            if (group == null) {
                LOG.debug("Ignoring invalid group {}", groupKey);
            } else {
                AMembership membership = anyObject.getMembership(group.getKey());
                if (membership == null) {
                    membership = entityFactory.newEntity(AMembership.class);
                    membership.setRightEnd(group);
                    membership.setLeftEnd(anyObject);

                    anyObject.add(membership);

                    toBeProvisioned.addAll(group.getResourceNames());
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

        return propByRes;
    }

}
