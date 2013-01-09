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

import java.util.List;
import org.apache.syncope.client.to.WorkflowDefinitionTO;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.WorkflowSubCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/workflow")
public class WorkflowController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    private WorkflowDefinitionTO getDefinition(final WorkflowAdapter adapter) throws WorkflowException {
        WorkflowDefinitionTO result = adapter.getDefinition();

        auditManager.audit(Category.workflow, WorkflowSubCategory.getDefinition, Result.success,
                "Successfully read workflow definition");

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/definition/user")
    @Transactional(readOnly = true)
    public WorkflowDefinitionTO getUserDefinition() throws WorkflowException {
        return getDefinition(uwfAdapter);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/definition/role")
    @Transactional(readOnly = true)
    public WorkflowDefinitionTO getRoleDefinition() throws WorkflowException {
        return getDefinition(rwfAdapter);
    }

    private void updateDefinition(final WorkflowAdapter adapter, final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        adapter.updateDefinition(definition);

        auditManager.audit(Category.workflow, WorkflowSubCategory.updateDefinition, Result.success,
                "Successfully updated workflow definition");
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT, value = "/definition/user")
    public void updateUserDefinition(@RequestBody final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        updateDefinition(uwfAdapter, definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT, value = "/definition/role")
    public void updateRoleDefinition(@RequestBody final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        updateDefinition(rwfAdapter, definition);
    }

    private List<String> getDefinedTasks(final WorkflowAdapter adapter) throws WorkflowException {
        List<String> definedTasks = adapter.getDefinedTasks();

        auditManager.audit(Category.workflow, WorkflowSubCategory.getDefinedTasks, Result.success,
                "Successfully got the list of defined workflow tasks: " + definedTasks.size());

        return definedTasks;
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/tasks/user")
    public ModelAndView getDefinedUserTasks() throws WorkflowException {
        return new ModelAndView().addObject(getDefinedTasks(uwfAdapter));
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/tasks/role")
    public ModelAndView getDefinedRoleTasks() throws WorkflowException {
        return new ModelAndView().addObject(getDefinedTasks(rwfAdapter));
    }
}
