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

import java.util.Map;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAnyDataBinder implements GroupDataBinder {

    @Autowired
    private ConnObjectUtils connObjectUtils;

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
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroups);
        if (groupTO.getName() == null) {
            LOG.error("No name specified for this group");

            invalidGroups.getElements().add("No name specified for this group");
        } else {
            group.setName(groupTO.getName());
        }

        // attributes, derived attributes, virtual attributes and resources
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
        if (groupTO.getADynMembershipCond() != null) {
            setDynMembership(group, AnyTypeKind.USER, groupTO.getUDynMembershipCond());
        }

        return group;
    }

    @Override
    public PropagationByResource update(final Group toBeUpdated, final GroupMod groupMod) {
        // Re-merge any pending change from workflow tasks
        Group group = groupDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // fetch account ids before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(group);

        // realm
        setRealm(group, groupMod);
        // name
        if (groupMod.getName() != null && !groupMod.getName().equals(group.getName())) {
            propByRes.addAll(ResourceOperation.UPDATE, group.getResourceNames());

            group.setName(groupMod.getName());
        }

        // owner
        if (groupMod.getUserOwner() != null) {
            group.setUserOwner(groupMod.getUserOwner().getKey() == null
                    ? null
                    : userDAO.find(groupMod.getUserOwner().getKey()));
        }
        if (groupMod.getGroupOwner() != null) {
            group.setGroupOwner(groupMod.getGroupOwner().getKey() == null
                    ? null
                    : groupDAO.find(groupMod.getGroupOwner().getKey()));
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(group, groupMod, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce));

        // check if some account id was changed by the update above
        Map<String, String> newConnObjectKeys = getConnObjectKeys(group);
        for (Map.Entry<String, String> entry : oldConnObjectKeys.entrySet()) {
            if (newConnObjectKeys.containsKey(entry.getKey())
                    && !entry.getValue().equals(newConnObjectKeys.get(entry.getKey()))) {

                propByRes.addOldAccountId(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        // dynamic membership
        if (group.getADynMembership() != null && groupMod.getADynMembershipCond() == null) {
            group.setADynMembership(null);
        } else if (group.getADynMembership() == null && groupMod.getADynMembershipCond() != null) {
            setDynMembership(group, AnyTypeKind.ANY_OBJECT, groupMod.getADynMembershipCond());
        } else if (group.getADynMembership() != null && groupMod.getADynMembershipCond() != null
                && !group.getADynMembership().getFIQLCond().equals(groupMod.getADynMembershipCond())) {

            group.getADynMembership().getMembers().clear();
            setDynMembership(group, AnyTypeKind.ANY_OBJECT, groupMod.getADynMembershipCond());
        }
        if (group.getUDynMembership() != null && groupMod.getUDynMembershipCond() == null) {
            group.setUDynMembership(null);
        } else if (group.getUDynMembership() == null && groupMod.getUDynMembershipCond() != null) {
            setDynMembership(group, AnyTypeKind.USER, groupMod.getUDynMembershipCond());
        } else if (group.getUDynMembership() != null && groupMod.getUDynMembershipCond() != null
                && !group.getUDynMembership().getFIQLCond().equals(groupMod.getUDynMembershipCond())) {

            group.getUDynMembership().getMembers().clear();
            setDynMembership(group, AnyTypeKind.USER, groupMod.getUDynMembershipCond());
        }

        return propByRes;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group) {
        connObjectUtils.retrieveVirAttrValues(group);

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

        fillTO(groupTO, group.getRealm().getFullPath(),
                group.getPlainAttrs(), group.getDerAttrs(), group.getVirAttrs(), group.getResources());

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
        return getGroupTO(groupDAO.authFind(key));
    }
}
