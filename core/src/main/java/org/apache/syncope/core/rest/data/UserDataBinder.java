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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.membership.MAttr;
import org.apache.syncope.core.persistence.beans.membership.MDerAttr;
import org.apache.syncope.core.persistence.beans.membership.MVirAttr;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.apache.syncope.core.util.PasswordEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = {Throwable.class})
public class UserDataBinder extends AbstractAttributableDataBinder {

    private static final String[] IGNORE_USER_PROPERTIES = {"memberships", "attributes", "derivedAttributes",
        "virtualAttributes", "resources"};

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Transactional(readOnly = true)
    public SyncopeUser getUserFromId(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        if (userId == null) {
            throw new NotFoundException("Null user id");
        }

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        if (!user.getUsername().equals(EntitlementUtil.getAuthenticatedUsername())) {
            Set<Long> roleIds = user.getRoleIds();
            Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
            roleIds.removeAll(adminRoleIds);
            if (!roleIds.isEmpty()) {
                throw new UnauthorizedRoleException(roleIds);
            }
        }

        return user;
    }

    @Transactional(readOnly = true)
    public UserTO getAuthenticatedUserTO() throws NotFoundException {
        SyncopeUser authUser = userDAO.find(SecurityContextHolder.getContext().getAuthentication().getName());
        return getUserTO(authUser);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(final String username, final String password)
            throws NotFoundException, UnauthorizedRoleException {

        return verifyPassword(getUserFromUsername(username), password);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(final SyncopeUser user, final String password)
            throws NotFoundException, UnauthorizedRoleException {

        return PasswordEncoder.verifyPassword(password, user.getCipherAlgorithm(), user.getPassword());
    }

    @Transactional(readOnly = true)
    public SyncopeUser getUserFromUsername(final String username)
            throws NotFoundException, UnauthorizedRoleException {

        if (username == null) {
            throw new NotFoundException("Null username");
        }

        SyncopeUser user = userDAO.find(username);
        if (user == null) {
            throw new NotFoundException("User " + username);
        }

        Set<Long> roleIds = user.getRoleIds();
        Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        roleIds.removeAll(adminRoleIds);

        if (!roleIds.isEmpty()) {
            throw new UnauthorizedRoleException(roleIds);
        }

        return user;
    }

    /**
     * Get pre-configured password cipher algorithm.
     *
     * @return cipher algorithm.
     * @throws NotFoundException in case of algorithm not included into <code>CipherAlgorithm</code>.
     */
    private CipherAlgorithm getCipherAlgoritm()
            throws NotFoundException {

        final String algorithm = confDAO.find("password.cipher.algorithm", "AES").getValue();

        try {
            return CipherAlgorithm.valueOf(algorithm);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Cipher algorithm " + algorithm);
        }
    }

    public void create(final SyncopeUser user, final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

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
        int passwordHistorySize = 0;

        try {
            Policy policy = policyDAO.getGlobalPasswordPolicy();
            PasswordPolicySpec passwordPolicy = policy.getSpecification();
            passwordHistorySize = passwordPolicy.getHistoryLength();
        } catch (Exception ignore) {
            // ignore exceptions
        }

        if (userTO.getPassword() == null || userTO.getPassword().isEmpty()) {
            LOG.error("No password provided");
        } else {
            try {
                user.setPassword(userTO.getPassword(), getCipherAlgoritm(), passwordHistorySize);
            } catch (NotFoundException e) {
                final SyncopeClientException invalidAlgorith =
                        new SyncopeClientException(SyncopeClientExceptionType.NotFound);
                invalidAlgorith.addElement(e.getMessage());
                scce.addException(invalidAlgorith);

                throw scce;
            }
        }

        // set username
        user.setUsername(userTO.getUsername());

        // set creation date (at execution time)
        user.setCreationDate(new Date());
    }

    /**
     * Update user, given UserMod.
     *
     * @param user to be updated
     * @param userMod bean containing update request
     * @return updated user + propagation by resource
     * @throws SyncopeClientCompositeErrorException if anything goes wrong
     * @see PropagationByResource
     */
    public PropagationByResource update(final SyncopeUser user, final UserMod userMod)
            throws SyncopeClientCompositeErrorException {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        // when requesting to add user to new resources, either directly or
        // through role subscription, password is mandatory (issue 147)
        // first, let's take current resources into account
        Set<String> currentResources = user.getResourceNames();

        // password
        if (userMod.getPassword() != null) {
            int passwordHistorySize = 0;
            try {
                Policy policy = policyDAO.getGlobalPasswordPolicy();
                PasswordPolicySpec passwordPolicy = policy.getSpecification();
                passwordHistorySize = passwordPolicy.getHistoryLength();
            } catch (Exception ignore) {
                // ignore exceptions
            }

            try {
                user.setPassword(userMod.getPassword(), getCipherAlgoritm(), passwordHistorySize);
            } catch (NotFoundException e) {
                final SyncopeClientException invalidAlgorith =
                        new SyncopeClientException(SyncopeClientExceptionType.NotFound);
                invalidAlgorith.addElement(e.getMessage());
                scce.addException(invalidAlgorith);

                throw scce;
            }

            user.setChangePwdDate(new Date());

            propByRes.addAll(ResourceOperation.UPDATE, currentResources);
        }

        // username
        if (userMod.getUsername() != null && !userMod.getUsername().equals(user.getUsername())) {
            String oldUsername = user.getUsername();

            user.setUsername(userMod.getUsername());
            propByRes.addAll(ResourceOperation.UPDATE, currentResources);

            for (ExternalResource resource : user.getResources()) {
                for (AbstractMappingItem mapItem : resource.getUmapping().getItems()) {
                    if (mapItem.isAccountid() && mapItem.getIntMappingType() == IntMappingType.Username) {
                        propByRes.addOldAccountId(resource.getName(), oldUsername);
                    }
                }
            }
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(user, userMod, AttributableUtil.getInstance(AttributableType.USER), scce));

        // store the role ids of membership required to be added
        Set<Long> membershipToBeAddedRoleIds = new HashSet<Long>();
        for (MembershipMod membToBeAdded : userMod.getMembershipsToBeAdded()) {
            membershipToBeAddedRoleIds.add(membToBeAdded.getRole());
        }

        final Set<String> toBeDeprovisioned = new HashSet<String>();
        final Set<String> toBeProvisioned = new HashSet<String>();

        // memberships to be removed
        for (Long membershipId : userMod.getMembershipsToBeRemoved()) {
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
                if (membershipToBeAddedRoleIds.contains(membership.getSyncopeRole().getId())) {

                    Set<Long> attributeIds = new HashSet<Long>(membership.getAttributes().size());
                    for (AbstractAttr attribute : membership.getAttributes()) {
                        attributeIds.add(attribute.getId());
                    }
                    for (Long attributeId : attributeIds) {
                        attrDAO.delete(attributeId, MAttr.class);
                    }
                    attributeIds.clear();

                    // remove derived attributes
                    for (AbstractDerAttr derAttr : membership.getDerivedAttributes()) {

                        attributeIds.add(derAttr.getId());
                    }
                    for (Long derAttrId : attributeIds) {
                        derAttrDAO.delete(derAttrId, MDerAttr.class);
                    }
                    attributeIds.clear();

                    // remove virtual attributes
                    for (AbstractVirAttr virAttr : membership.getVirtualAttributes()) {
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
        for (MembershipMod membershipMod : userMod.getMembershipsToBeAdded()) {
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
                }

                propByRes.merge(fill(membership, membershipMod,
                        AttributableUtil.getInstance(AttributableType.MEMBERSHIP), scce));
            }
        }

        // now, let's see if there are new resource subscriptions without providing password
        if (StringUtils.isBlank(userMod.getPassword())) {
            Set<String> updatedResources = user.getResourceNames();
            updatedResources.removeAll(currentResources);

            for (String res : updatedResources) {
                final ExternalResource extRes = resourceDAO.find(res);

                if (extRes != null && extRes.getUmapping() != null && !MappingUtil.getMatchingMappingItems(
                        extRes.getUmapping().getItems(), "password", IntMappingType.Password).isEmpty()) {

                    SyncopeClientException sce =
                            new SyncopeClientException(SyncopeClientExceptionType.RequiredValuesMissing);
                    sce.addElement("password cannot be empty " + "when subscribing to new resources");
                    scce.addException(sce);

                    throw scce;
                }
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
    public UserTO getUserTO(final SyncopeUser user) {
        UserTO userTO = new UserTO();

        BeanUtils.copyProperties(user, userTO, IGNORE_USER_PROPERTIES);

        connObjectUtil.retrieveVirAttrValues(user, AttributableUtil.getInstance(AttributableType.USER));

        fillTO(userTO, user.getAttributes(), user.getDerivedAttributes(), user.getVirtualAttributes(),
                user.getResources());

        MembershipTO membershipTO;
        for (Membership membership : user.getMemberships()) {
            membershipTO = new MembershipTO();
            membershipTO.setId(membership.getId());
            membershipTO.setRoleId(membership.getSyncopeRole().getId());
            membershipTO.setRoleName(membership.getSyncopeRole().getName());

            fillTO(membershipTO, membership.getAttributes(), membership.getDerivedAttributes(), membership.
                    getVirtualAttributes(), membership.getResources());

            userTO.addMembership(membershipTO);
        }

        return userTO;
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final String username)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserTO(getUserFromUsername(username));
    }

    @Transactional(readOnly = true)
    public UserTO getUserTO(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserTO(getUserFromId(userId));
    }
}
