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
package org.apache.syncope.client.console.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public abstract class EventSelectionPanel extends Panel {

    private static final long serialVersionUID = 752233163798301002L;

    private final Set<String> selected = new HashSet<>();

    public EventSelectionPanel(final String id, final EventCategory eventCategory, final IModel<List<String>> model) {
        super(id);
        setOutputMarkupId(true);

        List<String> ops = getOps(eventCategory);

        // needed to avoid model reset: model have to be managed into SelectedEventsPanel
        selected.addAll(model.getObject());

        CheckGroup<String> successGroup = new CheckGroup<>("successGroup", selected);
        successGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                Set<OpEvent> toBeRemoved = new HashSet<>();
                Set<OpEvent> toBeAdded = new HashSet<>();

                getOps(eventCategory).forEach(event -> {
                    OpEvent opEvent = new OpEvent(
                            eventCategory.getType(),
                            eventCategory.getCategory(),
                            eventCategory.getSubcategory(),
                            event,
                            OpEvent.Outcome.SUCCESS);

                    if (successGroup.getModelObject().contains(opEvent.toString())) {
                        toBeAdded.add(opEvent);
                    } else {
                        toBeRemoved.add(opEvent);
                    }
                });

                send(EventSelectionPanel.this.getPage(), Broadcast.BREADTH,
                        new SelectedEventsPanel.EventSelectionChanged(target, toBeAdded, toBeRemoved));
            }
        });
        successGroup.setVisible(!ops.isEmpty());
        add(successGroup);

        add(new Label("successLabel", new ResourceModel("Success", "Success"))).setVisible(!ops.isEmpty());

        CheckGroupSelector successSelector = new CheckGroupSelector("successSelector", successGroup);
        successSelector.setVisible(!ops.isEmpty());
        add(successSelector);

        ListView<String> categoryView = new ListView<>("categoryView", ops) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label("subcategory", Model.of(item.getModelObject())));
            }
        };
        add(categoryView);

        ListView<String> successView = new ListView<>("successView", ops) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check<>("successCheck",
                        new Model<>(OpEvent.toString(
                                eventCategory.getType(),
                                eventCategory.getCategory(),
                                eventCategory.getSubcategory(),
                                item.getModelObject(),
                                OpEvent.Outcome.SUCCESS)),
                        successGroup));
            }
        };
        successGroup.add(successView);

        CheckGroup<String> failureGroup = new CheckGroup<>("failureGroup", selected);
        failureGroup.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                Set<OpEvent> toBeRemoved = new HashSet<>();
                Set<OpEvent> toBeAdded = new HashSet<>();

                getOps(eventCategory).forEach(event -> {
                    OpEvent opEvent = new OpEvent(
                            eventCategory.getType(),
                            eventCategory.getCategory(),
                            eventCategory.getSubcategory(),
                            event,
                            OpEvent.Outcome.FAILURE);

                    if (failureGroup.getModelObject().contains(opEvent.toString())) {
                        toBeAdded.add(opEvent);
                    } else {
                        toBeRemoved.add(opEvent);
                    }
                });

                send(EventSelectionPanel.this.getPage(), Broadcast.BREADTH,
                        new SelectedEventsPanel.EventSelectionChanged(target, toBeAdded, toBeRemoved));
            }
        });
        failureGroup.setVisible(!ops.isEmpty());
        add(failureGroup);

        add(new Label("failureLabel", new ResourceModel("Failure", "Failure"))).setVisible(!ops.isEmpty());

        CheckGroupSelector failureSelector = new CheckGroupSelector("failureSelector", failureGroup);
        failureSelector.setVisible(!ops.isEmpty());
        add(failureSelector);

        ListView<String> failureView = new ListView<>("failureView", ops) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check<>("failureCheck",
                        new Model<>(OpEvent.toString(
                                eventCategory.getType(),
                                eventCategory.getCategory(),
                                eventCategory.getSubcategory(),
                                item.getModelObject(),
                                OpEvent.Outcome.FAILURE)),
                        failureGroup));
            }
        };
        failureGroup.add(failureView);
    }

    private List<String> getOps(final EventCategory eventCategoryTO) {
        List<String> ops = eventCategoryTO.getOps();

        if (ops.isEmpty()) {
            if ((OpEvent.CategoryType.PROPAGATION == eventCategoryTO.getType()
                    || OpEvent.CategoryType.PULL == eventCategoryTO.getType()
                    || OpEvent.CategoryType.PUSH == eventCategoryTO.getType())
                    && StringUtils.isEmpty(eventCategoryTO.getCategory())) {

                ops.add(eventCategoryTO.getType().toString());
            } else if (OpEvent.CategoryType.TASK == eventCategoryTO.getType()
                    && StringUtils.isNotEmpty(eventCategoryTO.getCategory())) {

                ops.add(eventCategoryTO.getCategory());
            } else if (OpEvent.CategoryType.REPORT == eventCategoryTO.getType()
                    && StringUtils.isNotEmpty(eventCategoryTO.getCategory())) {

                ops.add(eventCategoryTO.getCategory());
            }
        } else {
            Collections.sort(ops);
        }

        return ops;
    }

    /**
     * To be extended in order to add actions on events.
     *
     * @param event event.
     */
    protected abstract void onEventAction(IEvent<?> event);

    @Override
    public void onEvent(final IEvent<?> event) {
        onEventAction(event);
    }
}
