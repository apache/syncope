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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.ConstraintValidatorContext;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.persistence.common.validation.AbstractValidator;
import org.apache.syncope.core.persistence.common.validation.PlainAttrValidator;
import org.apache.syncope.core.persistence.common.validation.PlainAttrValueValidator;

public class AttributableValidator extends AbstractValidator<AttributableCheck, Neo4jAttributable<?>> {

    private static final PlainAttrValidator ATTR_VALIDATOR = new PlainAttrValidator();

    private static final PlainAttrValueValidator ATTR_VALUE_VALIDATOR = new PlainAttrValueValidator();

    @Override
    public boolean isValid(final Neo4jAttributable<?> entity, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        AtomicReference<Boolean> isValid = new AtomicReference<>(Boolean.TRUE);
        entity.getPlainAttrs().forEach(attr -> {
            isValid.getAndSet(isValid.get() && ATTR_VALIDATOR.isValid(attr, context));
            attr.getValues().forEach(
                    value -> isValid.getAndSet(isValid.get() && ATTR_VALUE_VALIDATOR.isValid(value, context)));
        });

        return isValid.get();
    }
}
