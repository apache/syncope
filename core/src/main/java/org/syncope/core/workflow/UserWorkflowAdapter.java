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

import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.to.WorkflowFormTO;
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
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Create an user, optionally disabling password policy check.
     *
     * @param userTO user to be created and wether to propagate it as active
     * @param disablePwdPolicyCheck disable password policy check?
     * @return user just created
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, Boolean>> create(UserTO userTO,
            boolean disablePwdPolicyCheck)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Execute a task on an user.
     *
     * @param userTO user to be subject to task
     * @param taskId to be executed
     * @return user just updated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> execute(UserTO userTO, String taskId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

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
    WorkflowResult<Long> activate(Long userId, String token)
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
    WorkflowResult<Long> update(UserMod userMod)
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
    WorkflowResult<Long> suspend(Long userId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException;

    /**
     * Suspend an user.
     *
     * @param user to be suspended
     * @return user just suspended
     * @throws UnauthorizedRoleException authorization exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> suspend(SyncopeUser user)
            throws UnauthorizedRoleException, WorkflowException;

    /**
     * Reactivate an user.
     *
     * @param userId user to be reactivated
     * @return user just reactivated
     * @throws UnauthorizedRoleException authorization exception
     * @throws NotFoundException user not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Long> reactivate(Long userId)
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

    /**
     * Get workflow definition.
     *
     * @return workflow definition as XML
     * @throws WorkflowException workflow exception
     */
    WorkflowDefinitionTO getDefinition()
            throws WorkflowException;

    /**
     * Update workflow definition.
     *
     * @param definition definition as XML
     * @throws NotFoundException definition not found exception
     * @throws WorkflowException workflow exception
     */
    void updateDefinition(WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException;

    /**
     * Get list of defined tasks in workflow.
     *
     * @return list of defined tasks in workflow
     * @throws WorkflowException workflow exception
     */
    List<String> getDefinedTasks()
            throws WorkflowException;

    /**
     * Get all defined forms for current workflow process instances.
     *
     * @return list of defined forms 
     */
    List<WorkflowFormTO> getForms();

    /**
     * Get form for given workflowId (if present).
     *
     * @param workflowId workflow id
     * @return form (if present), otherwise null
     * @throws NotFoundException definition not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowFormTO getForm(String workflowId)
            throws NotFoundException, WorkflowException;

    /**
     * Claim a form for a given user.
     *
     * @param taskId Workflow task to which the form is associated
     * @param username claiming username
     * @return updated form
     * @throws NotFoundException not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowFormTO claimForm(String taskId, String username)
            throws NotFoundException, WorkflowException;

    /**
     * Submit a form.
     *
     * @param form to be submitted
     * @param username submitting username
     * @return user updated by this form submit
     * @throws NotFoundException not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, String>> submitForm(
            WorkflowFormTO form, String username)
            throws NotFoundException, WorkflowException;
}
