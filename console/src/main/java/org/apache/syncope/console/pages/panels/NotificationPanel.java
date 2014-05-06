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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

public class NotificationPanel extends FeedbackPanel {

    private static final long serialVersionUID = 5895940553202128621L;

    private final String cssClass = "notificationpanel";

    private String additionalCSSClass = "notificationpanel_top_right";

    // Create a notifcation panel with the default additional class, specified as a field variable
    public NotificationPanel(final String id) {
        super(id);

        init(id, additionalCSSClass);
    }

    // Create a notifcation panel with a custom additional class, overwriting the field variable
    public NotificationPanel(final String id, final String additionalCSSClass) {
        super(id);

        this.additionalCSSClass = additionalCSSClass;

        init(id, additionalCSSClass);
    }

    public NotificationPanel(final String id, final String additionalCSSClass, final  IFeedbackMessageFilter 
            feedbackMessageFilter) {
        super(id, feedbackMessageFilter);

        this.additionalCSSClass = additionalCSSClass;

        init(id, additionalCSSClass);
    }

    private void init(final String id, final String additionalCSSClass) {
        // set custom markup id and ouput it, to find the component later on in the js function
        setMarkupId(id);
        setOutputMarkupId(true);

        // Add the additional cssClass and hide the element by default
        add(new AttributeModifier("class", new Model<String>(cssClass + " " + additionalCSSClass)));
        add(new AttributeModifier("style", new Model<String>("opacity: 0;")));
    }

    /**
     * Method to refresh the notification panel
     *
     * if there are any feedback messages for the user, find the gravest level, format the notification panel
     * accordingly and show it
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
                    new Model<String>(cssClass
                            + " " + additionalCSSClass
                            + " notificationpanel_border_" + String.valueOf(highestFeedbackLevel))));

            // refresh the panel and call the js function with the panel markup id 
            // and the total count of messages
            target.add(this);
            target.appendJavaScript("showNotification('" + getMarkupId() + "', " + getCurrentMessages().size() + ");");
        }
    }

    /**
     * Returns css class for the single rows of the panel
     * @param message
     */
    @Override
    protected String getCSSClass(final FeedbackMessage message) {
        return "notificationpanel_row_" + message.getLevelAsString();
    }
}
