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
package org.apache.syncope.core.logic.init;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.core.flowable.impl.FlowableDeployUtils;
import org.apache.syncope.core.flowable.impl.FlowableRuntimeUtils;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.persistence.api.SyncopeLoader;
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import org.apache.syncope.core.spring.ResourceWithFallbackLoader;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowableLoader implements SyncopeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FlowableLoader.class);

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
        EntitlementsHolder.getInstance().init(FlowableEntitlement.values());

        try (InputStream wfIn = userWorkflowDef.getResource().getInputStream()) {
            byte[] wfDef = IOUtils.toByteArray(wfIn);

            dpEngine.getEngines().forEach((domain, processEngine) -> {
                List<ProcessDefinition> processes = processEngine.getRepositoryService().
                        createProcessDefinitionQuery().processDefinitionKey(FlowableRuntimeUtils.WF_PROCESS_ID).
                        list();
                LOG.debug(FlowableRuntimeUtils.WF_PROCESS_ID + " Flowable processes in repository: {}", processes);

                // Only loads process definition from file if not found in repository
                if (processes.isEmpty()) {
                    processEngine.getRepositoryService().createDeployment().addInputStream(
                            userWorkflowDef.getResource().getFilename(), new ByteArrayInputStream(wfDef)).deploy();

                    ProcessDefinition procDef = processEngine.getRepositoryService().createProcessDefinitionQuery().
                            processDefinitionKey(FlowableRuntimeUtils.WF_PROCESS_ID).latestVersion().
                            singleResult();

                    FlowableDeployUtils.deployModel(processEngine, procDef);

                    LOG.debug("Flowable Workflow definition loaded for domain {}", domain);

                    if (processEngine.getProcessEngineConfiguration().getIdGenerator() instanceof DbIdGenerator) {
                        // jump to the next ID block
                        for (int i = 0; i < processEngine.getProcessEngineConfiguration().getIdBlockSize(); i++) {
                            processEngine.getProcessEngineConfiguration().getIdGenerator().getNextId();
                        }
                    }
                }
            });
        } catch (IOException e) {
            LOG.error("While loading " + userWorkflowDef.getResource().getFilename(), e);
        }
    }
}
