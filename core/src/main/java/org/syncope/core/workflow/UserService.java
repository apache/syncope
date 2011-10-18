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
package org.syncope.core.workflow;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.rest.data.UserDataBinder;

@Transactional(rollbackFor = {
    Throwable.class
})
public class UserService {

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

        for (String resource : user.getExternalResourceNames()) {
            if (mandatoryResources.contains(resource)) {
                mandatoryResourceNames.add(resource);
            }
        }
        for (SyncopeRole role : user.getRoles()) {
            if (mandatoryRoles.contains(role.getId())) {
                for (ExternalResource resource : role.getExternalResources()) {
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

        return userDAO.save(user);
    }

    public Map.Entry<SyncopeUser, PropagationByResource> update(
            final SyncopeUser user, final UserMod userMod) {

        PropagationByResource propByRes = userDataBinder.update(user, userMod);

        return new DefaultMapEntry(userDAO.save(user), propByRes);
    }

    public void delete(final SyncopeUser user) {
        userDAO.delete(user);
    }
}
