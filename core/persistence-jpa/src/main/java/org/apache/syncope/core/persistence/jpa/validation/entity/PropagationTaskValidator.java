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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.List;

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;

public class PropagationTaskValidator extends AbstractValidator<PropagationTaskCheck, PropagationTask> {

    @Override
    public boolean isValid(final PropagationTask task, final ConstraintValidatorContext context) {
        boolean isValid;

        if (task == null) {
            isValid = true;
        } else {
            isValid = task.getOperation() != null
                    && !task.getAttributes().isEmpty()
                    && task.getResource() != null;

            if (isValid) {
                List<? extends TaskExec> executions = task.getExecs();
                for (TaskExec execution : executions) {
                    try {
                        ExecStatus.valueOf(execution.getStatus());
                    } catch (IllegalArgumentException e) {
                        LOG.error("Invalid execution status '" + execution.getStatus() + '\'', e);
                        isValid = false;
                    }
                }
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidPropagationTask, "Invalid task")).
                        addPropertyNode(task.getClass().getSimpleName()).addConstraintViolation();
            }
        }

        return isValid;
    }
}
