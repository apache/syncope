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
package org.apache.syncope.core.persistence.api.attrvalue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.EntityViolationType;

/**
 * Exception thrown when any JPA entity fails bean validation.
 */
public class InvalidEntityException extends ValidationException {

    private static final long serialVersionUID = 3249297275444409691L;

    private final String entityClassSimpleName;

    private final Map<Class<?>, Set<EntityViolationType>> violations = new HashMap<>();

    /**
     * Constructs a singleton map of violations from given parameters.
     *
     * @param entityClass class of invalid entity
     * @param entityViolationType type of violation found
     * @param message message to be associated to the violation
     */
    public InvalidEntityException(
            final Class<?> entityClass,
            final EntityViolationType entityViolationType,
            final String message) {

        super();

        this.entityClassSimpleName = entityClass.getSimpleName();

        entityViolationType.setMessage(Optional.ofNullable(message).map(String::trim).orElse(StringUtils.EMPTY));

        this.violations.put(entityClass, EnumSet.noneOf(EntityViolationType.class));
        this.violations.get(entityClass).add(entityViolationType);
    }

    /**
     * Constructs a map of violations out of given {@code ConstraintViolation} set.
     *
     * @param entityClassSimpleName simple class name of invalid entity
     * @param violations as returned by bean validation
     */
    public InvalidEntityException(
            final String entityClassSimpleName,
            final Set<ConstraintViolation<Object>> violations) {

        super();

        this.entityClassSimpleName = entityClassSimpleName;

        violations.forEach(violation -> {
            String key = StringUtils.substringBefore(violation.getMessageTemplate(), ";").trim();
            String message = StringUtils.substringAfter(violation.getMessageTemplate(), ";").trim();

            EntityViolationType entityViolationType;
            try {
                entityViolationType = EntityViolationType.valueOf(key);
            } catch (IllegalArgumentException e) {
                entityViolationType = EntityViolationType.Standard;
            }
            entityViolationType.setMessage(message);
            entityViolationType.setPropertyPath(violation.getPropertyPath().toString());
            entityViolationType.setInvalidValue(violation.getInvalidValue());

            if (!this.violations.containsKey(violation.getLeafBean().getClass())) {
                this.violations.put(violation.getLeafBean().getClass(), EnumSet.noneOf(EntityViolationType.class));
            }
            this.violations.get(violation.getLeafBean().getClass()).add(entityViolationType);
        });
    }

    public final boolean hasViolation(final EntityViolationType type) {
        return violations.keySet().stream().anyMatch(entity -> violations.get(entity).contains(type));
    }

    public String getEntityClassSimpleName() {
        return entityClassSimpleName;
    }

    public final Map<Class<?>, Set<EntityViolationType>> getViolations() {
        return violations;
    }

    @Override
    public String getMessage() {
        return violations.entrySet().stream().
                map(entry -> entry.getKey().getSimpleName() + " " + entry.getValue().toString()).
                collect(Collectors.joining(","));
    }
}
