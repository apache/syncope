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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.FormValue;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.editor.language.json.converter.BpmnJsonConverterUtil;
import org.flowable.editor.language.json.converter.UserTaskJsonConverter;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;

public class DropdownAwareUserTaskJsonConverter extends UserTaskJsonConverter {

    @Override
    protected void convertJsonToFormProperties(final JsonNode objectNode, final BaseElement element) {
        JsonNode formPropertiesNode = JsonConverterUtil.getProperty(PROPERTY_FORM_PROPERTIES, objectNode);
        if (formPropertiesNode != null) {
            formPropertiesNode = BpmnJsonConverterUtil.validateIfNodeIsTextual(formPropertiesNode);
            JsonNode propertiesArray = formPropertiesNode.get("formProperties");
            if (propertiesArray != null) {
                for (JsonNode formNode : propertiesArray) {
                    JsonNode formIdNode = formNode.get(PROPERTY_FORM_ID);
                    if (formIdNode != null && StringUtils.isNotEmpty(formIdNode.asText())) {

                        FormProperty formProperty = new FormProperty();
                        formProperty.setId(formIdNode.asText());
                        formProperty.setName(getValueAsString(PROPERTY_FORM_NAME, formNode));
                        formProperty.setType(getValueAsString(PROPERTY_FORM_TYPE, formNode));
                        formProperty.setExpression(getValueAsString(PROPERTY_FORM_EXPRESSION, formNode));
                        formProperty.setVariable(getValueAsString(PROPERTY_FORM_VARIABLE, formNode));

                        if ("date".equalsIgnoreCase(formProperty.getType())) {
                            formProperty.setDatePattern(getValueAsString(PROPERTY_FORM_DATE_PATTERN, formNode));

                        } else if ("enum".equalsIgnoreCase(formProperty.getType())
                                || "dropdown".equalsIgnoreCase(formProperty.getType())) {

                            JsonNode enumValuesNode = formNode.get(PROPERTY_FORM_ENUM_VALUES);
                            if (enumValuesNode != null) {
                                List<FormValue> formValueList = new ArrayList<>();
                                for (JsonNode enumNode : enumValuesNode) {
                                    if (enumNode.get(PROPERTY_FORM_ENUM_VALUES_ID) != null && !enumNode.get(
                                            PROPERTY_FORM_ENUM_VALUES_ID).isNull() && enumNode.get(
                                                    PROPERTY_FORM_ENUM_VALUES_NAME) != null
                                            && !enumNode.get(PROPERTY_FORM_ENUM_VALUES_NAME).isNull()) {

                                        FormValue formValue = new FormValue();
                                        formValue.setId(enumNode.get(PROPERTY_FORM_ENUM_VALUES_ID).asText());
                                        formValue.setName(enumNode.get(PROPERTY_FORM_ENUM_VALUES_NAME).asText());
                                        formValueList.add(formValue);

                                    } else if (enumNode.get("value") != null && !enumNode.get("value").isNull()) {
                                        FormValue formValue = new FormValue();
                                        formValue.setId(enumNode.get("value").asText());
                                        formValue.setName(enumNode.get("value").asText());
                                        formValueList.add(formValue);
                                    }
                                }
                                formProperty.setFormValues(formValueList);
                            }
                        }

                        formProperty.setRequired(getValueAsBoolean(PROPERTY_FORM_REQUIRED, formNode));
                        formProperty.setReadable(getValueAsBoolean(PROPERTY_FORM_READABLE, formNode));
                        formProperty.setWriteable(getValueAsBoolean(PROPERTY_FORM_WRITABLE, formNode));

                        if (element instanceof StartEvent startEvent) {
                            startEvent.getFormProperties().add(formProperty);
                        } else if (element instanceof UserTask userTask) {
                            userTask.getFormProperties().add(formProperty);
                        }
                    }
                }
            }
        }
    }
}
