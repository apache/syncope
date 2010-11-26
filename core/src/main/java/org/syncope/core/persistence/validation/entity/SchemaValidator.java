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
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.types.EntityViolationType;

public class SchemaValidator
        implements ConstraintValidator<SchemaCheck, AbstractSchema> {

    @Override
    public void initialize(final SchemaCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final AbstractSchema object,
            final ConstraintValidatorContext context) {

        boolean isValid = false;

        if (object == null) {
            isValid = true;
        } else {
            isValid = object.isMultivalue()
                    ? !object.isUniqueConstraint() : true;

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.MultivalueAndUniqueConstraint.
                        toString()).addConstraintViolation();
            }
        }

        return isValid;
    }
}
