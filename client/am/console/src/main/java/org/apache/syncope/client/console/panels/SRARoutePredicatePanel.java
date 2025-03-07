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
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateCond;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
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

public class SRARoutePredicatePanel extends Panel {

    private static final long serialVersionUID = -5321936363511301735L;

    public SRARoutePredicatePanel(final String id, final IModel<List<SRARoutePredicate>> model) {
        super(id);
        setOutputMarkupId(true);

        WebMarkupContainer predicateContainer = new WebMarkupContainer("predicateContainer");
        predicateContainer.setOutputMarkupId(true);
        add(predicateContainer);

        predicateContainer.add(new Label("factoryInfo", Model.of()).add(new PopoverBehavior(
                Model.of(),
                Model.of(getString("factoryInfo.help")),
                new PopoverConfig().withHtml(true).withPlacement(TooltipConfig.Placement.right)) {

            private static final long serialVersionUID = -7032694831250368230L;

            @Override
            protected String createRelAttribute() {
                return "factoryInfo";
            }
        }));

        ListView<SRARoutePredicate> predicates = new ListView<>("predicates", model) {

            private static final long serialVersionUID = 1814616131938968887L;

            @Override
            protected void populateItem(final ListItem<SRARoutePredicate> item) {
                SRARoutePredicate predicate = item.getModelObject();

                AjaxCheckBoxPanel negate =
                    new AjaxCheckBoxPanel("negate", "negate", new PropertyModel<>(predicate, "negate"));
                item.add(negate.hideLabel());

                AjaxDropDownChoicePanel<SRARoutePredicateFactory> factory =
                    new AjaxDropDownChoicePanel<>("factory", "factory", new PropertyModel<>(predicate, "factory"));
                factory.setChoices(List.of(SRARoutePredicateFactory.values()));
                item.add(factory.hideLabel());

                AjaxTextFieldPanel args =
                    new AjaxTextFieldPanel("args", "args", new PropertyModel<>(predicate, "args"));
                item.add(args.hideLabel());

                AjaxDropDownChoicePanel<SRARoutePredicateCond> cond =
                    new AjaxDropDownChoicePanel<>("cond", "cond", new PropertyModel<>(predicate, "cond"));
                cond.setChoices(List.of(SRARoutePredicateCond.values()));
                item.add(cond.hideLabel());

                ActionsPanel<Serializable> actions = new ActionsPanel<>("actions", null);
                actions.add(new ActionLink<>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        model.getObject().remove(item.getIndex());

                        item.getParent().removeAll();
                        target.add(SRARoutePredicatePanel.this);
                    }
                }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();
                if (model.getObject().size() > 1) {
                    if (item.getIndex() > 0) {
                        actions.add(new ActionLink<>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                                SRARoutePredicate pre = model.getObject().get(item.getIndex() - 1);
                                model.getObject().set(item.getIndex(), pre);
                                model.getObject().set(item.getIndex() - 1, predicate);

                                item.getParent().removeAll();
                                target.add(SRARoutePredicatePanel.this);
                            }
                        }, ActionLink.ActionType.UP, StringUtils.EMPTY).hideLabel();
                    }
                    if (item.getIndex() < model.getObject().size() - 1) {
                        actions.add(new ActionLink<>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                                SRARoutePredicate post = model.getObject().get(item.getIndex() + 1);
                                model.getObject().set(item.getIndex(), post);
                                model.getObject().set(item.getIndex() + 1, predicate);

                                item.getParent().removeAll();
                                target.add(SRARoutePredicatePanel.this);
                            }
                        }, ActionLink.ActionType.DOWN, StringUtils.EMPTY).hideLabel();
                    }
                }
                item.add(actions);
            }
        };
        predicates.setReuseItems(true);
        predicateContainer.add(predicates);

        IndicatingAjaxButton addPredicateBtn = new IndicatingAjaxButton("addPredicateBtn") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                model.getObject().add(new SRARoutePredicate());
                target.add(SRARoutePredicatePanel.this);
            }
        };
        addPredicateBtn.setDefaultFormProcessing(false);
        predicateContainer.add(addPredicateBtn);
    }
}
