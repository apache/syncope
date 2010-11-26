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
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.types.EntityViolationType;

public class AttrValueValidator
        implements ConstraintValidator<AttrValueCheck, AbstractAttrValue> {

    @Override
    public void initialize(final AttrValueCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final AbstractAttrValue object,
            final ConstraintValidatorContext context) {

        boolean isValid = false;

        if (object == null) {
            isValid = true;
        } else {
            int nonNullVales = 0;
            if (object.getBooleanValue() != null) {
                nonNullVales++;
            }
            if (object.getDateValue() != null) {
                nonNullVales++;
            }
            if (object.getDoubleValue() != null) {
                nonNullVales++;
            }
            if (object.getLongValue() != null) {
                nonNullVales++;
            }
            if (object.getStringValue() != null) {
                nonNullVales++;
            }
            isValid = nonNullVales == 1;

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.MoreThanOneNonNull.toString()).
                        addConstraintViolation();
            }
        }

        return isValid;
    }
}
