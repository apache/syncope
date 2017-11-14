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
package org.apache.syncope.common.lib.scim.types;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class SCIMEntitlement {

    public static final String SCIM_CONF_GET = "SCIM_CONF_GET";

    public static final String SCIM_CONF_SET = "SCIM_CONF_SET";

    private static final Set<String> VALUES;

    static {
        Set<String> values = new TreeSet<>();
        for (Field field : SCIMEntitlement.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        VALUES = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return VALUES;
    }

    private SCIMEntitlement() {
        // private constructor for static utility class
    }
}
