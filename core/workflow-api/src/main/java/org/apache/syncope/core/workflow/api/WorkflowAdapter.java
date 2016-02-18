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
package org.apache.syncope.core.workflow.api;

import java.io.OutputStream;
import java.util.List;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.core.provisioning.api.WorkflowResult;

public interface WorkflowAdapter {

    /**
     * @return any string that might be interpreted as 'prefix' (say table prefix in SQL environments)
     */
    String getPrefix();

    /**
     * Export workflow definition.
     *
     * @param format export format
     * @param os export stream
     */
    void exportDefinition(WorkflowDefinitionFormat format, OutputStream os);

    /**
     * Export workflow graphical representation (if available).
     *
     * @param os export stream
     */
    void exportDiagram(OutputStream os);

    /**
     * Update workflow definition.
     *
     * @param format import format
     * @param definition definition
     */
    void importDefinition(WorkflowDefinitionFormat format, String definition);

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
     */
    WorkflowFormTO getForm(String workflowId);

    /**
     * Claim a form for a given object.
     *
     * @param taskId Workflow task to which the form is associated
     * @return updated form
     */
    WorkflowFormTO claimForm(String taskId);

    /**
     * Submit a form.
     *
     * @param form to be submitted
     * @return object updated by this form submit
     */
    WorkflowResult<? extends AnyPatch> submitForm(WorkflowFormTO form);
}
