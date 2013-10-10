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
package org.apache.syncope.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.ReferenceMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;

/**
 * Utility class for manipulating classes extending AbstractAttributableTO and AbstractAttributableMod.
 *
 * @see AbstractAttributableTO
 * @see AbstractAttributableMod
 */
public final class AttributableOperations {

    private AttributableOperations() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractAttributableTO> T clone(final T original) {
        return (T) SerializationUtils.clone(original);
    }

    private static void populate(final Map<String, AttributeTO> updatedAttrs,
            final Map<String, AttributeTO> originalAttrs, final AbstractAttributableMod result) {

        populate(updatedAttrs, originalAttrs, result, false);
    }

    private static void populate(final Map<String, AttributeTO> updatedAttrs,
            final Map<String, AttributeTO> originalAttrs, final AbstractAttributableMod result,
            final boolean virtuals) {

        for (Map.Entry<String, AttributeTO> entry : updatedAttrs.entrySet()) {
            AttributeMod mod = new AttributeMod();
            mod.setSchema(entry.getKey());

            Set<String> updatedValues = new HashSet<String>(entry.getValue().getValues());

            Set<String> originalValues = originalAttrs.containsKey(entry.getKey())
                    ? new HashSet<String>(originalAttrs.get(entry.getKey()).getValues())
                    : Collections.<String>emptySet();

            if (!updatedValues.equals(originalValues)) {
                // avoid unwanted inputs
                updatedValues.remove("");
                if (!entry.getValue().isReadonly()) {
                    mod.getValuesToBeAdded().addAll(updatedValues);

                    if (!mod.isEmpty()) {
                        if (virtuals) {
                            result.getVirAttrsToRemove().add(mod.getSchema());
                        } else {
                            result.getAttrsToRemove().add(mod.getSchema());
                        }
                    }
                }

                mod.getValuesToBeRemoved().addAll(originalValues);

                if (!mod.isEmpty()) {
                    if (virtuals) {
                        result.getVirAttrsToUpdate().add(mod);
                    } else {
                        result.getAttrsToUpdate().add(mod);
                    }
                }
            }
        }
    }

    private static void diff(
            final AbstractAttributableTO updated,
            final AbstractAttributableTO original,
            final AbstractAttributableMod result,
            final boolean incremental) {

        // 1. check same id
        if (updated.getId() != original.getId()) {
            throw new IllegalArgumentException("AttributableTO's id must be the same");
        }
        result.setId(updated.getId());

        // 2. attributes
        Map<String, AttributeTO> updatedAttrs = new HashMap<String, AttributeTO>(updated.getAttrMap());
        Map<String, AttributeTO> originalAttrs = new HashMap<String, AttributeTO>(original.getAttrMap());

        Set<String> originalAttrNames = new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getAttrsToRemove().clear();
            result.getAttrsToRemove().addAll(originalAttrNames);
        }

        Set<String> emptyUpdatedAttrs = new HashSet<String>();
        for (Map.Entry<String, AttributeTO> entry : updatedAttrs.entrySet()) {
            if (entry.getValue().getValues() == null || entry.getValue().getValues().isEmpty()) {

                emptyUpdatedAttrs.add(entry.getKey());
            }
        }
        for (String emptyUpdatedAttr : emptyUpdatedAttrs) {
            updatedAttrs.remove(emptyUpdatedAttr);
            result.getAttrsToRemove().add(emptyUpdatedAttr);
        }

        populate(updatedAttrs, originalAttrs, result);

        // 3. derived attributes
        updatedAttrs = updated.getDerAttrMap();
        originalAttrs = original.getDerAttrMap();

