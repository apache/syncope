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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AbstractReplacePatchItem;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for comparing {@link AnyTO} instances in order to generate {@link AnyUR} instances.
 */
public final class AnyOperations {

    private static final Logger LOG = LoggerFactory.getLogger(AnyOperations.class);

    private static final List<String> NULL_SINGLETON_LIST = Collections.singletonList(null);

    private AnyOperations() {
        // empty constructor for static utility classes
    }

    private static <T, K extends AbstractReplacePatchItem<T>> K replacePatchItem(
            final T updated, final T original, final K proto) {

        if ((original == null && updated == null) || (original != null && original.equals(updated))) {
            return null;
        }

        proto.setValue(updated);
        return proto;
    }

    private static void relationships(
            final RelatableTO updated,
            final RelatableTO original,
            final boolean incremental,
            final Set<RelationshipUR> updateReqs) {

        Map<Pair<String, String>, RelationshipTO> updatedRels =
                EntityTOUtils.buildRelationshipMap(updated.getRelationships());
        Map<Pair<String, String>, RelationshipTO> originalRels =
                EntityTOUtils.buildRelationshipMap(original.getRelationships());

        updatedRels.forEach((pair, relationship) -> {
            if (!originalRels.containsKey(pair)
                    || (originalRels.containsKey(pair) && !originalRels.get(pair).equals(relationship))) {

                RelationshipUR patch = new RelationshipUR.Builder(relationship.getType()).
                        otherEnd(relationship.getOtherEndKey()).
                        operation(PatchOperation.ADD_REPLACE).build();

                patch.getPlainAttrs().addAll(relationship.getPlainAttrs().stream().
                        filter(attr -> !isEmpty(attr)).toList());

                updateReqs.add(patch);
            }
        });

        if (!incremental) {
            originalRels.keySet().stream().filter(pair -> !updatedRels.containsKey(pair)).
                    forEach(pair -> updateReqs.add(
                    new RelationshipUR.Builder(originalRels.get(pair).getType()).
                            otherEnd(originalRels.get(pair).getOtherEndKey()).
                            operation(PatchOperation.DELETE).build()));
        }
    }

    private static void diff(
            final AnyTO updated, final AnyTO original, final AnyUR result, final boolean incremental) {

        // check same key
        if (updated.getKey() == null && original.getKey() != null
                || (updated.getKey() != null && !updated.getKey().equals(original.getKey()))) {

            throw new IllegalArgumentException("AnyTO's key must be the same");
        }
        result.setKey(updated.getKey());

        // 1. realm
        result.setRealm(replacePatchItem(updated.getRealm(), original.getRealm(), new StringReplacePatchItem()));

        // 2. auxiliary classes
        result.getAuxClasses().clear();

        if (!incremental) {
            original.getAuxClasses().stream().filter(auxClass -> !updated.getAuxClasses().contains(auxClass)).
                    forEach(auxClass -> result.getAuxClasses().add(new StringPatchItem.Builder().
                    operation(PatchOperation.DELETE).value(auxClass).build()));
        }

        updated.getAuxClasses().stream().filter(auxClass -> !original.getAuxClasses().contains(auxClass)).
                forEach(auxClass -> result.getAuxClasses().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(auxClass).build()));

        // 3. relationships
        relationships(updated, original, incremental, result.getRelationships());

        // 4. plain attributes
        Map<String, Attr> updatedAttrs = EntityTOUtils.buildAttrMap(updated.getPlainAttrs());
        Map<String, Attr> originalAttrs = EntityTOUtils.buildAttrMap(original.getPlainAttrs());

        result.getPlainAttrs().clear();

        if (!incremental) {
            originalAttrs.keySet().stream().
                    filter(attr -> !updatedAttrs.containsKey(attr)).forEach(
                    schema -> result.getPlainAttrs().add(
                            new AttrPatch.Builder(new Attr.Builder(schema).build()).
                                    operation(PatchOperation.DELETE).
                                    build()));
        }

        updatedAttrs.values().forEach(attr -> {
            if (isEmpty(attr)) {
                if (!incremental) {
                    result.getPlainAttrs().add(
                            new AttrPatch.Builder(new Attr.Builder(attr.getSchema()).build()).
                                    operation(PatchOperation.DELETE).
                                    build());
                }
            } else if (!originalAttrs.containsKey(attr.getSchema())
                    || !originalAttrs.get(attr.getSchema()).getValues().equals(attr.getValues())) {

                AttrPatch patch = new AttrPatch.Builder(attr).operation(PatchOperation.ADD_REPLACE).build();
                if (!patch.isEmpty()) {
                    result.getPlainAttrs().add(patch);
                }
            }
        });

        // 4. resources
        result.getResources().clear();

