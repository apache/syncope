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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
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
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
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
    private ApplicationDAO applicationDAO;

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
            userTO.getLinkedAccounts().forEach(account -> account.setPassword(null));
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

    private void linkedAccount(
            final User user,
            final LinkedAccountTO accountTO,
            final AnyUtils anyUtils,
            final SyncopeClientException invalidValues) {

        ExternalResource resource = resourceDAO.find(accountTO.getResource());
        if (resource == null) {
            LOG.debug("Ignoring invalid resource {}", accountTO.getResource());
        } else {
            Optional<? extends LinkedAccount> found =
                    user.getLinkedAccount(resource.getKey(), accountTO.getConnObjectKeyValue());
            LinkedAccount account = found.isPresent()
                    ? found.get()
                    : new Supplier<LinkedAccount>() {

                        @Override
                        public LinkedAccount get() {
                            LinkedAccount acct = entityFactory.newEntity(LinkedAccount.class);
                            acct.setOwner(user);
                            user.add(acct);

                            acct.setConnObjectKeyValue(accountTO.getConnObjectKeyValue());
                            acct.setResource(resource);

                            return acct;
                        }
                    }.get();

            account.setUsername(accountTO.getUsername());
            if (StringUtils.isBlank(accountTO.getPassword())) {
                account.setEncodedPassword(null, null);
            } else if (!accountTO.getPassword().equals(account.getPassword())) {
                account.setPassword(accountTO.getPassword(), CipherAlgorithm.AES);
            }
            account.setSuspended(accountTO.isSuspended());

            accountTO.getPlainAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()).
                    forEach(attrTO -> {
                        PlainSchema schema = getPlainSchema(attrTO.getSchema());
                        if (schema != null) {
                            LAPlainAttr attr = account.getPlainAttr(schema.getKey()).orElse(null);
                            if (attr == null) {
                                attr = entityFactory.newEntity(LAPlainAttr.class);
                                attr.setSchema(schema);
                                attr.setOwner(user);
                                attr.setAccount(account);
                            }
                            fillAttr(attrTO.getValues(), anyUtils, schema, attr, invalidValues);

                            if (attr.getValuesAsStrings().isEmpty()) {
                                attr.setOwner(null);
                            } else {
                                account.add(attr);
                            }
                        }
                    });

            accountTO.getPrivileges().forEach(key -> {
                Privilege privilege = applicationDAO.findPrivilege(key);
                if (privilege == null) {
                    LOG.debug("Invalid privilege {}, ignoring", key);
                } else {
                    account.add(privilege);
                }
            });
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
                LOG.error("{} cannot be related to {}", otherEnd, user);

                SyncopeClientException unrelatable =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " cannot be related");
                scce.addException(unrelatable);
            }
        });

        // memberships
        userCR.getMemberships().forEach(membershipTO -> {
            Group group = membershipTO.getGroupKey() == null
                    ? groupDAO.findByName(membershipTO.getGroupName())
                    : groupDAO.find(membershipTO.getGroupKey());
            if (group == null) {
                LOG.debug("Ignoring invalid group {}",
                        membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
            } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                UMembership membership = entityFactory.newEntity(UMembership.class);
                membership.setRightEnd(group);
                membership.setLeftEnd(user);

                user.add(membership);

                // membership attributes
                fill(user, membership, membershipTO, anyUtilsFactory.getInstance(AnyTypeKind.USER), scce);
            } else {
                LOG.error("{} cannot be assigned to {}", group, user);

                SyncopeClientException unassignable =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                unassignable.getElements().add("Group " + group.getName() + " cannot be assigned");
                scce.addException(unassignable);
            }
        });

        // linked accounts
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);
        userCR.getLinkedAccounts().forEach(accountTO
                -> linkedAccount(user, accountTO, anyUtilsFactory.getLinkedAccountInstance(), invalidValues));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // attributes and resources
        fill(user, userCR, anyUtilsFactory.getInstance(AnyTypeKind.USER), scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    private boolean isPasswordMapped(final ExternalResource resource) {
        return resource.getProvision(anyTypeDAO.findUser()).
                filter(provision -> provision.getMapping() != null).
                map(provision -> provision.getMapping().getItems().stream().anyMatch(item -> item.isPassword())).
                orElse(false);
    }

    @Override
    public Pair<PropagationByResource<String>, PropagationByResource<Pair<String, String>>> update(
            final User toBeUpdated, final UserUR userUR) {

        // Re-merge any pending change from workflow tasks
        User user = userDAO.save(toBeUpdated);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.USER);

        Collection<String> currentResources = userDAO.findAllResourceKeys(user.getKey());

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(user, anyUtils);

        // realm
        setRealm(user, userUR);

        // password
        if (userUR.getPassword() != null) {
            if (userUR.getPassword().getOperation() == PatchOperation.DELETE) {
                user.setEncodedPassword(null, null);
                propByRes.addAll(ResourceOperation.UPDATE, userUR.getPassword().getResources());
            } else if (StringUtils.isNotBlank(userUR.getPassword().getValue())) {
                if (userUR.getPassword().isOnSyncope()) {
                    setPassword(user, userUR.getPassword().getValue(), scce);
                    user.setChangePwdDate(new Date());
                }

                propByRes.addAll(ResourceOperation.UPDATE, userUR.getPassword().getResources());
            }
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
        userUR.getRelationships().stream().filter(patch -> patch.getRelationshipTO() != null).forEach(patch -> {
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
                        LOG.error("{} cannot be related to {}", otherEnd, user);

                        SyncopeClientException unrelatable =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                                + " cannot be related");
                        scce.addException(unrelatable);
                    }
                }
            }
        });

        // prepare for membership-related resource management
        Collection<ExternalResource> resources = userDAO.findAllResources(user);

        Map<String, Set<String>> reasons = new HashMap<>();
        user.getResources().forEach(
                resource -> reasons.put(resource.getKey(), new HashSet<>(Set.of(user.getKey()))));
        userDAO.findAllGroupKeys(user).forEach(group -> groupDAO.findAllResourceKeys(group).forEach(resource -> {
            if (!reasons.containsKey(resource)) {
                reasons.put(resource, new HashSet<>());
            }
            reasons.get(resource).add(group);
        }));

        Set<String> toBeDeprovisioned = new HashSet<>();
        Set<String> toBeProvisioned = new HashSet<>();

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // memberships
        userUR.getMemberships().stream().filter(patch -> patch.getGroup() != null).forEach(patch -> {
            user.getMembership(patch.getGroup()).ifPresent(membership -> {
                user.remove(membership);
                membership.setLeftEnd(null);
                user.getPlainAttrs(membership).forEach(attr -> {
                    user.remove(attr);
                    attr.setOwner(null);
                    attr.setMembership(null);
                    plainAttrValueDAO.deleteAll(attr, anyUtils);
                    plainAttrDAO.delete(attr);
                });

                if (patch.getOperation() == PatchOperation.DELETE) {
                    groupDAO.findAllResourceKeys(membership.getRightEnd().getKey()).stream().
                            filter(reasons::containsKey).
                            forEach(resource -> {
                                reasons.get(resource).remove(membership.getRightEnd().getKey());
                                toBeProvisioned.add(resource);
                            });
                }
            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.find(patch.getGroup());
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", patch.getGroup());
                } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    UMembership newMembership = entityFactory.newEntity(UMembership.class);
                    newMembership.setRightEnd(group);
                    newMembership.setLeftEnd(user);

                    user.add(newMembership);

                    patch.getPlainAttrs().forEach(attrTO -> {
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

                                processAttrPatch(
                                        user,
                                        new AttrPatch.Builder(attrTO).build(),
                                        schema,
                                        attr,
                                        anyUtils,
                                        resources,
                                        propByRes,
                                        invalidValues);
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
                                filter(this::isPasswordMapped).
                                forEach(resource -> userUR.getPassword().getResources().add(resource.getKey()));
                    }
                } else {
                    LOG.error("{} cannot be assigned to {}", group, user);

                    SyncopeClientException unassignable =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    unassignable.getElements().add("Group " + group.getName() + " cannot be assigned");
                    scce.addException(unassignable);
                }
            }
        });

        // linked accounts
        userUR.getLinkedAccounts().stream().filter(patch -> patch.getLinkedAccountTO() != null).forEach(patch -> {
            user.getLinkedAccount(
                    patch.getLinkedAccountTO().getResource(),
                    patch.getLinkedAccountTO().getConnObjectKeyValue()).ifPresent(account -> {

                if (patch.getOperation() == PatchOperation.DELETE) {
                    user.getLinkedAccounts().remove(account);
                    account.setOwner(null);

                    propByLinkedAccount.add(
                            ResourceOperation.DELETE,
                            Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue()));
                }

                account.getPlainAttrs().stream().collect(Collectors.toSet()).forEach(attr -> {
                    account.remove(attr);
                    attr.setOwner(null);
                    attr.setAccount(null);
                    plainAttrValueDAO.deleteAll(attr, anyUtilsFactory.getLinkedAccountInstance());
                    plainAttrDAO.delete(attr);
                });

            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                linkedAccount(
                        user,
                        patch.getLinkedAccountTO(),
                        anyUtilsFactory.getLinkedAccountInstance(),
                        invalidValues);
            }
        });
        user.getLinkedAccounts().forEach(account -> propByLinkedAccount.add(
                ResourceOperation.CREATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

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
                forEach(delete -> groupDAO.find(delete).getResources().stream().
                filter(resource -> !propByRes.contains(resource.getKey())).
                forEach(resource -> propByRes.add(ResourceOperation.DELETE, resource.getKey())));
        dynGroupMembs.getLeft().stream().
                filter(group -> dynGroupMembs.getRight().contains(group)).
                forEach(update -> groupDAO.find(update).getResources().stream().
                filter(resource -> !propByRes.contains(resource.getKey())).
                forEach(resource -> propByRes.add(ResourceOperation.UPDATE, resource.getKey())));
        dynGroupMembs.getRight().stream().
                filter(group -> !dynGroupMembs.getLeft().contains(group)).
                forEach(create -> groupDAO.find(create).getResources().stream().
                filter(resource -> !propByRes.contains(resource.getKey())).
                forEach(resource -> propByRes.add(ResourceOperation.CREATE, resource.getKey())));

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        userDAO.save(user);
        return Pair.of(propByRes, propByLinkedAccount);
    }

    @Transactional(readOnly = true)
    @Override
    public LinkedAccountTO getLinkedAccountTO(final LinkedAccount account) {
        LinkedAccountTO accountTO = new LinkedAccountTO.Builder(
                account.getKey(), account.getResource().getKey(), account.getConnObjectKeyValue()).
                username(account.getUsername()).
                password(account.getPassword()).
                suspended(BooleanUtils.isTrue(account.isSuspended())).
                build();

        account.getPlainAttrs().forEach(plainAttr -> {
            accountTO.getPlainAttrs().add(
                    new Attr.Builder(plainAttr.getSchema().getKey()).
                            values(plainAttr.getValuesAsStrings()).build());
        });

        accountTO.getPrivileges().addAll(account.getPrivileges().stream().
                map(Entity::getKey).collect(Collectors.toList()));

        return accountTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final User user, final boolean details) {
        UserTO userTO = new UserTO();

        userTO.setCreator(user.getCreator());
        userTO.setCreationDate(user.getCreationDate());
        userTO.setCreationContext(user.getCreationContext());
        userTO.setLastModifier(user.getLastModifier());
        userTO.setLastChangeDate(user.getLastChangeDate());
        userTO.setLastChangeContext(user.getLastChangeContext());

        userTO.setChangePwdDate(user.getChangePwdDate());
        userTO.setFailedLogins(user.getFailedLogins());
        userTO.setLastLoginDate(user.getLastLoginDate());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());

        userTO.setKey(user.getKey());
        userTO.setUsername(user.getUsername());
        userTO.setPassword(user.getPassword());
        userTO.setType(user.getType().getKey());
        userTO.setStatus(user.getStatus());
        userTO.setSuspended(BooleanUtils.isTrue(user.isSuspended()));
        userTO.setMustChangePassword(user.isMustChangePassword());

        if (user.getSecurityQuestion() != null) {
            userTO.setSecurityQuestion(user.getSecurityQuestion().getKey());
        }

        fillTO(userTO, user.getRealm().getFullPath(),
                user.getAuxClasses(),
                user.getPlainAttrs(),
                derAttrHandler.getValues(user),
                details ? virAttrHandler.getValues(user) : Map.of(),
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
                    user.getMemberships().stream().map(membership -> getMembershipTO(
                    user.getPlainAttrs(membership),
                    derAttrHandler.getValues(user, membership),
                    virAttrHandler.getValues(user, membership),
                    membership)).collect(Collectors.toList()));

            // dynamic memberships
            userTO.getDynMemberships().addAll(
                    userDAO.findDynGroups(user.getKey()).stream().
                            map(group -> new MembershipTO.Builder(group.getKey()).groupName(group.getName()).build()).
                            collect(Collectors.toList()));

            // linked accounts
            userTO.getLinkedAccounts().addAll(
                    user.getLinkedAccounts().stream().map(this::getLinkedAccountTO).collect(Collectors.toList()));
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final String key) {
        return getUserTO(userDAO.authFind(key), true);
    }
}
