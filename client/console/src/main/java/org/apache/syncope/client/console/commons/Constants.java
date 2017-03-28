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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

public final class Constants {

    public static final String SYNCOPE = "syncope";

    public static final String ON_CLICK = "click";

    public static final String ON_CHANGE = "change";

    public static final String ON_BLUR = "blur";

    public static final String PNG_EXT = ".png";

    public static final String FEEDBACK = "feedback";

    public static final String OPERATION_SUCCEEDED = "operation_succeeded";

    public static final String OPERATION_ERROR = "operation_error";

    public static final String SEARCH_ERROR = "search_error";

    public static final String ERROR = "error";

    public static final String BEFORE_LOGOUT_PAGE = "beforeLogoutPage";

    public static final String PARAM_PASSWORD_RESET_TOKEN = "pwdResetToken";

    public static final String PREF_USERS_DETAILS_VIEW = "users.details.view";

    public static final String PREF_USERS_PLAIN_ATTRS_VIEW = "users.plain.attrs.view";

    public static final String PREF_USERS_DER_ATTRS_VIEW = "users.der.attrs.view";

    public static final String PREF_CONF_SCHEMA_PAGINATOR_ROWS = "conf.schema.paginator.rows";

    public static final String PREF_USER_PLAIN_SCHEMA_PAGINATOR_ROWS = "user.schema.paginator.rows";

    public static final String PREF_USER_DER_SCHEMA_PAGINATOR_ROWS = "user.derived.schema.paginator.rows";

    public static final String PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS = "user.virtual.schema.paginator.rows";

    public static final String PREF_ANYTYPE_PAGINATOR_ROWS = "anytype.paginator.rows";

    public static final String PREF_SECURITY_QUESTIONS_PAGINATOR_ROWS = "security.questions.paginator.rows";

    public static final String PREF_PARAMETERS_PAGINATOR_ROWS = "parameters.paginator.rows";

    public static final String PREF_RELATIONSHIPTYPE_PAGINATOR_ROWS = "relationshiptype.painator.rows";

    public static final String PREF_GROUP_DETAILS_VIEW = "group.details.view";

    public static final String PREF_GROUP_PLAIN_ATTRS_VIEW = "group.plain.attrs.view";

    public static final String PREF_GROUP_DER_ATTRS_VIEW = "group.der.attrs.view";

    public static final String PREF_GROUP_PLAIN_SCHEMA_PAGINATOR_ROWS = "group.schema.paginator.rows";

    public static final String PREF_GROUP_DER_SCHEMA_PAGINATOR_ROWS = "group.derived.schema.paginator.rows";

    public static final String PREF_GROUP_VIR_SCHEMA_PAGINATOR_ROWS = "group.virtual.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_PLAIN_SCHEMA_PAGINATOR_ROWS = "membership.schema.paginator.rows";

    public static final String PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS = "membership.derived.aschema.paginator.rows";

    public static final String PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS = "membership.virtual.aschema.paginator.rows";

    public static final String PREF_USERS_PAGINATOR_ROWS = "users.paginator.rows";

    public static final String PREF_ANYOBJECT_PAGINATOR_ROWS = "anyobject.paginator.rows";

    public static final String PREF_GROUP_PAGINATOR_ROWS = "group.paginator.rows";

    public static final String PREF_CONNOBJECTS_PAGINATOR_ROWS = "connobjects.paginator.rows";

    public static final String PREF_ROLE_PAGINATOR_ROWS = "role.paginator.rows";

    public static final String PREF_ACCESS_TOKEN_PAGINATOR_ROWS = "accessToken.paginator.rows";

    public static final String PREF_WORKFLOW_FORM_PAGINATOR_ROWS = "role.paginator.workflow.form";

    public static final String PREF_RESOURCES_PAGINATOR_ROWS = "resources.paginator.rows";

    public static final String PREF_CONNECTORS_PAGINATOR_ROWS = "connectors.paginator.rows";

