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
package org.apache.syncope.server.logic.data;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.persistence.api.dao.ConfDAO;
import org.apache.syncope.persistence.api.dao.NotFoundException;
import org.apache.syncope.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.persistence.api.entity.DerAttr;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.MappingItem;
import org.apache.syncope.persistence.api.entity.PlainAttr;
import org.apache.syncope.persistence.api.entity.VirAttr;
import org.apache.syncope.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.provisioning.api.propagation.PropagationByResource;
import org.apache.syncope.server.security.AuthContextUtil;
import org.apache.syncope.server.security.Encryptor;
import org.apache.syncope.server.spring.BeanUtils;
import org.apache.syncope.server.utils.ConnObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class UserDataBinder extends AbstractAttributableDataBinder {

    private static final String[] IGNORE_USER_PROPERTIES = {
        "memberships", "plainAttrs", "derAttrs", "virAttrs", "resources", "securityQuestion", "securityAnswer"
    };

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

    @Resource(name = "adminUser")
    private String adminUser;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    private final Encryptor encryptor = Encryptor.getInstance();

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
    public Set<String> getResourceNamesForUser(final Long key) {
        return userDAO.authFecthUser(key).getResourceNames();
    }

    @Transactional(readOnly = true)
    public UserTO getAuthenticatedUserTO() {
        final UserTO authUserTO;

        final String authUsername = AuthContextUtil.getAuthenticatedUsername();
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
    public boolean verifyPassword(final String username, final String password) {
        return verifyPassword(userDAO.authFecthUser(username), password);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(final User user, final String password) {
        return encryptor.verify(password, user.getCipherAlgorithm(), user.getPassword());
    }

    private void setPassword(final User user, final String password,
            final SyncopeClientCompositeException scce) {

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

    public void create(final User user, final UserTO userTO, final boolean storePassword) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // memberships
        Role role;
        for (MembershipTO membershipTO : userTO.getMemberships()) {
            role = roleDAO.find(membershipTO.getRoleId());

            if (role == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid role " + membershipTO.getRoleName());
                }
            } else {
                Membership membership = null;
                if (user.getKey() != null) {
                    membership = user.getMembership(role.getKey()) == null
                            ? membershipDAO.find(user, role)
                            : user.getMembership(role.getKey());
                }
                if (membership == null) {
                    membership = entityFactory.newEntity(Membership.class);
                    membership.setRole(role);
                    membership.setUser(user);

                    user.addMembership(membership);
                }

                fill(membership, membershipTO, attrUtilFactory.getInstance(AttributableType.MEMBERSHIP), scce);
            }
        }

        // attributes, derived attributes, virtual attributes and resources
        fill(user, userTO, attrUtilFactory.getInstance(AttributableType.USER), scce);

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
    public PropagationByResource update(final User toBeUpdated, final UserMod userMod) {
        // Re-merge any pending change from workflow tasks
        User user = userDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        Set<String> currentResources = user.getResourceNames();

        // password
        if (StringUtils.isNotBlank(userMod.getPassword())) {
            setPassword(user, userMod.getPassword(), scce);
            user.setChangePwdDate(new Date());
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // username
        if (userMod.getUsername() != null && !userMod.getUsername().equals(user.getUsername())) {
            String oldUsername = user.getUsername();

            user.setUsername(userMod.getUsername());
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);

            for (ExternalResource resource : user.getResources()) {
                for (MappingItem mapItem : resource.getUmapping().getItems()) {
                    if (mapItem.isAccountid() && mapItem.getIntMappingType() == IntMappingType.Username) {
                        propByRes.addOldAccountId(resource.getKey(), oldUsername);
                    }
                }
            }
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
        propByRes.merge(fill(user, userMod, attrUtilFactory.getInstance(AttributableType.USER), scce));

        // store the role ids of membership required to be added
        Set<Long> membershipToBeAddedRoleIds = new HashSet<>();
        for (MembershipMod membToBeAdded : userMod.getMembershipsToAdd()) {
            membershipToBeAddedRoleIds.add(membToBeAdded.getRole());
        }

        final Set<String> toBeDeprovisioned = new HashSet<>();
        final Set<String> toBeProvisioned = new HashSet<>();

        // memberships to be removed
        for (Long membershipId : userMod.getMembershipsToRemove()) {
            LOG.debug("Membership to be removed: {}", membershipId);

            Membership membership = membershipDAO.find(membershipId);
            if (membership == null) {
                LOG.debug("Invalid membership id specified to be removed: {}", membershipId);
            } else {
                if (!membershipToBeAddedRoleIds.contains(membership.getRole().getKey())) {
                    toBeDeprovisioned.addAll(membership.getRole().getResourceNames());
                }

                // In order to make the removeMembership() below to work,
                // we need to be sure to take exactly the same membership
                // of the user object currently in memory (which has potentially
                // some modifications compared to the one stored in the DB
                membership = user.getMembership(membership.getRole().getKey());
                if (membershipToBeAddedRoleIds.contains(membership.getRole().getKey())) {
                    Set<Long> attributeIds = new HashSet<>(membership.getPlainAttrs().size());
                    for (PlainAttr attribute : membership.getPlainAttrs()) {
                        attributeIds.add(attribute.getKey());
                    }
                    for (Long attributeId : attributeIds) {
                        plainAttrDAO.delete(attributeId, MPlainAttr.class);
                    }
                    attributeIds.clear();

                    // remove derived attributes
                    for (DerAttr derAttr : membership.getDerAttrs()) {
                        attributeIds.add(derAttr.getKey());
                    }
                    for (Long derAttrId : attributeIds) {
                        derAttrDAO.delete(derAttrId, MDerAttr.class);
                    }
                    attributeIds.clear();

                    // remove virtual attributes
                    for (VirAttr virAttr : membership.getVirAttrs()) {
                        attributeIds.add(virAttr.getKey());
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

            Role role = roleDAO.find(membershipMod.getRole());
            if (role == null) {
                LOG.debug("Ignoring invalid role {}", membershipMod.getRole());
            } else {
                Membership membership = user.getMembership(role.getKey());
                if (membership == null) {
                    membership = entityFactory.newEntity(Membership.class);
                    membership.setRole(role);
                    membership.setUser(user);

                    user.addMembership(membership);

                    toBeProvisioned.addAll(role.getResourceNames());
                }

                propByRes.merge(fill(membership, membershipMod,
                        attrUtilFactory.getInstance(AttributableType.MEMBERSHIP), scce));
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

        return propByRes;
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final User user) {
        UserTO userTO = new UserTO();

        BeanUtils.copyProperties(user, userTO, IGNORE_USER_PROPERTIES);

        if (user.getSecurityQuestion() != null) {
            userTO.setSecurityQuestion(user.getSecurityQuestion().getKey());
        }

        connObjectUtil.retrieveVirAttrValues(user, attrUtilFactory.getInstance(AttributableType.USER));
        fillTO(userTO, user.getPlainAttrs(), user.getDerAttrs(), user.getVirAttrs(), user.getResources());

        MembershipTO membershipTO;
        for (Membership membership : user.getMemberships()) {
            membershipTO = new MembershipTO();

            // set sys info
            membershipTO.setCreator(membership.getCreator());
            membershipTO.setCreationDate(membership.getCreationDate());
            membershipTO.setLastModifier(membership.getLastModifier());
            membershipTO.setLastChangeDate(membership.getLastChangeDate());

            membershipTO.setKey(membership.getKey());
            membershipTO.setRoleId(membership.getRole().getKey());
            membershipTO.setRoleName(membership.getRole().getName());

            // SYNCOPE-458 retrieve also membership virtual attributes
            connObjectUtil.retrieveVirAttrValues(membership, attrUtilFactory.getInstance(AttributableType.MEMBERSHIP));

            fillTO(membershipTO,
                    membership.getPlainAttrs(), membership.getDerAttrs(), membership.getVirAttrs(),
                    Collections.<ExternalResource>emptyList());

            userTO.getMemberships().add(membershipTO);
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final String username) {
        return getUserTO(userDAO.authFecthUser(username));
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final Long key) {
        return getUserTO(userDAO.authFecthUser(key));
    }

    /**
     * SYNCOPE-459: build virtual attribute changes in case no other changes were made.
     *
     * @param key user id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    public PropagationByResource fillVirtual(
            final Long key, final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated) {

        return fillVirtual(
                userDAO.authFecthUser(key),
                vAttrsToBeRemoved,
                vAttrsToBeUpdated,
                attrUtilFactory.getInstance(AttributableType.USER));
    }

    /**
     * SYNCOPE-501: build membership virtual attribute changes in case no other changes were made.
     *
     * @param key user id
     * @param roleId role id
     * @param membershipId membership id
     * @param vAttrsToBeRemoved virtual attributes to be removed.
     * @param vAttrsToBeUpdated virtual attributes to be updated.
     * @param isRemoval flag to check if fill is on removed or added membership
     * @return operations to be performed on external resources for membership virtual attributes changes
     */
    public PropagationByResource fillMembershipVirtual(
            final Long key, final Long roleId, final Long membershipId, final Set<String> vAttrsToBeRemoved,
            final Set<AttrMod> vAttrsToBeUpdated, final boolean isRemoval) {

        final Membership membership = membershipId == null
                ? userDAO.authFecthUser(key).getMembership(roleId)
                : getMembershipFromId(membershipId);

        return membership == null ? new PropagationByResource() : isRemoval
                ? fillVirtual(
                        membership,
                        membership.getVirAttrs() == null
                                ? Collections.<String>emptySet()
                                : getAttributeNames(membership.getVirAttrs()),
                        vAttrsToBeUpdated,
                        attrUtilFactory.getInstance(AttributableType.MEMBERSHIP))
                : fillVirtual(
                        membership,
                        vAttrsToBeRemoved,
                        vAttrsToBeUpdated,
                        attrUtilFactory.getInstance(AttributableType.MEMBERSHIP));
    }

    private Set<String> getAttributeNames(final List<? extends VirAttr> virAttrs) {
        final Set<String> virAttrNames = new HashSet<String>();
        for (VirAttr attr : virAttrs) {
            virAttrNames.add(attr.getSchema().getKey());
        }
        return virAttrNames;
    }
}
