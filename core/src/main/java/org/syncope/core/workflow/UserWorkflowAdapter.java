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

import java.util.Set;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.propagation.PropagationException;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface UserWorkflowAdapter {

    /**
     * Create an user.
     *
     * @param userTO user to be created
     * @param mandatoryRoles roles for mandatory propagation
     * @param mandatoryResources resources for mandatory propagation
     * @return user just created
     * @throws WorkflowException workflow exception
     * @throws PropagationException propagation exception
     */
    SyncopeUser create(UserTO userTO,
            Set<Long> mandatoryRoles, Set<String> mandatoryResources)
            throws WorkflowException, PropagationException;

    /**
     * Updated an user.
     *
     * @param user user to be updated
     * @param userMod modification set to be performed
     * @param mandatoryRoles roles for mandatory propagation
     * @param mandatoryResources resources for mandatory propagation
     * @return user just updated
     * @throws WorkflowException workflow exception
     * @throws PropagationException propagation exception
     */
    SyncopeUser update(SyncopeUser user, UserMod userMod,
            Set<Long> mandatoryRoles, Set<String> mandatoryResources)
            throws WorkflowException, PropagationException;

    /**
     * Delete an user.
     *
     * @param user user to be deleted
     * @param mandatoryRoles roles for mandatory propagation
     * @param mandatoryResources resources for mandatory propagation
     * @throws WorkflowException workflow exception
     * @throws PropagationException propagation exception
     */
    void delete(SyncopeUser user,
            Set<Long> mandatoryRoles, Set<String> mandatoryResources)
            throws WorkflowException, PropagationException;
}
