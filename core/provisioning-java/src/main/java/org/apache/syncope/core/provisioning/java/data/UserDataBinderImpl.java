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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class UserDataBinderImpl extends AbstractAttributableDataBinder implements UserDataBinder {

    private static final String[] IGNORE_USER_PROPERTIES = {
        "realm", "roles", "memberships", "plainAttrs", "derAttrs", "virAttrs", "resources",
        "securityQuestion", "securityAnswer"
    };

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConnObjectUtils connObjectUtils;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    private final Encryptor encryptor = Encryptor.getInstance();

    @Transactional(readOnly = true)
    @Override
    public UserTO getAuthenticatedUserTO() {
        final UserTO authUserTO;

        final String authUsername = AuthContextUtils.getAuthenticatedUsername();
        if (anonymousUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(-2);
            authUserTO.setUsername(anonymousUser);
        } else if (adminUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setKey(-1);
            authUserTO.setUsername(adminUser);
        } else {
            User authUser = userDAO.find(authUsername);
            authUserTO = getUserTO(authUser);
        }

        return authUserTO;
    }

    @Transactional(readOnly = true)
    @Override
    public boolean verifyPassword(final String username, final String password) {
        return verifyPassword(userDAO.authFetch(username), password);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean verifyPassword(final User user, final String password) {
        return encryptor.verify(password, user.getCipherAlgorithm(), user.getPassword());
    }

    private void setPassword(final User user, final String password, final SyncopeClientCompositeException scce) {
        try {
            final String algorithm = confDAO.find(
                    "password.cipher.algorithm", CipherAlgorithm.AES.name()).getValues().get(0).getStringValue();
            CipherAlgorithm predefined = CipherAlgorithm.valueOf(algorithm);
            user.setPassword(password, predefined);
        } catch (IllegalArgumentException e) {
            final SyncopeClientException invalidCiperAlgorithm =
                    SyncopeClientException.build(ClientExceptionType.NotFound);
            invalidCiperAlgorithm.getElements().add(e.getMessage());
            scce.addException(invalidCiperAlgorithm);

            throw scce;
        }
    }

    @Override
    public void create(final User user, final UserTO userTO, final boolean storePassword) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // roles
        for (Long roleKey : userTO.getRoles()) {
            Role role = roleDAO.find(roleKey);
            if (role == null) {
                LOG.warn("Ignoring unknown role with id {}", roleKey);
            } else {
                user.addRole(role);
            }
        }

        // memberships
        Group group;
        for (MembershipTO membershipTO : userTO.getMemberships()) {
            group = groupDAO.find(membershipTO.getGroupKey());

            if (group == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid group " + membershipTO.getGroupName());
                }
            } else {
                Membership membership = null;
                if (user.getKey() != null) {
                    membership = user.getMembership(group.getKey()) == null
                            ? membershipDAO.find(user, group)
                            : user.getMembership(group.getKey());
                }
                if (membership == null) {
                    membership = entityFactory.newEntity(Membership.class);
                    membership.setGroup(group);
                    membership.setUser(user);

                    user.addMembership(membership);
                }

                fill(membership, membershipTO, attrUtilsFactory.getInstance(AttributableType.MEMBERSHIP), scce);
            }
        }

        // realm, attributes, derived attributes, virtual attributes and resources
        fill(user, userTO, attrUtilsFactory.getInstance(AttributableType.USER), scce);

        // set password
        if (StringUtils.isBlank(userTO.getPassword()) || !storePassword) {
            LOG.debug("Password was not provided or not required to be stored");
        } else {
            setPassword(user, userTO.getPassword(), scce);
        }

        // set username
        user.setUsername(userTO.getUsername());

        // security question / answer
        if (userTO.getSecurityQuestion() != null) {
            SecurityQuestion securityQuestion = securityQuestionDAO.find(userTO.getSecurityQuestion());
            if (securityQuestion != null) {
                user.setSecurityQuestion(securityQuestion);
            }
        }
        user.setSecurityAnswer(userTO.getSecurityAnswer());
    }

    /**
     * Update user, given UserMod.
     *
     * @param toBeUpdated user to be updated
     * @param userMod bean containing update request
     * @return updated user + propagation by resource
     * @see PropagationByResource
     */
    @Override
    public PropagationByResource update(final User toBeUpdated, final UserMod userMod) {
        // Re-merge any pending change from workflow tasks
        final User user = userDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Collection<String> currentResources = userDAO.findAllResourceNames(user);

        // fetch account ids before update
        Map<String, String> oldAccountIds = getAccountIds(user, AttributableType.USER);

        // realm
        setRealm(user, userMod);

        // password
        if (StringUtils.isNotBlank(userMod.getPassword())) {
            setPassword(user, userMod.getPassword(), scce);
            user.setChangePwdDate(new Date());
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // username
        if (userMod.getUsername() != null && !userMod.getUsername().equals(user.getUsername())) {
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);

            user.setUsername(userMod.getUsername());
            AuthContextUtils.updateAuthenticatedUsername(userMod.getUsername());
        }

        // security question / answer:
        // userMod.getSecurityQuestion() is null => remove user security question and answer
        // userMod.getSecurityQuestion() == 0 => don't change anything
        // userMod.getSecurityQuestion() > 0 => update user security question and answer
        if (userMod.getSecurityQuestion() == null) {
            user.setSecurityQuestion(null);
            user.setSecurityAnswer(null);
        } else if (userMod.getSecurityQuestion() > 0) {
            SecurityQuestion securityQuestion = securityQuestionDAO.find(userMod.getSecurityQuestion());
            if (securityQuestion != null) {
                user.setSecurityQuestion(securityQuestion);
                user.setSecurityAnswer(userMod.getSecurityAnswer());
            }
        }

        // roles
        CollectionUtils.forAllDo(userMod.getRolesToRemove(), new Closure<Long>() {

            @Override
            public void execute(final Long roleKey) {
                Role role = roleDAO.find(roleKey);
                if (role == null) {
                    LOG.warn("Ignoring unknown role with id {}", roleKey);
                } else {
                    user.removeRole(role);
                }
            }
        });
        CollectionUtils.forAllDo(userMod.getRolesToAdd(), new Closure<Long>() {

            @Override
            public void execute(final Long roleKey) {
                Role role = roleDAO.find(roleKey);
                if (role == null) {
                    LOG.warn("Ignoring unknown role with id {}", roleKey);
                } else {
                    user.addRole(role);
                }
            }
        });

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(user, userMod, attrUtilsFactory.getInstance(AttributableType.USER), scce));

        // store the group ids of membership required to be added
        Set<Long> membershipToBeAddedGroupKeys = CollectionUtils.collect(userMod.getMembershipsToAdd(),
                new Transformer<MembershipMod, Long>() {

                    @Override
                    public Long transform(final MembershipMod membToBeAdded) {
                        return membToBeAdded.getGroup();
                    }
                }, new HashSet<Long>());

        final Set<String> toBeDeprovisioned = new HashSet<>();
        final Set<String> toBeProvisioned = new HashSet<>();

        // memberships to be removed
        for (Long membKey : userMod.getMembershipsToRemove()) {
            LOG.debug("Membership to be removed: {}", membKey);

            Membership membership = membershipDAO.find(membKey);
            if (membership == null) {
                LOG.warn("Invalid membership id specified to be removed: {}", membKey);
            } else {
                if (!membershipToBeAddedGroupKeys.contains(membership.getGroup().getKey())) {
                    toBeDeprovisioned.addAll(membership.getGroup().getResourceNames());
                }

                // In order to make the removeMembership() below to work,
                // we need to be sure to take exactly the same membership
                // of the user object currently in memory (which has potentially
                // some modifications compared to the one stored in the DB
                membership = user.getMembership(membership.getGroup().getKey());
                if (membershipToBeAddedGroupKeys.contains(membership.getGroup().getKey())) {
                    Set<Long> attrKeys = new HashSet<>(membership.getPlainAttrs().size());
                    for (PlainAttr plainAttr : membership.getPlainAttrs()) {
                        attrKeys.add(plainAttr.getKey());
                    }
                    for (Long attrKey : attrKeys) {
                        plainAttrDAO.delete(attrKey, MPlainAttr.class);
                    }
                    attrKeys.clear();

                    // remove derived attributes
                    for (DerAttr derAttr : membership.getDerAttrs()) {
                        attrKeys.add(derAttr.getKey());
                    }
                    for (Long derAttrId : attrKeys) {
                        derAttrDAO.delete(derAttrId, MDerAttr.class);
                    }
                    attrKeys.clear();

                    // remove virtual attributes
                    for (VirAttr virAttr : membership.getVirAttrs()) {
                        attrKeys.add(virAttr.getKey());
                    }
                    for (Long virAttrId : attrKeys) {
                        virAttrDAO.delete(virAttrId, MVirAttr.class);
                    }
                    attrKeys.clear();
                } else {
                    user.removeMembership(membership);

                    membershipDAO.delete(membKey);
                }
            }
        }

        // memberships to be added
        for (MembershipMod membershipMod : userMod.getMembershipsToAdd()) {
            LOG.debug("Membership to be added: group({})", membershipMod.getGroup());

            Group group = groupDAO.find(membershipMod.getGroup());
            if (group == null) {
                LOG.debug("Ignoring invalid group {}", membershipMod.getGroup());
            } else {
                Membership membership = user.getMembership(group.getKey());
                if (membership == null) {
                    membership = entityFactory.newEntity(Membership.class);
                    membership.setGroup(group);
                    membership.setUser(user);

                    user.addMembership(membership);

                    toBeProvisioned.addAll(group.getResourceNames());
                }

                propByRes.merge(fill(membership, membershipMod,
                        attrUtilsFactory.getInstance(AttributableType.MEMBERSHIP), scce));
            }
        }

        propByRes.addAll(ResourceOperation.DELETE, toBeDeprovisioned);
        propByRes.addAll(ResourceOperation.UPDATE, toBeProvisioned);

        /**
         * In case of new memberships all the current resources have to be updated in order to propagate new group and
         * membership attribute values.
         */
        if (!toBeDeprovisioned.isEmpty() || !toBeProvisioned.isEmpty()) {
            currentResources.removeAll(toBeDeprovisioned);
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // check if some account id was changed by the update above
        Map<String, String> newAccountIds = getAccountIds(user, AttributableType.USER);
        for (Map.Entry<String, String> entry : oldAccountIds.entrySet()) {
            if (newAccountIds.containsKey(entry.getKey())
                    && !entry.getValue().equals(newAccountIds.get(entry.getKey()))) {

                propByRes.addOldAccountId(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        return propByRes;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final User user) {
        UserTO userTO = new UserTO();

        BeanUtils.copyProperties(user, userTO, IGNORE_USER_PROPERTIES);

        if (user.getSecurityQuestion() != null) {
            userTO.setSecurityQuestion(user.getSecurityQuestion().getKey());
        }

        connObjectUtils.retrieveVirAttrValues(user, attrUtilsFactory.getInstance(AttributableType.USER));
        fillTO(userTO, user.getRealm().getFullPath(),
                user.getPlainAttrs(), user.getDerAttrs(), user.getVirAttrs(), userDAO.findAllResources(user));

        for (Membership membership : user.getMemberships()) {
            MembershipTO membershipTO = new MembershipTO();

            // set sys info
            membershipTO.setCreator(membership.getCreator());
            membershipTO.setCreationDate(membership.getCreationDate());
            membershipTO.setLastModifier(membership.getLastModifier());
            membershipTO.setLastChangeDate(membership.getLastChangeDate());

            membershipTO.setKey(membership.getKey());
            membershipTO.setGroupKey(membership.getGroup().getKey());
            membershipTO.setGroupName(membership.getGroup().getName());

            // SYNCOPE-458 retrieve also membership virtual attributes
            connObjectUtils.retrieveVirAttrValues(
                    membership, attrUtilsFactory.getInstance(AttributableType.MEMBERSHIP));
            fillTO(membershipTO, null,
                    membership.getPlainAttrs(), membership.getDerAttrs(), membership.getVirAttrs(),
                    Collections.<ExternalResource>emptyList());

            userTO.getMemberships().add(membershipTO);
        }

        // dynamic memberships
        CollectionUtils.collect(userDAO.findDynRoleMemberships(user), new Transformer<Role, Long>() {

            @Override
            public Long transform(final Role role) {
                return role.getKey();
            }
        }, userTO.getDynRoles());
        CollectionUtils.collect(userDAO.findDynGroupMemberships(user), new Transformer<Group, Long>() {

            @Override
            public Long transform(final Group group) {
                return group.getKey();
            }
        }, userTO.getDynGroups());

        return userTO;
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final String username) {
        return getUserTO(userDAO.authFetch(username));
    }

    @Transactional(readOnly = true)
    @Override
    public UserTO getUserTO(final Long key) {
        return getUserTO(userDAO.authFetch(key));
    }

}
