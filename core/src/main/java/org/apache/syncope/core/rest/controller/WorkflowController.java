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
import java.lang.reflect.Method;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.AbstractBaseBean;
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
public class WorkflowController extends AbstractTransactionalController<AbstractBaseBean> {

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    private void exportDefinition(
            final WorkflowAdapter adapter, final WorkflowDefinitionFormat format, final OutputStream os)
            throws WorkflowException {

        adapter.exportDefinition(format, os);
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
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    public void importUserDefinition(final MediaType format, final String definition) {
        importDefinition(uwfAdapter, getFormat(format), definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    public void importRoleDefinition(final MediaType format, final String definition) {
        importDefinition(rwfAdapter, getFormat(format), definition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractBaseBean resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
