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
package org.apache.syncope.core.workflow.user.activiti;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.syncope.core.init.WorkflowLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivitiWorkflowLoader implements WorkflowLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ActivitiWorkflowLoader.class);

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public void load() {
        List<ProcessDefinition> processes = repositoryService.createProcessDefinitionQuery().processDefinitionKey(
                ActivitiUserWorkflowAdapter.WF_PROCESS_ID).list();
        LOG.debug(ActivitiUserWorkflowAdapter.WF_PROCESS_ID + " Activiti processes in repository: {}", processes);

        // Only loads process definition from file if not found in repository
        if (processes.isEmpty()) {
            InputStream wfDefinitionStream = null;
            try {
                wfDefinitionStream = getClass().getResourceAsStream(
                        "/" + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE);

                repositoryService.createDeployment().addInputStream(
                        ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, wfDefinitionStream).deploy();
            } finally {
                if (wfDefinitionStream != null) {
                    try {
                        wfDefinitionStream.close();
                    } catch (IOException e) {
                        LOG.error("While closing input stream for {}",
                                ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
                    }
                }
            }
        }
    }
}
