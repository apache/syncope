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
package org.apache.syncope.core.persistence.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.springframework.scheduling.support.CronExpression;

public class SchedTaskValidator extends AbstractValidator<SchedTaskCheck, SchedTask> {

    @Override
    public boolean isValid(final SchedTask task, final ConstraintValidatorContext context) {
        boolean isValid = true;
        if (task.getCronExpression() != null) {
            try {
                CronExpression.parse(task.getCronExpression());
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid cron expression '{}'", task.getCronExpression(), e);
                isValid = false;

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidSchedTask, "Invalid cron expression")).
                        addPropertyNode("cronExpression").addConstraintViolation();
            }
        }

        return isValid;
    }
}
