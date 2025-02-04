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

public final class IdRepoEntitlement {

    public static final String ANONYMOUS = "ANONYMOUS";

    public static final String MUST_CHANGE_PASSWORD = "MUST_CHANGE_PASSWORD";

    public static final String DOMAIN_CREATE = "DOMAIN_CREATE";

    public static final String DOMAIN_READ = "DOMAIN_READ";

    public static final String DOMAIN_UPDATE = "DOMAIN_UPDATE";

    public static final String DOMAIN_DELETE = "DOMAIN_DELETE";

    public static final String REALM_SEARCH = "REALM_SEARCH";

    public static final String REALM_CREATE = "REALM_CREATE";

    public static final String REALM_UPDATE = "REALM_UPDATE";

    public static final String REALM_DELETE = "REALM_DELETE";

    public static final String ANYTYPECLASS_LIST = "ANYTYPECLASS_LIST";

    public static final String ANYTYPECLASS_CREATE = "ANYTYPECLASS_CREATE";

    public static final String ANYTYPECLASS_READ = "ANYTYPECLASS_READ";

    public static final String ANYTYPECLASS_UPDATE = "ANYTYPECLASS_UPDATE";

    public static final String ANYTYPECLASS_DELETE = "ANYTYPECLASS_DELETE";

    public static final String ANYTYPE_LIST = "ANYTYPE_LIST";

    public static final String ANYTYPE_CREATE = "ANYTYPE_CREATE";

    public static final String ANYTYPE_READ = "ANYTYPE_READ";

    public static final String ANYTYPE_UPDATE = "ANYTYPE_UPDATE";

    public static final String ANYTYPE_DELETE = "ANYTYPE_DELETE";

    public static final String RELATIONSHIPTYPE_LIST = "RELATIONSHIPTYPE_LIST";

    public static final String RELATIONSHIPTYPE_CREATE = "RELATIONSHIPTYPE_CREATE";

    public static final String RELATIONSHIPTYPE_READ = "RELATIONSHIPTYPE_READ";

    public static final String RELATIONSHIPTYPE_UPDATE = "RELATIONSHIPTYPE_UPDATE";

    public static final String RELATIONSHIPTYPE_DELETE = "RELATIONSHIPTYPE_DELETE";

    public static final String ROLE_LIST = "ROLE_LIST";

    public static final String ROLE_CREATE = "ROLE_CREATE";

    public static final String ROLE_READ = "ROLE_READ";

    public static final String ROLE_UPDATE = "ROLE_UPDATE";

    public static final String ROLE_DELETE = "ROLE_DELETE";

    public static final String DYNREALM_CREATE = "DYNREALM_CREATE";

    public static final String DYNREALM_READ = "DYNREALM_READ";

    public static final String DYNREALM_UPDATE = "DYNREALM_UPDATE";

    public static final String DYNREALM_DELETE = "DYNREALM_DELETE";

    public static final String SCHEMA_CREATE = "SCHEMA_CREATE";

    public static final String SCHEMA_UPDATE = "SCHEMA_UPDATE";

    public static final String SCHEMA_DELETE = "SCHEMA_DELETE";

    public static final String USER_SEARCH = "USER_SEARCH";

    public static final String USER_CREATE = "USER_CREATE";

    public static final String USER_READ = "USER_READ";

    public static final String USER_UPDATE = "USER_UPDATE";

    public static final String USER_DELETE = "USER_DELETE";

    public static final String GROUP_CREATE = "GROUP_CREATE";

    public static final String GROUP_SEARCH = "GROUP_SEARCH";

    public static final String GROUP_READ = "GROUP_READ";

    public static final String GROUP_UPDATE = "GROUP_UPDATE";

    public static final String GROUP_DELETE = "GROUP_DELETE";

    public static final String KEYMASTER = "KEYMASTER";

    public static final String TASK_LIST = "TASK_LIST";

    public static final String TASK_CREATE = "TASK_CREATE";

    public static final String TASK_READ = "TASK_READ";

    public static final String TASK_UPDATE = "TASK_UPDATE";

    public static final String TASK_DELETE = "TASK_DELETE";

    public static final String TASK_EXECUTE = "TASK_EXECUTE";

    public static final String POLICY_LIST = "POLICY_LIST";

    public static final String POLICY_CREATE = "POLICY_CREATE";

    public static final String POLICY_READ = "POLICY_READ";

    public static final String POLICY_UPDATE = "POLICY_UPDATE";

