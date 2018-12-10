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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipPatch;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipPatch;
import org.apache.syncope.common.lib.request.AbstractReplacePatchItem;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
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
 * Utility class for comparing {@link AnyTO} instances in order to generate {@link AnyUR} instances.
 */
public final class AnyOperations {

    private static final Logger LOG = LoggerFactory.getLogger(AnyOperations.class);

    private static final Set<String> NULL_SINGLETON = Collections.singleton(null);

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
            final AnyTO updated, final AnyTO original, final AnyUR result, final boolean incremental) {

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
            if (isEmpty(attrTO)) {
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
     * @return {@link AnyObjectUR} containing differences
     */
    public static AnyObjectUR diff(
            final AnyObjectTO updated, final AnyObjectTO original, final boolean incremental) {

        AnyObjectUR result = new AnyObjectUR();

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

        updatedMembs.forEach((key, value) -> {
            MembershipPatch membershipPatch = new MembershipPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).group(value.getGroupKey()).build();

            diff(value, membershipPatch);

            if (!originalMembs.containsKey(key)
                    || (!membershipPatch.getPlainAttrs().isEmpty() || !membershipPatch.getVirAttrs().isEmpty())) {

                result.getMemberships().add(membershipPatch);
            }
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

    private static void diff(
            final MembershipTO updated,
            final MembershipPatch result) {

        // 1. plain attributes
        result.getPlainAttrs().addAll(updated.getPlainAttrs().stream().
                filter(attrTO -> !isEmpty(attrTO)).
                collect(Collectors.toSet()));

        // 2. virtual attributes
        result.getVirAttrs().clear();
        result.getVirAttrs().addAll(updated.getVirAttrs());
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

        updatedMembs.forEach((key, value) -> {
            MembershipPatch membershipPatch = new MembershipPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).group(value.getGroupKey()).build();

            diff(value, membershipPatch);

            if (!originalMembs.containsKey(key)
                    || (!membershipPatch.getPlainAttrs().isEmpty() || !membershipPatch.getVirAttrs().isEmpty())) {

                result.getMemberships().add(membershipPatch);
            }
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
        result.setUDynMembershipCond(updated.getUDynMembershipCond());
        result.getADynMembershipConds().putAll(updated.getADynMembershipConds());

        // 4. type extensions
        result.getTypeExtensions().addAll(updated.getTypeExtensions());

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <TO extends AnyTO, P extends AnyUR> P diff(
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
                if (patch.getOperation() == PatchOperation.ADD_REPLACE && !patch.getAttrTO().getValues().isEmpty()) {
                    rwattrs.put(patch.getAttrTO().getSchema(), patch.getAttrTO());
                }
            }
        });

        return rwattrs.values();
    }

    private static <T extends AnyTO, K extends AnyUR> void patch(final T to, final K req, final T result) {
        // check same key
        if (to.getKey() == null || !to.getKey().equals(req.getKey())) {
            throw new IllegalArgumentException(
                    to.getClass().getSimpleName() + " and " + req.getClass().getSimpleName()
                    + " keys must be the same");
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

        // 3. virtual attributes
        result.getVirAttrs().clear();
        result.getVirAttrs().addAll(req.getVirAttrs());

        // 4. resources
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
        if (anyTO instanceof UserTO) {
            return patch((UserTO) anyTO, (UserUR) anyUR);
        }
        if (anyTO instanceof GroupTO) {
            return patch((GroupTO) anyTO, (GroupUR) anyUR);
        }
        if (anyTO instanceof AnyObjectTO) {
            return patch((AnyObjectTO) anyTO, (AnyObjectUR) anyUR);
        }
        return null;
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

        result.setUDynMembershipCond(groupUR.getUDynMembershipCond());
        result.getADynMembershipConds().clear();
        result.getADynMembershipConds().putAll(groupUR.getADynMembershipConds());

        return result;
    }

    public static AnyObjectTO patch(final AnyObjectTO anyObjectTO, final AnyObjectUR anyObjectUR) {
        AnyObjectTO result = SerializationUtils.clone(anyObjectTO);
        patch(anyObjectTO, anyObjectUR, result);

        if (anyObjectUR.getName() != null) {
            result.setName(anyObjectUR.getName().getValue());
        }

        // 1. relationships
        anyObjectUR.getRelationships().
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
        anyObjectUR.getMemberships().
                forEach(membPatch -> {
                    if (membPatch.getGroup() == null) {
                        LOG.warn("Invalid {} specified: {}", MembershipPatch.class.getName(), membPatch);
                    } else {
                        result.getMemberships().stream().
                                filter(membership -> membPatch.getGroup().equals(membership.getGroupKey())).
                                findFirst().ifPresent(memb -> result.getMemberships().remove(memb));

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
        userUR.getRelationships().
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
        userUR.getMemberships().
                forEach(membPatch -> {
                    if (membPatch.getGroup() == null) {
                        LOG.warn("Invalid {} specified: {}", MembershipPatch.class.getName(), membPatch);
                    } else {
                        result.getMemberships().stream().
                                filter(membership -> membPatch.getGroup().equals(membership.getGroupKey())).
                                findFirst().ifPresent(memb -> result.getMemberships().remove(memb));

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
                filter(plainAttrTO -> isEmpty(plainAttrTO)).
                map(plainAttrTO -> new AttrPatch.Builder().
                operation(PatchOperation.DELETE).
                attrTO(new AttrTO.Builder().schema(plainAttrTO.getSchema()).build()).
                build()).collect(Collectors.toSet()));
    }

    private static boolean isEmpty(final AttrTO attrTO) {
        return attrTO.getValues().isEmpty() || NULL_SINGLETON.equals(attrTO.getValues());
    }
}
