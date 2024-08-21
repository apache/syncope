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
package org.apache.syncope.core.persistence.common.validation;

import jakarta.validation.ConstraintValidatorContext;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class AttributableValidator extends AbstractValidator<AttributableCheck, Attributable<?>> {

    protected boolean isValid(final PlainAttr<?> attr, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid;
        if (attr == null) {
            isValid = true;
        } else {
            if (attr.getSchema().isUniqueConstraint()) {
                isValid = attr.getValues().isEmpty() && attr.getUniqueValue() != null;
            } else {
                isValid = !attr.getValues().isEmpty() && attr.getUniqueValue() == null;

                if (!attr.getSchema().isMultivalue()) {
                    isValid &= attr.getValues().size() == 1;
                }
            }

            if (!isValid) {
                LOG.error("Invalid values for attribute schema={}, values={}",
                        attr.getSchema().getKey(), attr.getValuesAsStrings());

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidValueList,
                                "Invalid values " + attr.getValuesAsStrings())).
                        addPropertyNode(attr.getSchema().getKey()).addConstraintViolation();
            }
        }

        return isValid;
    }

    protected boolean isValid(final PlainAttrValue value, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid;
        if (value == null) {
            isValid = true;
        } else {
            int nonNullVales = 0;
            if (value.getBooleanValue() != null) {
                nonNullVales++;
            }
            if (value.getDateValue() != null) {
                nonNullVales++;
            }
            if (value.getDoubleValue() != null) {
                nonNullVales++;
            }
            if (value.getLongValue() != null) {
                nonNullVales++;
            }
            if (value.getBinaryValue() != null) {
                nonNullVales++;
            }
            if (value.getStringValue() != null) {
                nonNullVales++;
            }
            isValid = nonNullVales == 1;

            if (!isValid) {
                LOG.error("More than one non-null value for " + value);

                context.buildConstraintViolationWithTemplate(
                        AbstractValidator.getTemplate(EntityViolationType.MoreThanOneNonNull,
                                "More than one non-null value found")).
                        addPropertyNode(value.getClass().getSimpleName().replaceAll("\\n", " ")).
                        addConstraintViolation();

            } else if (value instanceof PlainAttrUniqueValue plainAttrUniqueValue) {
                PlainSchema uniqueValueSchema = plainAttrUniqueValue.getSchema();
                PlainSchema attrSchema = value.getAttr().getSchema();

                isValid = uniqueValueSchema.equals(attrSchema);

                if (!isValid) {
                    LOG.error("Unique value schema for " + value + " is " + uniqueValueSchema
                            + ", while owning attribute's schema is " + attrSchema);

                    context.buildConstraintViolationWithTemplate(
                            AbstractValidator.getTemplate(EntityViolationType.InvalidPlainAttr,
                                    "Unique value schema is " + uniqueValueSchema
                                    + ", while owning attribute's schema is " + attrSchema)).
                            addPropertyNode("schema").
                            addConstraintViolation();
                }
            }
        }

        return isValid;
    }

    @Override
    public boolean isValid(final Attributable<?> entity, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        AtomicReference<Boolean> isValid = new AtomicReference<>(Boolean.TRUE);
        entity.getPlainAttrs().forEach(attr -> {
            isValid.getAndSet(isValid.get() && isValid(attr, context));
            attr.getValues().forEach(value -> isValid.getAndSet(isValid.get() && isValid(value, context)));
        });

        return isValid.get();
    }
}
