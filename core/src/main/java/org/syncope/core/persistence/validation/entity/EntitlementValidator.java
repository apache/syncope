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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.types.EntityViolationType;

public class EntitlementValidator extends AbstractValidator
        implements ConstraintValidator<EntitlementCheck, Entitlement> {

    private static final Pattern ROLE_ENTITLEMENT_NAME_PATTERN =
            Pattern.compile("^ROLE_([\\d])+");

    @Override
    public void initialize(final EntitlementCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Entitlement object,
            final ConstraintValidatorContext context) {

        boolean isValid = false;

        if (object == null) {
            isValid = true;
        } else {
            if (object.getName() == null) {
                isValid = false;
            } else {
                Matcher matcher = ROLE_ENTITLEMENT_NAME_PATTERN.matcher(
                        object.getName());
                isValid = !matcher.matches();
            }

            if (!isValid) {
                LOG.error(object + " cannot have name "
                        + "starting by ROLE_");

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        EntityViolationType.InvalidEntitlementName.toString()).
                        addNode(object.toString()).
                        addConstraintViolation();
            }
        }

        return isValid;
    }
}
