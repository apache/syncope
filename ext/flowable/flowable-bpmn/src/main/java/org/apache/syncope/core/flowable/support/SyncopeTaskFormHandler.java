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

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.flowable.bpmn.model.FormProperty;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.form.AbstractFormType;
import org.flowable.engine.impl.form.DefaultTaskFormHandler;
import org.flowable.engine.impl.form.FormPropertyHandler;
import org.flowable.engine.impl.form.FormTypes;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link DefaultTaskFormHandler} with purpose of supporting more form types than Flowable's default.
 */
public class SyncopeTaskFormHandler extends DefaultTaskFormHandler {

    private static final long serialVersionUID = -5271243544388455797L;

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeTaskFormHandler.class);

    protected static Optional<AbstractFormType> parseFormPropertyType(
        final FormProperty formProperty, final ExpressionManager expressionManager) {

        AbstractFormType formType = null;

        switch (formProperty.getType()) {
            case "dropdown":
                if (formProperty.getFormValues().isEmpty()
                        || !DropdownValueProvider.NAME.equals(formProperty.getFormValues().getFirst().getId())) {

                    LOG.warn("A single value with id '" + DropdownValueProvider.NAME + "' was expected, ignoring");
                } else {
                    formType = new DropdownFormType(formProperty.getFormValues().getFirst().getName());
                }
                break;

            case "password":
                formType = new PasswordFormType();
                break;

            default:
        }

        return Optional.ofNullable(formType);
    }

    @Override
    public void parseConfiguration(
            final List<FormProperty> formProperties,
            final String formKey,
            final DeploymentEntity deployment,
            final ProcessDefinition processDefinition) {

        this.deploymentId = deployment.getId();

        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();

        if (StringUtils.isNotEmpty(formKey)) {
            this.formKey = expressionManager.createExpression(formKey);
        }

        FormTypes formTypes = CommandContextUtil.getProcessEngineConfiguration().getFormTypes();

        formProperties.forEach(formProperty -> {
            FormPropertyHandler formPropertyHandler = new FormPropertyHandler();
            formPropertyHandler.setId(formProperty.getId());
            formPropertyHandler.setName(formProperty.getName());

            AbstractFormType type = parseFormPropertyType(formProperty, expressionManager).
                orElseGet(() -> formTypes.parseFormPropertyType(formProperty));
            formPropertyHandler.setType(type);
            formPropertyHandler.setRequired(formProperty.isRequired());
            formPropertyHandler.setReadable(formProperty.isReadable());
            formPropertyHandler.setWritable(formProperty.isWriteable());
            formPropertyHandler.setVariableName(formProperty.getVariable());

            if (StringUtils.isNotEmpty(formProperty.getExpression())) {
                Expression expression = expressionManager.createExpression(formProperty.getExpression());
                formPropertyHandler.setVariableExpression(expression);
            }

            if (StringUtils.isNotEmpty(formProperty.getDefaultExpression())) {
                Expression defaultExpression = expressionManager.createExpression(formProperty.getDefaultExpression());
                formPropertyHandler.setDefaultExpression(defaultExpression);
            }

            formPropertyHandlers.add(formPropertyHandler);
        });
    }
}
