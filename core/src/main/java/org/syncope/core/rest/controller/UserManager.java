/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.types.EntityViolationType;
import org.syncope.types.SyncopeClientExceptionType;

public class UserManager {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(UserManager.class);

    @Autowired
    private UserDataBinder userDataBinder;

    @Autowired
    private UserDAO userDAO;

    public Set<String> getMandatoryResourceNames(final SyncopeUser user,
            Set<Long> mandatoryRoles, Set<String> mandatoryResources) {

        if (mandatoryRoles == null) {
            mandatoryRoles = Collections.EMPTY_SET;
        }
        if (mandatoryResources == null) {
            mandatoryResources = Collections.EMPTY_SET;
        }

        Set<String> mandatoryResourceNames = new HashSet<String>();

        for (TargetResource resource : user.getTargetResources()) {
            if (mandatoryResources.contains(resource.getName())) {
                mandatoryResourceNames.add(resource.getName());
            }
        }
        for (SyncopeRole role : user.getRoles()) {
            if (mandatoryRoles.contains(role.getId())) {
                for (TargetResource resource : role.getTargetResources()) {
                    mandatoryResourceNames.add(resource.getName());
                }
            }
        }

        return mandatoryResourceNames;
    }

    public SyncopeUser create(final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeUser user = new SyncopeUser();
        userDataBinder.create(user, userTO);

        try {
            user = userDAO.save(user);
        } catch (InvalidEntityException e) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidPassword);

            for (Map.Entry<Class, Set<EntityViolationType>> violation :
                    e.getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    sce.addElement(violationType.toString());
                }
            }

            scce.addException(sce);
            throw scce;
        }

        return user;
    }

    public Map.Entry<SyncopeUser, PropagationByResource> update(
            final SyncopeUser user, final UserMod userMod) {

        PropagationByResource propByRes = userDataBinder.update(user, userMod);

        SyncopeUser updated;
        try {
            updated = userDAO.save(user);
        } catch (InvalidEntityException e) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidPassword);

            for (Map.Entry<Class, Set<EntityViolationType>> violation :
                    e.getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    sce.addElement(violationType.toString());
                }
            }

            scce.addException(sce);
            throw scce;
        }

        return new DefaultMapEntry(updated, propByRes);
    }

    public void delete(final SyncopeUser user) {
        userDAO.delete(user);
    }
}
