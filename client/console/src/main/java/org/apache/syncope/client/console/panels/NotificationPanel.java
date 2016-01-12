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
package org.apache.syncope.client.console.panels;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.StyledNotificationBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.feedback.FeedbackMessage;

public class NotificationPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5895940553202128621L;

    private final Notification notification;

    private StyledNotificationBehavior behavior;

    public NotificationPanel(final String id) {
        super(id);

        //element.kendoNotification.widget.fn.options.autoHideAfter
        final Options options = new Options();
        options.set("position", "{ pinned: true }");
        options.set("templates",
                "[ { type: 'success', template: $('#successTemplate').html() },"
                + " { type: 'info', template: $('#successTemplate').html() },"
                + " { type: 'error', template: $('#errorTemplate').html() },"
                + " { type: 'warning', template: $('#errorTemplate').html() } ] ");

        notification = new Notification(Constants.FEEDBACK, options) {

            private static final long serialVersionUID = 785830249476640904L;

            @Override
            public StyledNotificationBehavior newWidgetBehavior(final String selector) {
                behavior = new StyledNotificationBehavior(selector, options);
                return behavior;
            }
        };

        add(notification);
    }

    public void refresh(final AjaxRequestTarget target) {
        if (anyMessage()) {
            for (FeedbackMessage message : getCurrentMessages()) {
                if (message.isError()) {
                    notification.error(target, message.getMessage());
                } else if (message.isSuccess() || message.isInfo()) {
                    notification.success(target, message.getMessage());
                } else {
                    notification.warn(target, message.getMessage());
                }
            }
        }
    }
}
