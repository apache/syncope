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

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.persistence.propagation.PropagationException;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.rest.controller.UserManager;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class NoOpUserWorkflowAdapter implements UserWorkflowAdapter {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(NoOpUserWorkflowAdapter.class);

    @Autowired
    private UserManager userManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PropagationManager propagationManager;

    @Override
    public SyncopeUser create(final UserTO userTO,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        SyncopeUser user = userManager.create(userTO);
        user.setStatus("active");
        user = userDAO.save(user);

        // Now that user is created locally, let's propagate
        Set<String> mandatoryResourceNames =
                userManager.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.create(
                user, userTO.getPassword(), mandatoryResourceNames);

        return user;
    }

    @Override
    public SyncopeUser update(final SyncopeUser user, final UserMod userMod,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        Map.Entry<SyncopeUser, PropagationByResource> updated =
                userManager.update(user, userMod);

        // Now that user is updated locally, let's propagate
        Set<String> mandatoryResourceNames =
                userManager.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.update(user, userMod.getPassword(),
                updated.getValue(), mandatoryResourceNames);

        return updated.getKey();
    }

    @Override
    public void delete(final SyncopeUser user,
            final Set<Long> mandatoryRoles,
            final Set<String> mandatoryResources)
            throws WorkflowException, PropagationException {

        // Propagate delete
        Set<String> mandatoryResourceNames =
                userManager.getMandatoryResourceNames(user,
                mandatoryRoles, mandatoryResources);
        if (!mandatoryResourceNames.isEmpty()) {
            LOG.debug("About to propagate onto these mandatory resources {}",
                    mandatoryResourceNames);
        }

        propagationManager.delete(user, mandatoryResourceNames);

        userManager.delete(user);
    }
}
