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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.types.GatewayRouteFilter;
import org.apache.syncope.common.lib.types.FilterFactory;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class GatewayRouteFilterPanel extends Panel {

    private static final long serialVersionUID = -4576690020841569281L;

    public GatewayRouteFilterPanel(final String id, final IModel<List<GatewayRouteFilter>> model) {
        super(id);
        setOutputMarkupId(true);

        WebMarkupContainer filterContainer = new WebMarkupContainer("filterContainer");
        filterContainer.setOutputMarkupId(true);
        add(filterContainer);

        filterContainer.add(new Label("factoryInfo", Model.of()).add(new PopoverBehavior(
                Model.<String>of(),
                Model.of(getString("factoryInfo.help")),
                new PopoverConfig().withHtml(true).withPlacement(TooltipConfig.Placement.right)) {

            private static final long serialVersionUID = -7032694831250368230L;

            @Override
            protected String createRelAttribute() {
                return "factoryInfo";
            }
        }));

        ListView<GatewayRouteFilter> filters = new ListView<GatewayRouteFilter>("filters", model) {

            private static final long serialVersionUID = 6741044372185745296L;

            @Override
            protected void populateItem(final ListItem<GatewayRouteFilter> item) {
                GatewayRouteFilter filter = item.getModelObject();

                AjaxDropDownChoicePanel<FilterFactory> factory =
                        new AjaxDropDownChoicePanel<>("factory", "factory", new PropertyModel<>(filter, "factory"));
                factory.setChoices(List.of(FilterFactory.values()));
                item.add(factory.hideLabel());

                AjaxTextFieldPanel args =
                        new AjaxTextFieldPanel("args", "args", new PropertyModel<>(filter, "args"));
                item.add(args.hideLabel());

                ActionsPanel<Serializable> actions = new ActionsPanel<>("actions", null);
                actions.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = 2041211756396714619L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        model.getObject().remove(item.getIndex());

                        item.getParent().removeAll();
                        target.add(GatewayRouteFilterPanel.this);
                    }
                }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();
                if (model.getObject().size() > 1) {
                    if (item.getIndex() > 0) {
                        actions.add(new ActionLink<Serializable>() {

                            private static final long serialVersionUID = 2041211756396714619L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                                GatewayRouteFilter pre = model.getObject().get(item.getIndex() - 1);
                                model.getObject().set(item.getIndex(), pre);
                                model.getObject().set(item.getIndex() - 1, filter);

                                item.getParent().removeAll();
                                target.add(GatewayRouteFilterPanel.this);
                            }
                        }, ActionLink.ActionType.UP, StringUtils.EMPTY).hideLabel();
                    }
                    if (item.getIndex() < model.getObject().size() - 1) {
                        actions.add(new ActionLink<Serializable>() {

                            private static final long serialVersionUID = 2041211756396714619L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                                GatewayRouteFilter post = model.getObject().get(item.getIndex() + 1);
                                model.getObject().set(item.getIndex(), post);
                                model.getObject().set(item.getIndex() + 1, filter);

                                item.getParent().removeAll();
                                target.add(GatewayRouteFilterPanel.this);
                            }
                        }, ActionLink.ActionType.DOWN, StringUtils.EMPTY).hideLabel();
                    }
                }
                item.add(actions);
            }
        };
        filters.setReuseItems(true);
        filterContainer.add(filters);

        IndicatingAjaxButton addFilterBtn = new IndicatingAjaxButton("addFilterBtn") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                model.getObject().add(new GatewayRouteFilter());
                target.add(GatewayRouteFilterPanel.this);
            }
        };
        addFilterBtn.setDefaultFormProcessing(false);
        filterContainer.add(addFilterBtn);
    }
}
