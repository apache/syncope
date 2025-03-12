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
package org.apache.syncope.core.persistence.neo4j.spring;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DynMembership;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.ProvidedKeyEntity;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeValidator {

    protected static final Logger LOG = LoggerFactory.getLogger(NodeValidator.class);

    protected final Validator validator;

    public NodeValidator(final Validator validator) {
        this.validator = validator;
    }

    public <T> T validate(final T node) {
        Set<ConstraintViolation<Object>> violations = validator.validate(node);
        if (!violations.isEmpty()) {
            LOG.warn("Bean validation errors found: {}", violations);

            Class<?> entityInt = null;
            for (Class<?> interf : ClassUtils.getAllInterfaces(node.getClass())) {
                if (!Entity.class.equals(interf)
                        && !ProvidedKeyEntity.class.equals(interf)
                        && !Schema.class.equals(interf)
                        && !Task.class.equals(interf)
                        && !Policy.class.equals(interf)
                        && !Groupable.class.equals(interf)
                        && !Any.class.equals(interf)
                        && !DynMembership.class.equals(interf)
                        && Entity.class.isAssignableFrom(interf)) {

                    entityInt = interf;
                }
            }

            throw new InvalidEntityException(entityInt == null ? "Entity" : entityInt.getSimpleName(), violations);
        }

        return node;
    }
}
