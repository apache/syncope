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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.misc.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAttributableDataBinder implements GroupDataBinder {

    @Autowired
    private ConnObjectUtils connObjectUtils;

    private <T extends AttrTemplate<S>, S extends Schema> void setAttrTemplates(
            final Group group, final List<String> schemaNames,
            final Class<T> templateClass, final Class<S> schemaClass) {

        List<T> toRemove = new ArrayList<>();
        for (T template : group.getAttrTemplates(templateClass)) {
            if (!schemaNames.contains(template.getSchema().getKey())) {
                toRemove.add(template);
            }
        }
        group.getAttrTemplates(templateClass).removeAll(toRemove);

        for (String schemaName : schemaNames) {
            if (group.getAttrTemplate(templateClass, schemaName) == null) {
                S schema = getSchema(schemaName, schemaClass);
                if (schema != null) {
                    try {
                        T template = entityFactory.newEntity(templateClass);
                        template.setSchema(schema);
                        template.setOwner(group);
                        group.getAttrTemplates(templateClass).add(template);
                    } catch (Exception e) {
                        LOG.error("Could not create template for {}", templateClass, e);
                    }
                }
            }
        }
    }

    private void setDynMembership(final Group group, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }

        DynGroupMembership dynMembership;
        if (group.getDynMembership() == null) {
            dynMembership = entityFactory.newEntity(DynGroupMembership.class);
            dynMembership.setGroup(group);
            group.setDynMembership(dynMembership);
        } else {
            dynMembership = group.getDynMembership();
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

        // attribute templates
        setAttrTemplates(group, groupTO.getGPlainAttrTemplates(), GPlainAttrTemplate.class, GPlainSchema.class);
        setAttrTemplates(group, groupTO.getGDerAttrTemplates(), GDerAttrTemplate.class, GDerSchema.class);
        setAttrTemplates(group, groupTO.getGVirAttrTemplates(), GVirAttrTemplate.class, GVirSchema.class);
        setAttrTemplates(group, groupTO.getMPlainAttrTemplates(), MPlainAttrTemplate.class, MPlainSchema.class);
        setAttrTemplates(group, groupTO.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        setAttrTemplates(group, groupTO.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);

        // attributes, derived attributes, virtual attributes and resources
        fill(group, groupTO, attrUtilsFactory.getInstance(AttributableType.GROUP), scce);

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

        if (groupTO.getDynMembershipCond() != null) {
            setDynMembership(group, groupTO.getDynMembershipCond());
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
        Map<String, String> oldAccountIds = getAccountIds(group, AttributableType.GROUP);

        // realm
        setRealm(group, groupMod);
        // name
        if (groupMod.getName() != null && !groupMod.getName().equals(group.getName())) {
            propByRes.addAll(ResourceOperation.UPDATE, group.getResourceNames());

            group.setName(groupMod.getName());
        }

        // attribute templates
        if (groupMod.isModGAttrTemplates()) {
            setAttrTemplates(group, groupMod.getGPlainAttrTemplates(), GPlainAttrTemplate.class, GPlainSchema.class);
        }
        if (groupMod.isModGDerAttrTemplates()) {
            setAttrTemplates(group, groupMod.getGDerAttrTemplates(), GDerAttrTemplate.class, GDerSchema.class);
        }
        if (groupMod.isModGVirAttrTemplates()) {
            setAttrTemplates(group, groupMod.getGVirAttrTemplates(), GVirAttrTemplate.class, GVirSchema.class);
        }
        if (groupMod.isModMAttrTemplates()) {
            setAttrTemplates(group, groupMod.getMPlainAttrTemplates(), MPlainAttrTemplate.class, MPlainSchema.class);
        }
        if (groupMod.isModMDerAttrTemplates()) {
            setAttrTemplates(group, groupMod.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        }
        if (groupMod.isModMVirAttrTemplates()) {
            setAttrTemplates(group, groupMod.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);
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
        propByRes.merge(fill(group, groupMod, attrUtilsFactory.getInstance(AttributableType.GROUP), scce));

        // check if some account id was changed by the update above
        Map<String, String> newAccountIds = getAccountIds(group, AttributableType.GROUP);
        for (Map.Entry<String, String> entry : oldAccountIds.entrySet()) {
            if (newAccountIds.containsKey(entry.getKey())
                    && !entry.getValue().equals(newAccountIds.get(entry.getKey()))) {

                propByRes.addOldAccountId(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        // dynamic membership
        if (group.getDynMembership() != null && groupMod.getDynMembershipCond() == null) {
            group.setDynMembership(null);
        } else if (group.getDynMembership() == null && groupMod.getDynMembershipCond() != null) {
            setDynMembership(group, groupMod.getDynMembershipCond());
        } else if (group.getDynMembership() != null && groupMod.getDynMembershipCond() != null
                && !group.getDynMembership().getFIQLCond().equals(groupMod.getDynMembershipCond())) {

            group.getDynMembership().getUsers().clear();
            setDynMembership(group, groupMod.getDynMembershipCond());
        }

        return propByRes;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group) {
        connObjectUtils.retrieveVirAttrValues(group, attrUtilsFactory.getInstance(AttributableType.GROUP));

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

        for (GPlainAttrTemplate template : group.getAttrTemplates(GPlainAttrTemplate.class)) {
            groupTO.getGPlainAttrTemplates().add(template.getSchema().getKey());
        }
        for (GDerAttrTemplate template : group.getAttrTemplates(GDerAttrTemplate.class)) {
            groupTO.getGDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (GVirAttrTemplate template : group.getAttrTemplates(GVirAttrTemplate.class)) {
            groupTO.getGVirAttrTemplates().add(template.getSchema().getKey());
        }
        for (MPlainAttrTemplate template : group.getAttrTemplates(MPlainAttrTemplate.class)) {
            groupTO.getMPlainAttrTemplates().add(template.getSchema().getKey());
        }
        for (MDerAttrTemplate template : group.getAttrTemplates(MDerAttrTemplate.class)) {
            groupTO.getMDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (MVirAttrTemplate template : group.getAttrTemplates(MVirAttrTemplate.class)) {
            groupTO.getMVirAttrTemplates().add(template.getSchema().getKey());
        }

        if (group.getDynMembership() != null) {
            groupTO.setDynMembershipCond(group.getDynMembership().getFIQLCond());
        }

        return groupTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Long key) {
        return getGroupTO(groupDAO.authFetch(key));
    }
}
