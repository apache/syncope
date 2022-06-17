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
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class BpmnProcessLogic extends AbstractTransactionalLogic<BpmnProcess> {

    protected final BpmnProcessManager bpmnProcessManager;

    public BpmnProcessLogic(final BpmnProcessManager bpmnProcessManager) {
        this.bpmnProcessManager = bpmnProcessManager;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<BpmnProcess> list() {
        return bpmnProcessManager.getProcesses();
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.BPMN_PROCESS_GET + "')")
    @Transactional(readOnly = true)
    public void exportDefinition(final String key, final BpmnProcessFormat format, final OutputStream os) {
        bpmnProcessManager.exportProcess(key, format, os);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.BPMN_PROCESS_GET + "')")
    @Transactional(readOnly = true)
    public void exportDiagram(final String key, final OutputStream os) {
        bpmnProcessManager.exportDiagram(key, os);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.BPMN_PROCESS_SET + "')")
    public void importDefinition(final String key, final BpmnProcessFormat format, final String definition) {
        bpmnProcessManager.importProcess(key, format, definition);
    }

    @PreAuthorize("hasRole('" + FlowableEntitlement.BPMN_PROCESS_DELETE + "')")
    public void delete(final String key) {
        bpmnProcessManager.deleteProcess(key);
    }

    @Override
    protected BpmnProcess resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
