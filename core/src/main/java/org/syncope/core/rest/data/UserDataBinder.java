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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class UserDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    public UserDataBinder(SchemaDAO schemaDAO,
            AttributeDAO attributeDAO,
            AttributeValueDAO attributeValueDAO,
            DerivedSchemaDAO derivedSchemaDAO,
            DerivedAttributeDAO derivedAttributeDAO,
            SyncopeUserDAO syncopeUserDAO,
            SyncopeRoleDAO syncopeRoleDAO,
            ResourceDAO resourceDAO,
            MembershipDAO membershipDAO) {

        this.schemaDAO = schemaDAO;
        this.attributeDAO = attributeDAO;
        this.attributeValueDAO = attributeValueDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
        this.syncopeUserDAO = syncopeUserDAO;
        this.syncopeRoleDAO = syncopeRoleDAO;
        this.resourceDAO = resourceDAO;
        this.membershipDAO = membershipDAO;
    }

    public SyncopeUser createSyncopeUser(UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeUser syncopeUser = new SyncopeUser();

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // password
        // TODO: check password policies
        SyncopeClientException invalidPassword = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPassword);
        if (userTO.getPassword() == null
                || userTO.getPassword().length() == 0) {

            log.error("No password provided");

            invalidPassword.addElement("Null password");
        } else {
            syncopeUser.setPassword(userTO.getPassword());
        }

        if (!invalidPassword.getElements().isEmpty()) {
            scce.addException(invalidPassword);
        }

        // attributes, derived attributes and resources
        syncopeUser = (SyncopeUser) fill(
                syncopeUser, userTO, AttributableUtil.USER, scce);

        // memberships
        SyncopeRole role = null;
        for (MembershipTO membershipTO : userTO.getMemberships()) {
            role = syncopeRoleDAO.find(membershipTO.getRole());

            if (role == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid role "
                            + membershipTO.getRole());
                }
            } else {
                Membership membership = new Membership();
                membership.setSyncopeRole(role);
                membership.setSyncopeUser(syncopeUser);

                membership = (Membership) fill(membership, membershipTO,
                        AttributableUtil.MEMBERSHIP, scce);

                syncopeUser.addMembership(membership);
            }
        }

        return syncopeUser;
    }

    public ResourceOperations updateSyncopeUser(
            SyncopeUser syncopeUser, UserMod userMod)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        // password
        if (userMod.getPassword() != null) {
            syncopeUser.setPassword(userMod.getPassword());
        }

        // attributes, derived attributes and resources
        ResourceOperations resourceOperations =
                fill(syncopeUser, userMod, AttributableUtil.USER, scce);

        // memberships
        SyncopeRole role = null;
        for (MembershipMod membershipMod : userMod.getMembershipMods()) {
            role = syncopeRoleDAO.find(membershipMod.getRole());

            if (role == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid role "
                            + membershipMod.getRole());
                }
            } else {
                Membership membership = membershipDAO.find(syncopeUser, role);
                if (membership == null) {
                    membership = new Membership();
                    membership.setSyncopeRole(role);
                    membership.setSyncopeUser(syncopeUser);

                    syncopeUser.addMembership(membership);
                }

                resourceOperations.merge(fill(membership, membershipMod,
                        AttributableUtil.MEMBERSHIP, scce));
            }
        }

        return resourceOperations;
    }

    public UserTO getUserTO(SyncopeUser user) {
        UserTO userTO = new UserTO();
        userTO.setId(user.getId());
        userTO.setCreationTime(user.getCreationTime());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());
        userTO.setPassword(user.getPassword());

        userTO = (UserTO) fillTO(userTO, user.getAttributes(),
                user.getDerivedAttributes(), user.getResources());

        MembershipTO membershipTO = new MembershipTO();
        for (Membership membership : user.getMemberships()) {
            membershipTO.setRole(membership.getSyncopeRole().getId());

            membershipTO = (MembershipTO) fillTO(membershipTO,
                    membership.getAttributes(),
                    membership.getDerivedAttributes(),
                    membership.getResources());

            userTO.addMembership(membershipTO);
        }

        return userTO;
    }
}
