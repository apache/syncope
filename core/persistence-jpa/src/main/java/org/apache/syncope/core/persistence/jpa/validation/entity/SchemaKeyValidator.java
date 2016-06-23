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
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtils;

public class SchemaKeyValidator extends AbstractValidator<SchemaKeyCheck, Object> {

    @Override
    public boolean isValid(final Object object, final ConstraintValidatorContext context) {
        String key = null;
        if (object instanceof PlainSchema) {
            key = ((PlainSchema) object).getKey();
        } else if (object instanceof DerSchema) {
            key = ((DerSchema) object).getKey();
        } else if (object instanceof VirSchema) {
            key = ((VirSchema) object).getKey();
        }

        boolean isValid = KEY_PATTERN.matcher(key).matches();
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, "Invalid schema key")).
                    addPropertyNode("key").addConstraintViolation();
        } else if (JPAAnyUtils.matchesFieldName(key)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, "Schema key not allowed: " + key)).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        }

        return isValid;
    }
}
