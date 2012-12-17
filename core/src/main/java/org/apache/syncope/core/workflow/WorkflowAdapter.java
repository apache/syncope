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
package org.apache.syncope.core.workflow;

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.to.WorkflowDefinitionTO;
import org.apache.syncope.client.to.WorkflowFormTO;
import org.apache.syncope.core.util.NotFoundException;

public interface WorkflowAdapter {

    /**
     * Give the class to be instantiated and invoked by SpringContextInitializer for loading anything needed by this
     * adapter.
     *
     * @return null if no init is needed or the WorkflowLoader class for handling initialization
     * @see org.apache.syncope.core.init.SpringContextInitializer
     */
    Class<? extends WorkflowLoader> getLoaderClass();

    /**
     * Get workflow definition.
     *
     * @return workflow definition as XML
     * @throws WorkflowException workflow exception
     */
    WorkflowDefinitionTO getDefinition() throws WorkflowException;

    /**
     * Update workflow definition.
     *
     * @param definition definition as XML
     * @throws NotFoundException definition not found exception
     * @throws WorkflowException workflow exception
     */
    void updateDefinition(WorkflowDefinitionTO definition) throws NotFoundException, WorkflowException;

    /**
     * Get list of defined tasks in workflow.
     *
     * @return list of defined tasks in workflow
     * @throws WorkflowException workflow exception
     */
    List<String> getDefinedTasks() throws WorkflowException;

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
    WorkflowFormTO getForm(String workflowId) throws NotFoundException, WorkflowException;

    /**
     * Claim a form for a given user.
     *
     * @param taskId Workflow task to which the form is associated
     * @param username claiming username
     * @return updated form
     * @throws NotFoundException not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowFormTO claimForm(String taskId, String username) throws NotFoundException, WorkflowException;

    /**
     * Submit a form.
     *
     * @param form to be submitted
     * @param username submitting username
     * @return user updated by this form submit
     * @throws NotFoundException not found exception
     * @throws WorkflowException workflow exception
     */
    WorkflowResult<Map.Entry<Long, String>> submitForm(WorkflowFormTO form, String username)
            throws NotFoundException, WorkflowException;
}
