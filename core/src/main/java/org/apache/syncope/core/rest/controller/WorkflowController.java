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

import java.lang.reflect.Method;
import org.apache.syncope.common.to.WorkflowDefinitionTO;
import org.apache.syncope.core.workflow.WorkflowAdapter;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/workflow")
public class WorkflowController extends AbstractTransactionalController<WorkflowDefinitionTO> {

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    private WorkflowDefinitionTO getDefinition(final WorkflowAdapter adapter) throws WorkflowException {
        WorkflowDefinitionTO result = adapter.getDefinition();
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

    private void updateDefinition(final WorkflowAdapter adapter, final WorkflowDefinitionTO definition) {
        adapter.updateDefinition(definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT, value = "/definition/user")
    public void updateUserDefinition(@RequestBody final WorkflowDefinitionTO definition) {
        updateDefinition(uwfAdapter, definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT, value = "/definition/role")
    public void updateRoleDefinition(@RequestBody final WorkflowDefinitionTO definition) {
        updateDefinition(rwfAdapter, definition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WorkflowDefinitionTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
