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
import javassist.NotFoundException;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.rest.controller.UnauthorizedRoleException;

/**
 * Interface for calling underlying workflow implementations.
 */
public interface UserWorkflowAdapter {

    /**
     * Create an user.
     *
     * @param userTO user to be created and wether to propagate it as active
     * @return user just created
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    Map.Entry<Long, Boolean> create(UserTO userTO)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Activate an user.
     *
     * @param userId user to be activated
     * @param token to be verified for activation
     * @return user just updated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    Long activate(Long userId, String token)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

    /**
     * Updated an user.
     *
     * @param userMod modification set to be performed
     * @return user just updated and propagations to be performed
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    Map.Entry<Long, PropagationByResource> update(UserMod userMod)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

    /**
     * Suspend an user.
     *
     * @param userId user to be suspended
     * @return user just suspended
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    Long suspend(Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

    /**
     * Suspend an user.
     *
     * @param userId user to be reactivated
     * @return user just reactivated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    Long reactivate(Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

    /**
     * Delete an user.
     *
     * @param userId user to be deleted
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    void delete(Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;
}
