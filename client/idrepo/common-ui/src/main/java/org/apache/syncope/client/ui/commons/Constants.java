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
package org.apache.syncope.client.ui.commons;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

public final class Constants {

    public static final String SYNCOPE = "syncope";

    public static final String UNKNOWN = "UNKNOWN";

    public static final String ROLE_AUTHENTICATED = "AUTHENTICATED";

    public static final String MENU_COLLAPSE = "MENU_COLLAPSE";

    public static final String VEIL_INDICATOR_MARKUP_ID = "veil";

    public static final String MODELER_CONTEXT = "modelerContext";

    public static final String MODEL_ID_PARAM = "modelId";

    public static final String KEY_FIELD_NAME = "key";

    public static final String SCHEMA_FIELD_NAME = "schema";

    public static final String DESCRIPTION_FIELD_NAME = "description";

    public static final String USERNAME_FIELD_NAME = "username";

    public static final String NAME_FIELD_NAME = "name";

    public static final String DEFAULT_TOKEN_FIELD_NAME = "token";

    public static final String ON_CLICK = "click";

    public static final String ON_DOUBLE_CLICK = "dblclick";

    public static final String ON_CHANGE = "change";

    public static final String ON_KEYUP = "keyup";

    public static final String ON_KEYDOWN = "keydown";

    public static final String ON_BLUR = "blur";

    public static final String PNG_EXT = ".png";

    public static final String FEEDBACK = "feedback";

    public static final String OPERATION_SUCCEEDED = "operation_succeeded";

    public static final String OPERATION_ERROR = "operation_error";

    public static final String OPERATION_NO_OP = "operation_no_op";

    public static final String CAPTCHA_ERROR = "captcha_error";

    public static final String SEARCH_ERROR = "search_error";

    public static final String UNEXPECTED_CONDITION_ERROR = "unexpected_condition_error";

    public static final String ERROR = "error";

    public static final String OUTER = "outer";

    public static final String ACTION = "action";

    public static final String CONFIRM_DELETE = "confirmDelete";

    public static final String BEFORE_LOGOUT_PAGE = "beforeLogoutPage";

    public static final String PARAM_PASSWORD_RESET_TOKEN = "pwdResetToken";

    public static final String PAGEPARAM_CREATE = "CREATE";

    public static final String PAGEPARAM_CURRENT_PAGE = "_current_page";

    public static final String PREF_ANY_DETAILS_VIEW = "any.%s.details.view";

    public static final String PREF_ANY_PLAIN_ATTRS_VIEW = "any.%s.plain.attrs.view";

    public static final String PREF_ANY_DER_ATTRS_VIEW = "any.%s.der.attrs.view";

    public static final String CREATED_ICON = "far fa-check-circle";

    public static final String SUSPENDED_ICON = "fas fa-ban";

    public static final String ACTIVE_ICON = "far fa-check-circle";

    public static final String UNDEFINED_ICON = "fas fa-question-circle";

    public static final String NOT_FOUND_ICON = "fas fa-minus-circle";

    public static final String WARNING_ICON = "fas fa-exclamation-circle";

    public static final int MAX_GROUP_LIST_SIZE = 30;

    public static final int MAX_ROLE_LIST_SIZE = 30;

    public static final String NOTIFICATION_TITLE_PARAM = "notificationTitle";

    public static final String NOTIFICATION_MSG_PARAM = "notificationMessage";

    public static final String NOTIFICATION_LEVEL_PARAM = "notificationLevel";

    public static final String ENDUSER_ANYLAYOUT = "enduser.anylayout";

    public static final String CONTENT_ID = "content";

    public static Component getJEXLPopover(final Component caller, final TooltipConfig.Placement placement) {
        return getJEXLPopover(caller, placement, caller.getString("jexl_ex1"), caller.getString("jexl_ex2"));
    }

    public static Component getJEXLPopover(
            final Component caller,
            final TooltipConfig.Placement placement,
            final String... jexlExamples) {

        StringBuilder body = new StringBuilder(caller.getString("jexl_info")).
                append("<ul>");
        for (String jexlExample : jexlExamples) {
            body.append("<li>").append(jexlExample).append("</li>");
        }
        body.append("</ul>").
                append("<a href='https://commons.apache.org/proper/commons-jexl/reference/' target='_blank'>").
                append(caller.getString("jexl_syntax_url")).
                append("</a>");

        return new Label("jexlInfo", Model.of()).add(new PopoverBehavior(
                Model.of(),
                Model.of(body.toString()),
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
