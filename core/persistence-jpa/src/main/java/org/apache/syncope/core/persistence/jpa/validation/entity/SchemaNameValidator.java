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
import org.apache.syncope.core.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

public class SchemaNameValidator extends AbstractValidator<SchemaNameCheck, Object> {

    private static final Set<String> UNALLOWED_USCHEMA_NAMES = new HashSet<>();

    private static final Set<String> UNALLOWED_MSCHEMA_NAMES = new HashSet<>();

    private static final Set<String> UNALLOWED_RSCHEMA_NAMES = new HashSet<>();

    private static final Set<String> UNALLOWED_CSCHEMA_NAMES = new HashSet<>();

    static {
        initUnallowedSchemaNames(JPAUser.class, UNALLOWED_USCHEMA_NAMES);
        initUnallowedSchemaNames(JPAMembership.class, UNALLOWED_MSCHEMA_NAMES);
        initUnallowedSchemaNames(JPAGroup.class, UNALLOWED_RSCHEMA_NAMES);
        initUnallowedSchemaNames(JPAConf.class, UNALLOWED_CSCHEMA_NAMES);
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
        final String schemaName;
        final Set<String> unallowedNames;

        if (object instanceof UPlainSchema) {
            schemaName = ((UPlainSchema) object).getKey();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof UDerSchema) {
            schemaName = ((UDerSchema) object).getKey();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof UVirSchema) {
            schemaName = ((UVirSchema) object).getKey();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof MPlainSchema) {
            schemaName = ((MPlainSchema) object).getKey();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof MDerSchema) {
            schemaName = ((MDerSchema) object).getKey();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof MVirSchema) {
            schemaName = ((MVirSchema) object).getKey();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof GPlainSchema) {
            schemaName = ((GPlainSchema) object).getKey();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else if (object instanceof GDerSchema) {
            schemaName = ((GDerSchema) object).getKey();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else if (object instanceof GVirSchema) {
            schemaName = ((GVirSchema) object).getKey();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else if (object instanceof CPlainSchema) {
            schemaName = ((CPlainSchema) object).getKey();
            unallowedNames = UNALLOWED_CSCHEMA_NAMES;
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
