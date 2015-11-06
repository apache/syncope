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
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAnyDataBinder implements GroupDataBinder {

    private void setDynMembership(final Group group, final AnyTypeKind anyTypeKind, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }

        DynGroupMembership<?> dynMembership;
        if (anyTypeKind == AnyTypeKind.ANY_OBJECT && group.getADynMembership() == null) {
            dynMembership = entityFactory.newEntity(ADynGroupMembership.class);
            dynMembership.setGroup(group);
            group.setADynMembership((ADynGroupMembership) dynMembership);
        } else if (anyTypeKind == AnyTypeKind.USER && group.getUDynMembership() == null) {
            dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
            dynMembership.setGroup(group);
            group.setUDynMembership((UDynGroupMembership) dynMembership);
        } else {
            dynMembership = anyTypeKind == AnyTypeKind.ANY_OBJECT
                    ? group.getADynMembership()
                    : group.getUDynMembership();
        }
        dynMembership.setFIQLCond(dynMembershipFIQL);
    }

    @Override
    public Group create(final Group group, final GroupTO groupTO) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (groupTO.getName() == null) {
            LOG.error("No name specified for this group");

            invalidGroups.getElements().add("No name specified for this group");
        } else {
            group.setName(groupTO.getName());
        }

        // realm, attributes, derived attributes, virtual attributes and resources
        fill(group, groupTO, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        // owner
        if (groupTO.getUserOwner() != null) {
            User owner = userDAO.find(groupTO.getUserOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid user specified as owner: {}", groupTO.getUserOwner());
            } else {
                group.setUserOwner(owner);
            }
        }
        if (groupTO.getGroupOwner() != null) {
            Group owner = groupDAO.find(groupTO.getGroupOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid group specified as owner: {}", groupTO.getGroupOwner());
            } else {
                group.setGroupOwner(owner);
            }
        }

        if (groupTO.getADynMembershipCond() != null) {
            setDynMembership(group, AnyTypeKind.ANY_OBJECT, groupTO.getADynMembershipCond());
        }
        if (groupTO.getUDynMembershipCond() != null) {
            setDynMembership(group, AnyTypeKind.USER, groupTO.getUDynMembershipCond());
        }

        return group;
    }

    @Override
    public PropagationByResource update(final Group toBeUpdated, final GroupPatch groupPatch) {
        // Re-merge any pending change from workflow tasks
        Group group = groupDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(group);

        // realm
        setRealm(group, groupPatch);

        // name
        if (groupPatch.getName() != null && StringUtils.isNotBlank(groupPatch.getName().getValue())) {
            propByRes.addAll(ResourceOperation.UPDATE, group.getResourceNames());

            group.setName(groupPatch.getName().getValue());
        }

        // owner
        if (groupPatch.getUserOwner() != null) {
            group.setUserOwner(groupPatch.getUserOwner().getValue() == null
                    ? null
                    : userDAO.find(groupPatch.getUserOwner().getValue()));
        }
        if (groupPatch.getGroupOwner() != null) {
            group.setGroupOwner(groupPatch.getGroupOwner().getValue() == null
                    ? null
                    : groupDAO.find(groupPatch.getGroupOwner().getValue()));
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(group, groupPatch, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce));

        // check if some connObjectKey was changed by the update above
        Map<String, String> newConnObjectKeys = getConnObjectKeys(group);
        for (Map.Entry<String, String> entry : oldConnObjectKeys.entrySet()) {
            if (newConnObjectKeys.containsKey(entry.getKey())
                    && !entry.getValue().equals(newConnObjectKeys.get(entry.getKey()))) {

                propByRes.addOldConnObjectKey(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        // dynamic membership
        if (groupPatch.getADynMembershipCond() != null) {
            if (groupPatch.getADynMembershipCond().getValue() == null) {
                group.setADynMembership(null);
            } else {
                group.getADynMembership().getMembers().clear();
                setDynMembership(group, AnyTypeKind.ANY_OBJECT, groupPatch.getADynMembershipCond().getValue());
            }
        }
        if (groupPatch.getUDynMembershipCond() != null) {
            if (groupPatch.getUDynMembershipCond().getValue() == null) {
                group.setUDynMembership(null);
            } else {
                group.getUDynMembership().getMembers().clear();
                setDynMembership(group, AnyTypeKind.USER, groupPatch.getUDynMembershipCond().getValue());
            }
        }

        return propByRes;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group, final boolean details) {
        GroupTO groupTO = new GroupTO();

        // set sys info
        groupTO.setCreator(group.getCreator());
        groupTO.setCreationDate(group.getCreationDate());
        groupTO.setLastModifier(group.getLastModifier());
        groupTO.setLastChangeDate(group.getLastChangeDate());

        groupTO.setKey(group.getKey());
        groupTO.setName(group.getName());

        if (group.getUserOwner() != null) {
            groupTO.setUserOwner(group.getUserOwner().getKey());
        }
        if (group.getGroupOwner() != null) {
            groupTO.setGroupOwner(group.getGroupOwner().getKey());
        }

        Map<DerSchema, String> derAttrValues = derAttrHandler.getValues(group);
        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHander.getValues(group)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(groupTO, group.getRealm().getFullPath(), group.getAuxClasses(),
                group.getPlainAttrs(), derAttrValues, virAttrValues, group.getResources());

        if (group.getADynMembership() != null) {
            groupTO.setADynMembershipCond(group.getADynMembership().getFIQLCond());
        }
        if (group.getUDynMembership() != null) {
            groupTO.setUDynMembershipCond(group.getUDynMembership().getFIQLCond());
        }

        return groupTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Long key) {
        return getGroupTO(groupDAO.authFind(key), true);
    }
}
