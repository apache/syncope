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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class UserDataBinderImpl extends AbstractAnyDataBinder implements UserDataBinder {

    protected final RoleDAO roleDAO;

    protected final SecurityQuestionDAO securityQuestionDAO;

    protected final AccessTokenDAO accessTokenDAO;

    protected final DelegationDAO delegationDAO;

    protected final ConfParamOps confParamOps;

    protected final SecurityProperties securityProperties;

    public UserDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final RoleDAO roleDAO,
            final SecurityQuestionDAO securityQuestionDAO,
            final AccessTokenDAO accessTokenDAO,
            final DelegationDAO delegationDAO,
            final ConfParamOps confParamOps,
            final SecurityProperties securityProperties) {

        super(anyTypeDAO,
                realmSearchDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator);

        this.roleDAO = roleDAO;
        this.securityQuestionDAO = securityQuestionDAO;
        this.accessTokenDAO = accessTokenDAO;
        this.delegationDAO = delegationDAO;
        this.confParamOps = confParamOps;
        this.securityProperties = securityProperties;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getAuthenticatedUserTO() {
        UserTO authUserTO;

        String authUsername = AuthContextUtils.getUsername();
        if (securityProperties.getAnonymousUser().equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(null);
            authUserTO.setUsername(securityProperties.getAnonymousUser());
        } else if (securityProperties.getAdminUser().equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(null);
            authUserTO.setUsername(securityProperties.getAdminUser());
        } else {
            User authUser = userDAO.findByUsername(authUsername).
                    orElseThrow(() -> new NotFoundException("User" + authUsername));
            authUserTO = getUserTO(authUser, true);
        }

        return authUserTO;
    }

    protected SyncopeClientException addToSyncopeClientException(
            final SyncopeClientCompositeException scce,
            final RuntimeException e) {

        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.NotFound);
        sce.getElements().add(e.getMessage());
        scce.addException(sce);
        return scce;
    }

    protected void setPassword(
            final User user,
            final String password,
            final SyncopeClientCompositeException scce) {

        try {
            setCipherAlgorithm(user);
            user.setPassword(password);
        } catch (IllegalArgumentException e) {
            throw addToSyncopeClientException(scce, e);
        }
    }

    protected void setSecurityAnswer(
            final User user,
            final String securityAnswer,
            final SyncopeClientCompositeException scce) {

        try {
            setCipherAlgorithm(user);
            user.setSecurityAnswer(securityAnswer);
        } catch (IllegalArgumentException e) {
            throw addToSyncopeClientException(scce, e);
        }
    }

    protected void setCipherAlgorithm(final User user) {
        if (user.getCipherAlgorithm() == null) {
            user.setCipherAlgorithm(CipherAlgorithm.valueOf(confParamOps.get(AuthContextUtils.getDomain(),
                    "password.cipher.algorithm", CipherAlgorithm.AES.name(), String.class)));
        }
    }

    protected void linkedAccount(
            final UserTO anyTO,
            final User user,
            final LinkedAccountTO accountTO,
            final SyncopeClientException invalidValues) {

        ExternalResource resource = resourceDAO.findById(accountTO.getResource()).orElse(null);
        if (resource == null) {
            LOG.debug("Ignoring invalid resource {}", accountTO.getResource());
            return;
        }

        LinkedAccount account = user.getLinkedAccount(resource.getKey(), accountTO.getConnObjectKeyValue()).
                map(LinkedAccount.class::cast).orElseGet(() -> {
            LinkedAccount acct = entityFactory.newEntity(LinkedAccount.class);
            acct.setOwner(user);
            user.add(acct);

            acct.setConnObjectKeyValue(accountTO.getConnObjectKeyValue());
            acct.setResource(resource);

            return acct;
        });

        account.setUsername(accountTO.getUsername());
        if (StringUtils.isBlank(accountTO.getPassword())) {
            account.setEncodedPassword(null, null);
        } else if (!accountTO.getPassword().equals(account.getPassword())) {
            if (account.getCipherAlgorithm() == null) {
                account.setCipherAlgorithm(CipherAlgorithm.AES);
            }
            account.setPassword(accountTO.getPassword());
        }
        account.setSuspended(accountTO.isSuspended());

        accountTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = account.getPlainAttr(schema.getKey()).orElseGet(() -> {
                PlainAttr newAttr = new PlainAttr();
                newAttr.setPlainSchema(schema);
                return newAttr;
            });
            fillAttr(anyTO, attrTO.getValues(), schema, attr, invalidValues);

            if (!attr.getValuesAsStrings().isEmpty()) {
                account.add(attr);
            }
        }));
    }

    @Override
    public void create(final User user, final UserCR userCR) {
        UserTO anyTO = new UserTO();
        EntityTOUtils.toAnyTO(userCR, anyTO);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // set username
        user.setUsername(userCR.getUsername());

        // set password
        if (StringUtils.isBlank(userCR.getPassword()) || !userCR.isStorePassword()) {
            LOG.debug("Password was not provided or not required to be stored");
        } else {
            setPassword(user, userCR.getPassword(), scce);
            user.setChangePwdDate(OffsetDateTime.now());
        }

        user.setMustChangePassword(userCR.isMustChangePassword());

        // security question / answer
        if (userCR.getSecurityQuestion() != null) {
            securityQuestionDAO.findById(userCR.getSecurityQuestion()).ifPresent(user::setSecurityQuestion);
        }
        setSecurityAnswer(user, userCR.getSecurityAnswer(), scce);

        // roles
        userCR.getRoles().forEach(roleKey -> roleDAO.findById(roleKey).ifPresentOrElse(
                user::add,
                () -> LOG.warn("Ignoring unknown role with id {}", roleKey)));

        // realm
        Realm realm = realmSearchDAO.findByFullPath(userCR.getRealm()).orElse(null);
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + userCR.getRealm());
            scce.addException(noRealm);
        }
        user.setRealm(realm);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        userCR.getRelationships().forEach(relationshipTO -> {
            AnyObject otherEnd = anyObjectDAO.findById(relationshipTO.getOtherEndKey()).orElse(null);
            if (otherEnd == null) {
                LOG.debug("Ignoring invalid anyObject {}", relationshipTO.getOtherEndKey());
            } else if (relationshipTO.getEnd() == RelationshipTO.End.RIGHT) {
                SyncopeClientException noRight =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                noRight.getElements().add(
                        "Relationships shall be created or updated only from their left end");
                scce.addException(noRight);
            } else if (relationships.contains(Pair.of(otherEnd.getKey(), relationshipTO.getType()))) {
                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " in relationship " + relationshipTO.getType());
                scce.addException(assigned);
            } else if (user.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                relationships.add(Pair.of(otherEnd.getKey(), relationshipTO.getType()));

                relationshipTypeDAO.findById(relationshipTO.getType()).ifPresentOrElse(
                        relationshipType -> {
                            URelationship relationship = entityFactory.newEntity(URelationship.class);
                            relationship.setType(relationshipType);
                            relationship.setRightEnd(otherEnd);
                            relationship.setLeftEnd(user);

                            user.add(relationship);
                        },
                        () -> LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType()));
            } else {
                SyncopeClientException unrelatable =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " cannot be related");
                scce.addException(unrelatable);
            }
        });

        // memberships
        Set<String> groups = new HashSet<>();
        userCR.getMemberships().forEach(membershipTO -> {
            Group group = membershipTO.getGroupKey() == null
                    ? groupDAO.findByName(membershipTO.getGroupName()).orElse(null)
                    : groupDAO.findById(membershipTO.getGroupKey()).orElse(null);
            if (group == null) {
                LOG.debug("Ignoring invalid group {}",
                        membershipTO.getGroupKey() + " / " + membershipTO.getGroupName());
            } else if (groups.contains(group.getKey())) {
                LOG.error("{} was already assigned to {}", group, user);

                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                assigned.getElements().add("Group " + group.getName() + " was already assigned");
                scce.addException(assigned);
            } else {
                groups.add(group.getKey());

                UMembership membership = entityFactory.newEntity(UMembership.class);
                membership.setRightEnd(group);
                membership.setLeftEnd(user);

                user.add(membership);

                // membership attributes
                fill(anyTO, user, membership, membershipTO, scce);
            }
        });

        // linked accounts
        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);
        userCR.getLinkedAccounts().forEach(acct -> linkedAccount(anyTO, user, acct, invalidValues));
        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        // attributes and resources
        fill(anyTO, user, userCR, anyUtilsFactory.getInstance(AnyTypeKind.USER), scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected boolean isPasswordMapped(final ExternalResource resource) {
        return resource.getProvisionByAnyType(AnyTypeKind.USER.name()).
                filter(provision -> provision.getMapping() != null).
                map(provision -> provision.getMapping().getItems().stream().anyMatch(Item::isPassword)).
                orElse(false);
    }

    @Override
    public Pair<PropagationByResource<String>, PropagationByResource<Pair<String, String>>> update(
            final User toBeUpdated, final UserUR userUR) {

        // Re-merge any pending change from workflow tasks
        User user = userDAO.save(toBeUpdated);

        UserTO anyTO = AnyOperations.patch(getUserTO(user, true), userUR);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.USER);

        // password
        String password = null;
        Set<String> changePwdRes = new HashSet<>();
        if (userUR.getPassword() != null) {
            if (userUR.getPassword().getOperation() == PatchOperation.DELETE) {
                user.setEncodedPassword(null, null);

                changePwdRes.addAll(userUR.getPassword().getResources());
            } else if (StringUtils.isNotBlank(userUR.getPassword().getValue())) {
                if (userUR.getPassword().isOnSyncope()) {
                    setPassword(user, userUR.getPassword().getValue(), scce);
                    user.setChangePwdDate(OffsetDateTime.now());
                }

                password = userUR.getPassword().getValue();
                changePwdRes.addAll(userUR.getPassword().getResources());
            }

            if (!changePwdRes.isEmpty()) {
                propByRes.addAll(ResourceOperation.UPDATE, userUR.getPassword().getResources());
            }
        }

        // Save projection on Resources (before update)
        Map<String, ConnObject> beforeOnResources =
                onResources(user, userDAO.findAllResourceKeys(user.getKey()), password, changePwdRes);

        // realm
        setRealm(user, userUR);

        // username
        if (userUR.getUsername() != null && StringUtils.isNotBlank(userUR.getUsername().getValue())) {
            String oldUsername = user.getUsername();
            user.setUsername(userUR.getUsername().getValue());

            if (oldUsername.equals(AuthContextUtils.getUsername())) {
                AuthContextUtils.updateUsername(userUR.getUsername().getValue());
            }

            accessTokenDAO.findByOwner(oldUsername).ifPresent(accessToken -> {
                accessToken.setOwner(userUR.getUsername().getValue());
                accessTokenDAO.save(accessToken);
            });
        }

        // security question / answer:
        if (userUR.getSecurityQuestion() != null) {
            if (userUR.getSecurityQuestion().getValue() == null) {
                user.setSecurityQuestion(null);
                user.setSecurityAnswer(null);
            } else {
                securityQuestionDAO.findById(userUR.getSecurityQuestion().getValue()).
                        ifPresent(securityQuestion -> {
                            user.setSecurityQuestion(securityQuestion);
                            setSecurityAnswer(user, userUR.getSecurityAnswer().getValue(), scce);
                        });
            }
        }

        // roles
        for (StringPatchItem patch : userUR.getRoles()) {
            roleDAO.findById(patch.getValue()).ifPresentOrElse(
                    role -> {
                        switch (patch.getOperation()) {
                            case ADD_REPLACE:
                                user.add(role);
                                break;

                            case DELETE:
                            default:
                                user.getRoles().remove(role);
                        }
                    },
                    () -> LOG.warn("Ignoring unknown role with key {}", patch.getValue()));
        }

        // attributes and resources
        fill(anyTO, user, userUR, anyUtils, scce);

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        userUR.getRelationships().stream().filter(patch -> patch.getRelationshipTO() != null).forEach(patch -> {
            RelationshipType relationshipType = relationshipTypeDAO.findById(patch.getRelationshipTO().getType()).
                    orElse(null);
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
            } else {
                user.getRelationship(relationshipType, patch.getRelationshipTO().getOtherEndKey()).
                        ifPresent(relationship -> {
                            user.getRelationships().remove(relationship);
                            relationship.setLeftEnd(null);
                        });

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    AnyObject otherEnd = anyObjectDAO.findById(patch.getRelationshipTO().getOtherEndKey()).orElse(null);
                    if (otherEnd == null) {
                        LOG.debug("Ignoring invalid any object {}", patch.getRelationshipTO().getOtherEndKey());
                    } else if (relationships.contains(
                            Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()))) {

                        SyncopeClientException assigned =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        assigned.getElements().add("User was already in relationship "
                                + patch.getRelationshipTO().getType() + " with "
                                + otherEnd.getType().getKey() + " " + otherEnd.getName());
                        scce.addException(assigned);
                    } else if (patch.getRelationshipTO().getEnd() == RelationshipTO.End.RIGHT) {
                        SyncopeClientException noRight =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        noRight.getElements().add(
                                "Relationships shall be created or updated only from their left end");
                        scce.addException(noRight);
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
        userUR.getMemberships().stream().filter(patch -> patch.getGroup() != null).forEach(patch -> {
            user.getMembership(patch.getGroup()).ifPresent(membership -> {
                user.remove(membership);
                membership.setLeftEnd(null);
                user.getPlainAttrs(membership).forEach(user::remove);
                userDAO.deleteMembership(membership);

                if (patch.getOperation() == PatchOperation.DELETE) {
                    propByRes.addAll(
                            ResourceOperation.UPDATE,
                            groupDAO.findAllResourceKeys((membership.getRightEnd().getKey())));
                }
            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                Group group = groupDAO.findById(patch.getGroup()).orElse(null);
                if (group == null) {
                    LOG.debug("Ignoring invalid group {}", patch.getGroup());
                } else if (groups.contains(group.getKey())) {
                    LOG.error("Multiple patches for group {} of {} were found", group, user);

                    SyncopeClientException assigned =
                            SyncopeClientException.build(ClientExceptionType.InvalidMembership);
                    assigned.getElements().add("Multiple patches for group " + group.getName() + " were found");
                    scce.addException(assigned);
                } else {
                    groups.add(group.getKey());

                    UMembership newMembership = entityFactory.newEntity(UMembership.class);
                    newMembership.setRightEnd(group);
                    newMembership.setLeftEnd(user);

                    user.add(newMembership);

                    patch.getPlainAttrs().forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresentOrElse(
                            schema -> user.getPlainAttr(schema.getKey(), newMembership).ifPresentOrElse(
                                    attr -> LOG.debug(
                                            "Plain attribute found for {} and membership of {}, nothing to do",
                                            schema, newMembership.getRightEnd()),
                                    () -> {
                                        LOG.debug("No plain attribute found for {} and membership of {}",
                                                schema, newMembership.getRightEnd());

                                        PlainAttr newAttr = new PlainAttr();
                                        newAttr.setMembership(newMembership.getKey());
                                        newAttr.setPlainSchema(schema);
                                        user.add(newAttr);

                                        processAttrPatch(
                                                anyTO,
                                                user,
                                                new AttrPatch.Builder(attrTO).build(),
                                                schema,
                                                newAttr,
                                                invalidValues);
                                    }),
                            () -> LOG.debug("Invalid {}{}, ignoring...",
                                    PlainSchema.class.getSimpleName(), attrTO.getSchema())));
                    if (!invalidValues.isEmpty()) {
                        scce.addException(invalidValues);
                    }

                    propByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));

                    // SYNCOPE-686: if password is invertible and we are adding resources with password mapping,
                    // ensure that they are counted for password propagation
                    if (toBeUpdated.canDecodeSecrets()) {
                        if (userUR.getPassword() == null) {
                            userUR.setPassword(new PasswordPatch());
                        }
                        group.getResources().stream().
                                filter(this::isPasswordMapped).
                                forEach(resource -> userUR.getPassword().getResources().add(resource.getKey()));
                    }
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

                new HashSet<>(account.getPlainAttrs()).forEach(account::remove);
            });
            if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                linkedAccount(
                        anyTO,
                        user,
                        patch.getLinkedAccountTO(),
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
        Map<String, ConnObject> afterOnResources =
                onResources(user, userDAO.findAllResourceKeys(user.getKey()), password, changePwdRes);
        propByRes.merge(propByRes(beforeOnResources, afterOnResources));

        if (userUR.getMustChangePassword() != null) {
            user.setMustChangePassword(userUR.getMustChangePassword().getValue());

            propByRes.addAll(
                    ResourceOperation.UPDATE,
                    anyUtils.getAllResources(saved).stream().
                            map(resource -> resource.getProvisionByAnyType(saved.getType().getKey()).
                            filter(mappingManager::hasMustChangePassword).
                            map(provision -> resource.getKey()).
                            orElse(null)).
                            filter(Objects::nonNull).
                            toList());
        }

        return Pair.of(propByRes, propByLinkedAccount);
    }

    protected LinkedAccountTO getLinkedAccountTO(final LinkedAccount account, final boolean returnPasswordValue) {
        LinkedAccountTO accountTO = new LinkedAccountTO.Builder(
                account.getKey(), account.getResource().getKey(), account.getConnObjectKeyValue()).
                username(account.getUsername()).
                password(returnPasswordValue ? account.getPassword() : null).
                suspended(BooleanUtils.isTrue(account.isSuspended())).
                build();

        account.getPlainAttrs().forEach(plainAttr -> accountTO.getPlainAttrs().add(
                new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        return accountTO;
    }

    @Transactional(readOnly = true)
    @Override
    public LinkedAccountTO getLinkedAccountTO(final LinkedAccount account) {
        return getLinkedAccountTO(account, true);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final User user, final boolean details) {
        Boolean returnPasswordValue = confParamOps.get(AuthContextUtils.getDomain(),
                "return.password.value", Boolean.FALSE, Boolean.class);

        UserTO userTO = new UserTO();
        userTO.setKey(user.getKey());
        userTO.setUsername(user.getUsername());
        userTO.setStatus(user.getStatus());
        userTO.setSuspended(BooleanUtils.isTrue(user.isSuspended()));
        userTO.setMustChangePassword(user.isMustChangePassword());

        if (returnPasswordValue) {
            userTO.setPassword(user.getPassword());
            userTO.setSecurityAnswer(user.getSecurityAnswer());
        }
        Optional.ofNullable(user.getSecurityQuestion()).
                map(SecurityQuestion::getKey).
                ifPresent(userTO::setSecurityQuestion);

        userTO.setCreationDate(user.getCreationDate());
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

        fillTO(userTO,
                user.getRealm().getFullPath(),
                user.getAuxClasses(),
                user.getPlainAttrs(),
                derAttrHandler.getValues(user),
                details ? virAttrHandler.getValues(user) : Map.of(),
                userDAO.findAllResources(user));

        // dynamic realms
        userTO.getDynRealms().addAll(userDAO.findDynRealms(user.getKey()));

        if (details) {
            // roles
            userTO.getRoles().addAll(user.getRoles().stream().map(Role::getKey).toList());

            // dynamic roles
            userTO.getDynRoles().addAll(
                    userDAO.findDynRoles(user.getKey()).stream().map(Role::getKey).toList());

            // relationships
            userTO.getRelationships().addAll(user.getRelationships().stream().map(relationship -> getRelationshipTO(
                    relationship.getType().getKey(), RelationshipTO.End.LEFT, relationship.getRightEnd())).
                    toList());

            // memberships
            userTO.getMemberships().addAll(user.getMemberships().stream().
                    map(membership -> getMembershipTO(user.getPlainAttrs(membership),
                    derAttrHandler.getValues(user, membership),
                    virAttrHandler.getValues(user, membership),
                    membership)).toList());

            // dynamic memberships
            userTO.getDynMemberships().addAll(userDAO.findDynGroups(user.getKey()).stream().
                    map(group -> new MembershipTO.Builder(group.getKey()).groupName(group.getName()).build()).
                    toList());

            // linked accounts
            userTO.getLinkedAccounts().addAll(user.getLinkedAccounts().stream().
                    map(account -> getLinkedAccountTO(account, returnPasswordValue)).
                    toList());

            // delegations
            userTO.getDelegatingDelegations().addAll(
                    delegationDAO.findByDelegating(user).stream().map(Delegation::getKey).toList());
            userTO.getDelegatedDelegations().addAll(
                    delegationDAO.findByDelegated(user).stream().map(Delegation::getKey).toList());
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final String key) {
        return getUserTO(userDAO.authFind(key), true);
    }
}
