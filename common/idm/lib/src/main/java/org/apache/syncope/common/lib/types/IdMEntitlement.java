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
package org.apache.syncope.common.lib.types;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class IdMEntitlement {

    public static final String RESOURCE_LIST = "RESOURCE_LIST";

    public static final String RESOURCE_CREATE = "RESOURCE_CREATE";

    public static final String RESOURCE_READ = "RESOURCE_READ";

    public static final String RESOURCE_UPDATE = "RESOURCE_UPDATE";

    public static final String RESOURCE_DELETE = "RESOURCE_DELETE";

    public static final String RESOURCE_GET_CONNOBJECT = "RESOURCE_GET_CONNOBJECT";

    public static final String RESOURCE_LIST_CONNOBJECT = "RESOURCE_LIST_CONNOBJECT";

    public static final String CONNECTOR_LIST = "CONNECTOR_LIST";

    public static final String CONNECTOR_CREATE = "CONNECTOR_CREATE";

    public static final String CONNECTOR_READ = "CONNECTOR_READ";

    public static final String CONNECTOR_UPDATE = "CONNECTOR_UPDATE";

    public static final String CONNECTOR_DELETE = "CONNECTOR_DELETE";

    public static final String CONNECTOR_RELOAD = "CONNECTOR_RELOAD";

    public static final String REMEDIATION_LIST = "REMEDIATION_LIST";

    public static final String REMEDIATION_READ = "REMEDIATION_READ";

    public static final String REMEDIATION_REMEDY = "REMEDIATION_REMEDY";

    public static final String REMEDIATION_DELETE = "REMEDIATION_DELETE";

    private static final Set<String> VALUES;

    static {
        Set<String> values = new TreeSet<>();
        for (Field field : IdMEntitlement.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        VALUES = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return VALUES;
    }

    private IdMEntitlement() {
        // private constructor for static utility class
    }
}
