/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.commons;

public final class Constants {

    public static final String PREF_USERS_ATTRIBUTES_VIEW =
            "users.attributes.view";

    public static final String PREFS_COOKIE_NAME = "syncopeConsolePrefs";

    public static final String PREF_USER_SCHEMA_PAGINATOR_ROWS =
            "user.schema.paginator.rows";

    public static final String PREF_USER_DER_SCHEMA_PAGINATOR_ROWS =
            "user.derived.schema.paginator.rows";

    public static final String PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS =
            "user.virtual.schema.paginator.rows";

    public static final String PREF_ROLE_SCHEMA_PAGINATOR_ROWS =
            "role.schema.paginator.rows";

    public static final String PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS =
            "role.derived.schema.paginator.rows";

    public static final String PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS =
            "role.virtual.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS =
            "membership.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS =
            "membership.derived.aschema.paginator.rows";

    public static final String PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS =
            "membership.virtual.aschema.paginator.rows";

    public static final String PREF_USERS_PAGINATOR_ROWS =
            "users.paginator.rows";

    public static final String PREF_USERS_SEARCH_PAGINATOR_ROWS =
            "users.paginator.search.rows";

    public static final String PREF_RESOURCES_PAGINATOR_ROWS =
            "resources.paginator.rows";

    public static final String PREF_CONNECTORS_PAGINATOR_ROWS =
            "connectors.paginator.rows";

    public static final String PREF_CONFIGURATION_PAGINATOR_ROWS =
            "configuration.paginator.rows";

    public static final String PREF_TASKS_PAGINATOR_ROWS =
            "tasks.paginator.rows";

    public static final String PAGEPARAM_CREATE = "CREATE";

    public static final String PAGEPARAM_CURRENT_PAGE = "_current_page";

    /* DATE FORMATS FOR TASKS */
    public static final String ITALIAN_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String ENGLISH_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss";

    public static final String[] SCHEMA_FIELDS = {"name", "type"};

    public static final String[] VIRTUAL_SCHEMA_FIELDS = {"name"};

    public static final String[] DERIVED_SCHEMA_FIELDS = {"name", "expression"};

    public static final String PREF_POLICY_PAGINATOR_ROWS =
            "policy.paginator.rows";

    private Constants() {
    }
}