    public static final String POLICY_DELETE = "POLICY_DELETE";

    public static final String MAIL_TEMPLATE_LIST = "MAIL_TEMPLATE_LIST";

    public static final String MAIL_TEMPLATE_CREATE = "MAIL_TEMPLATE_CREATE";

    public static final String MAIL_TEMPLATE_READ = "MAIL_TEMPLATE_READ";

    public static final String MAIL_TEMPLATE_UPDATE = "MAIL_TEMPLATE_UPDATE";

    public static final String MAIL_TEMPLATE_DELETE = "MAIL_TEMPLATE_DELETE";

    public static final String NOTIFICATION_LIST = "NOTIFICATION_LIST";

    public static final String NOTIFICATION_CREATE = "NOTIFICATION_CREATE";

    public static final String NOTIFICATION_READ = "NOTIFICATION_READ";

    public static final String NOTIFICATION_UPDATE = "NOTIFICATION_UPDATE";

    public static final String NOTIFICATION_DELETE = "NOTIFICATION_DELETE";

    public static final String NOTIFICATION_EXECUTE = "NOTIFICATION_EXECUTE";

    public static final String REPORT_TEMPLATE_LIST = "REPORT_TEMPLATE_LIST";

    public static final String REPORT_TEMPLATE_CREATE = "REPORT_TEMPLATE_CREATE";

    public static final String REPORT_TEMPLATE_READ = "REPORT_TEMPLATE_READ";

    public static final String REPORT_TEMPLATE_UPDATE = "REPORT_TEMPLATE_UPDATE";

    public static final String REPORT_TEMPLATE_DELETE = "REPORT_TEMPLATE_DELETE";

    public static final String REPORT_LIST = "REPORT_LIST";

    public static final String REPORT_READ = "REPORT_READ";

    public static final String REPORT_CREATE = "REPORT_CREATE";

    public static final String REPORT_UPDATE = "REPORT_UPDATE";

    public static final String REPORT_DELETE = "REPORT_DELETE";

    public static final String REPORT_EXECUTE = "REPORT_EXECUTE";

    public static final String AUDIT_SEARCH = "AUDIT_SEARCH";

    public static final String AUDIT_LIST = "AUDIT_LIST";

    public static final String AUDIT_READ = "AUDIT_READ";

    public static final String AUDIT_SET = "AUDIT_SET";

    public static final String AUDIT_DELETE = "AUDIT_DELETE";

    public static final String SECURITY_QUESTION_CREATE = "SECURITY_QUESTION_CREATE";

    public static final String SECURITY_QUESTION_READ = "SECURITY_QUESTION_READ";

    public static final String SECURITY_QUESTION_UPDATE = "SECURITY_QUESTION_UPDATE";

    public static final String SECURITY_QUESTION_DELETE = "SECURITY_QUESTION_DELETE";

    public static final String ACCESS_TOKEN_LIST = "ACCESS_TOKEN_LIST";

    public static final String ACCESS_TOKEN_DELETE = "ACCESS_TOKEN_DELETE";

    public static final String IMPLEMENTATION_LIST = "IMPLEMENTATION_LIST";

    public static final String IMPLEMENTATION_READ = "IMPLEMENTATION_READ";

    public static final String IMPLEMENTATION_CREATE = "IMPLEMENTATION_CREATE";

    public static final String IMPLEMENTATION_UPDATE = "IMPLEMENTATION_UPDATE";

    public static final String IMPLEMENTATION_DELETE = "IMPLEMENTATION_DELETE";

    public static final String DELEGATION_LIST = "DELEGATION_LIST";

    public static final String DELEGATION_CREATE = "DELEGATION_CREATE";

    public static final String DELEGATION_READ = "DELEGATION_READ";

    public static final String DELEGATION_UPDATE = "DELEGATION_UPDATE";

    public static final String DELEGATION_DELETE = "DELEGATION_DELETE";

    public static final String COMMAND_RUN = "COMMAND_RUN";

    public static final String LOGGER_LIST = "LOGGER_LIST";

    public static final String LOGGER_UPDATE = "LOGGER_UPDATE";

    private static final Set<String> VALUES;

    static {
        Set<String> values = new TreeSet<>();
        for (Field field : IdRepoEntitlement.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        values.remove(ANONYMOUS);
        values.remove(MUST_CHANGE_PASSWORD);
        VALUES = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return VALUES;
    }

    private IdRepoEntitlement() {
        // private constructor for static utility class
    }
}
