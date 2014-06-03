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
package org.apache.syncope.console.pages.panels;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

public class NotificationPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5895940553202128621L;

    private static final String CSS_CLASS = "notificationpanel";

    private static final String DEFAULT_ADDITIONAL_CSS_CLASS = "notificationpanel_top_right";

    private final String additionalCSSClass;

    public NotificationPanel(final String id) {
        this(id, null, null);
    }

    public NotificationPanel(final String id, final String additionalCSSClass,
            final IFeedbackMessageFilter feedbackMessageFilter) {

        super(id, feedbackMessageFilter);

        this.additionalCSSClass = StringUtils.isBlank(additionalCSSClass)
                ? DEFAULT_ADDITIONAL_CSS_CLASS
                : additionalCSSClass;

        // set custom markup id and ouput it, to find the component later on in the js function
        setMarkupId(id);
        setOutputMarkupId(true);

        // Add the additional cssClass and hide the element by default
        add(new AttributeModifier("class", new Model<String>(this.CSS_CLASS + " " + this.additionalCSSClass)));
        add(new AttributeModifier("style", new Model<String>("opacity: 0;")));
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
            // including the border css which represents the highest level of feedback
            add(new AttributeModifier("class",
                    new Model<String>(CSS_CLASS
                            + " " + additionalCSSClass
                            + " notificationpanel_border_" + highestFeedbackLevel)));

            // refresh the panel and call the js function with the panel markup id 
            // and the total count of messages
            target.add(this);
            if (anyMessage(FeedbackMessage.ERROR)) {
                target.appendJavaScript(
                        "$('div#" + getMarkupId() + "').fadeTo('normal', 1.0);");
            } else {
                target.appendJavaScript(
                        "showNotification('" + getMarkupId() + "', " + getCurrentMessages().size() + ");");
            }
        }
    }

    @Override
    protected String getCSSClass(final FeedbackMessage message) {
        return "notificationpanel_row_" + message.getLevelAsString();
    }
}
