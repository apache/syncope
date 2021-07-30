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
package org.apache.syncope.common.lib.search;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;

public final class SearchableFields {

    private static final String[] ATTRIBUTES_NOTINCLUDED = {
        "serialVersionUID", "discriminator", "password", "type", "udynMembershipCond", "securityAnswer",
        "token", "tokenExpireTime"
    };

    private static final Set<String> ANY_FIELDS = new HashSet<>();

    static {
        ANY_FIELDS.addAll(get(UserTO.class).keySet());
        ANY_FIELDS.addAll(get(GroupTO.class).keySet());
        ANY_FIELDS.addAll(get(AnyObjectTO.class).keySet());
    }

    public static boolean contains(final String schema) {
        return ANY_FIELDS.contains(schema);
    }

    public static Map<String, AttrSchemaType> get(final Class<? extends AnyTO> anyRef) {
        final Map<String, AttrSchemaType> fields = new TreeMap<>(Collections.reverseOrder());

        // loop on class and all superclasses searching for field
        Class<?> clazz = anyRef;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!ArrayUtils.contains(ATTRIBUTES_NOTINCLUDED, field.getName())
                        && !Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    fields.put(field.getName(), AttrSchemaType.getAttrSchemaTypeByClass(field.getType()));
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    private SearchableFields() {
        // empty constructor for static utility class
    }
}