    public static final String PREF_NOTIFICATION_PAGINATOR_ROWS = "notification.paginator.rows";

    public static final String PREF_MAIL_TEMPLATE_PAGINATOR_ROWS = "mail.template.paginator.rows";

    public static final String PREF_PROPAGATION_TASKS_PAGINATOR_ROWS = "proagationtasks.paginator.rows";

    public static final String PREF_REPORT_TASKS_PAGINATOR_ROWS = "report.paginator.rows";

    public static final String PREF_REPORTLET_TASKS_PAGINATOR_ROWS = "reportlet.paginator.rows";

    public static final String PREF_POLICY_RULE_PAGINATOR_ROWS = "policy.rules.paginator.rows";

    public static final String PREF_POLICY_PAGINATOR_ROWS = "policy.paginator.rows";

    public static final String PREF_TASK_EXECS_PAGINATOR_ROWS = "task.execs.paginator.rows";

    public static final String PREF_NOTIFICATION_TASKS_PAGINATOR_ROWS = "notificationtasks.paginator.rows";

    public static final String PREF_SCHED_TASKS_PAGINATOR_ROWS = "schedtasks.paginator.rows";

    public static final String PREF_PULL_TASKS_PAGINATOR_ROWS = "pulltasks.paginator.rows";

    public static final String PREF_PUSH_TASKS_PAGINATOR_ROWS = "pushtasks.paginator.rows";

    public static final String PREF_TYPE_EXTENSIONS_PAGINATOR_ROWS = "typeextensions.paginator.rows";

    public static final String PREF_TODO_PAGINATOR_ROWS = "todo.paginator.rows";

    public static final String PREF_REPORT_PAGINATOR_ROWS = "report.paginator.rows";

    public static final String PAGEPARAM_CREATE = "CREATE";

    public static final String PAGEPARAM_CURRENT_PAGE = "_current_page";

    public static final String PREF_ANY_OBJECT_DETAILS_VIEW = "anyobject.%s.details.view";

    public static final String PREF_ANY_OBJECT_PLAIN_ATTRS_VIEW = "anyobject.%s.plain.attrs.view";

    public static final String PREF_ANY_OBJECT_DER_ATTRS_VIEW = "anyobject.%s.der.attrs.view";

    public static final String CREATED_ICON = "glyphicon glyphicon-ok-circle";

    public static final String SUSPENDED_ICON = "glyphicon glyphicon-ban-circle";

    public static final String ACTIVE_ICON = "glyphicon glyphicon-ok-circle";

    public static final String UNDEFINED_ICON = "glyphicon glyphicon-question-sign";

    public static final String NOT_FOUND_ICON = "glyphicon glyphicon-remove-circle";

    /**
     * ConnId's GuardedString is not in the classpath.
     */
    public static final String GUARDED_STRING = "org.identityconnectors.common.security.GuardedString";

    /**
     * ConnId's GuardedByteArray is not in the classpath.
     */
    public static final String GUARDED_BYTE_ARRAY = "org.identityconnectors.common.security.GuardedByteArray";

    public static Component getJEXLPopover(final Component caller, final TooltipConfig.Placement placement) {
        return new Label("jexlInfo", Model.of()).add(new PopoverBehavior(
                Model.<String>of(),
                Model.of(caller.getString("jexl_info")
                        + "<ul>"
                        + "<li>" + caller.getString("jexl_ex1") + "</li>"
                        + "<li>" + caller.getString("jexl_ex2") + "</li>"
                        + "</ul>"
                        + "<a href='https://commons.apache.org/proper/commons-jexl/reference/index.html' "
                        + "target='_blank'>" + caller.getString("jexl_syntax_url") + "</a>"),
                new PopoverConfig().withHtml(true).withPlacement(placement)) {

            private static final long serialVersionUID = -7867802555691605021L;

            @Override
            protected String createRelAttribute() {
                return "jexlInfo";
            }
        });
    }

    private Constants() {
        // private constructor for static utility class
    }
}
