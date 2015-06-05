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
package org.apache.syncope.common.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.ReferenceMod;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;

/**
 * Utility class for comparing manipulating {@link AnyTO} and {@link AnyMod}.
 */
public final class AnyOperations {

    private AnyOperations() {
        // empty constructor for static utility classes
    }

    private static void populate(final Map<String, AttrTO> updatedAttrs,
            final Map<String, AttrTO> originalAttrs, final AnyMod result) {

        populate(updatedAttrs, originalAttrs, result, false);
    }

    private static void populate(final Map<String, AttrTO> updatedAttrs,
            final Map<String, AttrTO> originalAttrs, final AnyMod result,
            final boolean virtuals) {

        for (Map.Entry<String, AttrTO> entry : updatedAttrs.entrySet()) {
            AttrMod mod = new AttrMod();
            mod.setSchema(entry.getKey());

            Set<String> updatedValues = new HashSet<>(entry.getValue().getValues());

            Set<String> originalValues = originalAttrs.containsKey(entry.getKey())
                    ? new HashSet<>(originalAttrs.get(entry.getKey()).getValues())
                    : Collections.<String>emptySet();

            if (!originalAttrs.containsKey(entry.getKey())) {
                // SYNCOPE-459: take care of user virtual attributes without any value
                updatedValues.remove("");
                mod.getValuesToBeAdded().addAll(new ArrayList<>(updatedValues));

                if (virtuals) {
                    result.getVirAttrsToUpdate().add(mod);
                } else {
                    result.getPlainAttrsToUpdate().add(mod);
                }
            } else if (!updatedValues.equals(originalValues)) {
                // avoid unwanted inputs
                updatedValues.remove("");
                if (!entry.getValue().isReadonly()) {
                    mod.getValuesToBeAdded().addAll(updatedValues);

                    if (!mod.isEmpty()) {
                        if (virtuals) {
                            result.getVirAttrsToRemove().add(mod.getSchema());
                        } else {
                            result.getPlainAttrsToRemove().add(mod.getSchema());
                        }
                    }
                }

                mod.getValuesToBeRemoved().addAll(originalValues);

                if (!mod.isEmpty()) {
                    if (virtuals) {
                        result.getVirAttrsToUpdate().add(mod);
                    } else {
                        result.getPlainAttrsToUpdate().add(mod);
                    }
                }
            }
        }
    }

    private static void diff(
            final AnyTO updated,
            final AnyTO original,
            final AnyMod result,
            final boolean incremental) {

        // 1. check same id
        if (updated.getKey() != original.getKey()) {
            throw new IllegalArgumentException("AnyTO's id must be the same");
        }
        result.setKey(updated.getKey());

        // 2. attributes
        Map<String, AttrTO> updatedAttrs = new HashMap<>(updated.getPlainAttrMap());
        Map<String, AttrTO> originalAttrs = new HashMap<>(original.getPlainAttrMap());

        Set<String> originalAttrNames = new HashSet<>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getPlainAttrsToRemove().clear();
            result.getPlainAttrsToRemove().addAll(originalAttrNames);
        }

        Set<String> emptyUpdatedAttrs = new HashSet<>();
        for (Map.Entry<String, AttrTO> entry : updatedAttrs.entrySet()) {
            if (entry.getValue().getValues() == null || entry.getValue().getValues().isEmpty()) {

                emptyUpdatedAttrs.add(entry.getKey());
            }
        }
        for (String emptyUpdatedAttr : emptyUpdatedAttrs) {
            updatedAttrs.remove(emptyUpdatedAttr);
            result.getPlainAttrsToRemove().add(emptyUpdatedAttr);
        }

        populate(updatedAttrs, originalAttrs, result);

        // 3. derived attributes
        updatedAttrs = updated.getDerAttrMap();
        originalAttrs = original.getDerAttrMap();

