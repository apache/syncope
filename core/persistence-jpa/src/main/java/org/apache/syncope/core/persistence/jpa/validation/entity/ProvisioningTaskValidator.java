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

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SyncMode;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.core.provisioning.api.sync.PushActions;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.provisioning.api.sync.ReconciliationFilterBuilder;

public class ProvisioningTaskValidator extends AbstractValidator<ProvisioningTaskCheck, ProvisioningTask> {

    private final SchedTaskValidator schedV;

    public ProvisioningTaskValidator() {
        super();

        schedV = new SchedTaskValidator();
    }

    @Override
    public boolean isValid(final ProvisioningTask task, final ConstraintValidatorContext context) {
        boolean isValid = schedV.isValid(task, context);

        if (isValid) {
            isValid = task.getResource() != null;
            if (!isValid) {
                LOG.error("Resource is null");

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidProvisioningTask, "Resource cannot be null")).
                        addPropertyNode("resource").addConstraintViolation();
            }

            if (!task.getActionsClassNames().isEmpty()) {
                for (String className : task.getActionsClassNames()) {
                    Class<?> actionsClass = null;
                    boolean isAssignable = false;
                    try {
                        actionsClass = Class.forName(className);
                        isAssignable = task instanceof JPASyncTask
                                ? SyncActions.class.isAssignableFrom(actionsClass)
                                : task instanceof JPAPushTask
                                        ? PushActions.class.isAssignableFrom(actionsClass)
                                        : false;
                    } catch (Exception e) {
                        LOG.error("Invalid {} / {} specified",
                                PushActions.class.getName(), SyncActions.class.getName(), e);
                        isValid = false;
                    }

                    if (actionsClass == null || !isAssignable) {
                        isValid = false;

                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(
                                getTemplate(EntityViolationType.InvalidProvisioningTask, "Invalid class name")).
                                addPropertyNode("actionsClassName").addConstraintViolation();
                    }
                }
            }

            if (isValid && task instanceof SyncTask
                    && ((SyncTask) task).getSyncMode() == SyncMode.FILTERED_RECONCILIATION) {

                Class<?> filterBuilderClass = null;
                boolean isAssignable = false;
                try {
                    filterBuilderClass = Class.forName(((SyncTask) task).getReconciliationFilterBuilderClassName());
                    isAssignable = ReconciliationFilterBuilder.class.isAssignableFrom(filterBuilderClass);
                } catch (Exception e) {
                    LOG.error("Invalid {} specified",
                            ReconciliationFilterBuilder.class.getName(), SyncActions.class.getName(), e);
                    isValid = false;
                }

                if (filterBuilderClass == null || !isAssignable) {
                    isValid = false;

                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidProvisioningTask, "Invalid class name")).
                            addPropertyNode("reconciliationFilterBuilderClassName").addConstraintViolation();
                }
            }
        }

        return isValid;
    }
}
