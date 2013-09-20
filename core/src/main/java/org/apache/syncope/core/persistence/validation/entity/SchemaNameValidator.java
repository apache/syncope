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
package org.apache.syncope.core.persistence.validation.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.ClassUtils;

import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.persistence.beans.membership.MDerSchema;
import org.apache.syncope.core.persistence.beans.membership.MSchema;
import org.apache.syncope.core.persistence.beans.membership.MVirSchema;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RDerSchema;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.RVirSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.beans.user.UDerSchema;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.beans.user.UVirSchema;

public class SchemaNameValidator extends AbstractValidator<SchemaNameCheck, Object> {

    private static final List<String> UNALLOWED_USCHEMA_NAMES = new ArrayList<String>();

    private static final List<String> UNALLOWED_MSCHEMA_NAMES = new ArrayList<String>();

    private static final List<String> UNALLOWED_RSCHEMA_NAMES = new ArrayList<String>();

    static {
        initUnallowedSchemaNames(SyncopeUser.class, UNALLOWED_USCHEMA_NAMES);
        initUnallowedSchemaNames(Membership.class, UNALLOWED_MSCHEMA_NAMES);
        initUnallowedSchemaNames(SyncopeRole.class, UNALLOWED_RSCHEMA_NAMES);
    }

    private static void initUnallowedSchemaNames(final Class<?> entityClass, final List<String> names) {
        List<Class<?>> classes = ClassUtils.getAllSuperclasses(entityClass);
        classes.add(SyncopeUser.class);
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
        final List<String> unallowedNames;

        if (object instanceof USchema) {
            schemaName = ((USchema) object).getName();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof UDerSchema) {
            schemaName = ((UDerSchema) object).getName();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof UVirSchema) {
            schemaName = ((UVirSchema) object).getName();
            unallowedNames = UNALLOWED_USCHEMA_NAMES;
        } else if (object instanceof MSchema) {
            schemaName = ((MSchema) object).getName();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof MDerSchema) {
            schemaName = ((MDerSchema) object).getName();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof MVirSchema) {
            schemaName = ((MVirSchema) object).getName();
            unallowedNames = UNALLOWED_MSCHEMA_NAMES;
        } else if (object instanceof RSchema) {
            schemaName = ((RSchema) object).getName();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else if (object instanceof RDerSchema) {
            schemaName = ((RDerSchema) object).getName();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else if (object instanceof RVirSchema) {
            schemaName = ((RVirSchema) object).getName();
            unallowedNames = UNALLOWED_RSCHEMA_NAMES;
        } else {
            schemaName = null;
            unallowedNames = Collections.emptyList();
        }

        boolean isValid = NAME_PATTERN.matcher(schemaName).matches();
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, "Invalid Schema name")).
                    addNode("name").addConstraintViolation();
        } else if (unallowedNames.contains(schemaName)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidName, "Schema name not allowed: " + schemaName)).
                    addNode("name").addConstraintViolation();

            return false;
        }

        return isValid;
    }
}