        if (!incremental) {
            original.getResources().stream().filter(resource -> !updated.getResources().contains(resource)).
                    forEach(resource -> result.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.DELETE).value(resource).build()));
        }

        updated.getResources().stream().filter(resource -> !original.getResources().contains(resource)).
                forEach(resource -> result.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(resource).build()));
    }

    private static void memberships(
            final GroupableRelatableTO updated,
            final GroupableRelatableTO original,
            final boolean incremental,
            final Set<MembershipUR> updateReqs) {

        Map<String, MembershipTO> updatedMembs = EntityTOUtils.buildMembershipMap(updated.getMemberships());
        Map<String, MembershipTO> originalMembs = EntityTOUtils.buildMembershipMap(original.getMemberships());

        updatedMembs.forEach((group, membership) -> {
            if (!originalMembs.containsKey(group)
                    || (originalMembs.containsKey(group) && !originalMembs.get(group).equals(membership))) {

                MembershipUR patch = new MembershipUR.Builder(group).operation(PatchOperation.ADD_REPLACE).build();

                patch.getPlainAttrs().addAll(membership.getPlainAttrs().stream().
                        filter(attr -> !isEmpty(attr)).toList());

                updateReqs.add(patch);
            }
        });

        if (!incremental) {
            originalMembs.keySet().stream().filter(group -> !updatedMembs.containsKey(group)).
                    forEach(group -> updateReqs.add(
                    new MembershipUR.Builder(group).operation(PatchOperation.DELETE).build()));
        }
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated AnyObjectTO
     * @param original original AnyObjectTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return {@link AnyObjectUR} containing differences
     */
    public static AnyObjectUR diff(
            final AnyObjectTO updated, final AnyObjectTO original, final boolean incremental) {

        AnyObjectUR result = new AnyObjectUR();

        diff(updated, original, result, incremental);

        // 1. name
        result.setName(replacePatchItem(updated.getName(), original.getName(), new StringReplacePatchItem()));

        // 2. memberships
        memberships(updated, original, incremental, result.getMemberships());

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated UserTO
     * @param original original UserTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return {@link UserUR} containing differences
     */
    public static UserUR diff(final UserTO updated, final UserTO original, final boolean incremental) {
        UserUR result = new UserUR();

        diff(updated, original, result, incremental);

        // 1. password
        if (updated.getPassword() != null
                && (original.getPassword() == null || !original.getPassword().equals(updated.getPassword()))) {

            result.setPassword(new PasswordPatch.Builder().
                    value(updated.getPassword()).
                    resources(updated.getResources()).build());
        }

        // 2. username
        result.setUsername(
                replacePatchItem(updated.getUsername(), original.getUsername(), new StringReplacePatchItem()));

        // 3. security question / answer
        if (updated.getSecurityQuestion() == null) {
            result.setSecurityQuestion(null);
            result.setSecurityAnswer(null);
        } else if (!updated.getSecurityQuestion().equals(original.getSecurityQuestion())
                || StringUtils.isNotBlank(updated.getSecurityAnswer())) {

            result.setSecurityQuestion(new StringReplacePatchItem.Builder().
                    value(updated.getSecurityQuestion()).build());
            result.setSecurityAnswer(
                    new StringReplacePatchItem.Builder().value(updated.getSecurityAnswer()).build());
        }

        result.setMustChangePassword(replacePatchItem(
                updated.isMustChangePassword(), original.isMustChangePassword(), new BooleanReplacePatchItem()));

        // 4. roles
        if (!incremental) {
            original.getRoles().stream().filter(role -> !updated.getRoles().contains(role)).
                    forEach(toRemove -> result.getRoles().add(new StringPatchItem.Builder().
                    operation(PatchOperation.DELETE).value(toRemove).build()));
        }

        updated.getRoles().stream().filter(role -> !original.getRoles().contains(role)).
                forEach(toAdd -> result.getRoles().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(toAdd).build()));

        // 5. memberships
        memberships(updated, original, incremental, result.getMemberships());

        // 6. linked accounts
        Map<Pair<String, String>, LinkedAccountTO> updatedAccounts =
                EntityTOUtils.buildLinkedAccountMap(updated.getLinkedAccounts());
        Map<Pair<String, String>, LinkedAccountTO> originalAccounts =
                EntityTOUtils.buildLinkedAccountMap(original.getLinkedAccounts());

        updatedAccounts.forEach((key, value) -> result.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.ADD_REPLACE).
                linkedAccountTO(value).build()));

        if (!incremental) {
            originalAccounts.keySet().stream().filter(account -> !updatedAccounts.containsKey(account)).
                    forEach(key -> result.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.DELETE).
                    linkedAccountTO(originalAccounts.get(key)).build()));
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated GroupTO
     * @param original original GroupTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return {@link GroupUR} containing differences
     */
    public static GroupUR diff(final GroupTO updated, final GroupTO original, final boolean incremental) {
        GroupUR result = new GroupUR();

        diff(updated, original, result, incremental);

        // 1. name
        result.setName(replacePatchItem(updated.getName(), original.getName(), new StringReplacePatchItem()));

        // 2. ownership
        result.setUserOwner(
                replacePatchItem(updated.getUserOwner(), original.getUserOwner(), new StringReplacePatchItem()));
        result.setGroupOwner(
                replacePatchItem(updated.getGroupOwner(), original.getGroupOwner(), new StringReplacePatchItem()));

        // 3. dynamic membership
        result.getDynMembershipConds().putAll(updated.getDynMembershipConds());

        // 4. type extensions
        result.getTypeExtensions().addAll(updated.getTypeExtensions());

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <TO extends AnyTO, P extends AnyUR> P diff(
            final TO updated, final TO original, final boolean incremental) {

        if (updated instanceof UserTO updatedUserTO && original instanceof UserTO originalUserTO) {
            return (P) diff(updatedUserTO, originalUserTO, incremental);
        }
        if (updated instanceof GroupTO updatedGroupTO && original instanceof GroupTO originalGroupTO) {
            return (P) diff(updatedGroupTO, originalGroupTO, incremental);
        }
        if (updated instanceof AnyObjectTO updatedAnyObjectTO && original instanceof AnyObjectTO originalObjectTO) {
            return (P) diff(updatedAnyObjectTO, originalObjectTO, incremental);
        }

        throw new IllegalArgumentException("Unsupported: " + updated.getClass().getName());
    }

    private static Collection<Attr> patch(final Map<String, Attr> attrs, final Set<AttrPatch> attrPatches) {
        Map<String, Attr> rwattrs = new HashMap<>(attrs);
        attrPatches.forEach(patch -> {
            if (patch.getAttr() == null) {
                LOG.warn("Invalid {} specified: {}", AttrPatch.class.getName(), patch);
            } else {
                rwattrs.remove(patch.getAttr().getSchema());
                if (patch.getOperation() == PatchOperation.ADD_REPLACE && !patch.getAttr().getValues().isEmpty()) {
                    rwattrs.put(patch.getAttr().getSchema(), patch.getAttr());
                }
            }
        });

        return rwattrs.values();
    }

    private static <T extends AnyTO, K extends AnyUR> void patch(final T to, final K req, final T result) {
        // check same key
        if (to.getKey() == null || !to.getKey().equals(req.getKey())) {
            throw new IllegalArgumentException(
                    to.getClass().getSimpleName() + " and "
                    + req.getClass().getSimpleName() + " keys must be the same");
        }

        // 0. realm
        if (req.getRealm() != null) {
            result.setRealm(req.getRealm().getValue());
        }

        // 1. auxiliary classes
        for (StringPatchItem auxClassPatch : req.getAuxClasses()) {
            switch (auxClassPatch.getOperation()) {
                case ADD_REPLACE:
                    result.getAuxClasses().add(auxClassPatch.getValue());
                    break;

                case DELETE:
                default:
                    result.getAuxClasses().remove(auxClassPatch.getValue());
            }
        }

        // 2. plain attributes
        result.getPlainAttrs().clear();
        result.getPlainAttrs().addAll(patch(EntityTOUtils.buildAttrMap(to.getPlainAttrs()), req.getPlainAttrs()));

        // 3. resources
        for (StringPatchItem resourcePatch : req.getResources()) {
            switch (resourcePatch.getOperation()) {
                case ADD_REPLACE:
                    result.getResources().add(resourcePatch.getValue());
                    break;

                case DELETE:
                default:
                    result.getResources().remove(resourcePatch.getValue());
            }
        }
    }

    public static AnyTO patch(final AnyTO anyTO, final AnyUR anyUR) {
        if (anyTO instanceof UserTO userTO) {
            return patch(userTO, (UserUR) anyUR);
        }
        if (anyTO instanceof GroupTO groupTO) {
            return patch(groupTO, (GroupUR) anyUR);
        }
        if (anyTO instanceof AnyObjectTO anyObjectTO) {
            return patch(anyObjectTO, (AnyObjectUR) anyUR);
        }
        return null;
    }

    private static void relationships(final Set<RelationshipUR> updateReqs, final RelatableTO relatable) {
        updateReqs.forEach(relPatch -> {
            if (relPatch.getType() == null || relPatch.getOtherEndKey() == null) {
                LOG.warn("Invalid {} specified: {}", RelationshipUR.class.getName(), relPatch);
            } else {
                relatable.getRelationships().stream().
                        filter(relationship -> relPatch.getType().equals(relationship.getType())
                        && relPatch.getOtherEndKey().equals(relationship.getOtherEndKey())).
                        findFirst().ifPresent(memb -> relatable.getRelationships().remove(memb));

                if (relPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                    RelationshipTO newRelationshipTO = new RelationshipTO.Builder(relPatch.getType()).
                            otherEnd(relPatch.getOtherEndKey()).
                            plainAttrs(relPatch.getPlainAttrs()).
                            build();

                    relatable.getRelationships().add(newRelationshipTO);
                }
            }
        });
    }

    public static GroupTO patch(final GroupTO groupTO, final GroupUR groupUR) {
        GroupTO result = SerializationUtils.clone(groupTO);
        patch(groupTO, groupUR, result);

        if (groupUR.getName() != null) {
            result.setName(groupUR.getName().getValue());
        }

        if (groupUR.getUserOwner() != null) {
            result.setGroupOwner(groupUR.getUserOwner().getValue());
        }
        if (groupUR.getGroupOwner() != null) {
            result.setGroupOwner(groupUR.getGroupOwner().getValue());
        }

        result.getDynMembershipConds().clear();
        result.getDynMembershipConds().putAll(groupUR.getDynMembershipConds());

        relationships(groupUR.getRelationships(), result);

        return result;
    }

    private static void memberships(final Set<MembershipUR> updateReqs, final GroupableRelatableTO groupable) {
        updateReqs.forEach(membPatch -> {
            if (membPatch.getGroup() == null) {
                LOG.warn("Invalid {} specified: {}", MembershipUR.class.getName(), membPatch);
            } else {
                groupable.getMemberships().stream().
                        filter(membership -> membPatch.getGroup().equals(membership.getGroupKey())).
                        findFirst().ifPresent(memb -> groupable.getMemberships().remove(memb));

                if (membPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                    MembershipTO newMembershipTO = new MembershipTO.Builder(membPatch.getGroup()).
                            // 3. plain attributes
                            plainAttrs(membPatch.getPlainAttrs()).
                            build();

                    groupable.getMemberships().add(newMembershipTO);
                }
            }
        });
    }

    public static AnyObjectTO patch(final AnyObjectTO anyObjectTO, final AnyObjectUR anyObjectUR) {
        AnyObjectTO result = SerializationUtils.clone(anyObjectTO);
        patch(anyObjectTO, anyObjectUR, result);

        if (anyObjectUR.getName() != null) {
            result.setName(anyObjectUR.getName().getValue());
        }

        // 1. relationships
        relationships(anyObjectUR.getRelationships(), result);

        // 2. memberships
        memberships(anyObjectUR.getMemberships(), result);

        return result;
    }

    public static UserTO patch(final UserTO userTO, final UserUR userUR) {
        UserTO result = SerializationUtils.clone(userTO);
        patch(userTO, userUR, result);

        // 1. password
        if (userUR.getPassword() != null) {
            result.setPassword(userUR.getPassword().getValue());
        }

        // 2. username
        if (userUR.getUsername() != null) {
            result.setUsername(userUR.getUsername().getValue());
        }

        // 3. relationships
        relationships(userUR.getRelationships(), result);

        // 4. memberships
        memberships(userUR.getMemberships(), result);

        // 5. roles
        for (StringPatchItem rolePatch : userUR.getRoles()) {
            switch (rolePatch.getOperation()) {
                case ADD_REPLACE:
                    result.getRoles().add(rolePatch.getValue());
                    break;

                case DELETE:
                default:
                    result.getRoles().remove(rolePatch.getValue());
            }
        }

        // 6. linked accounts
        userUR.getLinkedAccounts().forEach(accountPatch -> {
            if (accountPatch.getLinkedAccountTO() == null) {
                LOG.warn("Invalid {} specified: {}", LinkedAccountUR.class.getName(), accountPatch);
            } else {
                result.getLinkedAccounts().remove(accountPatch.getLinkedAccountTO());
                if (accountPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                    result.getLinkedAccounts().add(accountPatch.getLinkedAccountTO());
                }
            }
        });

        return result;
    }

    /**
     * Add PLAIN attribute DELETE patch for those attributes of the input AnyTO without values or containing null value
     *
     * @param anyTO User, Group or Any Object to look for attributes with no value
     * @param anyUR update req to enrich with DELETE statements
     */
    public static void cleanEmptyAttrs(final AnyTO anyTO, final AnyUR anyUR) {
        anyUR.getPlainAttrs().addAll(anyTO.getPlainAttrs().stream().
                filter(AnyOperations::isEmpty).
                map(plainAttr -> new AttrPatch.Builder(new Attr.Builder(plainAttr.getSchema()).build()).
                operation(PatchOperation.DELETE).
                build()).collect(Collectors.toSet()));
    }

    private static boolean isEmpty(final Attr attr) {
        return attr.getValues().isEmpty() || NULL_SINGLETON_LIST.equals(attr.getValues());
    }
}
