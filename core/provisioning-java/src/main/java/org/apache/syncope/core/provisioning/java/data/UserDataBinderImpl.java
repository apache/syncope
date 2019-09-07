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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class UserDataBinderImpl extends AbstractAnyDataBinder implements UserDataBinder {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private ConfParamOps confParamOps;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Transactional(readOnly = true)
    @Override
    public UserTO returnUserTO(final UserTO userTO) {
        if (!confParamOps.get(AuthContextUtils.getDomain(), "return.password.value", false, Boolean.class)) {
            userTO.setPassword(null);
        }
        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getAuthenticatedUserTO() {
        final UserTO authUserTO;

        String authUsername = AuthContextUtils.getUsername();
        if (anonymousUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(null);
            authUserTO.setUsername(anonymousUser);
        } else if (adminUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(null);
            authUserTO.setUsername(adminUser);
        } else {
            User authUser = userDAO.findByUsername(authUsername);
            authUserTO = getUserTO(authUser, true);
        }

        return authUserTO;
    }

    private void setPassword(final User user, final String password, final SyncopeClientCompositeException scce) {
        try {
            String algorithm = confParamOps.get(AuthContextUtils.getDomain(),
                    "password.cipher.algorithm", CipherAlgorithm.AES.name(), String.class);
            user.setPassword(password, CipherAlgorithm.valueOf(algorithm));
        } catch (IllegalArgumentException e) {
            SyncopeClientException invalidCiperAlgorithm = SyncopeClientException.build(ClientExceptionType.NotFound);
            invalidCiperAlgorithm.getElements().add(e.getMessage());
            scce.addException(invalidCiperAlgorithm);

            throw scce;
        }
    }

    @Override
    public void create(final User user, final UserCR userCR) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // set username
        user.setUsername(userCR.getUsername());

        // set password
        if (StringUtils.isBlank(userCR.getPassword()) || !userCR.isStorePassword()) {
            LOG.debug("Password was not provided or not required to be stored");
        } else {
            setPassword(user, userCR.getPassword(), scce);
            user.setChangePwdDate(new Date());
        }

        user.setMustChangePassword(userCR.isMustChangePassword());

        // security question / answer
        if (userCR.getSecurityQuestion() != null) {
            SecurityQuestion securityQuestion = securityQuestionDAO.find(userCR.getSecurityQuestion());
            if (securityQuestion != null) {
                user.setSecurityQuestion(securityQuestion);
            }
        }
        user.setSecurityAnswer(userCR.getSecurityAnswer());

        // roles
        userCR.getRoles().forEach(roleKey -> {
            Role role = roleDAO.find(roleKey);
            if (role == null) {
                LOG.warn("Ignoring unknown role with id {}", roleKey);
            } else {
                user.add(role);
            }
        });

        // realm
        Realm realm = realmDAO.findByFullPath(userCR.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + userCR.getRealm());
            scce.addException(noRealm);
        }
        user.setRealm(realm);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.USER);
        if (user.getRealm() != null) {
            // relationships
            userCR.getRelationships().forEach(relationshipTO -> {
                AnyObject otherEnd = anyObjectDAO.find(relationshipTO.getOtherEndKey());
                if (otherEnd == null) {
                    LOG.debug("Ignoring invalid anyObject " + relationshipTO.getOtherEndKey());
                } else if (user.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                    RelationshipType relationshipType = relationshipTypeDAO.find(relationshipTO.getType());
                    if (relationshipType == null) {
                        LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType());
                    } else {
                        URelationship relationship = entityFactory.newEntity(URelationship.class);
                        relationship.setType(relationshipType);
                        relationship.setRightEnd(otherEnd);
                        relationship.setLeftEnd(user);

                        user.add(relationship);
                    }
                } else {
                    LOG.error("{} cannot be assigned to {}", otherEnd, user);

                    SyncopeClientException unassignabled =
                            SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                    unassignabled.getElements().add("Cannot be assigned: " + otherEnd);
                    scce.addException(unassignabled);
                }
            });

            // memberships
            userCR.getMemberships().forEach(membershipTO -> {
                Group group = membershipTO.getGroupKey() == null
                        ? groupDAO.findByName(membershipTO.getGroupName())
                        : groupDAO.find(membershipTO.getGroupKey());
                if (group == null) {
                    LOG.debug("Ignoring invalid group "
                            + membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
                } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    UMembership membership = entityFactory.newEntity(UMembership.class);
                    membership.setRightEnd(group);
                    membership.setLeftEnd(user);

                    user.add(membership);

                    // membership attributes
                    fill(user, membership, membershipTO, anyUtils, scce);
                } else {
                    LOG.error("{} cannot be assigned to {}", group, user);

                    SyncopeClientException unassignable =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignable.getElements().add("Cannot be assigned: " + group);
                    scce.addException(unassignable);
                }
            });
        }

        // attributes and resources
        fill(user, userCR, anyUtils, scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    private boolean isPasswordMapped(final ExternalResource resource) {
        boolean result = false;

        Optional<? extends Provision> provision = resource.getProvision(anyTypeDAO.findUser());
        if (provision.isPresent() && provision.get().getMapping() != null) {
            result = provision.get().getMapping().getItems().stream().anyMatch(item -> item.isPassword());
        }

        return result;
    }

    @Override
    public PropagationByResource update(final User toBeUpdated, final UserUR userUR) {
        // Re-merge any pending change from workflow tasks
        User user = userDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.USER);

        Collection<String> currentResources = userDAO.findAllResourceKeys(user.getKey());

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(user, anyUtils);

        // realm
        setRealm(user, userUR);

        // password
        if (userUR.getPassword() != null && StringUtils.isNotBlank(userUR.getPassword().getValue())) {
            if (userUR.getPassword().isOnSyncope()) {
                setPassword(user, userUR.getPassword().getValue(), scce);
                user.setChangePwdDate(new Date());
            }

            propByRes.addAll(ResourceOperation.UPDATE, userUR.getPassword().getResources());
        }

        // username
        if (userUR.getUsername() != null && StringUtils.isNotBlank(userUR.getUsername().getValue())) {
            String oldUsername = user.getUsername();
            user.setUsername(userUR.getUsername().getValue());

            if (oldUsername.equals(AuthContextUtils.getUsername())) {
                AuthContextUtils.updateUsername(userUR.getUsername().getValue());
            }

            AccessToken accessToken = accessTokenDAO.findByOwner(oldUsername);
            if (accessToken != null) {
                accessToken.setOwner(userUR.getUsername().getValue());
                accessTokenDAO.save(accessToken);
            }

            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // security question / answer:
        if (userUR.getSecurityQuestion() != null) {
            if (userUR.getSecurityQuestion().getValue() == null) {
                user.setSecurityQuestion(null);
                user.setSecurityAnswer(null);
            } else {
                SecurityQuestion securityQuestion =
                        securityQuestionDAO.find(userUR.getSecurityQuestion().getValue());
                if (securityQuestion != null) {
                    user.setSecurityQuestion(securityQuestion);
                    user.setSecurityAnswer(userUR.getSecurityAnswer().getValue());
                }
            }
        }

        if (userUR.getMustChangePassword() != null) {
            user.setMustChangePassword(userUR.getMustChangePassword().getValue());

            propByRes.addAll(
                    ResourceOperation.UPDATE,
                    anyUtils.getAllResources(toBeUpdated).stream().
                            filter(resource -> resource.getProvision(toBeUpdated.getType()).isPresent()).
                            filter(resource -> mappingManager.hasMustChangePassword(
                            resource.getProvision(toBeUpdated.getType()).get())).
                            map(Entity::getKey).
                            collect(Collectors.toSet()));
        }

        // roles
        for (StringPatchItem patch : userUR.getRoles()) {
            Role role = roleDAO.find(patch.getValue());
            if (role == null) {
                LOG.warn("Ignoring unknown role with key {}", patch.getValue());
            } else {
                switch (patch.getOperation()) {
                    case ADD_REPLACE:
                        user.add(role);
                        break;

                    case DELETE:
                    default:
                        user.getRoles().remove(role);
                }
            }
        }

        // attributes and resources
        propByRes.merge(fill(user, userUR, anyUtils, scce));

        // relationships
        userUR.getRelationships().stream().
                filter(patch -> patch.getRelationshipTO() != null).forEachOrdered((patch) -> {
            RelationshipType relationshipType = relationshipTypeDAO.find(patch.getRelationshipTO().getType());
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
            } else {
                user.getRelationship(relationshipType, patch.getRelationshipTO().getOtherEndKey()).
                        ifPresent(relationship -> {
                            user.getRelationships().remove(relationship);
                            relationship.setLeftEnd(null);
                        });

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    AnyObject otherEnd = anyObjectDAO.find(patch.getRelationshipTO().getOtherEndKey());
                    if (otherEnd == null) {
                        LOG.debug("Ignoring invalid any object {}", patch.getRelationshipTO().getOtherEndKey());
                    } else if (user.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                        URelationship newRelationship = entityFactory.newEntity(URelationship.class);
                        newRelationship.setType(relationshipType);
                        newRelationship.setRightEnd(otherEnd);
                        newRelationship.setLeftEnd(user);

                        user.add(newRelationship);
                    } else {
                        LOG.error("{} cannot be assigned to {}", otherEnd, user);

                        SyncopeClientException unassignable =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        unassignable.getElements().add("Cannot be assigned: " + otherEnd);
                        scce.addException(unassignable);
                    }
                }
            }
        });

        // prepare for membership-related resource management
        Collection<ExternalResource> resources = userDAO.findAllResources(user);

        Map<String, Set<String>> reasons = new HashMap<>();
        user.getResources().forEach(resource -> {
            reasons.put(resource.getKey(), new HashSet<>(Set.of(user.getKey())));
        });
        userDAO.findAllGroupKeys(user).forEach(group -> {
            groupDAO.findAllResourceKeys(group).forEach(resource -> {
                if (!reasons.containsKey(resource)) {
                    reasons.put(resource, new HashSet<>());
                }
                reasons.get(resource).add(group);
            });
        });

        Set<String> toBeDeprovisioned = new HashSet<>();
        Set<String> toBeProvisioned = new HashSet<>();

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // memberships
        userUR.getMemberships().stream().
                filter(membPatch -> membPatch.getGroup() != null).forEachOrdered((membPatch) -> {
            user.getMembership(membPatch.getGroup()).ifPresent(membership -> {
                user.remove(membership);
                membership.setLeftEnd(null);
                user.getPlainAttrs(membership).forEach(attr -> {
                    user.remove(attr);
                    attr.setOwner(null);
                    attr.setMembership(null);
                    plainAttrValueDAO.deleteAll(attr, anyUtils);
                    plainAttrDAO.delete(attr);
                });

                if (membPatch.getOperation() == PatchOperation.DELETE) {
                    groupDAO.findAllResourceKeys(membership.getRightEnd().getKey()).stream().
                            filter(resource -> reasons.containsKey(resource)).
                            forEach(resource -> {
                                reasons.get(resource).remove(membership.getRightEnd().getKey());
                                toBeProvisioned.add(resource);
                            });
                }
            });
            if (membPatch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.find(membPatch.getGroup());
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", membPatch.getGroup());
                } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    UMembership newMembership = entityFactory.newEntity(UMembership.class);
                    newMembership.setRightEnd(group);
                    newMembership.setLeftEnd(user);

                    user.add(newMembership);

                    membPatch.getPlainAttrs().forEach(attrTO -> {
                        PlainSchema schema = getPlainSchema(attrTO.getSchema());
                        if (schema == null) {
                            LOG.debug("Invalid " + PlainSchema.class.getSimpleName()
                                    + "{}, ignoring...", attrTO.getSchema());
                        } else {
                            UPlainAttr attr = user.getPlainAttr(schema.getKey(), newMembership).orElse(null);
                            if (attr == null) {
                                LOG.debug("No plain attribute found for {} and membership of {}",
                                        schema, newMembership.getRightEnd());

                                attr = anyUtils.newPlainAttr();
                                attr.setOwner(user);
                                attr.setMembership(newMembership);
                                attr.setSchema(schema);
                                user.add(attr);

                                AttrPatch patch = new AttrPatch.Builder(attrTO).build();
                                processAttrPatch(
                                        user, patch, schema, attr, anyUtils,
                                        resources, propByRes, invalidValues);
                            }
                        }
                    });
                    if (!invalidValues.isEmpty()) {
                        scce.addException(invalidValues);
                    }

                    toBeProvisioned.addAll(groupDAO.findAllResourceKeys(group.getKey()));

                    // SYNCOPE-686: if password is invertible and we are adding resources with password mapping,
                    // ensure that they are counted for password propagation
                    if (toBeUpdated.canDecodePassword()) {
                        if (userUR.getPassword() == null) {
                            userUR.setPassword(new PasswordPatch());
                        }
                        group.getResources().stream().
                                filter(resource -> isPasswordMapped(resource)).
                                forEachOrdered(resource -> {
                                    userUR.getPassword().getResources().add(resource.getKey());
                                });
                    }
                } else {
                    LOG.error("{} cannot be assigned to {}", group, user);

                    SyncopeClientException unassignabled =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignabled.getElements().add("Cannot be assigned: " + group);
                    scce.addException(unassignabled);
                }
            }
        });

        // finalize resource management
        reasons.entrySet().stream().
                filter(entry -> entry.getValue().isEmpty()).
                forEach(entry -> toBeDeprovisioned.add(entry.getKey()));

        propByRes.addAll(ResourceOperation.DELETE, toBeDeprovisioned);
        propByRes.addAll(ResourceOperation.UPDATE, toBeProvisioned);

        // in case of new memberships all current resources need to be updated in order to propagate new group
        // attribute values.
        if (!toBeDeprovisioned.isEmpty() || !toBeProvisioned.isEmpty()) {
            currentResources.removeAll(toBeDeprovisioned);
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // check if some connObjectKey was changed by the update above
        Map<String, String> newConnObjectKeys = getConnObjectKeys(user, anyUtils);
        oldConnObjectKeys.entrySet().stream().
                filter(entry -> newConnObjectKeys.containsKey(entry.getKey())
                && !entry.getValue().equals(newConnObjectKeys.get(entry.getKey()))).
                forEach(entry -> {
                    propByRes.addOldConnObjectKey(entry.getKey(), entry.getValue());
                    propByRes.add(ResourceOperation.UPDATE, entry.getKey());
                });

        Pair<Set<String>, Set<String>> dynGroupMembs = userDAO.saveAndGetDynGroupMembs(user);

        // finally check if any resource assignment is to be processed due to dynamic group membership change
        dynGroupMembs.getLeft().stream().
                filter(group -> !dynGroupMembs.getRight().contains(group)).
                forEach(delete -> {
                    groupDAO.find(delete).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.DELETE, resource.getKey());
                            });
                });
        dynGroupMembs.getLeft().stream().
                filter(group -> dynGroupMembs.getRight().contains(group)).
                forEach(update -> {
                    groupDAO.find(update).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.UPDATE, resource.getKey());
                            });
                });
        dynGroupMembs.getRight().stream().
                filter(group -> !dynGroupMembs.getLeft().contains(group)).
                forEach(create -> {
                    groupDAO.find(create).getResources().stream().
                            filter(resource -> !propByRes.contains(resource.getKey())).
                            forEach(resource -> {
                                propByRes.add(ResourceOperation.CREATE, resource.getKey());
                            });
                });

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        userDAO.save(user);
        return propByRes;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final User user, final boolean details) {
        UserTO userTO = new UserTO();
        userTO.setKey(user.getKey());
        userTO.setUsername(user.getUsername());
        userTO.setPassword(user.getPassword());
        userTO.setType(user.getType().getKey());
        userTO.setCreationDate(user.getCreationDate());
        userTO.setCreator(user.getCreator());
        userTO.setLastChangeDate(user.getLastChangeDate());
        userTO.setLastModifier(user.getLastModifier());
        userTO.setStatus(user.getStatus());
        userTO.setSuspended(BooleanUtils.isTrue(user.isSuspended()));
        userTO.setChangePwdDate(user.getChangePwdDate());
        userTO.setFailedLogins(user.getFailedLogins());
        userTO.setLastLoginDate(user.getLastLoginDate());
        userTO.setMustChangePassword(user.isMustChangePassword());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());

        if (user.getSecurityQuestion() != null) {
            userTO.setSecurityQuestion(user.getSecurityQuestion().getKey());
        }

        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHandler.getValues(user)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(userTO, user.getRealm().getFullPath(),
                user.getAuxClasses(),
                user.getPlainAttrs(),
                derAttrHandler.getValues(user),
                virAttrValues,
                userDAO.findAllResources(user),
                details);

        // dynamic realms
        userTO.getDynRealms().addAll(userDAO.findDynRealms(user.getKey()));

        if (details) {
            // roles
            userTO.getRoles().addAll(user.getRoles().stream().map(Entity::getKey).collect(Collectors.toList()));

            // dynamic roles
            userTO.getDynRoles().addAll(
                    userDAO.findDynRoles(user.getKey()).stream().map(Entity::getKey).collect(Collectors.toList()));

            // privileges
            userTO.getPrivileges().addAll(userDAO.findAllRoles(user).stream().
                    flatMap(role -> role.getPrivileges().stream()).map(Entity::getKey).collect(Collectors.toSet()));

            // relationships
            userTO.getRelationships().addAll(user.getRelationships().stream().
                    map(relationship -> getRelationshipTO(relationship.getType().getKey(), relationship.getRightEnd())).
                    collect(Collectors.toList()));

            // memberships
            userTO.getMemberships().addAll(
                    user.getMemberships().stream().map(membership -> {
                        return getMembershipTO(
                                user.getPlainAttrs(membership),
                                derAttrHandler.getValues(user, membership),
                                virAttrHandler.getValues(user, membership),
                                membership);
                    }).collect(Collectors.toList()));

            // dynamic memberships
            userTO.getDynMemberships().addAll(
                    userDAO.findDynGroups(user.getKey()).stream().
                            map(group -> new MembershipTO.Builder(group.getKey()).groupName(group.getName()).build()).
                            collect(Collectors.toList()));
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final String key) {
        return getUserTO(userDAO.authFind(key), true);
    }
}
