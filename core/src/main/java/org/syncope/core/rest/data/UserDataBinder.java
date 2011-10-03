/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import org.syncope.core.util.AttributableUtil;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.membership.MAttr;
import org.syncope.core.persistence.beans.membership.MDerAttr;
import org.syncope.core.persistence.beans.membership.MVirAttr;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.types.CipherAlgorithm;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.PropagationOperation;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class UserDataBinder extends AbstractAttributableDataBinder {

    public void create(final SyncopeUser user, final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // password
        int passwordHistorySize = 0;

        try {
            Policy policy = policyDAO.getGlobalPasswordPolicy();
            PasswordPolicy passwordPolicy = policy.getSpecification();
            passwordHistorySize = passwordPolicy.getHistoryLength();
        } catch (Throwable ignore) {
            // ignore exceptions
        }

        SyncopeClientException invalidPassword = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPassword);

        if (userTO.getPassword() == null || userTO.getPassword().isEmpty()) {
            LOG.error("No password provided");

            invalidPassword.addElement("Null password");
        } else {
            user.setPassword(userTO.getPassword(), getCipherAlgoritm(),
                    passwordHistorySize);
        }

        if (!invalidPassword.getElements().isEmpty()) {
            scce.addException(invalidPassword);
        }

        // memberships
        SyncopeRole role;
        for (MembershipTO membershipTO : userTO.getMemberships()) {
            role = roleDAO.find(membershipTO.getRoleId());

            if (role == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring invalid role "
                            + membershipTO.getRoleName());
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

                fill(membership, membershipTO, AttributableUtil.MEMBERSHIP,
                        scce);
            }
        }

        // attributes, derived attributes, virtual attributes and resources
        fill(user, userTO, AttributableUtil.USER, scce);
    }

    public PropagationByResource update(SyncopeUser user, UserMod userMod)
            throws SyncopeClientCompositeErrorException {

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // password
        if (userMod.getPassword() != null) {
            int passwordHistorySize = 0;
            try {
                Policy policy = policyDAO.getGlobalPasswordPolicy();
                PasswordPolicy passwordPolicy = policy.getSpecification();
                passwordHistorySize = passwordPolicy.getHistoryLength();
            } catch (Throwable ignore) {
                // ignore exceptions
            }

            user.setPassword(userMod.getPassword(), getCipherAlgoritm(),
                    passwordHistorySize);
            propByRes.addAll(PropagationOperation.UPDATE,
                    user.getTargetResources());
        }

        // attributes, derived attributes, virtual attributes and resources
        propByRes.merge(fill(user, userMod, AttributableUtil.USER, scce));

        // store the role ids of membership required to be added
        Set<Long> membershipToBeAddedRoleIds = new HashSet<Long>();
        for (MembershipMod membershipToBeAdded :
                userMod.getMembershipsToBeAdded()) {

            membershipToBeAddedRoleIds.add(membershipToBeAdded.getRole());
        }

        // memberships to be removed
        Membership membership = null;
        for (Long membershipId :
                userMod.getMembershipsToBeRemoved()) {

            LOG.debug("Membership to be removed: {}", membershipId);

            membership = membershipDAO.find(membershipId);
            if (membership == null) {
                LOG.debug("Invalid membership id specified to be removed: {}",
                        membershipId);
            } else {
                for (TargetResource resource :
                        membership.getSyncopeRole().getTargetResources()) {

                    if (!membershipToBeAddedRoleIds.contains(
                            membership.getSyncopeRole().getId())) {

                        propByRes.add(PropagationOperation.DELETE, resource);
                    }
                }

                // In order to make the removeMembership() below to work,
                // we need to be sure to take exactly the same membership
                // of the user object currently in memory (which has potentially
                // some modifications compared to the one stored in the DB
                membership = user.getMembership(
                        membership.getSyncopeRole().getId());
                if (membershipToBeAddedRoleIds.contains(
                        membership.getSyncopeRole().getId())) {

                    Set<Long> attributeIds = new HashSet<Long>(
                            membership.getAttributes().size());
                    for (AbstractAttr attribute : membership.getAttributes()) {
                        attributeIds.add(attribute.getId());
                    }
                    for (Long attributeId : attributeIds) {
                        attributeDAO.delete(attributeId, MAttr.class);
                    }
                    attributeIds.clear();

                    // remove derived attributes
                    for (AbstractDerAttr derAttr :
                            membership.getDerivedAttributes()) {

                        attributeIds.add(derAttr.getId());
                    }
                    for (Long derAttrId : attributeIds) {
                        derAttrDAO.delete(derAttrId, MDerAttr.class);
                    }
                    attributeIds.clear();

                    // remove virtual attributes
                    for (AbstractVirAttr virAttr :
                            membership.getVirtualAttributes()) {

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
        SyncopeRole role = null;
        for (MembershipMod membershipMod :
                userMod.getMembershipsToBeAdded()) {

            LOG.debug("Membership to be added: role({})",
                    membershipMod.getRole());

            role = roleDAO.find(membershipMod.getRole());
            if (role == null) {
                LOG.debug("Ignoring invalid role {}", membershipMod.getRole());
            } else {
                membership = user.getMembership(role.getId());
                if (membership == null) {
                    membership = new Membership();
                    membership.setSyncopeRole(role);
                    membership.setSyncopeUser(user);

                    user.addMembership(membership);

                    propByRes.addAll(PropagationOperation.UPDATE,
                            role.getTargetResources());
                }

                propByRes.merge(fill(membership, membershipMod,
                        AttributableUtil.MEMBERSHIP, scce));
            }
        }

        return propByRes;
    }

    public UserTO getUserTO(final SyncopeUser user) {
        UserTO userTO = new UserTO();
        userTO.setId(user.getId());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());
        userTO.setPassword(user.getPassword());
        userTO.setStatus(user.getStatus());

        fillTO(userTO,
                user.getAttributes(),
                user.getDerivedAttributes(),
                user.getVirtualAttributes(),
                user.getTargetResources());

        MembershipTO membershipTO;
        for (Membership membership : user.getMemberships()) {
            membershipTO = new MembershipTO();
            membershipTO.setId(membership.getId());
            membershipTO.setRoleId(membership.getSyncopeRole().getId());
            membershipTO.setRoleName(membership.getSyncopeRole().getName());

            fillTO(membershipTO,
                    membership.getAttributes(),
                    membership.getDerivedAttributes(),
                    membership.getVirtualAttributes(),
                    membership.getTargetResources());

            userTO.addMembership(membershipTO);
        }

        return userTO;
    }

    private CipherAlgorithm getCipherAlgoritm() {
        CipherAlgorithm cipherAlgoritm;

        try {
            cipherAlgoritm = CipherAlgorithm.valueOf(
                    confDAO.find("password.cipher.algorithm").getValue());
        } catch (Exception e) {
            LOG.error("Cipher algorithm nof found. Let's use AES", e);
            cipherAlgoritm = CipherAlgorithm.AES;
        }

        return cipherAlgoritm;
    }
}
