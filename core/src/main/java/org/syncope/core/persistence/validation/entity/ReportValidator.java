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

import java.text.ParseException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.quartz.CronExpression;
import org.syncope.core.persistence.beans.Report;
import org.syncope.types.EntityViolationType;

public class ReportValidator extends AbstractValidator
        implements ConstraintValidator<ReportCheck, Report> {

    @Override
    public void initialize(final ReportCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Report object,
            final ConstraintValidatorContext context) {

        boolean isValid = true;

        if (object.getCronExpression() != null) {
            try {
                new CronExpression(object.getCronExpression());
            } catch (ParseException e) {
                LOG.error("Invalid cron expression '"
                        + object.getCronExpression() + "'", e);
                isValid = false;

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.InvalidReport.name()).
                        addNode(object + ".cronExpression=="
                        + object.getCronExpression()).addConstraintViolation();
            }
        }

        return isValid;
    }
}
