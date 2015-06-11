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
import java.util.HashSet;
import java.util.Set;

public final class Entitlement {

    public static final String ANONYMOUS = "ANONYMOUS";

    public static final String REALM_LIST = "REALM_LIST";

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

    public static final String SCHEMA_LIST = "SCHEMA_LIST";

    public static final String SCHEMA_CREATE = "SCHEMA_CREATE";

    public static final String SCHEMA_READ = "SCHEMA_READ";

    public static final String SCHEMA_UPDATE = "SCHEMA_UPDATE";

    public static final String SCHEMA_DELETE = "SCHEMA_DELETE";

    public static final String USER_SEARCH = "USER_SEARCH";

    public static final String USER_LIST = "USER_LIST";

    public static final String USER_CREATE = "USER_CREATE";

    public static final String USER_READ = "USER_READ";

    public static final String USER_UPDATE = "USER_UPDATE";

    public static final String USER_DELETE = "USER_DELETE";

    public static final String USER_VIEW = "USER_VIEW";

    public static final String GROUP_SEARCH = "GROUP_SEARCH";

    public static final String GROUP_CREATE = "GROUP_CREATE";

    public static final String GROUP_READ = "GROUP_READ";

    public static final String GROUP_UPDATE = "GROUP_UPDATE";

    public static final String GROUP_DELETE = "GROUP_DELETE";

    public static final String ANY_OBJECT_SEARCH = "ANY_OBJECT_SEARCH";

    public static final String ANY_OBJECT_LIST = "ANY_OBJECT_LIST";

    public static final String ANY_OBJECT_CREATE = "ANY_OBJECT_CREATE";

    public static final String ANY_OBJECT_READ = "ANY_OBJECT_READ";

    public static final String ANY_OBJECT_UPDATE = "ANY_OBJECT_UPDATE";

    public static final String ANY_OBJECT_DELETE = "ANY_OBJECT_DELETE";

    public static final String RESOURCE_LIST = "RESOURCE_LIST";

    public static final String RESOURCE_CREATE = "RESOURCE_CREATE";

    public static final String RESOURCE_READ = "RESOURCE_READ";

    public static final String RESOURCE_UPDATE = "RESOURCE_UPDATE";

    public static final String RESOURCE_DELETE = "RESOURCE_DELETE";

    public static final String RESOURCE_GETCONNECTOROBJECT = "RESOURCE_GETCONNECTOROBJECT";

    public static final String CONNECTOR_LIST = "CONNECTOR_LIST";

    public static final String CONNECTOR_CREATE = "CONNECTOR_CREATE";

    public static final String CONNECTOR_READ = "CONNECTOR_READ";

    public static final String CONNECTOR_UPDATE = "CONNECTOR_UPDATE";

    public static final String CONNECTOR_DELETE = "CONNECTOR_DELETE";

    public static final String CONNECTOR_RELOAD = "CONNECTOR_RELOAD";

    public static final String CONFIGURATION_EXPORT = "CONFIGURATION_EXPORT";

    public static final String CONFIGURATION_LIST = "CONFIGURATION_LIST";

    public static final String CONFIGURATION_SET = "CONFIGURATION_SET";

    public static final String CONFIGURATION_DELETE = "CONFIGURATION_DELETE";

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

    public static final String WORKFLOW_DEF_READ = "WORKFLOW_DEF_READ";

    public static final String WORKFLOW_DEF_UPDATE = "WORKFLOW_DEF_UPDATE";

    public static final String WORKFLOW_TASK_LIST = "WORKFLOW_TASK_LIST";

    public static final String WORKFLOW_FORM_LIST = "WORKFLOW_FORM_LIST";

    public static final String WORKFLOW_FORM_READ = "WORKFLOW_FORM_READ";

    public static final String WORKFLOW_FORM_CLAIM = "WORKFLOW_FORM_CLAIM";

    public static final String WORKFLOW_FORM_SUBMIT = "WORKFLOW_FORM_SUBMIT";

    public static final String NOTIFICATION_LIST = "NOTIFICATION_LIST";

    public static final String NOTIFICATION_CREATE = "NOTIFICATION_CREATE";

    public static final String NOTIFICATION_READ = "NOTIFICATION_READ";

    public static final String NOTIFICATION_UPDATE = "NOTIFICATION_UPDATE";

    public static final String NOTIFICATION_DELETE = "NOTIFICATION_DELETE";

    public static final String REPORT_LIST = "REPORT_LIST";

    public static final String REPORT_READ = "REPORT_READ";

    public static final String REPORT_CREATE = "REPORT_CREATE";

    public static final String REPORT_UPDATE = "REPORT_UPDATE";

    public static final String REPORT_DELETE = "REPORT_DELETE";

    public static final String REPORT_EXECUTE = "REPORT_EXECUTE";

    public static final String LOG_LIST = "LOG_LIST";

    public static final String LOG_SET_LEVEL = "LOG_SET_LEVEL";

    public static final String LOG_DELETE = "LOG_DELETE";

    public static final String AUDIT_LIST = "AUDIT_LIST";

    public static final String AUDIT_ENABLE = "AUDIT_ENABLE";

    public static final String AUDIT_DISABLE = "AUDIT_DISABLE";

    public static final String SECURITY_QUESTION_CREATE = "SECURITY_QUESTION_CREATE";

    public static final String SECURITY_QUESTION_UPDATE = "SECURITY_QUESTION_UPDATE";

    public static final String SECURITY_QUESTION_DELETE = "SECURITY_QUESTION_DELETE";

    public static final String ROUTE_READ = "ROUTE_READ";

    public static final String ROUTE_LIST = "ROUTE_LIST";

    public static final String ROUTE_UPDATE = "ROUTE_UPDATE";

    private static final Set<String> ENTITLEMENTS;

    static {
        Set<String> values = new HashSet<>();
        for (Field field : Entitlement.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        ENTITLEMENTS = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return ENTITLEMENTS;
    }

    private Entitlement() {
        // private constructor for static utility class
    }
}
