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
package org.apache.syncope.core.workflow.java;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.workflow.api.WorkflowDefinitionFormat;
import org.apache.syncope.core.workflow.api.WorkflowException;

/**
 * Simple implementation basically not involving any workflow engine.
 */
public class DefaultAnyObjectWorkflowAdapter extends AbstractAnyObjectWorkflowAdapter {

    @Override
    public WorkflowResult<String> create(final AnyObjectTO anyObjectTO) {
        AnyObject anyObject = entityFactory.newEntity(AnyObject.class);
        dataBinder.create(anyObject, anyObjectTO);
        anyObject = anyObjectDAO.save(anyObject);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.CREATE, anyObject.getResourceKeys());

        return new WorkflowResult<>(anyObject.getKey(), propByRes, "create");
    }

    @Override
    protected WorkflowResult<String> doUpdate(final AnyObject anyObject, final AnyObjectPatch anyObjectPatch) {
        PropagationByResource propByRes = dataBinder.update(anyObject, anyObjectPatch);

        AnyObject updated = anyObjectDAO.save(anyObject);

        return new WorkflowResult<>(updated.getKey(), propByRes, "update");
    }

    @Override
    protected void doDelete(final AnyObject anyObject) {
        anyObjectDAO.delete(anyObject);
    }

    @Override
    public WorkflowResult<String> execute(final AnyObjectTO anyObject, final String taskId) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDefinition(final WorkflowDefinitionFormat format, final OutputStream os) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void exportDiagram(final OutputStream os) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public void importDefinition(final WorkflowDefinitionFormat format, final String definition) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return Collections.emptyList();
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId) {
        return null;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

    @Override
    public WorkflowResult<AnyObjectPatch> submitForm(final WorkflowFormTO form) {
        throw new WorkflowException(new UnsupportedOperationException("Not supported."));
    }

}
