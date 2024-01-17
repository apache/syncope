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
package org.apache.syncope.client.console.widgets;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import java.io.Serializable;
import java.util.List;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AlertWidget<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 7667120094526529934L;

    protected static final Logger LOG = LoggerFactory.getLogger(AlertWidget.class);

    protected static final int MAX_SIZE = 5;

    protected final Label linkAlertsNumber;

    protected final Label headerAlertsNumber;

    protected final WebMarkupContainer latestAlertsList;

    protected IModel<List<T>> latestAlerts;

    public AlertWidget(final String id) {
        super(id);
        this.latestAlerts = getLatestAlerts();

        setOutputMarkupId(true);

        final LoadableDetachableModel<Long> size = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 7474274077691068779L;

            @Override
            protected Long load() {
                return getLatestAlertsSize();
            }
        };

        add(getIcon("icon"));

        linkAlertsNumber = new Label("alerts", size) {

            private static final long serialVersionUID = 4755868673082976208L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                boolean warning = false;
                try {
                    warning = Integer.parseInt(getDefaultModelObject().toString()) > 0;
                } catch (Exception e) {
                    LOG.error("Invalid value found: {}", getDefaultModelObject(), e);
                }

                if (warning) {
                    tag.put("class", "navbar-badge badge text-bg-danger");
                } else {
                    tag.put("class", "navbar-badge badge text-bg-success");
                }
            }
        };
        add(linkAlertsNumber.setOutputMarkupId(true));

        headerAlertsNumber = new Label("number", size);
        headerAlertsNumber.setOutputMarkupId(true);
        add(headerAlertsNumber);

        add(getEventsLink("alertsLink"));

        latestAlertsList = new WebMarkupContainer("latestAlertsList");
        latestAlertsList.setOutputMarkupId(true);
        add(latestAlertsList);
    }

    protected long getLatestAlertsSize() {
        return latestAlerts.getObject().size();
    }

    protected abstract IModel<List<T>> getLatestAlerts();

    protected abstract AbstractLink getEventsLink(String linkid);

    protected abstract Icon getIcon(String iconid);

    public static class AlertLink<T> extends Panel {

        private static final long serialVersionUID = -6011939604125512766L;

        public AlertLink(final String id, final T alert) {
            super(id);
            add(new Label("alert", alert.toString()));
        }
    }
}
