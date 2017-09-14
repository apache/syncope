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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.RelationshipPatch;
import org.apache.syncope.common.lib.patch.AbstractReplacePatchItem;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for comparing {@link AnyTO} instances in order to generate {@link AnyPatch} instances.
 */
public final class AnyOperations {

    private static final Logger LOG = LoggerFactory.getLogger(AnyOperations.class);

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

    private static void diff(
            final MembershipTO updated,
            final MembershipTO original,
            final MembershipPatch result,
            final boolean incremental) {

        // check same key
        if (updated.getGroupKey() == null && original.getGroupKey() != null
                || (updated.getGroupKey() != null && !updated.getGroupKey().equals(original.getGroupKey()))) {

            throw new IllegalArgumentException("Memberships must be the same");
        }
        result.setGroup(updated.getGroupKey());

        // 1. plain attributes
        result.getPlainAttrs().clear();
        result.getPlainAttrs().addAll(updated.getPlainAttrs());

        // 2. virtual attributes
        result.getVirAttrs().clear();
        result.getVirAttrs().addAll(updated.getVirAttrs());
    }

    private static void diff(
            final AnyTO updated, final AnyTO original, final AnyPatch result, final boolean incremental) {

        // check same key
        if (updated.getKey() == null && original.getKey() != null
                || (updated.getKey() != null && !updated.getKey().equals(original.getKey()))) {

            throw new IllegalArgumentException("AnyTO's key must be the same");
        }
        result.setKey(updated.getKey());

        // 1. realm
        result.setRealm(replacePatchItem(updated.getRealm(), original.getRealm(), new StringReplacePatchItem()));

        // 2. auxilairy classes
        result.getAuxClasses().clear();

        if (!incremental) {
            original.getAuxClasses().stream().filter(auxClass -> !updated.getAuxClasses().contains(auxClass)).
                    forEach(auxClass -> {
                        result.getAuxClasses().add(new StringPatchItem.Builder().
                                operation(PatchOperation.DELETE).value(auxClass).build());
                    });
        }

        updated.getAuxClasses().stream().filter(auxClass -> !original.getAuxClasses().contains(auxClass)).
                forEach(auxClass -> {
                    result.getAuxClasses().add(new StringPatchItem.Builder().
                            operation(PatchOperation.ADD_REPLACE).value(auxClass).build());
                });

        // 3. plain attributes
        Map<String, AttrTO> updatedAttrs = EntityTOUtils.buildAttrMap(updated.getPlainAttrs());
        Map<String, AttrTO> originalAttrs = EntityTOUtils.buildAttrMap(original.getPlainAttrs());

        result.getPlainAttrs().clear();

        if (!incremental) {
            originalAttrs.keySet().stream().filter(attr -> !updatedAttrs.containsKey(attr)).
                    forEach(schema -> {
                        result.getPlainAttrs().add(new AttrPatch.Builder().
                                operation(PatchOperation.DELETE).
                                attrTO(new AttrTO.Builder().schema(schema).build()).
                                build());
                    });
        }

        updatedAttrs.values().forEach(attrTO -> {
            if (attrTO.getValues().isEmpty()) {
                if (!incremental) {
                    result.getPlainAttrs().add(new AttrPatch.Builder().
                            operation(PatchOperation.DELETE).
                            attrTO(new AttrTO.Builder().schema(attrTO.getSchema()).build()).
                            build());
                }
            } else if (!originalAttrs.containsKey(attrTO.getSchema())
                    || !originalAttrs.get(attrTO.getSchema()).getValues().equals(attrTO.getValues())) {

                AttrPatch patch = new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).attrTO(attrTO).
                        build();
                if (!patch.isEmpty()) {
                    result.getPlainAttrs().add(patch);
                }
            }
        });

        // 4. virtual attributes
        result.getVirAttrs().clear();
        result.getVirAttrs().addAll(updated.getVirAttrs());

        // 5. resources
        result.getResources().clear();

        if (!incremental) {
            original.getResources().stream().filter(resource -> !updated.getResources().contains(resource)).
                    forEach(resource -> {
                        result.getResources().add(new StringPatchItem.Builder().
                                operation(PatchOperation.DELETE).value(resource).build());
                    });
        }

