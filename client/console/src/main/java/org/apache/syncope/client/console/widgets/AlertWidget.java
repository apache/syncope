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
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconTypeBuilder;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconTypeBuilder.FontAwesomeGraphic;
import java.io.Serializable;
import java.util.List;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AlertWidget<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 7667120094526529934L;

    protected static final Logger LOG = LoggerFactory.getLogger(AlertWidget.class);

    protected final Label linkAlertsNumber;

    protected final Label headerAlertsNumber;

    protected final WebMarkupContainer latestAlertsList;

    protected final ListView<T> latestFive;

    protected IModel<List<T>> latestAlerts;

    public AlertWidget(final String id) {
        super(id);
        this.latestAlerts = getLatestAlerts();

        setOutputMarkupId(true);

        final LoadableDetachableModel<Integer> size = new LoadableDetachableModel<Integer>() {

            private static final long serialVersionUID = 7474274077691068779L;

            @Override
            protected Integer load() {
                return AlertWidget.this.latestAlerts.getObject().size();
            }
        };

        final LoadableDetachableModel<List<T>> items = new LoadableDetachableModel<List<T>>() {

            private static final long serialVersionUID = 7474274077691068779L;

            @Override
            protected List<T> load() {
                final List<T> latest = AlertWidget.this.latestAlerts.getObject();
                return latest.subList(0, latest.size() < 6 ? latest.size() : 5);
            }
        };

        add(getIcon("icon"));

        linkAlertsNumber = new Label("alerts", size) {

            private static final long serialVersionUID = 4755868673082976208L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                if (Integer.parseInt(getDefaultModelObject().toString()) > 0) {
                    tag.put("class", "label label-danger");
                } else {
                    tag.put("class", "label label-info");
                }
            }
        };
        add(linkAlertsNumber.setOutputMarkupId(true));

        headerAlertsNumber = new Label("number", size);
        headerAlertsNumber.setOutputMarkupId(true);
        add(headerAlertsNumber);

        latestAlertsList = new WebMarkupContainer("latestAlertsList");
        latestAlertsList.setOutputMarkupId(true);
        add(latestAlertsList);

        latestFive = new ListView<T>("latestAlerts", items) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<T> item) {
                item.add(getAlertLink("alert", item.getModelObject()).setRenderBodyOnly(true));
            }
        };
        latestAlertsList.add(latestFive.setReuseItems(false).setOutputMarkupId(true));

        add(getEventsLink("alertsLink"));
    }

    protected abstract IModel<List<T>> getLatestAlerts();

    protected Panel getAlertLink(final String panelid, final T alert) {
        return new AlertLink<>(panelid, alert);
    }

    protected abstract AbstractLink getEventsLink(final String linkid);

    protected Icon getIcon(final String iconid) {
        return new Icon(iconid, FontAwesomeIconTypeBuilder.on(FontAwesomeGraphic.flag_o).build());
    }

    public static class AlertLink<T> extends Panel {

        private static final long serialVersionUID = -6011939604125512766L;

        public AlertLink(final String id, final T alert) {
            super(id);
            add(new Label("alert", alert.toString()));
        }
    }

}
