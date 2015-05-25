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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

public class SchemaNameValidator extends AbstractValidator<SchemaNameCheck, Object> {

    private static final Set<String> UNALLOWED_SCHEMA_NAMES = new HashSet<>();

    static {
        initUnallowedSchemaNames(JPAAnyObject.class, UNALLOWED_SCHEMA_NAMES);
        initUnallowedSchemaNames(JPAGroup.class, UNALLOWED_SCHEMA_NAMES);
        initUnallowedSchemaNames(JPAUser.class, UNALLOWED_SCHEMA_NAMES);
        initUnallowedSchemaNames(JPAConf.class, UNALLOWED_SCHEMA_NAMES);
    }

    private static void initUnallowedSchemaNames(final Class<?> entityClass, final Set<String> names) {
        List<Class<?>> classes = ClassUtils.getAllSuperclasses(entityClass);
        if (!classes.contains(JPAUser.class)) {
            classes.add(JPAUser.class);
        }
        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    names.add(field.getName());
                }
            }
        }
    }

    @Override
    public boolean isValid(final Object object, final ConstraintValidatorContext context) {
        String schemaName;
        Set<String> unallowedNames = UNALLOWED_SCHEMA_NAMES;
        if (object instanceof PlainSchema) {
            schemaName = ((PlainSchema) object).getKey();
        } else if (object instanceof DerSchema) {
            schemaName = ((DerSchema) object).getKey();
        } else if (object instanceof VirSchema) {
            schemaName = ((VirSchema) object).getKey();
        } else {
            schemaName = null;
            unallowedNames = Collections.emptySet();
        }

        boolean isValid = NAME_PATTERN.matcher(schemaName).matches();
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, "Invalid Schema name")).
                    addPropertyNode("name").addConstraintViolation();
        } else if (unallowedNames.contains(schemaName)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, "Schema name not allowed: " + schemaName)).
                    addPropertyNode("name").addConstraintViolation();

            return false;
        }

        return isValid;
    }
}
