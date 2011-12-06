/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.SerializationUtils;
import org.syncope.client.mod.AbstractAttributableMod;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.ReferenceMod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.UserTO;

/**
 * Utility class for manipulating classes extending AbstractAttributableTO 
 * and AbstractAttributableMod.
 * @see AbstractAttributableTO
 * @see AbstractAttributableMod
 */
public final class AttributableOperations {

    private AttributableOperations() {
    }

    public static <T extends AbstractAttributableTO> T clone(final T original) {
        return (T) SerializationUtils.clone(original);
    }

    private static void populate(final Map<String, AttributeTO> updatedAttrs,
            final Map<String, AttributeTO> originalAttrs,
            final AbstractAttributableMod result) {

        for (Map.Entry<String, AttributeTO> entry : updatedAttrs.entrySet()) {
            AttributeMod attrMod = new AttributeMod();
            attrMod.setSchema(entry.getKey());

            Set<String> updatedValues = new HashSet<String>(
                    entry.getValue().getValues());
            Set<String> originalValues =
                    originalAttrs.containsKey(entry.getKey())
                    ? new HashSet<String>(
                    originalAttrs.get(entry.getKey()).getValues())
                    : Collections.EMPTY_SET;

            updatedValues.removeAll(originalValues);
            // avoid unwanted inputs
            updatedValues.remove("");
            if (!entry.getValue().isReadonly()) {
                attrMod.setValuesToBeAdded(
                        new ArrayList<String>(updatedValues));
                if (!attrMod.isEmpty()) {
                    result.addAttributeToBeRemoved(attrMod.getSchema());
                }
            }

            originalValues.removeAll(entry.getValue().getValues());
            attrMod.setValuesToBeRemoved(new ArrayList<String>(originalValues));

            if (!attrMod.isEmpty()) {
                result.addAttributeToBeUpdated(attrMod);
            }
        }
    }

    private static void diff(
            final AbstractAttributableTO updated,
            final AbstractAttributableTO original,
            final AbstractAttributableMod result) {

        // 1. check same id
        if (updated.getId() != original.getId()) {
            throw new IllegalArgumentException(
                    "AttributableTO's id must be the same");
        }
        result.setId(updated.getId());

        // 2. attributes
        Map<String, AttributeTO> updatedAttrs = updated.getAttributeMap();
        Map<String, AttributeTO> originalAttrs = original.getAttributeMap();

        Set<String> originalAttrNames =
                new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());
        result.setAttributesToBeRemoved(originalAttrNames);

        Set<String> emptyUpdatedAttrs = new HashSet<String>();
        for (Map.Entry<String, AttributeTO> entry : updatedAttrs.entrySet()) {
            if (entry.getValue().getValues() == null
                    || entry.getValue().getValues().isEmpty()) {

                emptyUpdatedAttrs.add(entry.getKey());
            }
        }
        for (String emptyUpdatedAttr : emptyUpdatedAttrs) {
            updatedAttrs.remove(emptyUpdatedAttr);
            result.addAttributeToBeRemoved(emptyUpdatedAttr);
        }

        populate(updatedAttrs, originalAttrs, result);

        // 3. derived attributes
        updatedAttrs = updated.getDerivedAttributeMap();
        originalAttrs = original.getDerivedAttributeMap();

