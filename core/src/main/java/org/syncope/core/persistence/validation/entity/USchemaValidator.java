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

import java.util.Arrays;
import java.util.List;
import javax.persistence.Transient;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.types.EntityViolationType;

public class USchemaValidator extends AbstractValidator
        implements ConstraintValidator<USchemaCheck, Object> {

    @Transient
    private static List<String> PERMITTED_USCHEMA_NAMES =
            Arrays.asList(new String[]{
                "failedLogins",
                "username",
                "password",
                "lastLoginDate",
                "creationDate",
                "changePwdDate"});

    @Override
    public void initialize(final USchemaCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final Object object,
            final ConstraintValidatorContext context) {

        EntityViolationType violation = null;

        try {
            if (object != null) {
                final String schemaName;

                if (object instanceof USchema) {
                    schemaName = ((USchema) object).getName();
                    violation = EntityViolationType.InvalidUSchema;
                } else if (object instanceof UDerSchema) {
                    schemaName = ((UDerSchema) object).getName();
                    violation = EntityViolationType.InvalidUDerSchema;
                } else if (object instanceof UVirSchema) {
                    schemaName = ((UVirSchema) object).getName();
                    violation = EntityViolationType.InvalidUVirSchema;
                } else {
                    schemaName = null;
                }

                if (PERMITTED_USCHEMA_NAMES.contains(schemaName)) {
                    throw new Exception("Schema name not permitted");
                }
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error saving schema", e);

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    violation.toString()).
                    addNode(object.toString()).
                    addConstraintViolation();

            return false;
        }
    }
}
