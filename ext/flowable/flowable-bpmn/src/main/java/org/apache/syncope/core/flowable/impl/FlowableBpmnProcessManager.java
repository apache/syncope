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
package org.apache.syncope.core.flowable.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.apache.syncope.core.flowable.support.DomainProcessEngine;
import org.apache.syncope.core.flowable.support.DropdownAwareJsonConverter;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.editor.constants.ModelDataJsonConstants;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = { Throwable.class })
public class FlowableBpmnProcessManager implements BpmnProcessManager {

    protected static final Logger LOG = LoggerFactory.getLogger(BpmnProcessManager.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String MODEL_DATA_JSON_MODEL = "model";

    protected final DomainProcessEngine engine;

    public FlowableBpmnProcessManager(final DomainProcessEngine engine) {
        this.engine = engine;
    }

    protected Model getModel(final ProcessDefinition procDef) {
        try {
            Model model = engine.getRepositoryService().createModelQuery().
                    deploymentId(procDef.getDeploymentId()).singleResult();
            if (model == null) {
                throw new NotFoundException("Could not find Model for deployment " + procDef.getDeploymentId());
            }
            return model;
        } catch (Exception e) {
            throw new WorkflowException("While accessing process " + procDef.getKey(), e);
        }
    }

    @Override
    public List<BpmnProcess> getProcesses() {
        try {
            return engine.getRepositoryService().createProcessDefinitionQuery().latestVersion().list().stream().
                    map(procDef -> {
                        BpmnProcess defTO = new BpmnProcess();
                        defTO.setKey(procDef.getKey());
                        defTO.setName(procDef.getName());

                        try {
                            defTO.setModelId(getModel(procDef).getId());
                        } catch (NotFoundException e) {
                            LOG.warn("No model found for definition {}, ignoring", procDef.getDeploymentId(), e);
                        }

                        defTO.setUserWorkflow(FlowableRuntimeUtils.WF_PROCESS_ID.equals(procDef.getKey()));

                        return defTO;
                    }).toList();
        } catch (FlowableException e) {
            throw new WorkflowException("While listing available process definitions", e);
        }
    }

    protected void exportProcessModel(final String key, final OutputStream os) {
        Model model = getModel(FlowableRuntimeUtils.getLatestProcDefByKey(engine, key));

        try {
            ObjectNode modelNode = (ObjectNode) MAPPER.readTree(model.getMetaInfo());
            modelNode.put(ModelDataJsonConstants.MODEL_ID, model.getId());
            modelNode.replace(MODEL_DATA_JSON_MODEL,
                    MAPPER.readTree(engine.getRepositoryService().getModelEditorSource(model.getId())));

            os.write(modelNode.toString().getBytes());
        } catch (IOException e) {
            LOG.error("While exporting workflow definition {}", model.getId(), e);
        }
    }

    protected void exportProcessResource(final String deploymentId, final String resourceName, final OutputStream os) {
        try (InputStream procDefIS = engine.getRepositoryService().getResourceAsStream(deploymentId, resourceName)) {
            IOUtils.copy(procDefIS, os);
        } catch (IOException e) {
            LOG.error("While exporting {}", resourceName, e);
        }
    }

    @Override
    public void exportProcess(final String key, final BpmnProcessFormat format, final OutputStream os) {
        switch (format) {
            case JSON:
                exportProcessModel(key, os);
                break;

            case XML:
            default:
                ProcessDefinition procDef = FlowableRuntimeUtils.getLatestProcDefByKey(engine, key);
                if (procDef == null) {
                    throw new NotFoundException("Process Definition " + key);
                }
                exportProcessResource(procDef.getDeploymentId(), procDef.getResourceName(), os);
        }
    }

    @Override
    public void exportDiagram(final String key, final OutputStream os) {
        ProcessDefinition procDef = FlowableRuntimeUtils.getLatestProcDefByKey(engine, key);
        if (procDef == null) {
            throw new NotFoundException("Workflow process definition for " + key);
        }
        exportProcessResource(procDef.getDeploymentId(), procDef.getDiagramResourceName(), os);
    }

    @Override
    public void importProcess(final String key, final BpmnProcessFormat format, final String definition) {
        ProcessDefinition procDef = FlowableRuntimeUtils.getLatestProcDefByKey(engine, key);
        String resourceName = procDef == null ? key + ".bpmn20.xml" : procDef.getResourceName();
        Deployment deployment;
        switch (format) {
            case JSON:
                JsonNode definitionNode;
                try {
                    definitionNode = MAPPER.readTree(definition);
                    if (definitionNode.has(MODEL_DATA_JSON_MODEL)) {
                        definitionNode = definitionNode.get(MODEL_DATA_JSON_MODEL);
                    }
                    if (!definitionNode.has(BpmnJsonConverter.EDITOR_CHILD_SHAPES)) {
                        throw new IllegalArgumentException(
                                "Could not find JSON node " + BpmnJsonConverter.EDITOR_CHILD_SHAPES);
                    }

                    BpmnModel bpmnModel = new DropdownAwareJsonConverter().convertToBpmnModel(definitionNode);
                    deployment = FlowableDeployUtils.deployDefinition(
                            engine,
                            resourceName,
                            new BpmnXMLConverter().convertToXML(bpmnModel));
                } catch (Exception e) {
                    throw new WorkflowException("While creating or updating process " + key, e);
                }
                break;

            case XML:
            default:
                deployment = FlowableDeployUtils.deployDefinition(
                        engine,
                        resourceName,
                        definition.getBytes());
        }

        try {
            procDef = engine.getRepositoryService().createProcessDefinitionQuery().
                    deploymentId(deployment.getId()).latestVersion().singleResult();
        } catch (FlowableException e) {
            throw new WorkflowException("While accessing deployment " + deployment.getId(), e);
        }
        if (!key.equals(procDef.getKey())) {
            throw new WorkflowException("Mismatching key: expected " + key + ", found " + procDef.getKey());
        }
        FlowableDeployUtils.deployModel(engine, procDef);
    }

    @Override
    public void deleteProcess(final String key) {
        if (FlowableRuntimeUtils.WF_PROCESS_ID.equals(key)) {
            throw new WorkflowException("Cannot delete the main process " + FlowableRuntimeUtils.WF_PROCESS_ID);
        }

        try {
            engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(key).list().
                    forEach(procDef -> {
                        engine.getRepositoryService().deleteModel(getModel(procDef).getId());
                        engine.getRepositoryService().deleteDeployment(procDef.getDeploymentId(), true);
                    });
        } catch (Exception e) {
            throw new WorkflowException("While deleting process " + key, e);
        }
    }
}
