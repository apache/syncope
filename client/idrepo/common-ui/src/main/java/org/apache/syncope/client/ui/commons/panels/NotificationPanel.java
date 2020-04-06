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
package org.apache.syncope.client.ui.commons.panels;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.notification.Notification;
import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.StyledNotificationBehavior;
import org.apache.wicket.IGenericComponent;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessagesModel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class NotificationPanel extends Panel
        implements IFeedback, IGenericComponent<List<FeedbackMessage>, NotificationPanel> {

    private static final long serialVersionUID = 5895940553202128621L;

    private final Notification notification;

    public NotificationPanel(final String id) {
        super(id);

        Options options = new Options();
        options.set("appendTo", "'#appendto'");
        options.set("stacking", "'up'");
        options.set("templates",
                "[ { type: 'success', template: $('#successTemplate').html() },"
                + " { type: 'info', template: $('#infoTemplate').html() },"
                + " { type: 'error', template: $('#errorTemplate').html() },"
                + " { type: 'warning', template: $('#warningTemplate').html() } ] ");

        notification = new Notification(Constants.FEEDBACK, options) {

            private static final long serialVersionUID = 785830249476640904L;

            @Override
            public StyledNotificationBehavior newWidgetBehavior(final String selector) {
                return new StyledNotificationBehavior(selector, options);
            }
        };

        add(notification);
    }

    public final void hide(final IPartialPageRequestHandler handler) {
        this.notification.hide(handler);
    }

    public final void refresh(final IPartialPageRequestHandler handler) {
        this.getModelObject().forEach(message -> {
            if (message.isError()) {
                this.notification.error(handler, message.getMessage());
            } else if (message.isWarning()) {
                // this is necessary before check for success and info in order to show warnings: isSuccess and isInfo
                // return true also in case of warnings ...
                this.notification.warn(handler, message.getMessage());
            } else if (message.isSuccess()) {
                this.notification.success(handler, message.getMessage());
            } else if (message.isInfo()) {
                this.notification.info(handler, message.getMessage());
            } else {
                this.notification.warn(handler, message.getMessage());
            }
            message.markRendered();
        });
    }

    @Override
    protected IModel<?> initModel() {
        return new FeedbackMessagesModel(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel<List<FeedbackMessage>> getModel() {
        return (IModel<List<FeedbackMessage>>) this.getDefaultModel();
    }

    @Override
    public NotificationPanel setModel(final IModel<List<FeedbackMessage>> model) {
        this.setDefaultModel(model);
        return this;
    }

    @Override
    public NotificationPanel setModelObject(final List<FeedbackMessage> object) {
        this.setDefaultModelObject(object);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FeedbackMessage> getModelObject() {
        return (List<FeedbackMessage>) this.getDefaultModelObject();
    }

    public String getNotificationMarkupId() {
        return this.notification.getMarkupId();
    }
    
}
