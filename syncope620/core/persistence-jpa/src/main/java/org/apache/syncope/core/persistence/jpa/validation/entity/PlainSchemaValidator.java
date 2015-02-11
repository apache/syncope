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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class PlainSchemaValidator extends AbstractValidator<PlainSchemaCheck, PlainSchema> {

    @Override
    public boolean isValid(final PlainSchema schema, final ConstraintValidatorContext context) {
        boolean isValid = schema.getType() != AttrSchemaType.Enum
                || StringUtils.isNotBlank(schema.getEnumerationValues());
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidSchemaEnum, "Enumeration values missing")).
                    addPropertyNode("enumerationValues").addConstraintViolation();
        } else {
            isValid = schema.getType() != AttrSchemaType.Encrypted
                    || (schema.getSecretKey() != null && schema.getCipherAlgorithm() != null);
            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidSchemaEncrypted,
                                "SecretKey or CipherAlgorithm missing")).
                        addPropertyNode("secretKey").addPropertyNode("cipherAlgorithm").addConstraintViolation();
            } else {
                isValid = !schema.isMultivalue() || !schema.isUniqueConstraint();
                if (!isValid) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchemaMultivalueUnique,
                                    "Cannot contemporary be multivalue and have unique constraint")).
                            addPropertyNode("multiValue").addConstraintViolation();
                }
            }
        }

        return isValid;
    }
}
