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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import org.syncope.types.EntityViolationType;

public class InvalidEntityException extends ValidationException {

    private final Map<Class, Set<EntityViolationType>> violations;

    public InvalidEntityException(
            final Set<ConstraintViolation<Object>> violations) {

        super();

        this.violations = new HashMap<Class, Set<EntityViolationType>>();
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

            if (!this.violations.containsKey(
                    violation.getLeafBean().getClass())) {

                this.violations.put(violation.getLeafBean().getClass(),
                        EnumSet.noneOf(EntityViolationType.class));
            }

            this.violations.get(violation.getLeafBean().getClass()).
                    add(entityViolationType);
        }
    }

    public final boolean hasViolation(final EntityViolationType type) {
        boolean found = false;
        for (Class entity : violations.keySet()) {
            if (violations.get(entity).contains(type)) {
                found = true;
            }
        }

        return found;
    }

    public final Map<Class, Set<EntityViolationType>> getViolations() {
        return violations;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();

        for (Class entity : violations.keySet()) {
            sb.append(entity.getSimpleName()).append(" ").
                    append(violations.get(entity).toString()).
                    append(", ");
        }
        sb.delete(sb.lastIndexOf(", "), sb.length());

        return sb.toString();
    }
}