        originalAttrNames = new HashSet<>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getDerAttrsToRemove().clear();
            result.getDerAttrsToRemove().addAll(originalAttrNames);
        }

        Set<String> updatedAttrNames = new HashSet<>(updatedAttrs.keySet());
        updatedAttrNames.removeAll(originalAttrs.keySet());
        result.getDerAttrsToAdd().clear();
        result.getDerAttrsToAdd().addAll(updatedAttrNames);

        // 4. virtual attributes
        updatedAttrs = updated.getVirAttrMap();
        originalAttrs = original.getVirAttrMap();

        originalAttrNames = new HashSet<>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());

        if (!incremental) {
            result.getVirAttrsToRemove().clear();
            result.getVirAttrsToRemove().addAll(originalAttrNames);
        }

        populate(updatedAttrs, originalAttrs, result, true);

        // 5. resources
        Set<String> updatedRes = new HashSet<>(updated.getResources());
        Set<String> originalRes = new HashSet<>(original.getResources());

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
     * @param updated updated AnyObjectTO
     * @param original original AnyObjectTO
     * @return AnyObjectMod containing differences
     */
    public static AnyObjectMod diff(final AnyObjectTO updated, final AnyObjectTO original) {
        return diff(updated, original, false);
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated AnyObjectTO
     * @param original original AnyObjectTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return AnyObjectMod containing differences
     */
    public static AnyObjectMod diff(final AnyObjectTO updated, final AnyObjectTO original, final boolean incremental) {
        AnyObjectMod result = new AnyObjectMod();

        diff(updated, original, result, incremental);

        // 1. relationships
        Map<Long, RelationshipTO> updatedRels = updated.getRelationshipMap();
        Map<Long, RelationshipTO> originalRels = original.getRelationshipMap();

        for (Map.Entry<Long, RelationshipTO> entry : updatedRels.entrySet()) {
            if (!originalRels.containsKey(entry.getKey())) {
                result.getRelationshipsToAdd().add(entry.getKey());
            }
        }
        if (!incremental) {
            Set<Long> originalGroups = new HashSet<>(originalRels.keySet());
            originalGroups.removeAll(updatedRels.keySet());
            result.getRelationshipsToRemove().addAll(originalGroups);
        }

        // 2. memberships
        Map<Long, MembershipTO> updatedMembs = updated.getMembershipMap();
        Map<Long, MembershipTO> originalMembs = original.getMembershipMap();

        for (Map.Entry<Long, MembershipTO> entry : updatedMembs.entrySet()) {
            if (!originalMembs.containsKey(entry.getKey())) {
                result.getMembershipsToAdd().add(entry.getKey());
            }
        }
        if (!incremental) {
            Set<Long> originalGroups = new HashSet<>(originalMembs.keySet());
            originalGroups.removeAll(updatedMembs.keySet());
            result.getMembershipsToRemove().addAll(originalGroups);
        }

        return result;
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

        // 0. realm
        if (updated.getRealm() != null && (original.getRealm() == null
                || !original.getRealm().equals(updated.getRealm()))) {

            result.setRealm(updated.getRealm());
        }

        // 1. password
        if (updated.getPassword() != null && (original.getPassword() == null
                || !original.getPassword().equals(updated.getPassword()))) {

            result.setPassword(updated.getPassword());
        }

        // 2. username
        if (original.getUsername() != null && !original.getUsername().equals(updated.getUsername())) {
            result.setUsername(updated.getUsername());
        }

        // 3. security question / answer
        if (updated.getSecurityQuestion() == null) {
            result.setSecurityQuestion(null);
            result.setSecurityAnswer(null);
        } else if (!updated.getSecurityQuestion().equals(original.getSecurityQuestion())
                || StringUtils.isNotBlank(updated.getSecurityAnswer())) {

            result.setSecurityQuestion(updated.getSecurityQuestion());
            result.setSecurityAnswer(updated.getSecurityAnswer());
        }

        // 4. roles
        result.getRolesToRemove().addAll(CollectionUtils.subtract(original.getRoles(), updated.getRoles()));
        result.getRolesToAdd().addAll(CollectionUtils.subtract(updated.getRoles(), original.getRoles()));

        // 5. relationships
        Map<Long, RelationshipTO> updatedRels = updated.getRelationshipMap();
        Map<Long, RelationshipTO> originalRels = original.getRelationshipMap();

        for (Map.Entry<Long, RelationshipTO> entry : updatedRels.entrySet()) {
            if (!originalRels.containsKey(entry.getKey())) {
                result.getRelationshipsToAdd().add(entry.getKey());
            }
        }
        if (!incremental) {
            Set<Long> originalGroups = new HashSet<>(originalRels.keySet());
            originalGroups.removeAll(updatedRels.keySet());
            result.getRelationshipsToRemove().addAll(originalGroups);
        }

        // 6. memberships
        Map<Long, MembershipTO> updatedMembs = updated.getMembershipMap();
        Map<Long, MembershipTO> originalMembs = original.getMembershipMap();

        for (Map.Entry<Long, MembershipTO> entry : updatedMembs.entrySet()) {
            if (!originalMembs.containsKey(entry.getKey())) {
                result.getMembershipsToAdd().add(entry.getKey());
            }
        }
        if (!incremental) {
            Set<Long> originalGroups = new HashSet<>(originalMembs.keySet());
            originalGroups.removeAll(updatedMembs.keySet());
            result.getMembershipsToRemove().addAll(originalGroups);
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated GroupTO
     * @param original original GroupTO
     * @return GroupMod containing differences
     */
    public static GroupMod diff(final GroupTO updated, final GroupTO original) {
        return diff(updated, original, false);
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated GroupTO
     * @param original original GroupTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return GroupMod containing differences
     */
    public static GroupMod diff(final GroupTO updated, final GroupTO original, final boolean incremental) {
        GroupMod result = new GroupMod();

        diff(updated, original, result, incremental);

        // 1. name
        if (!original.getName().equals(updated.getName())) {
            result.setName(updated.getName());
        }

        // 2. owner
        result.setUserOwner(new ReferenceMod(updated.getUserOwner()));
        result.setGroupOwner(new ReferenceMod(updated.getGroupOwner()));

        // 3. dynMembershipCond
        result.setADynMembershipCond(updated.getADynMembershipCond());
        result.setUDynMembershipCond(updated.getUDynMembershipCond());

        return result;
    }

    private static List<AttrTO> getUpdateValues(final Map<String, AttrTO> attrs,
            final Set<String> attrsToBeRemoved, final Set<AttrMod> attrsToBeUpdated) {

        Map<String, AttrTO> rwattrs = new HashMap<>(attrs);
        for (String attrName : attrsToBeRemoved) {
            rwattrs.remove(attrName);
        }
        for (AttrMod attrMod : attrsToBeUpdated) {
            if (rwattrs.containsKey(attrMod.getSchema())) {
                AttrTO attrTO = rwattrs.get(attrMod.getSchema());
                attrTO.getValues().removeAll(attrMod.getValuesToBeRemoved());
                attrTO.getValues().addAll(attrMod.getValuesToBeAdded());
            } else {
                AttrTO attrTO = new AttrTO();
                attrTO.setSchema(attrMod.getSchema());
                attrTO.getValues().addAll(attrMod.getValuesToBeAdded());

                rwattrs.put(attrMod.getSchema(), attrTO);
            }
        }

        return new ArrayList<>(rwattrs.values());
    }

    private static <T extends AnyTO, K extends AnyMod> void apply(final T to,
            final K mod, final T result) {

        // 1. attributes
        result.getPlainAttrs().addAll(getUpdateValues(to.getPlainAttrMap(),
                mod.getPlainAttrsToRemove(), mod.getPlainAttrsToUpdate()));

        // 2. derived attributes
        Map<String, AttrTO> attrs = to.getDerAttrMap();
        for (String attrName : mod.getDerAttrsToRemove()) {
            attrs.remove(attrName);
        }
        for (String attrName : mod.getDerAttrsToAdd()) {
            AttrTO attrTO = new AttrTO();
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
        if (userTO.getKey() != userMod.getKey()) {
            throw new IllegalArgumentException("UserTO and UserMod ids must be the same");
        }

        UserTO result = SerializationUtils.clone(userTO);
        apply(userTO, userMod, result);

        // 0. realm
        if (userMod.getRealm() != null) {
            result.setRealm(userMod.getRealm());
        }

        // 1. password
        result.setPassword(userMod.getPassword());

        // 2. username
        if (userMod.getUsername() != null) {
            result.setUsername(userMod.getUsername());
        }

        // 3. roles
        result.getRoles().removeAll(userMod.getRolesToRemove());
        result.getRoles().addAll(userMod.getRolesToAdd());

        return result;
    }
}
