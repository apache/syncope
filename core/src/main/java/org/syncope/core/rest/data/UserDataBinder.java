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
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class UserDataBinder extends AbstractAttributableDataBinder {

    @Autowired
    public UserDataBinder(SchemaDAO schemaDAO,
            AttributeValueDAO attributeValueDAO,
            DerivedSchemaDAO derivedSchemaDAO,
            SyncopeRoleDAO syncopeRoleDAO,
            ResourceDAO resourceDAO) {

        this.schemaDAO = schemaDAO;
        this.attributeValueDAO = attributeValueDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
        this.syncopeRoleDAO = syncopeRoleDAO;
        this.resourceDAO = resourceDAO;
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

        syncopeUser = fillAbstractAttributable(
                syncopeUser, userTO, AttributableUtil.USER, scce);

        // roles
        SyncopeRole role = null;
        for (Long roleId : userTO.getRoles()) {
            role = syncopeRoleDAO.find(roleId);

            if (role == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid role " + roleId);
                }
            } else {
                syncopeUser.addRole(role);
                role.addUser(syncopeUser);
            }
        }

        return syncopeUser;
    }

    public UserTO getUserTO(SyncopeUser user) {
        UserTO userTO = new UserTO();
        userTO.setId(user.getId());
        userTO.setCreationTime(user.getCreationTime());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());
        userTO.setPassword(user.getPassword());

        userTO = getAbstractAttributableTO(userTO, user);

        for (SyncopeRole role : user.getRoles()) {
            userTO.addRole(role.getId());
        }

        return userTO;
    }
}
