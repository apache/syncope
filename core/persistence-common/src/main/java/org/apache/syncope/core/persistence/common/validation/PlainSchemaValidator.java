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

import static org.apache.syncope.core.persistence.api.entity.PlainAttrValue.SPRING_ENV_PROPERTY;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class PlainSchemaValidator extends AbstractValidator<PlainSchemaCheck, PlainSchema> {

    @Override
    public boolean isValid(final PlainSchema schema, final ConstraintValidatorContext context) {
        switch (schema.getType()) {
            case Enum -> {
                if (schema.getEnumValues().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchema, "Enumeration values missing")).
                            addPropertyNode("enumValues").addConstraintViolation();
                    return false;
                }
            }

            case Dropdown -> {
                if (schema.getDropdownValueProvider() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchema, "DropdownValueProvider missing")).
                            addPropertyNode("dropdownValueProvider").addConstraintViolation();
                    return false;
                }
            }

            case Encrypted -> {
                if (schema.getSecretKey() == null || schema.getCipherAlgorithm() == null) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchema, "SecretKey or CipherAlgorithm missing")).
                            addPropertyNode("secretKey").addPropertyNode("cipherAlgorithm").addConstraintViolation();
                    return false;
                }
                if (schema.getCipherAlgorithm() == CipherAlgorithm.AES
                        && !SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN.equals(schema.getConversionPattern())
                        && !SPRING_ENV_PROPERTY.matcher(schema.getSecretKey()).matches()
                        && Optional.ofNullable(schema.getSecretKey()).
                                map(key -> key.length() != 16 && key.length() != 24 && key.length() != 32).
                                orElse(true)) {

                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidSchema,
                                    "SecretKey length requirements not met for AES: must be 16 24 or 32")).
                            addPropertyNode("secretKey").addConstraintViolation();
                    return false;
                }
            }

            default -> {
            }
        }

        if (schema.isMultivalue() && schema.isUniqueConstraint()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidSchema,
                            "Cannot be multivalue and have unique constraint at the same time")).
                    addPropertyNode("multiValue").addConstraintViolation();
            return false;
        }

        return true;
    }
}
