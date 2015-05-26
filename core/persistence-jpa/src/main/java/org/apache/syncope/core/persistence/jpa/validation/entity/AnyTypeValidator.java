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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.AnyType;

public class AnyTypeValidator extends AbstractValidator<AnyTypeCheck, AnyType> {

    @Override
    public boolean isValid(final AnyType object, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        boolean isValid;
        switch (object.getKind()) {
            case USER:
                isValid = AnyTypeKind.USER.name().equalsIgnoreCase(object.getKey());
                break;

            case GROUP:
                isValid = AnyTypeKind.GROUP.name().equalsIgnoreCase(object.getKey());
                break;

            case ANY_OBJECT:
            default:
                isValid = !AnyTypeKind.USER.name().equalsIgnoreCase(object.getKey())
                        && !AnyTypeKind.GROUP.name().equalsIgnoreCase(object.getKey());
        }

        if (!isValid) {
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidAnyType, "Name / kind mismatch")).
                    addPropertyNode("name").addConstraintViolation();
        }

        return isValid;
    }
}
