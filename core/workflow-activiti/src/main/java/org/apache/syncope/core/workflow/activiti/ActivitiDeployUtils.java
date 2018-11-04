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
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.syncope.core.workflow.api.WorkflowException;

public final class ActivitiDeployUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    public static Deployment deployDefinition(
            final ProcessEngine engine, final String resourceName, final byte[] definition) {

        try {
            return engine.getRepositoryService().createDeployment().
                    addInputStream(resourceName, new ByteArrayInputStream(definition)).deploy();
        } catch (ActivitiException e) {
            throw new WorkflowException("While importing " + resourceName, e);
        }
    }

    public static void deployModel(final ProcessEngine engine, final ProcessDefinition procDef) {
        XMLStreamReader xtr = null;
        try (InputStream bpmnStream = engine.getRepositoryService().
                getResourceAsStream(procDef.getDeploymentId(), procDef.getResourceName());
                InputStreamReader isr = new InputStreamReader(bpmnStream)) {

            xtr = XML_INPUT_FACTORY.createXMLStreamReader(isr);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

            Model model = engine.getRepositoryService().newModel();
            ObjectNode modelObjectNode = OBJECT_MAPPER.createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, procDef.getName());
            model.setMetaInfo(modelObjectNode.toString());
            model.setName(procDef.getName());
            model.setDeploymentId(procDef.getDeploymentId());
            model.setVersion(procDef.getVersion());

            engine.getRepositoryService().saveModel(model);
            engine.getRepositoryService().addModelEditorSource(
                    model.getId(),
                    new BpmnJsonConverter().convertToJson(bpmnModel).toString().getBytes());
        } catch (Exception e) {
            throw new WorkflowException("While importing " + procDef.getResourceName(), e);
        } finally {
            if (xtr != null) {
                try {
                    xtr.close();
                } catch (XMLStreamException e) {
                    // ignore
                }
            }
        }
    }

    private ActivitiDeployUtils() {
        // private constructor for static utility class
    }
}
