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
package org.apache.syncope.core.workflow.activiti;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.workflow.activiti.spring.DomainProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivitiDefinitionLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ActivitiDefinitionLoader.class);

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
            LOG.error("While loading " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        } finally {
            IOUtils.closeQuietly(wfIn);
        }

        for (Map.Entry<String, ProcessEngine> entry : dpEngine.getEngines().entrySet()) {
            List<ProcessDefinition> processes = entry.getValue().getRepositoryService().
                    createProcessDefinitionQuery().processDefinitionKey(ActivitiUserWorkflowAdapter.WF_PROCESS_ID).
                    list();
            LOG.debug(ActivitiUserWorkflowAdapter.WF_PROCESS_ID + " Activiti processes in repository: {}", processes);

            // Only loads process definition from file if not found in repository
            if (processes.isEmpty()) {
                entry.getValue().getRepositoryService().createDeployment().addInputStream(
                        ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, new ByteArrayInputStream(wfDef)).deploy();

                ProcessDefinition procDef = entry.getValue().getRepositoryService().createProcessDefinitionQuery().
                        processDefinitionKey(ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().
                        singleResult();

                Model model = entry.getValue().getRepositoryService().newModel();
                ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
                modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, procDef.getName());
                modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
                modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, procDef.getDescription());
                model.setMetaInfo(modelObjectNode.toString());
                model.setName(procDef.getName());
                model.setDeploymentId(procDef.getDeploymentId());
                ActivitiImportUtils.fromJSON(entry.getValue(), procDef, model);

                LOG.debug("Activiti Workflow definition loaded for domain {}", entry.getKey());
            }

            // jump to the next ID block
            for (int i = 0; i < entry.getValue().getProcessEngineConfiguration().getIdBlockSize(); i++) {
                SpringProcessEngineConfiguration.class.cast(entry.getValue().getProcessEngineConfiguration()).
                        getIdGenerator().getNextId();
            }
        }
    }
}
