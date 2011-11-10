/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.validation.entity;

import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.types.EntityViolationType;
import org.syncope.core.propagation.PropagationTaskExecStatus;

public class PropagationTaskValidator extends AbstractValidator
        implements ConstraintValidator<PropagationTaskCheck, PropagationTask> {

    @Override
    public void initialize(final PropagationTaskCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final PropagationTask object,
            final ConstraintValidatorContext context) {

        boolean isValid;

        if (object == null) {
            isValid = true;
        } else {
            isValid = object.getPropagationMode() != null
                    && object.getResourceOperationType() != null
                    && !object.getAttributes().isEmpty()
                    && object.getResource() != null;

            if (isValid) {
                List<TaskExec> executions = object.getExecs();
                for (TaskExec execution : executions) {
                    try {
                        PropagationTaskExecStatus.valueOf(execution.getStatus());
                    } catch (IllegalArgumentException e) {
                        LOG.error("Invalid execution status '"
                                + execution.getStatus() + "'", e);
                        isValid = false;
                    }
                }
            }

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.InvalidPropagationTask.toString()).
                        addConstraintViolation();
            }
        }

        return isValid;
    }
}
