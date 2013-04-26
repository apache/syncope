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
package org.apache.syncope.core.persistence.validation.entity;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;

public class PropagationTaskValidator extends AbstractValidator implements
        ConstraintValidator<PropagationTaskCheck, PropagationTask> {

    @Override
    public void initialize(final PropagationTaskCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final PropagationTask task, final ConstraintValidatorContext context) {

        boolean isValid;

        if (task == null) {
            isValid = true;
        } else {
            isValid = task.getPropagationMode() != null
                    && task.getPropagationOperation() != null
                    && !task.getAttributes().isEmpty()
                    && task.getResource() != null;

            if (isValid) {
                List<TaskExec> executions = task.getExecs();
                for (TaskExec execution : executions) {
                    try {
                        PropagationTaskExecStatus.valueOf(execution.getStatus());
                    } catch (IllegalArgumentException e) {
                        LOG.error("Invalid execution status '" + execution.getStatus() + "'", e);
                        isValid = false;
                    }
                }
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidPropagationTask, "Invalid task")).
                        addNode(task.getClass().getSimpleName()).addConstraintViolation();
            }
        }

        return isValid;
    }
}
