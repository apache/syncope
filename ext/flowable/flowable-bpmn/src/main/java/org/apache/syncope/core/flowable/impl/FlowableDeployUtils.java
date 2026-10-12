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

import static org.flowable.bpmn.constants.BpmnXMLConstants.ATTRIBUTE_TASK_SERVICE_EXPRESSION;
import static org.flowable.bpmn.constants.BpmnXMLConstants.FLOWABLE_EXTENSIONS_NAMESPACE;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.editor.constants.ModelDataJsonConstants;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;

public final class FlowableDeployUtils {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    static {
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }

    private static boolean containsExpression(final byte[] xml) throws XMLStreamException, IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(xml)) {
            XMLStreamReader xtr = XML_INPUT_FACTORY.createXMLStreamReader(in);
            try {
                while (xtr.hasNext()) {
                    if (xtr.next() == XMLStreamConstants.START_ELEMENT) {
                        for (int i = 0; i < xtr.getAttributeCount(); i++) {
                            if (FLOWABLE_EXTENSIONS_NAMESPACE.equals(xtr.getAttributeNamespace(i))
                                    && ATTRIBUTE_TASK_SERVICE_EXPRESSION.equals(xtr.getAttributeLocalName(i))) {

                                return true;
                            }
                        }
                    }
                }
                return false;
            } finally {
                xtr.close();
            }
        }
    }

    public static Deployment deployDefinition(
            final ProcessEngine engine, final String resourceName, final byte[] definition) {

        try {
            if (containsExpression(definition)) {
                throw new WorkflowException("Attribute flowable:expression is not allowed");
            }
        } catch (IOException | XMLStreamException e) {
            throw new WorkflowException("While checking " + resourceName, e);
        }

        try {
            return engine.getRepositoryService().createDeployment().addBytes(resourceName, definition).deploy();
        } catch (FlowableException e) {
            throw new WorkflowException("While importing " + resourceName, e);
        }
    }

    public static void deployModel(final ProcessEngine engine, final ProcessDefinition procDef) {
        XMLStreamReader xtr = null;
        try (InputStream bpmnStream = engine.getRepositoryService().
                getResourceAsStream(procDef.getDeploymentId(), procDef.getResourceName())) {

            xtr = XML_INPUT_FACTORY.createXMLStreamReader(bpmnStream);
            BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

            Model model = engine.getRepositoryService().newModel();
            ObjectNode modelObjectNode = MAPPER.createObjectNode();
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

    private FlowableDeployUtils() {
        // private constructor for static utility class
    }
}
