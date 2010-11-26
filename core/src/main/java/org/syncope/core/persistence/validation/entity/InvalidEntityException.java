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

import java.util.EnumSet;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import org.syncope.types.EntityViolationType;

public class InvalidEntityException extends ValidationException {

    final private Class entityClass;

    final private Set<EntityViolationType> violations;

    public InvalidEntityException(final Class entityClass,
            final Set<ConstraintViolation<Object>> violations) {

        super();

        this.entityClass = entityClass;

        this.violations = EnumSet.noneOf(EntityViolationType.class);
        EntityViolationType entityViolationType;
        for (ConstraintViolation<Object> violation : violations) {
            try {
                entityViolationType = EntityViolationType.valueOf(
                        violation.getMessageTemplate());
            } catch (IllegalArgumentException e) {
                entityViolationType = EntityViolationType.Standard;
                entityViolationType.setMessageTemplate(
                        violation.getPropertyPath() + ": "
                        + violation.getMessage());
            }

            this.violations.add(entityViolationType);
        }
    }

    @Override
    public String getMessage() {
        return entityClass.getSimpleName() + " " + violations.toString();
    }
}
