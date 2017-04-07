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
import java.util.List;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowDefinitionAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowDefinitionAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowDefinitionAdapter;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionAdapter;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowLogic extends AbstractTransactionalLogic<WorkflowDefinitionTO> {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyObjectWorkflowDefinitionAdapter awfAdapter;

    @Autowired
    private UserWorkflowDefinitionAdapter uwfAdapter;

    @Autowired
    private GroupWorkflowDefinitionAdapter gwfAdapter;

    private WorkflowDefinitionAdapter getAdapter(final String anyTypeKey) {
        AnyType anyType = anyTypeDAO.find(anyTypeKey);
        if (anyType == null) {
            LOG.error("Could not find anyType '" + anyTypeKey + "'");
            throw new NotFoundException(anyTypeKey);
        }

        switch (anyType.getKind()) {
            case ANY_OBJECT:
                return awfAdapter;

            case GROUP:
                return gwfAdapter;

            case USER:
            default:
                return uwfAdapter;
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_LIST + "')")
    @Transactional(readOnly = true)
    public List<WorkflowDefinitionTO> list(final String anyType) {
        return getAdapter(anyType).getDefinitions();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_GET + "')")
    @Transactional(readOnly = true)
    public void exportDefinition(
            final String anyType, final String key, final WorkflowDefinitionFormat format, final OutputStream os) {

        getAdapter(anyType).exportDefinition(key, format, os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_GET + "')")
    @Transactional(readOnly = true)
    public void exportDiagram(final String anyType, final String key, final OutputStream os) {
        getAdapter(anyType).exportDiagram(key, os);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_GET + "')")
    public void importDefinition(
            final String anyType, final String key, final WorkflowDefinitionFormat format, final String definition) {

        getAdapter(anyType).importDefinition(key, format, definition);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.WORKFLOW_DEF_DELETE + "')")
    public void delete(final String anyType, final String key) {
        getAdapter(anyType).deleteDefinition(key);
    }

    @Override
    protected WorkflowDefinitionTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }

}
