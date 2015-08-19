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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

public class NotificationPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5895940553202128621L;

    public NotificationPanel(final String id) {
        this(id, null);
    }

    public NotificationPanel(
            final String id, final IFeedbackMessageFilter feedbackMessageFilter) {

        super(id, feedbackMessageFilter);

        this.add(new AjaxEventBehavior(Constants.ON_CLICK) {

            private static final long serialVersionUID = -7133385027739964990L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.appendJavaScript("setTimeout(\"$('div#" + getMarkupId() + "').fadeOut('normal')\", 0);");
            }
        });

        setOutputMarkupId(true);

        this.add(new AttributeModifier("class", new Model<>("alert")));
        this.add(new AttributeModifier("style", new Model<>("opacity: 0;")));
    }

    private String getCSSClass(final int level) {
        return level == FeedbackMessage.SUCCESS
                ? "alert alert-success"
                : level == FeedbackMessage.INFO
                        ? "alert alert-info"
                        : level == FeedbackMessage.WARNING
                                ? "alert alert-warning"
                                : "alert alert-danger";
    }

    /**
     * Method to refresh the notification panel.
     *
     * If there are any feedback messages for the user, find the gravest level, format the notification panel
     * accordingly and show it.
     *
     * @param target AjaxRequestTarget to add panel and the calling javascript function
     */
    public void refresh(final AjaxRequestTarget target) {
        // any feedback at all in the current form?
        if (anyMessage()) {
            int highestFeedbackLevel = FeedbackMessage.INFO;

            // any feedback with the given level?
            if (anyMessage(FeedbackMessage.WARNING)) {
                highestFeedbackLevel = FeedbackMessage.WARNING;
            }
            if (anyMessage(FeedbackMessage.ERROR)) {
                highestFeedbackLevel = FeedbackMessage.ERROR;
            }

            // add the css classes to the notification panel,
            add(new AttributeModifier("class", new Model<>(getCSSClass(highestFeedbackLevel))));

            // refresh the panel and call the js function with the panel markup id 
            // and the total count of messages
            target.add(this);

            if (anyMessage(FeedbackMessage.ERROR)) {
                target.appendJavaScript("$('div#" + getMarkupId() + "').fadeTo('normal', 1.0);");
            } else {
                target.appendJavaScript(
                        "showNotification('" + getMarkupId() + "', " + getCurrentMessages().size() + ");");
            }
        }
    }

    @Override
    protected String getCSSClass(final FeedbackMessage message) {
        return "notificationpanel_row";
    }
}
