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

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.SRARouteRestClient;
import org.apache.syncope.client.console.rest.SRAStatistics;
import org.apache.syncope.client.console.rest.SRAStatisticsRestClient;
import org.apache.syncope.client.console.widgets.NumberWidget;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SRAStatisticsPanel extends Panel {

    private static final long serialVersionUID = 23816535591360L;

    protected static final List<Buttons.Type> TYPES = List.of(
            Buttons.Type.Info, Buttons.Type.Success, Buttons.Type.Warning, Buttons.Type.Danger, Buttons.Type.Dark);

    @SpringBean
    protected SRARouteRestClient sraRouteRestClient;

    @SpringBean
    protected SRAStatisticsRestClient sraStatisticsRestClient;

    protected final NumberWidget count;

    protected final NumberWidget totalTime;

    protected final NumberWidget max;

    protected final List<Pair<String, String>> selected = new ArrayList<>();

    protected final LoadableDetachableModel<Map<String, String>> routes = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 9089911876466472133L;

        @Override
        protected Map<String, String> load() {
            return sraRouteRestClient.list().stream().
                    collect(Collectors.toMap(SRARouteTO::getKey, SRARouteTO::getName));
        }
    };

    protected int current;

    public SRAStatisticsPanel(final String id, final List<NetworkService> instances) {
        super(id);

        SRAStatistics stats = sraStatisticsRestClient.get(instances, selected);

        count = new NumberWidget("count", "text-bg-success", stats.getMeasurement("COUNT").orElse(0F),
                "count", "fas fa-pen-nib");
        add(count);

        totalTime = new NumberWidget("totalTime", "bg-info", stats.getMeasurement("TOTAL_TIME").orElse(0F),
                "total time", "fas fa-stopwatch");
        add(totalTime);

        max = new NumberWidget("max", "text-bg-warning", stats.getMeasurement("MAX").orElse(0F),
                "max", "fas fa-greater-than");
        add(max);

        ListView<SRAStatistics.Tag> availableTags = new ListView<>("availableTags", stats.getAvailableTags()) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<SRAStatistics.Tag> tag) {
                String btnCss = next().cssClassName();
                tag.add(new Label("label", tag.getModelObject().getTag()));
                tag.add(new ListView<>("tag", tag.getModelObject().getValues()) {

                    private static final long serialVersionUID = -9112553137618363167L;

                    @Override
                    protected void populateItem(final ListItem<String> value) {
                        AjaxLink<String> valueLink = new AjaxLink<>("valueLink") {

                            private static final long serialVersionUID = 6250423506463465679L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                Pair<String, String> selection =
                                        Pair.of(tag.getModelObject().getTag(), value.getModelObject());
                                if (selected.contains(selection)) {
                                    selected.remove(selection);
                                } else {
                                    selected.add(selection);
                                }

                                SRAStatistics refresh = sraStatisticsRestClient.get(instances, selected);

                                count.refresh(refresh.getMeasurement("COUNT").orElse(0F));
                                totalTime.refresh(refresh.getMeasurement("TOTAL_TIME").orElse(0F));
                                max.refresh(refresh.getMeasurement("MAX").orElse(0F));

                                target.add(count);
                                target.add(totalTime);
                                target.add(max);
                            }

                            @Override
                            protected void onComponentTag(final ComponentTag tag) {
                                super.onComponentTag(tag);
                                tag.append("class", btnCss, " ");
                            }
                        };

                        IModel<String> valueLabel = routes.getObject().containsKey(value.getModelObject())
                                ? Model.of(routes.getObject().get(value.getModelObject()))
                                : value.getModel();
                        valueLink.add(new Label("valueLabel", valueLabel));
                        value.add(valueLink);
                    }
                });
            }
        };
        add(availableTags);
    }

    protected Buttons.Type next() {
        if (current < TYPES.size()) {
            Buttons.Type type = TYPES.get(current);
            current++;
            return type;
        }

        current = 0;
        return next();
    }
}
