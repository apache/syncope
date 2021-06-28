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

import java.util.Collections;
import java.util.Date;
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
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Delegation;
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
    private ConfDAO confDAO;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private ApplicationDAO applicationDAO;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private DelegationDAO delegationDAO;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Transactional(readOnly = true)
    @Override
    public UserTO returnUserTO(final UserTO userTO) {
        if (!confDAO.find("return.password.value", false)) {
            userTO.setPassword(null);
            userTO.getLinkedAccounts().forEach(account -> account.setPassword(null));
        }
        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getAuthenticatedUserTO() {
        UserTO authUserTO;

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
            String algorithm = confDAO.find("password.cipher.algorithm", CipherAlgorithm.AES.name());
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
    public void create(final User user, final UserTO userTO, final boolean storePassword) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // set username
        user.setUsername(userTO.getUsername());

        // set password
        if (StringUtils.isBlank(userTO.getPassword()) || !storePassword) {
            LOG.debug("Password was not provided or not required to be stored");
        } else {
            setPassword(user, userTO.getPassword(), scce);
            user.setChangePwdDate(new Date());
        }

        user.setMustChangePassword(userTO.isMustChangePassword());

        // security question / answer
        if (userTO.getSecurityQuestion() != null) {
            SecurityQuestion securityQuestion = securityQuestionDAO.find(userTO.getSecurityQuestion());
            if (securityQuestion != null) {
                user.setSecurityQuestion(securityQuestion);
            }
        }
        user.setSecurityAnswer(userTO.getSecurityAnswer());

        // roles
        userTO.getRoles().forEach(roleKey -> {
            Role role = roleDAO.find(roleKey);
            if (role == null) {
                LOG.warn("Ignoring unknown role with id {}", roleKey);
            } else {
                user.add(role);
            }
        });

        // realm
        Realm realm = realmDAO.findByFullPath(userTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + userTO.getRealm());
            scce.addException(noRealm);
        }
        user.setRealm(realm);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        userTO.getRelationships().forEach(relationshipTO -> {
            AnyObject otherEnd = anyObjectDAO.find(relationshipTO.getOtherEndKey());
            if (otherEnd == null) {
                LOG.debug("Ignoring invalid anyObject " + relationshipTO.getOtherEndKey());
            } else if (relationships.contains(Pair.of(otherEnd.getKey(), relationshipTO.getType()))) {
                LOG.error("{} was already in relationship {} with {}", otherEnd, relationshipTO.getType(), user);

                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " in relationship " + relationshipTO.getType());
                scce.addException(assigned);
            } else if (user.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                relationships.add(Pair.of(otherEnd.getKey(), relationshipTO.getType()));

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
        Set<String> groups = new HashSet<>();
        userTO.getMemberships().forEach(membershipTO -> {
            Group group = membershipTO.getGroupKey() == null
                    ? groupDAO.findByName(membershipTO.getGroupName())
                    : groupDAO.find(membershipTO.getGroupKey());
            if (group == null) {
                LOG.debug("Ignoring invalid group {}",
                        membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
            } else if (groups.contains(group.getKey())) {
                LOG.error("{} was already assigned to {}", group, user);

                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                assigned.getElements().add("Group " + group.getName() + " was already assigned");
                scce.addException(assigned);
            } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                groups.add(group.getKey());

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
        userTO.getLinkedAccounts().forEach(accountTO
                -> linkedAccount(user, accountTO, anyUtilsFactory.getLinkedAccountInstance(), invalidValues));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // attributes and resources
        fill(user, userTO, anyUtilsFactory.getInstance(AnyTypeKind.USER), scce);

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
            final User toBeUpdated, final UserPatch userPatch) {

        // Re-merge any pending change from workflow tasks
        User user = userDAO.save(toBeUpdated);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.USER);

        // realm
        setRealm(user, userPatch);

        // password
        String password = null;
        boolean changePwd = false;
        if (userPatch.getPassword() != null) {
            if (userPatch.getPassword().getOperation() == PatchOperation.DELETE) {
                user.setEncodedPassword(null, null);

                changePwd = true;
            } else if (StringUtils.isNotBlank(userPatch.getPassword().getValue())) {
                if (userPatch.getPassword().isOnSyncope()) {
                    setPassword(user, userPatch.getPassword().getValue(), scce);
                    user.setChangePwdDate(new Date());
                }

                password = userPatch.getPassword().getValue();
                changePwd = true;
            }

            if (changePwd) {
                propByRes.addAll(ResourceOperation.UPDATE, userPatch.getPassword().getResources());
            }
        }

        // Save projection on Resources (before update)
        Map<String, ConnObjectTO> beforeOnResources =
                onResources(user, userDAO.findAllResourceKeys(user.getKey()), password, changePwd);

        // username
        if (userPatch.getUsername() != null && StringUtils.isNotBlank(userPatch.getUsername().getValue())) {
            String oldUsername = user.getUsername();
            user.setUsername(userPatch.getUsername().getValue());

            if (oldUsername.equals(AuthContextUtils.getUsername())) {
                AuthContextUtils.updateUsername(userPatch.getUsername().getValue());
            }

            AccessToken accessToken = accessTokenDAO.findByOwner(oldUsername);
            if (accessToken != null) {
                accessToken.setOwner(userPatch.getUsername().getValue());
                accessTokenDAO.save(accessToken);
            }
        }

        // security question / answer:
        if (userPatch.getSecurityQuestion() != null) {
            if (userPatch.getSecurityQuestion().getValue() == null) {
                user.setSecurityQuestion(null);
                user.setSecurityAnswer(null);
            } else {
                SecurityQuestion securityQuestion =
                        securityQuestionDAO.find(userPatch.getSecurityQuestion().getValue());
                if (securityQuestion != null) {
                    user.setSecurityQuestion(securityQuestion);
                    user.setSecurityAnswer(userPatch.getSecurityAnswer().getValue());
                }
            }
        }

        // roles
        for (StringPatchItem patch : userPatch.getRoles()) {
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
        fill(user, userPatch, anyUtils, scce);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        userPatch.getRelationships().stream().filter(patch -> patch.getRelationshipTO() != null).forEach(patch -> {
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
                    } else if (relationships.contains(
                            Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()))) {

                        LOG.error("{} was already in relationship {} with {}",
                                user, patch.getRelationshipTO().getType(), otherEnd);

                        SyncopeClientException assigned =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        assigned.getElements().add("User was already in relationship "
                                + patch.getRelationshipTO().getType() + " with "
                                + otherEnd.getType().getKey() + " " + otherEnd.getName());
                        scce.addException(assigned);
                    } else if (user.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                        relationships.add(Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()));

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

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        // memberships
        Set<String> groups = new HashSet<>();
        userPatch.getMemberships().stream().filter(patch -> patch.getGroup() != null).forEach(patch -> {
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
                    propByRes.addAll(
                            ResourceOperation.UPDATE,
                            groupDAO.findAllResourceKeys((membership.getRightEnd().getKey())));
                }
            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.find(patch.getGroup());
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", patch.getGroup());
                } else if (groups.contains(group.getKey())) {
                    LOG.error("Multiple patches for group {} of {} were found", group, user);

                    SyncopeClientException assigned =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    assigned.getElements().add("Multiple patches for group " + group.getName() + " were found");
                    scce.addException(assigned);
                } else if (user.getRealm().getFullPath().startsWith(group.getRealm().getFullPath())) {
                    groups.add(group.getKey());

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
                                        new AttrPatch.Builder().attrTO(attrTO).build(),
                                        schema,
                                        attr,
                                        anyUtils,
                                        invalidValues);
                            }
                        }
                    });
                    if (!invalidValues.isEmpty()) {
                        scce.addException(invalidValues);
                    }

                    propByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));

                    // SYNCOPE-686: if password is invertible and we are adding resources with password mapping,
                    // ensure that they are counted for password propagation
                    if (toBeUpdated.canDecodePassword()) {
                        if (userPatch.getPassword() == null) {
                            userPatch.setPassword(new PasswordPatch());
                        }
                        group.getResources().stream().
                                filter(resource -> isPasswordMapped(resource)).
                                forEach(resource -> userPatch.getPassword().getResources().add(resource.getKey()));
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
        userPatch.getLinkedAccounts().stream().filter(patch -> patch.getLinkedAccountTO() != null).forEach(patch -> {
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

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        User saved = userDAO.save(user);

        // Build final information for next stage (propagation)
        Map<String, ConnObjectTO> afterOnResources =
                onResources(user, userDAO.findAllResourceKeys(user.getKey()), password, changePwd);
        propByRes.merge(propByRes(beforeOnResources, afterOnResources));

        if (userPatch.getMustChangePassword() != null) {
            user.setMustChangePassword(userPatch.getMustChangePassword().getValue());

            propByRes.addAll(
                    ResourceOperation.UPDATE,
                    anyUtils.getAllResources(saved).stream().
                            map(resource -> resource.getProvision(saved.getType())).
                            filter(Optional::isPresent).map(Optional::get).
                            filter(mappingManager::hasMustChangePassword).
                            map(provision -> provision.getResource().getKey()).
                            collect(Collectors.toSet()));
        }

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
            accountTO.getPlainAttrs().add(new AttrTO.Builder().
                    schema(plainAttr.getSchema().getKey()).
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

        fillTO(userTO, user.getRealm().getFullPath(),
                user.getAuxClasses(),
                user.getPlainAttrs(),
                derAttrHandler.getValues(user),
                details ? virAttrHandler.getValues(user) : Collections.emptyMap(),
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
                    userDAO.findDynGroups(user.getKey()).stream().map(group -> new MembershipTO.Builder().
                    group(group.getKey(), group.getName()).
                    build()).collect(Collectors.toList()));

            // linked accounts
            userTO.getLinkedAccounts().addAll(
                    user.getLinkedAccounts().stream().map(this::getLinkedAccountTO).collect(Collectors.toList()));

            // delegations
            userTO.getDelegatingDelegations().addAll(
                    delegationDAO.findByDelegating(user).stream().map(Delegation::getKey).collect(Collectors.toList()));
            userTO.getDelegatedDelegations().addAll(
                    delegationDAO.findByDelegated(user).stream().map(Delegation::getKey).collect(Collectors.toList()));
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final String key) {
        return getUserTO(userDAO.authFind(key), true);
    }
}