        originalAttrNames = new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getDerAttrsToRemove().clear();
            result.getDerAttrsToRemove().addAll(originalAttrNames);
        }

        Set<String> updatedAttrNames = new HashSet<String>(updatedAttrs.keySet());
        updatedAttrNames.removeAll(originalAttrs.keySet());
        result.getDerAttrsToAdd().clear();
        result.getDerAttrsToAdd().addAll(updatedAttrNames);

        // 4. virtual attributes
        updatedAttrs = updated.getVirAttrMap();
        originalAttrs = original.getVirAttrMap();

        originalAttrNames = new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getVirAttrsToRemove().clear();
            result.getVirAttrsToRemove().addAll(originalAttrNames);
        }

        populate(updatedAttrs, originalAttrs, result, true);

        // 5. resources
        Set<String> updatedRes = new HashSet<String>(updated.getResources());
        Set<String> originalRes = new HashSet<String>(original.getResources());

        updatedRes.removeAll(originalRes);
        result.getResourcesToAdd().clear();
        result.getResourcesToAdd().addAll(updatedRes);

        originalRes.removeAll(updated.getResources());

        if (!incremental) {
            result.getResourcesToRemove().clear();
            result.getResourcesToRemove().addAll(originalRes);
        }
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated UserTO
     * @param original original UserTO
     * @return UserMod containing differences
     */
    public static UserMod diff(final UserTO updated, final UserTO original) {
        return diff(updated, original, false);
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated UserTO
     * @param original original UserTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return UserMod containing differences
     */
    public static UserMod diff(final UserTO updated, final UserTO original, final boolean incremental) {
        UserMod result = new UserMod();

        diff(updated, original, result, incremental);

        // 1. password
        if (original.getPassword() != null && !original.getPassword().equals(updated.getPassword())) {
            result.setPassword(updated.getPassword());
        }

        // 2. username
        if (original.getUsername() != null && !original.getUsername().equals(updated.getUsername())) {
            result.setUsername(updated.getUsername());
        }

        // 3. memberships
        Map<Long, MembershipTO> updatedMembs = updated.getMembershipMap();
        Map<Long, MembershipTO> originalMembs = original.getMembershipMap();

        for (Map.Entry<Long, MembershipTO> entry : updatedMembs.entrySet()) {
            MembershipMod membMod = new MembershipMod();
            membMod.setRole(entry.getValue().getRoleId());

            if (originalMembs.containsKey(entry.getKey())) {
                diff(entry.getValue(), originalMembs.get(entry.getKey()), membMod, false);
            } else {
                for (AttributeTO attr : entry.getValue().getAttrs()) {

                    AttributeMod attrMod = new AttributeMod();
                    attrMod.setSchema(attr.getSchema());
                    attrMod.getValuesToBeAdded().addAll(attr.getValues());

                    if (!attrMod.isEmpty()) {
                        membMod.getAttrsToUpdate().add(attrMod);
                        membMod.getAttrsToRemove().add(attrMod.getSchema());
                    }
                }
                for (AttributeTO attr : entry.getValue().getDerAttrs()) {
                    membMod.getDerAttrsToAdd().add(attr.getSchema());
                }
                for (AttributeTO attr : entry.getValue().getVirAttrs()) {
                    AttributeMod attrMod = new AttributeMod();
                    attrMod.setSchema(attr.getSchema());
                    attrMod.getValuesToBeAdded().addAll(attr.getValues());

                    if (!attrMod.isEmpty()) {
                        membMod.getVirAttrsToUpdate().add(attrMod);
                        membMod.getAttrsToRemove().add(attrMod.getSchema());
                    }
                }
                membMod.getResourcesToAdd().addAll(entry.getValue().getResources());
            }

            if (!membMod.isEmpty()) {
                result.getMembershipsToAdd().add(membMod);
            }
        }

        if (!incremental) {
            Set<Long> originalRoles = new HashSet<Long>(originalMembs.keySet());
            originalRoles.removeAll(updatedMembs.keySet());
            for (Long roleId : originalRoles) {
                result.getMembershipsToRemove().add(originalMembs.get(roleId).getId());
            }
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated RoleTO
     * @param original original RoleTO
     * @return RoleMod containing differences
     */
    public static RoleMod diff(final RoleTO updated, final RoleTO original) {
        return diff(updated, original, false);
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated RoleTO
     * @param original original RoleTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return RoleMod containing differences
     */
    public static RoleMod diff(final RoleTO updated, final RoleTO original, final boolean incremental) {
        RoleMod result = new RoleMod();

        diff(updated, original, result, incremental);

        // 1. inheritance
        result.setInheritOwner(updated.isInheritOwner());
        result.setInheritTemplates(updated.isInheritTemplates());
        result.setInheritAccountPolicy(updated.isInheritAccountPolicy());
        result.setInheritPasswordPolicy(updated.isInheritPasswordPolicy());
        result.setInheritAttributes(updated.isInheritAttrs());
        result.setInheritDerAttrs(updated.isInheritDerAttrs());
        result.setInheritVirAttrs(updated.isInheritVirAttrs());

        // 2. policies
        result.setAccountPolicy(new ReferenceMod(updated.getAccountPolicy()));
        result.setPasswordPolicy(new ReferenceMod(updated.getPasswordPolicy()));

        // 3. name
        if (!original.getName().equals(updated.getName())) {
            result.setName(updated.getName());
        }

        // 4. entitlements
        Set<String> updatedEnts = new HashSet<String>(updated.getEntitlements());
        Set<String> originalEnts = new HashSet<String>(original.getEntitlements());
        if (updatedEnts.equals(originalEnts)) {
            result.setModEntitlements(false);
            result.getEntitlements().clear();
        } else {
            result.setModEntitlements(true);
            result.getEntitlements().addAll(updated.getEntitlements());
        }

        // 5. templates
        Set<String> updatedTemplates = new HashSet<String>(updated.getRAttrTemplates());
        Set<String> originalTemplates = new HashSet<String>(original.getRAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModRAttrTemplates(false);
            result.getRAttrTemplates().clear();
        } else {
            result.setModRAttrTemplates(true);
            result.getRAttrTemplates().addAll(updated.getRAttrTemplates());
        }
        updatedTemplates = new HashSet<String>(updated.getRDerAttrTemplates());
        originalTemplates = new HashSet<String>(original.getRDerAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModRDerAttrTemplates(false);
            result.getRDerAttrTemplates().clear();
        } else {
            result.setModRDerAttrTemplates(true);
            result.getRDerAttrTemplates().addAll(updated.getRDerAttrTemplates());
        }
        updatedTemplates = new HashSet<String>(updated.getRVirAttrTemplates());
        originalTemplates = new HashSet<String>(original.getRVirAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModRVirAttrTemplates(false);
            result.getRVirAttrTemplates().clear();
        } else {
            result.setModRVirAttrTemplates(true);
            result.getRVirAttrTemplates().addAll(updated.getRVirAttrTemplates());
        }
        updatedTemplates = new HashSet<String>(updated.getMAttrTemplates());
        originalTemplates = new HashSet<String>(original.getMAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModMAttrTemplates(false);
            result.getMAttrTemplates().clear();
        } else {
            result.setModMAttrTemplates(true);
            result.getMAttrTemplates().addAll(updated.getMAttrTemplates());
        }
        updatedTemplates = new HashSet<String>(updated.getMDerAttrTemplates());
        originalTemplates = new HashSet<String>(original.getMDerAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModMDerAttrTemplates(false);
            result.getMDerAttrTemplates().clear();
        } else {
            result.setModMDerAttrTemplates(true);
            result.getMDerAttrTemplates().addAll(updated.getMDerAttrTemplates());
        }
        updatedTemplates = new HashSet<String>(updated.getMVirAttrTemplates());
        originalTemplates = new HashSet<String>(original.getMVirAttrTemplates());
        if (updatedTemplates.equals(originalTemplates)) {
            result.setModMVirAttrTemplates(false);
            result.getMVirAttrTemplates().clear();
        } else {
            result.setModMVirAttrTemplates(true);
            result.getMVirAttrTemplates().addAll(updated.getMVirAttrTemplates());
        }

        // 6. owner
        result.setUserOwner(new ReferenceMod(updated.getUserOwner()));
        result.setRoleOwner(new ReferenceMod(updated.getRoleOwner()));

        return result;
    }

    private static List<AttributeTO> getUpdateValues(final Map<String, AttributeTO> attrs,
            final Set<String> attrsToBeRemoved, final Set<AttributeMod> attrsToBeUpdated) {

        Map<String, AttributeTO> rwattrs = new HashMap<String, AttributeTO>(attrs);
        for (String attrName : attrsToBeRemoved) {
            rwattrs.remove(attrName);
        }
        for (AttributeMod attrMod : attrsToBeUpdated) {
            if (rwattrs.containsKey(attrMod.getSchema())) {
                AttributeTO attrTO = rwattrs.get(attrMod.getSchema());
                attrTO.getValues().removeAll(attrMod.getValuesToBeRemoved());
                attrTO.getValues().addAll(attrMod.getValuesToBeAdded());
            } else {
                AttributeTO attrTO = new AttributeTO();
                attrTO.setSchema(attrMod.getSchema());
                attrTO.getValues().addAll(attrMod.getValuesToBeAdded());

                rwattrs.put(attrMod.getSchema(), attrTO);
            }
        }

        return new ArrayList<AttributeTO>(rwattrs.values());
    }

    private static <T extends AbstractAttributableTO, K extends AbstractAttributableMod> void apply(final T to,
            final K mod, final T result) {

        // 1. attributes
        result.getAttrs().addAll(getUpdateValues(to.getAttrMap(),
                mod.getAttrsToRemove(), mod.getAttrsToUpdate()));

        // 2. derived attributes
        Map<String, AttributeTO> attrs = to.getDerAttrMap();
        for (String attrName : mod.getDerAttrsToRemove()) {
            attrs.remove(attrName);
        }
        for (String attrName : mod.getDerAttrsToAdd()) {
            AttributeTO attrTO = new AttributeTO();
            attrTO.setSchema(attrName);

            attrs.put(attrName, attrTO);
        }
        result.getDerAttrs().addAll(attrs.values());

        // 3. virtual attributes
        result.getVirAttrs().addAll(getUpdateValues(to.getVirAttrMap(),
                mod.getVirAttrsToRemove(), mod.getVirAttrsToUpdate()));

        // 4. resources
        result.getResources().removeAll(mod.getResourcesToRemove());
        result.getResources().addAll(mod.getResourcesToAdd());
    }

    public static UserTO apply(final UserTO userTO, final UserMod userMod) {
        // 1. check same id
        if (userTO.getId() != userMod.getId()) {
            throw new IllegalArgumentException("UserTO and UserMod ids must be the same");
        }

        UserTO result = clone(userTO);
        apply(userTO, userMod, result);

        // 1. password
        result.setPassword(userMod.getPassword());

        // 2. username
        if (userMod.getUsername() != null) {
            result.setUsername(userMod.getUsername());
        }
        // 3. memberships
        Map<Long, MembershipTO> membs = result.getMembershipMap();
        for (Long membId : userMod.getMembershipsToRemove()) {
            result.getMemberships().remove(membs.get(membId));
        }
        for (MembershipMod membMod : userMod.getMembershipsToAdd()) {
            MembershipTO membTO = new MembershipTO();
            membTO.setRoleId(membMod.getRole());

            apply(membTO, membMod, membTO);
        }

        return result;
    }
}
