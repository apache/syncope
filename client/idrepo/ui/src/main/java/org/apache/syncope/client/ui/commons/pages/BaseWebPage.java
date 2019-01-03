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
package org.apache.syncope.client.ui.commons.pages;

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseWebPage extends WebPage implements IAjaxIndicatorAware {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseWebPage.class);

    private static final long serialVersionUID = 761365360759466774L;

    protected final WebMarkupContainer body;

    protected NotificationPanel notificationPanel;

    public BaseWebPage() {
        this(null);
    }

    public BaseWebPage(final PageParameters parameters) {
        super(parameters);

        body = new WebMarkupContainer("body");

        notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        body.addOrReplace(notificationPanel.setOutputMarkupId(true));
        add(body);
    }

    public NotificationPanel getNotificationPanel() {
        return notificationPanel;
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return Constants.VEIL_INDICATOR_MARKUP_ID;
    }

}
