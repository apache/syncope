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
package org.apache.syncope.core.rest.controller;

import java.io.OutputStream;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.WorkflowSubCategory;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.workflow.WorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    private void exportDefinition(
            final WorkflowAdapter adapter, final WorkflowDefinitionFormat format, final OutputStream os)
            throws WorkflowException {

        adapter.exportDefinition(format, os);

        auditManager.audit(Category.workflow, WorkflowSubCategory.exportDefinition, Result.success,
                "Successfully exported workflow definition");
    }

    private WorkflowDefinitionFormat getFormat(final MediaType format) {
        return format.equals(MediaType.APPLICATION_JSON_TYPE)
                ? WorkflowDefinitionFormat.JSON
                : WorkflowDefinitionFormat.XML;
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @Transactional(readOnly = true)
    public void exportUserDefinition(final MediaType format, final OutputStream os)
            throws WorkflowException {

        exportDefinition(uwfAdapter, getFormat(format), os);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @Transactional(readOnly = true)
    public void exportRoleDefinition(final MediaType format, final OutputStream os)
            throws WorkflowException {

        exportDefinition(rwfAdapter, getFormat(format), os);
    }

    private void exportDiagram(final WorkflowAdapter adapter, final OutputStream os)
            throws WorkflowException {

        adapter.exportDiagram(os);

        auditManager.audit(Category.workflow, WorkflowSubCategory.exportDiagram, Result.success,
                "Successfully export workflow diagram");
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @Transactional(readOnly = true)
    public void exportUserDiagram(final OutputStream os)
            throws WorkflowException {

        exportDiagram(uwfAdapter, os);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @Transactional(readOnly = true)
    public void exportRoleDiagram(final OutputStream os)
            throws WorkflowException {

        exportDiagram(rwfAdapter, os);
    }

    private void importDefinition(
            final WorkflowAdapter adapter, final WorkflowDefinitionFormat format, final String definition) {

        adapter.importDefinition(format, definition);

        auditManager.audit(Category.workflow, WorkflowSubCategory.importDefinition, Result.success,
                "Successfully imported workflow definition");
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    public void importUserDefinition(final MediaType format, final String definition) {
        importDefinition(uwfAdapter, getFormat(format), definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    public void importRoleDefinition(final MediaType format, final String definition) {
        importDefinition(rwfAdapter, getFormat(format), definition);
    }

    private List<String> getDefinedTasks(final WorkflowAdapter adapter) {
        List<String> definedTasks = adapter.getDefinedTasks();

        auditManager.audit(Category.workflow, WorkflowSubCategory.getDefinedTasks, Result.success,
                "Successfully got the list of defined workflow tasks: " + definedTasks.size());

        return definedTasks;
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    public List<String> getDefinedUserTasks() {
        return getDefinedTasks(uwfAdapter);
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    public List<String> getDefinedRoleTasks() {
        return getDefinedTasks(rwfAdapter);
    }
}