        originalAttrNames = new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());
        result.setDerivedAttributesToBeRemoved(originalAttrNames);

        Set<String> updatedAttrNames =
                new HashSet<String>(updatedAttrs.keySet());
        updatedAttrNames.removeAll(originalAttrs.keySet());
        result.setDerivedAttributesToBeAdded(updatedAttrNames);

        // 4. virtual attributes
        updatedAttrs = updated.getVirtualAttributeMap();
        originalAttrs = original.getVirtualAttributeMap();

        originalAttrNames = new HashSet<String>(originalAttrs.keySet());
        originalAttrNames.removeAll(updatedAttrs.keySet());
        result.setVirtualAttributesToBeRemoved(originalAttrNames);

        populate(updatedAttrs, originalAttrs, result);

        // 5. resources
        Set<String> updatedRes = new HashSet<String>(updated.getResources());
        Set<String> originalRes = new HashSet<String>(original.getResources());

        updatedRes.removeAll(originalRes);
        result.setResourcesToBeAdded(updatedRes);

        originalRes.removeAll(updated.getResources());
        result.setResourcesToBeRemoved(originalRes);
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     * @param updated updated UserTO
     * @param original original UserTO
     * @return UserMod containing differences
     */
    public static UserMod diff(final UserTO updated, final UserTO original) {
        UserMod result = new UserMod();

        diff(updated, original, result);

        // 1. password
        if (original.getPassword() != null
                && !original.getPassword().equals(updated.getPassword())) {

            result.setPassword(updated.getPassword());
        }

        // 2. username
        if (original.getUsername() != null
                && !original.getUsername().equals(updated.getUsername())) {

            result.setUsername(updated.getUsername());
        }

        // 3. memberships
        Map<Long, MembershipTO> updatedMembs = updated.getMembershipMap();
        Map<Long, MembershipTO> originalMembs = original.getMembershipMap();

        for (Map.Entry<Long, MembershipTO> entry : updatedMembs.entrySet()) {
            MembershipMod membMod = new MembershipMod();
            membMod.setRole(entry.getValue().getRoleId());

            if (originalMembs.containsKey(entry.getKey())) {
                diff(entry.getValue(), originalMembs.get(entry.getKey()),
                        membMod);
            } else {
                for (AttributeTO attr :
                        entry.getValue().getAttributes()) {

                    AttributeMod attrMod = new AttributeMod();
                    attrMod.setSchema(attr.getSchema());
                    attrMod.setValuesToBeAdded(attr.getValues());

                    if (!attrMod.isEmpty()) {
                        membMod.addAttributeToBeUpdated(attrMod);
                        result.addAttributeToBeRemoved(attrMod.getSchema());
                    }
                }
                for (AttributeTO attr :
                        entry.getValue().getDerivedAttributes()) {

                    membMod.addDerivedAttributeToBeAdded(attr.getSchema());
                }
                for (AttributeTO attr :
                        entry.getValue().getVirtualAttributes()) {

                    AttributeMod attrMod = new AttributeMod();
                    attrMod.setSchema(attr.getSchema());
                    attrMod.setValuesToBeAdded(attr.getValues());

                    if (!attrMod.isEmpty()) {
                        membMod.addVirtualAttributeToBeUpdated(attrMod);
                        result.addAttributeToBeRemoved(attrMod.getSchema());
                    }
                }
                membMod.setResourcesToBeAdded(entry.getValue().getResources());
            }

            if (!membMod.isEmpty()) {
                result.addMembershipToBeAdded(membMod);
            }
        }

        Set<Long> originalRoles = new HashSet<Long>(originalMembs.keySet());
        originalRoles.removeAll(updatedMembs.keySet());
        for (Long roleId : originalRoles) {
            result.addMembershipToBeRemoved(originalMembs.get(roleId).getId());
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     * @param updated updated RoleTO
     * @param original original RoleTO
     * @return RoleMod containing differences
     */
    public static RoleMod diff(final RoleTO updated, final RoleTO original) {
        RoleMod result = new RoleMod();

        diff(updated, original, result);

        // 1. inheritance
        result.setInheritAccountPolicy(updated.isInheritAccountPolicy());
        result.setInheritPasswordPolicy(updated.isInheritPasswordPolicy());
        result.setInheritAttributes(updated.isInheritAttributes());
        result.setInheritDerivedAttributes(
                updated.isInheritDerivedAttributes());
        result.setInheritVirtualAttributes(
                updated.isInheritVirtualAttributes());

        // 2. policies
        ReferenceMod refAccountPolicy = new ReferenceMod();
        refAccountPolicy.setId(updated.getAccountPolicy());
        result.setAccountPolicy(refAccountPolicy);

        ReferenceMod refPasswordPolicy = new ReferenceMod();
        refPasswordPolicy.setId(updated.getPasswordPolicy());
        result.setPasswordPolicy(refPasswordPolicy);

        // 3. name
        if (!original.getName().equals(updated.getName())) {
            result.setName(updated.getName());
        }

        // 4. entitlements
        Set<String> updatedEnts = new HashSet<String>(
                updated.getEntitlements());
        Set<String> originalEnts = new HashSet<String>(
                original.getEntitlements());
        if (!updatedEnts.equals(originalEnts)) {
            result.setEntitlements(updated.getEntitlements());
        }

        return result;
    }

    private static List<AttributeTO> getUpdateValues(
            final Map<String, AttributeTO> attrs,
            final Set<String> attrsToBeRemoved,
            final Set<AttributeMod> attrsToBeUpdated) {

        for (String attrName : attrsToBeRemoved) {
            attrs.remove(attrName);
        }
        for (AttributeMod attrMod : attrsToBeUpdated) {
            if (attrs.containsKey(attrMod.getSchema())) {
                AttributeTO attrTO = attrs.get(attrMod.getSchema());
                attrTO.getValues().removeAll(attrMod.getValuesToBeRemoved());
                attrTO.getValues().addAll(attrMod.getValuesToBeAdded());
            } else {
                AttributeTO attrTO = new AttributeTO();
                attrTO.setSchema(attrMod.getSchema());
                attrTO.setValues(attrMod.getValuesToBeAdded());

                attrs.put(attrMod.getSchema(), attrTO);
            }
        }

        return new ArrayList(attrs.values());
    }

    private static <T extends AbstractAttributableTO, K extends AbstractAttributableMod> void apply(
            final T to, final K mod, final T result) {

        // 1. check same id
        if (to.getId() != mod.getId()) {
            throw new IllegalArgumentException(
                    "AttributableTO and AttributableMod ids must be the same");
        }

        // 2. attributes
        result.setAttributes(getUpdateValues(to.getAttributeMap(),
                mod.getAttributesToBeRemoved(),
                mod.getAttributesToBeUpdated()));

        // 3. derived attributes
        Map<String, AttributeTO> attrs = to.getDerivedAttributeMap();
        for (String attrName : mod.getDerivedAttributesToBeRemoved()) {
            attrs.remove(attrName);
        }
        for (String attrName : mod.getDerivedAttributesToBeAdded()) {
            AttributeTO attrTO = new AttributeTO();
            attrTO.setSchema(attrName);

            attrs.put(attrName, attrTO);
        }
        result.setDerivedAttributes(new ArrayList(attrs.values()));

        // 4. virtual attributes
        result.setVirtualAttributes(
                getUpdateValues(to.getVirtualAttributeMap(),
                mod.getVirtualAttributesToBeRemoved(),
                mod.getVirtualAttributesToBeUpdated()));

        // 5. resources
        result.getResources().removeAll(mod.getResourcesToBeRemoved());
        result.getResources().addAll(mod.getResourcesToBeAdded());
    }

    public static UserTO apply(final UserTO userTO, final UserMod userMod) {
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
        for (Long membId : userMod.getMembershipsToBeRemoved()) {
            result.removeMembership(membs.get(membId));
        }
        for (MembershipMod membMod : userMod.getMembershipsToBeAdded()) {
            MembershipTO membTO = new MembershipTO();
            membTO.setRoleId(membMod.getRole());

            apply(membTO, membMod, membTO);
        }

        return result;
    }
}
