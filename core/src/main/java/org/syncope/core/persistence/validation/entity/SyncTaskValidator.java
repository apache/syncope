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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.SyncTask;
import org.syncope.types.EntityViolationType;

public class SyncTaskValidator extends AbstractValidator
        implements ConstraintValidator<SyncTaskCheck, SyncTask> {

    private final SchedTaskValidator schedV;

    public SyncTaskValidator() {
        super();

        schedV = new SchedTaskValidator();
    }

    @Override
    public void initialize(final SyncTaskCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final SyncTask object,
            final ConstraintValidatorContext context) {

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
                    context.buildConstraintViolationWithTemplate(
                            EntityViolationType.InvalidSyncTask.toString()).
                            addNode(object + ".resource is NULL").
                            addConstraintViolation();
                }
            }
        }

        return isValid;
    }
}
