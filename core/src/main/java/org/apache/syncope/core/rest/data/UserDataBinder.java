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
package org.apache.syncope.core.rest.data;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeClientCompositeException;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SecurityQuestion;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.SecurityQuestionDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.Encryptor;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class UserDataBinder extends AbstractAttributableDataBinder {

    private static final String[] IGNORE_USER_PROPERTIES = {
        "memberships", "attrs", "derAttrs", "virAttrs", "resources", "securityQuestion", "securityAnswer"
    };

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    private final Encryptor encryptor = Encryptor.getInstance();

    private void securityChecks(final SyncopeUser user) {
        // Allows anonymous (during self-registration) and self (during self-update) to read own SyncopeUser,
        // otherwise goes thorugh security checks to see if needed role entitlements are owned
        if (!EntitlementUtil.getAuthenticatedUsername().equals(anonymousUser)
                && !EntitlementUtil.getAuthenticatedUsername().equals(user.getUsername())) {

            Set<Long> roleIds = user.getRoleIds();
            Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
            roleIds.removeAll(adminRoleIds);
            if (!roleIds.isEmpty()) {
                throw new UnauthorizedRoleException(roleIds);
            }
        }
    }

    @Transactional(readOnly = true)
    public SyncopeUser getUserFromId(final Long userId) {
        if (userId == null) {
            throw new NotFoundException("Null user id");
        }

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        securityChecks(user);

        return user;
    }

    @Transactional(readOnly = true)
    public SyncopeUser getUserFromUsername(final String username) {
        if (username == null) {
            throw new NotFoundException("Null username");
        }

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        securityChecks(user);

        return user;
    }

    @Transactional(readOnly = true)
    public Membership getMembershipFromId(final Long membershipId) {
        if (membershipId == null) {
            throw new NotFoundException("Null membership id");
        }

        Membership membership = membershipDAO.find(membershipId);
        if (membership == null) {
            throw new NotFoundException("Membership " + membershipId);
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public Set<String> getResourceNamesForUserId(final Long userId) {
        return getUserFromId(userId).getResourceNames();
    }

    @Transactional(readOnly = true)
    public UserTO getAuthenticatedUserTO() {
        final UserTO authUserTO;

        final String authUsername = EntitlementUtil.getAuthenticatedUsername();
        if (anonymousUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setId(-2);
            authUserTO.setUsername(anonymousUser);
        } else if (adminUser.equals(authUsername)) {
            authUserTO = new UserTO();
            authUserTO.setId(-1);
            authUserTO.setUsername(adminUser);
        } else {
            SyncopeUser authUser = userDAO.find(authUsername);
            authUserTO = getUserTO(authUser, true);
        }

        return authUserTO;
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(final String username, final String password) {
        return verifyPassword(getUserFromUsername(username), password);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(final SyncopeUser user, final String password) {
        return encryptor.verify(password, user.getCipherAlgorithm(), user.getPassword());
    }

    private void setPassword(final SyncopeUser user, final String password,
            final SyncopeClientCompositeException scce) {

        try {
            user.setPassword(password, Encryptor.getPredefinedCipherAlgoritm());
        } catch (NotFoundException e) {
            final SyncopeClientException invalidCiperAlgorithm =
                    SyncopeClientException.build(ClientExceptionType.NotFound);
            invalidCiperAlgorithm.getElements().add(e.getMessage());
            scce.addException(invalidCiperAlgorithm);

            throw scce;
        }
    }

    public void create(final SyncopeUser user, final UserTO userTO, final boolean storePassword) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // memberships
        SyncopeRole role;
        for (MembershipTO membershipTO : userTO.getMemberships()) {
            role = roleDAO.find(membershipTO.getRoleId());

            if (role == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid role " + membershipTO.getRoleName());
                }
            } else {
                Membership membership = null;
                if (user.getId() != null) {
                    membership = user.getMembership(role.getId()) == null
                            ? membershipDAO.find(user, role)
                            : user.getMembership(role.getId());
                }
                if (membership == null) {
                    membership = new Membership();
                    membership.setSyncopeRole(role);
                    membership.setSyncopeUser(user);

                    user.addMembership(membership);
                }

                fill(membership, membershipTO, AttributableUtil.getInstance(AttributableType.MEMBERSHIP), scce);
            }
        }

        // attributes, derived attributes, virtual attributes and resources
        fill(user, userTO, AttributableUtil.getInstance(AttributableType.USER), scce);

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
    public PropagationByResource update(final SyncopeUser toBeUpdated, final UserMod userMod) {
        // Re-merge any pending change from workflow tasks
        SyncopeUser user = userDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Set<String> currentResources = user.getResourceNames();

        // fetch account ids before update
        Map<String, String> oldAccountIds = getAccountIds(user, AttributableType.USER);

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

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(user, userMod, AttributableUtil.getInstance(AttributableType.USER), scce));

        // store the role ids of membership required to be added
        Set<Long> membershipToBeAddedRoleIds = new HashSet<Long>();
        for (MembershipMod membToBeAdded : userMod.getMembershipsToAdd()) {
            membershipToBeAddedRoleIds.add(membToBeAdded.getRole());
        }

        final Set<String> toBeDeprovisioned = new HashSet<String>();
        final Set<String> toBeProvisioned = new HashSet<String>();

        // memberships to be removed
        for (Long membershipId : userMod.getMembershipsToRemove()) {
            LOG.debug("Membership to be removed: {}", membershipId);

            Membership membership = membershipDAO.find(membershipId);
            if (membership == null) {
                LOG.debug("Invalid membership id specified to be removed: {}", membershipId);
            } else {
                if (!membershipToBeAddedRoleIds.contains(membership.getSyncopeRole().getId())) {
                    toBeDeprovisioned.addAll(membership.getSyncopeRole().getResourceNames());
                }

                // In order to make the removeMembership() below to work,
                // we need to be sure to take exactly the same membership
                // of the user object currently in memory (which has potentially
                // some modifications compared to the one stored in the DB
                membership = user.getMembership(membership.getSyncopeRole().getId());
                if (membership != null && membershipToBeAddedRoleIds.contains(membership.getSyncopeRole().getId())) {
                    Set<Long> attributeIds = new HashSet<Long>(membership.getAttrs().size());
                    for (AbstractAttr attribute : membership.getAttrs()) {
                        attributeIds.add(attribute.getId());
                    }
                    for (Long attributeId : attributeIds) {
                        attrDAO.delete(attributeId, MAttr.class);
                    }
                    attributeIds.clear();

                    // remove derived attributes
                    for (AbstractDerAttr derAttr : membership.getDerAttrs()) {
                        attributeIds.add(derAttr.getId());
                    }
                    for (Long derAttrId : attributeIds) {
                        derAttrDAO.delete(derAttrId, MDerAttr.class);
                    }
                    attributeIds.clear();

                    // remove virtual attributes
                    for (AbstractVirAttr virAttr : membership.getVirAttrs()) {
                        attributeIds.add(virAttr.getId());
                    }
                    for (Long virAttrId : attributeIds) {
                        virAttrDAO.delete(virAttrId, MVirAttr.class);
                    }
                    attributeIds.clear();
                } else {
                    user.removeMembership(membership);

                    membershipDAO.delete(membershipId);
                }
            }
        }

        // memberships to be added
        for (MembershipMod membershipMod : userMod.getMembershipsToAdd()) {
            LOG.debug("Membership to be added: role({})", membershipMod.getRole());

            SyncopeRole role = roleDAO.find(membershipMod.getRole());
            if (role == null) {
                LOG.debug("Ignoring invalid role {}", membershipMod.getRole());
            } else {
                Membership membership = user.getMembership(role.getId());
                if (membership == null) {
                    membership = new Membership();
                    membership.setSyncopeRole(role);
                    membership.setSyncopeUser(user);

                    user.addMembership(membership);

                    toBeProvisioned.addAll(role.getResourceNames());

                    // SYNCOPE-686: if password is invertible and we are adding resources with password mapping,
                    // ensure that they are counted for password propagation
                    if (toBeUpdated.canDecodePassword()) {
                        for (ExternalResource resource : role.getResources()) {
                            if (resource.getUmapping().getPasswordItem() != null) {
                                if (userMod.getPwdPropRequest() == null) {
                                    userMod.setPwdPropRequest(new StatusMod());
                                }

                                userMod.getPwdPropRequest().getResourceNames().add(resource.getName());
                            }
                        }
                    }
                }

                propByRes.merge(fill(membership, membershipMod,
                        AttributableUtil.getInstance(AttributableType.MEMBERSHIP), scce));
            }
        }

        propByRes.addAll(ResourceOperation.DELETE, toBeDeprovisioned);
        propByRes.addAll(ResourceOperation.UPDATE, toBeProvisioned);

        /**
         * In case of new memberships all the current resources have to be updated in order to propagate new role and
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
    public UserTO getUserTO(final SyncopeUser user, final boolean details) {
        UserTO userTO = new UserTO();

        BeanUtils.copyProperties(user, userTO, IGNORE_USER_PROPERTIES);

        if (user.getSecurityQuestion() != null) {
            userTO.setSecurityQuestion(user.getSecurityQuestion().getId());
        }

        if (details) {
            connObjectUtil.retrieveVirAttrValues(user, AttributableUtil.getInstance(AttributableType.USER));
        }

        fillTO(userTO, user.getAttrs(), user.getDerAttrs(), user.getVirAttrs(), user.getResources());

        if (details) {
            for (Membership membership : user.getMemberships()) {
                MembershipTO membershipTO = new MembershipTO();

                // set sys info
                membershipTO.setCreator(membership.getCreator());
                membershipTO.setCreationDate(membership.getCreationDate());
                membershipTO.setLastModifier(membership.getLastModifier());
                membershipTO.setLastChangeDate(membership.getLastChangeDate());

                membershipTO.setId(membership.getId());
                membershipTO.setRoleId(membership.getSyncopeRole().getId());
                membershipTO.setRoleName(membership.getSyncopeRole().getName());

                // SYNCOPE-458 retrieve also membership virtual attributes
                connObjectUtil.retrieveVirAttrValues(membership, AttributableUtil.getInstance(
                        AttributableType.MEMBERSHIP));

                fillTO(membershipTO,
                        membership.getAttrs(), membership.getDerAttrs(), membership.getVirAttrs(),
                        Collections.<ExternalResource>emptyList());

                userTO.getMemberships().add(membershipTO);
            }
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final String username) {
        return getUserTO(getUserFromUsername(username), true);
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final Long userId) {
        return getUserTO(getUserFromId(userId), true);
    }

    /**
     * SYNCOPE-459: build virtual attribute changes in case no other changes were made.
     *
     * @param id user id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    public PropagationByResource fillVirtual(
            final Long id, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated) {

        return fillVirtual(
                getUserFromId(id),
                vAttrsToBeRemoved,
                vAttrsToBeUpdated,
                AttributableUtil.getInstance(AttributableType.USER));
    }

    /**
     * SYNCOPE-501: build membership virtual attribute changes in case no other changes were made.
     *
     * @param userId user id
     * @param roleId role id
     * @param membershipId membership id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @param isRemoval flag to check if fill is on removed or added membership
     * @return operations to be performed on external resources for membership virtual attributes changes
     */
    public PropagationByResource fillMembershipVirtual(
            final Long userId, final Long roleId, final Long membershipId, final Set<String> vAttrsToBeRemoved,
            final Set<AttributeMod> vAttrsToBeUpdated, final boolean isRemoval) {

        final Membership membership = membershipId == null
                ? getUserFromId(userId).getMembership(roleId)
                : getMembershipFromId(membershipId);

        return membership == null ? new PropagationByResource() : isRemoval
                ? fillVirtual(
                        membership,
                        membership.getVirAttrs() == null
                                ? Collections.<String>emptySet()
                                : getAttributeNames(membership.getVirAttrs()),
                        vAttrsToBeUpdated,
                        AttributableUtil.getInstance(AttributableType.MEMBERSHIP))
                : fillVirtual(
                        membership,
                        vAttrsToBeRemoved,
                        vAttrsToBeUpdated,
                        AttributableUtil.getInstance(AttributableType.MEMBERSHIP));
    }

    private Set<String> getAttributeNames(final List<? extends AbstractVirAttr> virAttrs) {
        final Set<String> virAttrNames = new HashSet<String>();
        for (AbstractVirAttr attr : virAttrs) {
            virAttrNames.add(attr.getSchema().getName());
        }
        return virAttrNames;
    }
}
