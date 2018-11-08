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

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class PlainAttrValueValidator extends AbstractValidator<PlainAttrValueCheck, PlainAttrValue> {

    @Override
    public boolean isValid(final PlainAttrValue value, final ConstraintValidatorContext context) {
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
                        getTemplate(EntityViolationType.MoreThanOneNonNull, "More than one non-null value found")).
                        addPropertyNode(value.getClass().getSimpleName().replaceAll("\\n", " ")).
                        addConstraintViolation();

            } else if (value instanceof PlainAttrUniqueValue) {
                PlainSchema uniqueValueSchema = ((PlainAttrUniqueValue) value).getSchema();
                PlainSchema attrSchema = value.getAttr().getSchema();

                isValid = uniqueValueSchema.equals(attrSchema);

                if (!isValid) {
                    LOG.error("Unique value schema for " + value + " is " + uniqueValueSchema
                            + ", while owning attribute's schema is " + attrSchema);

                    context.buildConstraintViolationWithTemplate(getTemplate(EntityViolationType.InvalidPlainAttr,
                            "Unique value schema is " + uniqueValueSchema
                            + ", while owning attribute's schema is " + attrSchema)).addPropertyNode("schema").
                            addConstraintViolation();
                }
            }
        }

        return isValid;
    }
}
