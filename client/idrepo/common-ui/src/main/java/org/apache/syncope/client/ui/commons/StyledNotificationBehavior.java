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

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import com.googlecode.wicket.kendo.ui.widget.notification.NotificationBehavior;
import java.io.Serializable;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;

public class StyledNotificationBehavior extends NotificationBehavior {

    private static final long serialVersionUID = -3985689554352173472L;

    private static final String AUTOHIDEAFTER_GOOD = "3000";

    private static final String AUTOHIDEAFTER_BAD = "0";

    public StyledNotificationBehavior(final String selector, final Options options) {
        super(selector, options);
    }

    @Override
    public void show(final IPartialPageRequestHandler handler, final Serializable message, final String level) {
        if (handler != null) {
            handler.appendJavaScript(jQueryShow(this.format(String.valueOf(message), level), this.widget(), level));
        }
    }

    public static String jQueryShow(final CharSequence message, final String widget, final String level) {
        return String.format("%s.options.autoHideAfter = %s; %s.show( { message: '%s' } , '%s');",
                widget,
                Notification.SUCCESS.equalsIgnoreCase(level) || Notification.INFO.equalsIgnoreCase(level)
                ? AUTOHIDEAFTER_GOOD : AUTOHIDEAFTER_BAD,
                widget,
                message,
                level.toLowerCase());
    }
}