        updated.getResources().stream().filter(resource -> !original.getResources().contains(resource)).
                forEach(resource -> {
                    result.getResources().add(new StringPatchItem.Builder().
                            operation(PatchOperation.ADD_REPLACE).value(resource).build());
                });
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated AnyObjectTO
     * @param original original AnyObjectTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return AnyObjectPatch containing differences
     */
    public static AnyObjectPatch diff(
            final AnyObjectTO updated, final AnyObjectTO original, final boolean incremental) {

        AnyObjectPatch result = new AnyObjectPatch();

        diff(updated, original, result, incremental);

        // 1. name
        result.setName(replacePatchItem(updated.getName(), original.getName(), new StringReplacePatchItem()));

        // 2. relationships
        Map<Pair<String, String>, RelationshipTO> updatedRels =
                EntityTOUtils.buildRelationshipMap(updated.getRelationships());
        Map<Pair<String, String>, RelationshipTO> originalRels =
                EntityTOUtils.buildRelationshipMap(original.getRelationships());

        updatedRels.entrySet().stream().
                filter(entry -> (!originalRels.containsKey(entry.getKey()))).
                forEachOrdered(entry -> {
                    result.getRelationships().add(new RelationshipPatch.Builder().
                            operation(PatchOperation.ADD_REPLACE).
                            relationshipTO(entry.getValue()).build());
                });

        if (!incremental) {
            originalRels.keySet().stream().filter(relationship -> !updatedRels.containsKey(relationship)).
                    forEach(key -> {
                        result.getRelationships().add(new RelationshipPatch.Builder().
                                operation(PatchOperation.DELETE).
                                relationshipTO(originalRels.get(key)).build());
                    });
        }

        // 3. memberships
        Map<String, MembershipTO> updatedMembs = EntityTOUtils.buildMembershipMap(updated.getMemberships());
        Map<String, MembershipTO> originalMembs = EntityTOUtils.buildMembershipMap(original.getMemberships());

        updatedMembs.entrySet().stream().
                filter(entry -> (!originalMembs.containsKey(entry.getKey()))).
                forEachOrdered(entry -> {
                    result.getMemberships().add(new MembershipPatch.Builder().
                            operation(PatchOperation.ADD_REPLACE).group(entry.getValue().getGroupKey()).
                            build());
                });

        if (!incremental) {
            originalMembs.keySet().stream().filter(membership -> !updatedMembs.containsKey(membership)).
                    forEach(key -> {
                        result.getMemberships().add(new MembershipPatch.Builder().
                                operation(PatchOperation.DELETE).group(originalMembs.get(key).getGroupKey()).build());
                    });
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated UserTO
     * @param original original UserTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return UserPatch containing differences
     */
    public static UserPatch diff(final UserTO updated, final UserTO original, final boolean incremental) {
        UserPatch result = new UserPatch();

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
                    forEach(toRemove -> {
                        result.getRoles().add(new StringPatchItem.Builder().
                                operation(PatchOperation.DELETE).value(toRemove).build());
                    });
        }

        updated.getRoles().stream().filter(role -> !original.getRoles().contains(role)).
                forEach(toAdd -> {
                    result.getRoles().add(new StringPatchItem.Builder().
                            operation(PatchOperation.ADD_REPLACE).value(toAdd).build());
                });

        // 5. relationships
        Map<Pair<String, String>, RelationshipTO> updatedRels =
                EntityTOUtils.buildRelationshipMap(updated.getRelationships());
        Map<Pair<String, String>, RelationshipTO> originalRels =
                EntityTOUtils.buildRelationshipMap(original.getRelationships());

        updatedRels.entrySet().stream().
                filter(entry -> (!originalRels.containsKey(entry.getKey()))).
                forEachOrdered(entry -> {
                    result.getRelationships().add(new RelationshipPatch.Builder().
                            operation(PatchOperation.ADD_REPLACE).
                            relationshipTO(entry.getValue()).build());
                });

        if (!incremental) {
            originalRels.keySet().stream().filter(relationship -> !updatedRels.containsKey(relationship)).
                    forEach(key -> {
                        result.getRelationships().add(new RelationshipPatch.Builder().
                                operation(PatchOperation.DELETE).
                                relationshipTO(originalRels.get(key)).build());
                    });
        }

        // 6. memberships
        Map<String, MembershipTO> updatedMembs = EntityTOUtils.buildMembershipMap(updated.getMemberships());
        Map<String, MembershipTO> originalMembs = EntityTOUtils.buildMembershipMap(original.getMemberships());

        updatedMembs.entrySet().stream().
                map(entry -> {
                    MembershipPatch membershipPatch = new MembershipPatch.Builder().
                            operation(PatchOperation.ADD_REPLACE).group(entry.getValue().getGroupKey()).build();
                    MembershipTO omemb;
                    if (originalMembs.containsKey(entry.getKey())) {
                        // get the original membership
                        omemb = originalMembs.get(entry.getKey());
                    } else {
                        // create an empty one to generate the patch
                        omemb = new MembershipTO.Builder().group(entry.getKey()).build();
                    }
                    diff(entry.getValue(), omemb, membershipPatch, incremental);
                    return membershipPatch;
                }).
                forEachOrdered(membershipPatch -> {
                    result.getMemberships().add(membershipPatch);
                });

        if (!incremental) {
            originalMembs.keySet().stream().filter(membership -> !updatedMembs.containsKey(membership)).
                    forEach(key -> {
                        result.getMemberships().add(new MembershipPatch.Builder().
                                operation(PatchOperation.DELETE).group(originalMembs.get(key).getGroupKey()).build());
                    });
        }

        return result;
    }

    /**
     * Calculate modifications needed by first in order to be equal to second.
     *
     * @param updated updated GroupTO
     * @param original original GroupTO
     * @param incremental perform incremental diff (without removing existing info)
     * @return GroupPatch containing differences
     */
    public static GroupPatch diff(final GroupTO updated, final GroupTO original, final boolean incremental) {
        GroupPatch result = new GroupPatch();

        diff(updated, original, result, incremental);

        // 1. name
        result.setName(replacePatchItem(updated.getName(), original.getName(), new StringReplacePatchItem()));

        // 2. ownership
        result.setUserOwner(
                replacePatchItem(updated.getUserOwner(), original.getUserOwner(), new StringReplacePatchItem()));
        result.setGroupOwner(
                replacePatchItem(updated.getGroupOwner(), original.getGroupOwner(), new StringReplacePatchItem()));

        // 3. dynamic membership
        result.setUDynMembershipCond(updated.getUDynMembershipCond());
        result.getADynMembershipConds().putAll(updated.getADynMembershipConds());

        // 4. type extensions
        result.getTypeExtensions().addAll(updated.getTypeExtensions());

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <TO extends AnyTO, P extends AnyPatch> P diff(
            final TO updated, final TO original, final boolean incremental) {

        if (updated instanceof UserTO && original instanceof UserTO) {
            return (P) diff((UserTO) updated, (UserTO) original, incremental);
        } else if (updated instanceof GroupTO && original instanceof GroupTO) {
            return (P) diff((GroupTO) updated, (GroupTO) original, incremental);
        } else if (updated instanceof AnyObjectTO && original instanceof AnyObjectTO) {
            return (P) diff((AnyObjectTO) updated, (AnyObjectTO) original, incremental);
        }

        throw new IllegalArgumentException("Unsupported: " + updated.getClass().getName());
    }

    private static Collection<AttrTO> patch(final Map<String, AttrTO> attrs, final Set<AttrPatch> attrPatches) {
        Map<String, AttrTO> rwattrs = new HashMap<>(attrs);
        attrPatches.forEach(patch -> {
            if (patch.getAttrTO() == null) {
                LOG.warn("Invalid {} specified: {}", AttrPatch.class.getName(), patch);
            } else {
                AttrTO removed = rwattrs.remove(patch.getAttrTO().getSchema());
                if (removed != null && removed.getSchemaInfo() != null) {
                    patch.getAttrTO().setSchemaInfo(removed.getSchemaInfo());
                }
                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    rwattrs.put(patch.getAttrTO().getSchema(), patch.getAttrTO());
                }
            }
        });

        return rwattrs.values();
    }

    private static <T extends AnyTO, K extends AnyPatch> void patch(final T to, final K patch, final T result) {
        // check same key
        if (to.getKey() == null || !to.getKey().equals(patch.getKey())) {
            throw new IllegalArgumentException(
                    to.getClass().getSimpleName() + " and " + patch.getClass().getSimpleName()
                    + " keys must be the same");
        }

        // 0. realm
        if (patch.getRealm() != null) {
            result.setRealm(patch.getRealm().getValue());
        }

        // 1. auxiliary classes
        for (StringPatchItem auxClassPatch : patch.getAuxClasses()) {
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
        result.getPlainAttrs().addAll(patch(EntityTOUtils.buildAttrMap(to.getPlainAttrs()), patch.getPlainAttrs()));

        // 3. virtual attributes
        result.getVirAttrs().clear();
        result.getVirAttrs().addAll(patch.getVirAttrs());

        // 4. resources
        for (StringPatchItem resourcePatch : patch.getResources()) {
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

    public static AnyTO patch(final AnyTO anyTO, final AnyPatch anyPatch) {
        if (anyTO instanceof UserTO) {
            return patch((UserTO) anyTO, (UserPatch) anyPatch);
        }
        if (anyTO instanceof GroupTO) {
            return patch((GroupTO) anyTO, (GroupPatch) anyPatch);
        }
        if (anyTO instanceof AnyObjectTO) {
            return patch((AnyObjectTO) anyTO, (AnyObjectPatch) anyPatch);
        }
        return null;
    }

    public static GroupTO patch(final GroupTO groupTO, final GroupPatch groupPatch) {
        GroupTO result = SerializationUtils.clone(groupTO);
        patch(groupTO, groupPatch, result);

        if (groupPatch.getName() != null) {
            result.setName(groupPatch.getName().getValue());
        }

        if (groupPatch.getUserOwner() != null) {
            result.setGroupOwner(groupPatch.getUserOwner().getValue());
        }
        if (groupPatch.getGroupOwner() != null) {
            result.setGroupOwner(groupPatch.getGroupOwner().getValue());
        }

        result.setUDynMembershipCond(groupPatch.getUDynMembershipCond());
        result.getADynMembershipConds().clear();
        result.getADynMembershipConds().putAll(groupPatch.getADynMembershipConds());

        return result;
    }

    public static AnyObjectTO patch(final AnyObjectTO anyObjectTO, final AnyObjectPatch anyObjectPatch) {
        AnyObjectTO result = SerializationUtils.clone(anyObjectTO);
        patch(anyObjectTO, anyObjectPatch, result);

        if (anyObjectPatch.getName() != null) {
            result.setName(anyObjectPatch.getName().getValue());
        }

        // 1. relationships
        anyObjectPatch.getRelationships().
                forEach(relPatch -> {
                    if (relPatch.getRelationshipTO() == null) {
                        LOG.warn("Invalid {} specified: {}", RelationshipPatch.class.getName(), relPatch);
                    } else {
                        result.getRelationships().remove(relPatch.getRelationshipTO());
                        if (relPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                            result.getRelationships().add(relPatch.getRelationshipTO());
                        }
                    }
                });

        // 2. memberships
        anyObjectPatch.getMemberships().
                forEach(membPatch -> {
                    if (membPatch.getGroup() == null) {
                        LOG.warn("Invalid {} specified: {}", MembershipPatch.class.getName(), membPatch);
                    } else {
                        Optional<MembershipTO> memb = result.getMemberships().stream().
                                filter(membership -> membPatch.getGroup().equals(membership.getGroupKey())).findFirst();
                        if (memb.isPresent()) {
                            result.getMemberships().remove(memb.get());
                        }

                        if (membPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                            MembershipTO newMembershipTO =
                                    new MembershipTO.Builder().group(membPatch.getGroup()).build();

                            // 3. plain attributes
                            newMembershipTO.getPlainAttrs().addAll(membPatch.getPlainAttrs());

                            // 4. virtual attributes
                            newMembershipTO.getVirAttrs().addAll(membPatch.getVirAttrs());

                            result.getMemberships().add(newMembershipTO);
                        }
                    }
                });

        return result;
    }

    public static UserTO patch(final UserTO userTO, final UserPatch userPatch) {
        UserTO result = SerializationUtils.clone(userTO);
        patch(userTO, userPatch, result);

        // 1. password
        if (userPatch.getPassword() != null) {
            result.setPassword(userPatch.getPassword().getValue());
        }

        // 2. username
        if (userPatch.getUsername() != null) {
            result.setUsername(userPatch.getUsername().getValue());
        }

        // 3. relationships
        userPatch.getRelationships().
                forEach(relPatch -> {
                    if (relPatch.getRelationshipTO() == null) {
                        LOG.warn("Invalid {} specified: {}", RelationshipPatch.class.getName(), relPatch);
                    } else {
                        result.getRelationships().remove(relPatch.getRelationshipTO());
                        if (relPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                            result.getRelationships().add(relPatch.getRelationshipTO());
                        }
                    }
                });

        // 4. memberships
        userPatch.getMemberships().
                forEach(membPatch -> {
                    if (membPatch.getGroup() == null) {
                        LOG.warn("Invalid {} specified: {}", MembershipPatch.class.getName(), membPatch);
                    } else {
                        Optional<MembershipTO> memb = result.getMemberships().stream().
                                filter(membership -> membPatch.getGroup().equals(membership.getGroupKey())).findFirst();
                        if (memb.isPresent()) {
                            result.getMemberships().remove(memb.get());
                        }

                        if (membPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                            MembershipTO newMembershipTO =
                                    new MembershipTO.Builder().group(membPatch.getGroup()).build();

                            // 3. plain attributes
                            newMembershipTO.getPlainAttrs().addAll(membPatch.getPlainAttrs());

                            // 4. virtual attributes
                            newMembershipTO.getVirAttrs().addAll(membPatch.getVirAttrs());

                            result.getMemberships().add(newMembershipTO);
                        }
                    }
                });

        // 5. roles
        for (StringPatchItem rolePatch : userPatch.getRoles()) {
            switch (rolePatch.getOperation()) {
                case ADD_REPLACE:
                    result.getRoles().add(rolePatch.getValue());
                    break;

                case DELETE:
                default:
                    result.getRoles().remove(rolePatch.getValue());
            }
        }

        return result;
    }
}
