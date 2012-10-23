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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.sync.SyncActions;
import org.apache.syncope.types.EntityViolationType;

public class SyncTaskValidator extends AbstractValidator implements ConstraintValidator<SyncTaskCheck, SyncTask> {

    private final SchedTaskValidator schedV;

    public SyncTaskValidator() {
        super();

        schedV = new SchedTaskValidator();
    }

    @Override
    public void initialize(final SyncTaskCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final SyncTask object, final ConstraintValidatorContext context) {

        boolean isValid;

        if (object == null) {
            isValid = true;
        } else {
            isValid = schedV.isValid(object, context);

            if (isValid) {
                isValid = object.getResource() != null;
                if (!isValid) {
                    LOG.error("Resource is null");

                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(EntityViolationType.InvalidSyncTask.toString())
                            .addNode(object + ".resource is NULL").addConstraintViolation();
                }

                if (StringUtils.isNotBlank(object.getActionsClassName())) {
                    Class<?> syncActionsClass = null;
                    boolean isAssignable = false;
                    try {
                        syncActionsClass = Class.forName(object.getActionsClassName());
                        isAssignable = SyncActions.class.isAssignableFrom(syncActionsClass);
                    } catch (Exception e) {
                        LOG.error("Invalid SyncActions specified", e);
                        isValid = false;
                    }

                    if (syncActionsClass == null || !isAssignable) {
                        isValid = false;

                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(EntityViolationType.InvalidSyncTask.toString())
                                .addNode(object + ".actionsClassName is not valid").addConstraintViolation();
                    }
                }
            }
        }

        return isValid;
    }
}
