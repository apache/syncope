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
package org.apache.syncope.client.console.commons;

public final class Constants {

    public static final String ON_CLICK = "onclick";

    public static final String ON_CHANGE = "onchange";

    public static final String ON_KEYUP = "onkeyup";

    public static final String ON_BLUR = "onblur";

    public static final String PNG_EXT = ".png";

    public static final String FEEDBACK = "feedback";

    public static final String OPERATION_SUCCEEDED = "operation_succeeded";

    public static final String OPERATION_ERROR = "operation_error";

    public static final String SEARCH_ERROR = "search_error";

    public static final String ERROR = "error";

    public static final String PARAM_PASSWORD_RESET_TOKEN = "pwdResetToken";

    public static final String PREF_USERS_DETAILS_VIEW = "users.details.view";

    public static final String PREF_USERS_ATTRIBUTES_VIEW = "users.attributes.view";

    public static final String PREF_USERS_DERIVED_ATTRIBUTES_VIEW = "users.derived.attributes.view";

    public static final String PREF_CONF_SCHEMA_PAGINATOR_ROWS = "conf.schema.paginator.rows";

    public static final String PREF_USER_PLAIN_SCHEMA_PAGINATOR_ROWS = "user.schema.paginator.rows";

    public static final String PREF_USER_DER_SCHEMA_PAGINATOR_ROWS = "user.derived.schema.paginator.rows";

    public static final String PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS = "user.virtual.schema.paginator.rows";

    public static final String PREF_ANYTYPE_PAGINATOR_ROWS = "anytype.paginator.rows";

    public static final String PREF_SECURITY_QUESTIONS_PAGINATOR_ROWS = "security.questions.paginator.rows";

    public static final String PREF_PARAMETERS_PAGINATOR_ROWS = "parameters.paginator.rows";

    public static final String PREF_RELATIONSHIPTYPE_PAGINATOR_ROWS = "relationshiptype.painator.rows";

    public static final String PREF_GROUP_DETAILS_VIEW = "group.details.view";

    public static final String PREF_GROUP_ATTRIBUTES_VIEW = "group.attributes.view";

    public static final String PREF_GROUP_DERIVED_ATTRIBUTES_VIEW = "group.derived.attributes.view";

    public static final String PREF_GROUP_PLAIN_SCHEMA_PAGINATOR_ROWS = "group.schema.paginator.rows";

    public static final String PREF_GROUP_DER_SCHEMA_PAGINATOR_ROWS = "group.derived.schema.paginator.rows";

    public static final String PREF_GROUP_VIR_SCHEMA_PAGINATOR_ROWS = "group.virtual.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_PLAIN_SCHEMA_PAGINATOR_ROWS = "membership.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS = "membership.derived.aschema.paginator.rows";

    public static final String PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS = "membership.virtual.aschema.paginator.rows";

    public static final String PREF_USERS_PAGINATOR_ROWS = "users.paginator.rows";

    public static final String PREF_ANYOBJECT_PAGINATOR_ROWS = "anyobject.paginator.rows";

    public static final String PREF_GROUP_PAGINATOR_ROWS = "group.paginator.rows";

    public static final String PREF_ROLE_PAGINATOR_ROWS = "role.paginator.rows";

    public static final String PREF_RESOURCES_PAGINATOR_ROWS = "resources.paginator.rows";

    public static final String PREF_CONNECTORS_PAGINATOR_ROWS = "connectors.paginator.rows";

    public static final String PREF_NOTIFICATION_PAGINATOR_ROWS = "notification.paginator.rows";

    public static final String PREF_PROPAGATION_TASKS_PAGINATOR_ROWS = "proagationtasks.paginator.rows";

    public static final String PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS = "notificationtasks.paginator.rows";

    public static final String PREF_SCHED_TASKS_PAGINATOR_ROWS = "schedtasks.paginator.rows";

    public static final String PREF_SYNC_TASKS_PAGINATOR_ROWS = "synctasks.paginator.rows";

    public static final String PREF_TODO_PAGINATOR_ROWS = "todo.paginator.rows";

    public static final String PREF_REPORT_PAGINATOR_ROWS = "report.paginator.rows";

    public static final String PAGEPARAM_CREATE = "CREATE";

    public static final String PAGEPARAM_CURRENT_PAGE = "_current_page";

    public static final String PREF_POLICY_PAGINATOR_ROWS = "policy.paginator.rows";

    public static final String PREF_ANY_DETAILS_VIEW = "any.%s.details.view";

    public static final String PREF_ANY_ATTRIBUTES_VIEW = "any.%s.attributes.view";

    public static final String PREF_ANY_DERIVED_ATTRIBUTES_VIEW = "any.%s.derived.attributes.view";

    public static final String SUSPENDED_ICON = "glyphicon glyphicon-ban-circle";

    public static final String ACTIVE_ICON = "glyphicon glyphicon-ok-circle";

    public static final String UNDEFINED_ICON = "glyphicon glyphicon-question-sign";

    public static final String NOT_FOUND_ICON = "glyphicon glyphicon-remove-circle";

    ;

    /**
     * ConnId's GuardedString is not in the classpath.
     */
    public static final String GUARDED_STRING = "org.identityconnectors.common.security.GuardedString";

    /**
     * ConnId's GuardedByteArray is not in the classpath.
     */
    public static final String GUARDED_BYTE_ARRAY = "org.identityconnectors.common.security.GuardedByteArray";

    private Constants() {
        // private constructor for static utility class
    }
}
