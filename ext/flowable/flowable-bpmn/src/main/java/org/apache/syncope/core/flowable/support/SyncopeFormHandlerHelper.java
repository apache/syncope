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
package org.apache.syncope.core.flowable.support;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.form.TaskFormHandler;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Used to inject {@link SyncopeTaskFormHandler} rather than
 * {@link org.flowable.engine.impl.form.DefaultTaskFormHandler}.
 */
public class SyncopeFormHandlerHelper extends FormHandlerHelper {

    @Override
    public TaskFormHandler getTaskFormHandlder(final String procDefId, final String taskId) {
        Process process = ProcessDefinitionUtil.getProcess(procDefId);
        FlowElement flowElement = process.getFlowElement(taskId, true);
        if (flowElement instanceof final UserTask userTask) {

            ProcessDefinition processDefinitionEntity = ProcessDefinitionUtil.getProcessDefinition(procDefId);
            DeploymentEntity deploymentEntity = CommandContextUtil.getProcessEngineConfiguration().
                    getDeploymentEntityManager().findById(processDefinitionEntity.getDeploymentId());

            TaskFormHandler taskFormHandler = new SyncopeTaskFormHandler();
            taskFormHandler.parseConfiguration(
                    userTask.getFormProperties(), userTask.getFormKey(), deploymentEntity, processDefinitionEntity);
            return taskFormHandler;
        }

        return null;
    }
}
