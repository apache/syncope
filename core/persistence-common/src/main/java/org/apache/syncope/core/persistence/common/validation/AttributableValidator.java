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
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class AttributableValidator extends AbstractValidator<AttributableCheck, Attributable> {

    protected boolean isValid(
            final PlainAttr attr,
            final PlainSchema schema,
            final ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        boolean isValid;
        if (attr == null) {
            isValid = true;
        } else {
            if (schema.isUniqueConstraint()) {
                isValid = attr.getValues().isEmpty() && attr.getUniqueValue() != null;
            } else {
                isValid = !attr.getValues().isEmpty() && attr.getUniqueValue() == null;

                if (!schema.isMultivalue()) {
                    isValid &= attr.getValues().size() == 1;
                }
            }

            if (!isValid) {
                LOG.error("Invalid values for attribute schema={}, values={}",
                        attr.getSchema(), attr.getValuesAsStrings());

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidValueList,
                                "Invalid values " + attr.getValuesAsStrings())).
                        addPropertyNode(attr.getSchema()).addConstraintViolation();
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
                LOG.error("More than one non-null value for {}", value);

                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.MoreThanOneNonNull, "More than one non-null value found")).
                        addPropertyNode(value.getClass().getSimpleName().replaceAll("\\n", " ")).
                        addConstraintViolation();
            }
        }

        return isValid;
    }

    @Override
    public boolean isValid(final Attributable entity, final ConstraintValidatorContext context) {
        PlainSchemaDAO schemaDAO = ApplicationContextProvider.getApplicationContext().getBean(PlainSchemaDAO.class);

        context.disableDefaultConstraintViolation();

        Mutable<Boolean> isValid = new MutableObject<>(true);
        entity.getPlainAttrs().forEach(attr -> schemaDAO.findById(attr.getSchema()).ifPresentOrElse(
                schema -> {
                    isValid.setValue(isValid.getValue() && isValid(attr, schema, context));
                    attr.getValues().forEach(value -> isValid.setValue(isValid.getValue() && isValid(value, context)));
                }, () -> {
                    isValid.setValue(false);
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchema, "Invalid schema " + attr.getSchema())).
                            addPropertyNode(attr.getSchema()).addConstraintViolation();
                }));

        return isValid.getValue();
    }
}
