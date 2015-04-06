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
import org.apache.syncope.core.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.misc.ConnObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAttributableDataBinder implements GroupDataBinder {

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private EntitlementDAO entitlementDAO;

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

    @Override
    public Group create(final Group group, final GroupTO groupTO) {
        group.setInheritOwner(groupTO.isInheritOwner());

        group.setInheritPlainAttrs(groupTO.isInheritPlainAttrs());
        group.setInheritDerAttrs(groupTO.isInheritDerAttrs());
        group.setInheritVirAttrs(groupTO.isInheritVirAttrs());

        group.setInheritTemplates(groupTO.isInheritTemplates());

        group.setInheritPasswordPolicy(groupTO.isInheritPasswordPolicy());
        group.setInheritAccountPolicy(groupTO.isInheritAccountPolicy());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name and parent
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroups);
        if (groupTO.getName() == null) {
            LOG.error("No name specified for this group");

            invalidGroups.getElements().add("No name specified for this group");
        } else {
            group.setName(groupTO.getName());
        }
        Long parentGroupKey = null;
        if (groupTO.getParent() != 0) {
            Group parentGroup = groupDAO.find(groupTO.getParent());
            if (parentGroup == null) {
                LOG.error("Could not find group with id " + groupTO.getParent());

                invalidGroups.getElements().add(String.valueOf(groupTO.getParent()));
                scce.addException(invalidGroups);
            } else {
                group.setParent(parentGroup);
                parentGroupKey = group.getParent().getKey();
            }
        }

        Group otherGroup = groupDAO.find(groupTO.getName(), parentGroupKey);
        if (otherGroup != null) {
            LOG.error("Another group exists with the same name and the same parent group: " + otherGroup);

            invalidGroups.getElements().add(groupTO.getName());
        }

        // attribute templates
        setAttrTemplates(group, groupTO.getGPlainAttrTemplates(), GPlainAttrTemplate.class, GPlainSchema.class);
        setAttrTemplates(group, groupTO.getGDerAttrTemplates(), GDerAttrTemplate.class, GDerSchema.class);
        setAttrTemplates(group, groupTO.getGVirAttrTemplates(), GVirAttrTemplate.class, GVirSchema.class);
        setAttrTemplates(group, groupTO.getMPlainAttrTemplates(), MPlainAttrTemplate.class, MPlainSchema.class);
        setAttrTemplates(group, groupTO.getMDerAttrTemplates(), MDerAttrTemplate.class, MDerSchema.class);
        setAttrTemplates(group, groupTO.getMVirAttrTemplates(), MVirAttrTemplate.class, MVirSchema.class);

        // attributes, derived attributes, virtual attributes and resources
        fill(group, groupTO, attrUtilFactory.getInstance(AttributableType.GROUP), scce);

        // entitlements
        for (String entitlementName : groupTO.getEntitlements()) {
            Entitlement entitlement = entitlementDAO.find(entitlementName);
            if (entitlement == null) {
                LOG.warn("Ignoring invalid entitlement {}", entitlementName);
            } else {
                group.addEntitlement(entitlement);
            }
        }

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

        // policies
        if (groupTO.getPasswordPolicy() != null) {
            group.setPasswordPolicy((PasswordPolicy) policyDAO.find(groupTO.getPasswordPolicy()));
        }
        if (groupTO.getAccountPolicy() != null) {
            group.setAccountPolicy((AccountPolicy) policyDAO.find(groupTO.getAccountPolicy()));
        }

        return group;
    }

    @Override
    public PropagationByResource update(final Group group, final GroupMod groupMod) {
        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // fetch account ids before update
        Map<String, String> oldAccountIds = getAccountIds(group, AttributableType.GROUP);

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroups);
        if (groupMod.getName() != null) {
            Group otherGroup = groupDAO.find(groupMod.getName(),
                    group.getParent() == null ? null : group.getParent().getKey());
            if (otherGroup == null || group.equals(otherGroup)) {
                if (!groupMod.getName().equals(group.getName())) {
                    propByRes.addAll(ResourceOperation.UPDATE, group.getResourceNames());

                    group.setName(groupMod.getName());
                }
            } else {
                LOG.error("Another group exists with the same name and the same parent group: " + otherGroup);

                invalidGroups.getElements().add(groupMod.getName());
                scce.addException(invalidGroups);
            }
        }

        if (groupMod.getInheritOwner() != null) {
            group.setInheritOwner(groupMod.getInheritOwner());
        }

        if (groupMod.getInheritTemplates() != null) {
            group.setInheritTemplates(groupMod.getInheritTemplates());
        }

        if (groupMod.getInheritPlainAttrs() != null) {
            group.setInheritPlainAttrs(groupMod.getInheritPlainAttrs());
        }
        if (groupMod.getInheritDerAttrs() != null) {
            group.setInheritDerAttrs(groupMod.getInheritDerAttrs());
        }
        if (groupMod.getInheritVirAttrs() != null) {
            group.setInheritVirAttrs(groupMod.getInheritVirAttrs());
        }

        if (groupMod.getInheritPasswordPolicy() != null) {
            group.setInheritPasswordPolicy(groupMod.getInheritPasswordPolicy());
        }
        if (groupMod.getInheritAccountPolicy() != null) {
            group.setInheritAccountPolicy(groupMod.getInheritAccountPolicy());
        }

        // entitlements
        if (groupMod.isModEntitlements()) {
            group.getEntitlements().clear();
            for (String entitlementName : groupMod.getEntitlements()) {
                Entitlement entitlement = entitlementDAO.find(entitlementName);
                if (entitlement == null) {
                    LOG.warn("Ignoring invalid entitlement {}", entitlementName);
                } else {
                    group.addEntitlement(entitlement);
                }
            }
        }

        // attribute templates
        if (groupMod.isModRAttrTemplates()) {
            setAttrTemplates(group, groupMod.getRPlainAttrTemplates(), GPlainAttrTemplate.class, GPlainSchema.class);
        }
        if (groupMod.isModRDerAttrTemplates()) {
            setAttrTemplates(group, groupMod.getRDerAttrTemplates(), GDerAttrTemplate.class, GDerSchema.class);
        }
        if (groupMod.isModRVirAttrTemplates()) {
            setAttrTemplates(group, groupMod.getRVirAttrTemplates(), GVirAttrTemplate.class, GVirSchema.class);
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

        // policies
        if (groupMod.getPasswordPolicy() != null) {
            group.setPasswordPolicy(groupMod.getPasswordPolicy().getKey() == null
                    ? null
                    : (PasswordPolicy) policyDAO.find(groupMod.getPasswordPolicy().getKey()));
        }
        if (groupMod.getAccountPolicy() != null) {
            group.setAccountPolicy(groupMod.getAccountPolicy().getKey() == null
                    ? null
                    : (AccountPolicy) policyDAO.find(groupMod.getAccountPolicy().getKey()));
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
        propByRes.merge(fill(group, groupMod, attrUtilFactory.getInstance(AttributableType.GROUP), scce));

        // check if some account id was changed by the update above
        Map<String, String> newAccountIds = getAccountIds(group, AttributableType.GROUP);
        for (Map.Entry<String, String> entry : oldAccountIds.entrySet()) {
            if (newAccountIds.containsKey(entry.getKey())
                    && !entry.getValue().equals(newAccountIds.get(entry.getKey()))) {

                propByRes.addOldAccountId(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        return propByRes;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group) {
        connObjectUtil.retrieveVirAttrValues(group, attrUtilFactory.getInstance(AttributableType.GROUP));

        GroupTO groupTO = new GroupTO();

        // set sys info
        groupTO.setCreator(group.getCreator());
        groupTO.setCreationDate(group.getCreationDate());
        groupTO.setLastModifier(group.getLastModifier());
        groupTO.setLastChangeDate(group.getLastChangeDate());

        groupTO.setKey(group.getKey());
        groupTO.setName(group.getName());

        groupTO.setInheritOwner(group.isInheritOwner());

        groupTO.setInheritTemplates(group.isInheritTemplates());

        groupTO.setInheritPlainAttrs(group.isInheritPlainAttrs());
        groupTO.setInheritDerAttrs(group.isInheritDerAttrs());
        groupTO.setInheritVirAttrs(group.isInheritVirAttrs());

        groupTO.setInheritPasswordPolicy(group.isInheritPasswordPolicy());
        groupTO.setInheritAccountPolicy(group.isInheritAccountPolicy());

        if (group.getParent() != null) {
            groupTO.setParent(group.getParent().getKey());
        }

        if (group.getUserOwner() != null) {
            groupTO.setUserOwner(group.getUserOwner().getKey());
        }
        if (group.getGroupOwner() != null) {
            groupTO.setGroupOwner(group.getGroupOwner().getKey());
        }

        // -------------------------
        // Retrieve all [derived/virtual] attributes (inherited and not)
        // -------------------------        
        final List<? extends GPlainAttr> allAttributes = group.findLastInheritedAncestorPlainAttrs();

        final List<? extends GDerAttr> allDerAttributes = group.findLastInheritedAncestorDerAttrs();

        final List<? extends GVirAttr> allVirAttributes = group.findLastInheritedAncestorVirAttrs();
        // -------------------------

        fillTO(groupTO, allAttributes, allDerAttributes, allVirAttributes, group.getResources());

        for (Entitlement entitlement : group.getEntitlements()) {
            groupTO.getEntitlements().add(entitlement.getKey());
        }

        for (GPlainAttrTemplate template : group.findInheritedTemplates(GPlainAttrTemplate.class)) {
            groupTO.getGPlainAttrTemplates().add(template.getSchema().getKey());
        }
        for (GDerAttrTemplate template : group.findInheritedTemplates(GDerAttrTemplate.class)) {
            groupTO.getGDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (GVirAttrTemplate template : group.findInheritedTemplates(GVirAttrTemplate.class)) {
            groupTO.getGVirAttrTemplates().add(template.getSchema().getKey());
        }
        for (MPlainAttrTemplate template : group.findInheritedTemplates(MPlainAttrTemplate.class)) {
            groupTO.getMPlainAttrTemplates().add(template.getSchema().getKey());
        }
        for (MDerAttrTemplate template : group.findInheritedTemplates(MDerAttrTemplate.class)) {
            groupTO.getMDerAttrTemplates().add(template.getSchema().getKey());
        }
        for (MVirAttrTemplate template : group.findInheritedTemplates(MVirAttrTemplate.class)) {
            groupTO.getMVirAttrTemplates().add(template.getSchema().getKey());
        }

        groupTO.setPasswordPolicy(group.getPasswordPolicy() == null
                ? null
                : group.getPasswordPolicy().getKey());
        groupTO.setAccountPolicy(group.getAccountPolicy() == null
                ? null
                : group.getAccountPolicy().getKey());

        return groupTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Long key) {
        return getGroupTO(groupDAO.authFetch(key));
    }
}
