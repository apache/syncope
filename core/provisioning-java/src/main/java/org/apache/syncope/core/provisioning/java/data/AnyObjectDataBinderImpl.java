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

import static org.apache.syncope.core.provisioning.java.data.AbstractAnyDataBinder.LOG;

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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class AnyObjectDataBinderImpl extends AbstractAnyDataBinder implements AnyObjectDataBinder {

    private static final String[] IGNORE_PROPERTIES = {
        "realm", "memberships", "plainAttrs", "derAttrs", "virAttrs", "resources"
    };

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final Long key) {
        return getAnyObjectTO(anyObjectDAO.authFind(key));
    }

    @Override
    public AnyObjectTO getAnyObjectTO(final AnyObject anyObject) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();

        BeanUtils.copyProperties(anyObject, anyObjectTO, IGNORE_PROPERTIES);

        connObjectUtils.retrieveVirAttrValues(anyObject);
        fillTO(anyObjectTO, anyObject.getRealm().getFullPath(),
                anyObject.getPlainAttrs(), anyObject.getDerAttrs(), anyObject.getVirAttrs(),
                anyObjectDAO.findAllResources(anyObject));

        for (AMembership membership : anyObject.getMemberships()) {
            MembershipTO membershipTO = new MembershipTO();

            membershipTO.setKey(membership.getKey());
            membershipTO.setRightKey(membership.getRightEnd().getKey());
            membershipTO.setGroupName(membership.getRightEnd().getName());

            anyObjectTO.getMemberships().add(membershipTO);
        }

        // dynamic memberships
        CollectionUtils.collect(anyObjectDAO.findDynGroupMemberships(anyObject), new Transformer<Group, Long>() {

            @Override
            public Long transform(final Group group) {
                return group.getKey();
            }
        }, anyObjectTO.getDynGroups());

        return anyObjectTO;
    }

    @Override
    public void create(final AnyObject anyObject, final AnyObjectTO anyObjectTO) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

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

        // store the group ids of membership required to be added
        Set<Long> membershipToBeAddedGroupKeys = new HashSet<>(anyObjectMod.getMembershipsToAdd());

        final Set<String> toBeDeprovisioned = new HashSet<>();
        final Set<String> toBeProvisioned = new HashSet<>();

        // memberships to be removed
        for (Long groupKey : anyObjectMod.getMembershipsToRemove()) {
            LOG.debug("Membership to be removed for group {}", groupKey);

            AMembership membership = anyObject.getMembership(groupKey);
            if (membership == null) {
                LOG.warn("Invalid group key specified for membership to be removed: {}", groupKey);
            } else {
                if (membershipToBeAddedGroupKeys.contains(membership.getRightEnd().getKey())) {
                    anyObject.remove(membership);
                } else {
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
