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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.concurrent.atomic.AtomicReference;
import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.JSONAttributable;

public class JPAJSONAttributableValidator extends AbstractValidator<JPAJSONAttributableCheck, JSONAttributable<?>> {

    @Override
    public boolean isValid(final JSONAttributable<?> entity, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        PlainAttrValidator attrValidator = new PlainAttrValidator();
        PlainAttrValueValidator attrValueValidator = new PlainAttrValueValidator();

        AtomicReference<Boolean> isValid = new AtomicReference<>(Boolean.TRUE);
        entity.getPlainAttrList().forEach(attr -> {
            PlainAttr<?> plainAttr = (PlainAttr<?>) attr;
            isValid.getAndSet(isValid.get() && attrValidator.isValid(plainAttr, context));
            plainAttr.getValues().forEach(
                    value -> isValid.getAndSet(isValid.get() && attrValueValidator.isValid(value, context)));
        });

        return isValid.get();
    }
}
