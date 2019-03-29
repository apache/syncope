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

public final class IdRepoImplementationType {

    public static final String JWT_SSO_PROVIDER = "JWT_SSO_PROVIDER";

    public static final String REPORTLET = "REPORTLET";

    public static final String ACCOUNT_RULE = "ACCOUNT_RULE";

    public static final String PASSWORD_RULE = "PASSWORD_RULE";

    public static final String TASKJOB_DELEGATE = "TASKJOB_DELEGATE";

    public static final String LOGIC_ACTIONS = "LOGIC_ACTIONS";

    public static final String VALIDATOR = "VALIDATOR";

    public static final String RECIPIENTS_PROVIDER = "RECIPIENTS_PROVIDER";

    public static final String AUDIT_APPENDER = "AUDIT_APPENDER";

    private static final Set<String> VALUES;

    static {
        Set<String> values = new TreeSet<>();
        for (Field field : IdRepoImplementationType.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        VALUES = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return VALUES;
    }

    private IdRepoImplementationType() {
        // private constructor for static utility class
    }
}
