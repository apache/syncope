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
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.beans.BeanUtils;

public class SchemaKeyValidator extends AbstractValidator<SchemaKeyCheck, Schema> {

    protected static final Set<String> ANY_FIELDS = new HashSet<>();

    static {
        ANY_FIELDS.add("id");
        Stream.of(User.class, Group.class, AnyObject.class).forEach(clazz -> {
            for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(clazz)) {
                ANY_FIELDS.add(pd.getName());
            }
        });
    }

    @Override
    public boolean isValid(final Schema schema, final ConstraintValidatorContext context) {
        if (schema.getKey() == null || !Entity.ID_PATTERN.matcher(schema.getKey()).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, schema.getKey())).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        } else if (ANY_FIELDS.contains(schema.getKey())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidKey, "Schema key not allowed: " + schema.getKey())).
                    addPropertyNode("key").addConstraintViolation();

            return false;
        }

        return true;
    }
}
