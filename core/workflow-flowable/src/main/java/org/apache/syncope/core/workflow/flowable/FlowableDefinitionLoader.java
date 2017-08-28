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
package org.apache.syncope.core.workflow.flowable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.workflow.flowable.spring.DomainProcessEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowableDefinitionLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FlowableDefinitionLoader.class);

    @Resource(name = "userWorkflowDef")
    private ResourceWithFallbackLoader userWorkflowDef;

    @Autowired
    private DomainProcessEngine dpEngine;

    @Override
    public Integer getPriority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void load() {
        byte[] wfDef = new byte[0];

        InputStream wfIn = null;
        try {
            wfIn = userWorkflowDef.getResource().getInputStream();
            wfDef = IOUtils.toByteArray(wfIn);
        } catch (IOException e) {
            LOG.error("While loading " + userWorkflowDef.getResource().getFilename(), e);
        } finally {
            IOUtils.closeQuietly(wfIn);
        }

        for (Map.Entry<String, ProcessEngine> entry : dpEngine.getEngines().entrySet()) {
            List<ProcessDefinition> processes = entry.getValue().getRepositoryService().
                    createProcessDefinitionQuery().processDefinitionKey(FlowableUserWorkflowAdapter.WF_PROCESS_ID).
                    list();
            LOG.debug(FlowableUserWorkflowAdapter.WF_PROCESS_ID + " Flowable processes in repository: {}", processes);

            // Only loads process definition from file if not found in repository
            if (processes.isEmpty()) {
                entry.getValue().getRepositoryService().createDeployment().addInputStream(
                        userWorkflowDef.getResource().getFilename(), new ByteArrayInputStream(wfDef)).deploy();

                ProcessDefinition procDef = entry.getValue().getRepositoryService().createProcessDefinitionQuery().
                        processDefinitionKey(FlowableUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().
                        singleResult();

                FlowableDeployUtils.deployModel(entry.getValue(), procDef);

                LOG.debug("Flowable Workflow definition loaded for domain {}", entry.getKey());
            }

            // jump to the next ID block
            for (int i = 0; i < entry.getValue().getProcessEngineConfiguration().getIdBlockSize(); i++) {
                SpringProcessEngineConfiguration.class.cast(entry.getValue().getProcessEngineConfiguration()).
                        getIdGenerator().getNextId();
            }
        }
    }
}
