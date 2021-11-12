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
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtils;

public class SchemaKeyValidator extends AbstractValidator<SchemaKeyCheck, Schema> {

    @Override
    public boolean isValid(final Schema schema, final ConstraintValidatorContext context) {

        if (schema.getKey() == null || !Entity.ID_PATTERN.matcher(schema.getKey()).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, schema.getKey())).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        } else if (JPAAnyUtils.matchesFieldName(schema.getKey())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, "Schema key not allowed: " + schema.getKey())).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        }

        return true;
    }
}
