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
package org.apache.syncope.core.logic;

import java.io.OutputStream;
import java.lang.reflect.Method;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.apache.syncope.core.workflow.api.WorkflowAdapter;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowLogic extends AbstractTransactionalLogic<AbstractBaseBean> {

    @Autowired
    private AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private GroupWorkflowAdapter gwfAdapter;

    private void exportDefinition(
            final WorkflowAdapter adapter, final WorkflowDefinitionFormat format, final OutputStream os) {

        adapter.exportDefinition(format, os);
    }

    private WorkflowDefinitionFormat getFormat(final MediaType format) {
        return format.equals(MediaType.APPLICATION_JSON_TYPE)
                ? WorkflowDefinitionFormat.JSON
                : WorkflowDefinitionFormat.XML;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportAnyObjectDefinition(final MediaType format, final OutputStream os) {
        exportDefinition(awfAdapter, getFormat(format), os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportUserDefinition(final MediaType format, final OutputStream os) {
        exportDefinition(uwfAdapter, getFormat(format), os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportGroupDefinition(final MediaType format, final OutputStream os) {
        exportDefinition(gwfAdapter, getFormat(format), os);
    }

    private void exportDiagram(final WorkflowAdapter adapter, final OutputStream os) {
        adapter.exportDiagram(os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportAnyObjectDiagram(final OutputStream os) {
        exportDiagram(awfAdapter, os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportUserDiagram(final OutputStream os) {
        exportDiagram(uwfAdapter, os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_READ + "')")
    @Transactional(readOnly = true)
    public void exportGroupDiagram(final OutputStream os) {
        exportDiagram(gwfAdapter, os);
    }

    private void importDefinition(
            final WorkflowAdapter adapter, final WorkflowDefinitionFormat format, final String definition) {

        adapter.importDefinition(format, definition);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_UPDATE + "')")
    public void importAnyObjectDefinition(final MediaType format, final String definition) {
        importDefinition(awfAdapter, getFormat(format), definition);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_UPDATE + "')")
    public void importUserDefinition(final MediaType format, final String definition) {
        importDefinition(uwfAdapter, getFormat(format), definition);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_UPDATE + "')")
    public void importGroupDefinition(final MediaType format, final String definition) {
        importDefinition(gwfAdapter, getFormat(format), definition);
    }

    @Override
    protected AbstractBaseBean resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
