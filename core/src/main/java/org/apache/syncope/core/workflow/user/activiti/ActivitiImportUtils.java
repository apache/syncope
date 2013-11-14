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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.workflow.WorkflowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivitiImportUtils {

    @Autowired
    private RepositoryService repositoryService;

    public void fromXML(final byte[] definition) {
        try {
            repositoryService.createDeployment().addInputStream(ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE,
                    new ByteArrayInputStream(definition)).deploy();
        } catch (ActivitiException e) {
            throw new WorkflowException("While updating process " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        }
    }

    public void fromJSON(final byte[] definition, final ProcessDefinition procDef, final Model model) {
        try {
            model.setVersion(procDef.getVersion());
            model.setDeploymentId(procDef.getDeploymentId());
            repositoryService.saveModel(model);

            repositoryService.addModelEditorSource(model.getId(), definition);
        } catch (Exception e) {
            throw new WorkflowException("While updating process " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        }
    }

    public void fromJSON(final ProcessDefinition procDef, final Model model) {
        InputStream bpmnStream = null;
        InputStreamReader isr = null;
        XMLStreamReader xtr = null;
        try {
            bpmnStream = repositoryService.getResourceAsStream(
                    procDef.getDeploymentId(), procDef.getResourceName());
            isr = new InputStreamReader(bpmnStream);
            xtr = XMLInputFactory.newInstance().createXMLStreamReader(isr);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

            fromJSON(new BpmnJsonConverter().convertToJson(bpmnModel).toString().getBytes(), procDef, model);
        } catch (Exception e) {
            throw new WorkflowException("While updating process " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        } finally {
            if (xtr != null) {
                try {
                    xtr.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(bpmnStream);
        }
    }
}
