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

import org.apache.syncope.NotFoundException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.workflow.UserWorkflowAdapter;
import org.apache.syncope.workflow.WorkflowException;
import org.apache.syncope.to.WorkflowDefinitionTO;
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
    private UserWorkflowAdapter wfAdapter;

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/definition")
    @Transactional(readOnly = true)
    public WorkflowDefinitionTO getDefinition() throws WorkflowException {

        WorkflowDefinitionTO result = wfAdapter.getDefinition();

        auditManager.audit(Category.workflow, WorkflowSubCategory.getDefinition, Result.success,
                "Successfully got workflow definition");

        return result;
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT, value = "/definition")
    public void updateDefinition(@RequestBody final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        wfAdapter.updateDefinition(definition);

        auditManager.audit(Category.workflow, WorkflowSubCategory.updateDefinition, Result.success,
                "Successfully updated workflow definition");
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/tasks")
    public ModelAndView getDefinedTasks() throws WorkflowException {

        List<String> definedTasks = wfAdapter.getDefinedTasks();

        auditManager.audit(Category.workflow, WorkflowSubCategory.getDefinedTasks, Result.success,
                "Successfully got the list of defined workflow tasks: " + definedTasks.size());

        return new ModelAndView().addObject(definedTasks);
    }
}
