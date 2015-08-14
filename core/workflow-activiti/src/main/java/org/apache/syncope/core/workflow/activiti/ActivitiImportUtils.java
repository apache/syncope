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
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.workflow.api.WorkflowException;

public final class ActivitiImportUtils {

    public static void fromXML(final ProcessEngine engine, final byte[] definition) {
        try {
            engine.getRepositoryService().createDeployment().
                    addInputStream(ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE,
                            new ByteArrayInputStream(definition)).deploy();
        } catch (ActivitiException e) {
            throw new WorkflowException("While updating process " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        }
    }

    public static void fromJSON(
            final ProcessEngine engine, final byte[] definition, final ProcessDefinition procDef, final Model model) {

        try {
            model.setVersion(procDef.getVersion());
            model.setDeploymentId(procDef.getDeploymentId());
            engine.getRepositoryService().saveModel(model);

            engine.getRepositoryService().addModelEditorSource(model.getId(), definition);
        } catch (Exception e) {
            throw new WorkflowException("While updating process " + ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE, e);
        }
    }

    public static void fromJSON(final ProcessEngine engine, final ProcessDefinition procDef, final Model model) {
        InputStream bpmnStream = null;
        InputStreamReader isr = null;
        XMLStreamReader xtr = null;
        try {
            bpmnStream = engine.getRepositoryService().getResourceAsStream(
                    procDef.getDeploymentId(), procDef.getResourceName());
            isr = new InputStreamReader(bpmnStream);
            xtr = XMLInputFactory.newInstance().createXMLStreamReader(isr);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

            fromJSON(engine, new BpmnJsonConverter().convertToJson(bpmnModel).toString().getBytes(), procDef, model);
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

    private ActivitiImportUtils() {
        // private constructor for static utility class
    }
}
