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
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

    public EventSelectionPanel(
            final String id, final EventCategoryTO eventCategoryTO, final IModel<List<String>> model) {
        super(id);
        setOutputMarkupId(true);

        List<String> events = getEvents(eventCategoryTO);

        // needed to avoid model reset: model have to be managed into SelectedEventsPanel
        selected.addAll(model.getObject());

        final CheckGroup<String> successGroup = new CheckGroup<>("successGroup", selected);
        successGroup.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {

                final Set<String> toBeRemoved = new HashSet<>();
                final Set<String> toBeAdded = new HashSet<>();

                for (String event : getEvents(eventCategoryTO)) {
                    String eventString = AuditLoggerName.buildEvent(
                            eventCategoryTO.getType(),
                            eventCategoryTO.getCategory(),
                            eventCategoryTO.getSubcategory(),
                            event,
                            AuditElements.Result.SUCCESS);

                    if (successGroup.getModelObject().contains(eventString)) {
                        toBeAdded.add(eventString);
                    } else {
                        toBeRemoved.add(eventString);
                    }
                }

                send(EventSelectionPanel.this.getPage(), Broadcast.BREADTH,
                        new SelectedEventsPanel.EventSelectionChanged(target, toBeAdded, toBeRemoved));
            }
        });
        successGroup.setVisible(!events.isEmpty());
        add(successGroup);

        add(new Label("successLabel", new ResourceModel("Success", "Success"))).setVisible(!events.isEmpty());

        CheckGroupSelector successSelector = new CheckGroupSelector("successSelector", successGroup);
        successSelector.setVisible(!events.isEmpty());
        add(successSelector);

        ListView<String> categoryView = new ListView<String>("categoryView", events) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String subcategory = item.getModelObject();

                item.add(new Label("subcategory", new ResourceModel(subcategory, subcategory)));
            }
        };
        add(categoryView);

        ListView<String> successView = new ListView<String>("successView", events) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check<>("successCheck",
                        new Model<>(AuditLoggerName.buildEvent(
                                eventCategoryTO.getType(),
                                eventCategoryTO.getCategory(),
                                eventCategoryTO.getSubcategory(),
                                item.getModelObject(),
                                AuditElements.Result.SUCCESS)),
                        successGroup));
            }
        };
        successGroup.add(successView);

        final CheckGroup<String> failureGroup = new CheckGroup<>("failureGroup", selected);
        failureGroup.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                final Set<String> toBeRemoved = new HashSet<>();
                final Set<String> toBeAdded = new HashSet<>();

                for (String event : getEvents(eventCategoryTO)) {
                    final String eventString = AuditLoggerName.buildEvent(
                            eventCategoryTO.getType(),
                            eventCategoryTO.getCategory(),
                            eventCategoryTO.getSubcategory(),
                            event,
                            AuditElements.Result.FAILURE);

                    if (failureGroup.getModelObject().contains(eventString)) {
                        toBeAdded.add(eventString);
                    } else {
                        toBeRemoved.add(eventString);
                    }
                }

                send(EventSelectionPanel.this.getPage(), Broadcast.BREADTH,
                        new SelectedEventsPanel.EventSelectionChanged(target, toBeAdded, toBeRemoved));
            }
        });
        failureGroup.setVisible(!events.isEmpty());
        add(failureGroup);

        add(new Label("failureLabel", new ResourceModel("Failure", "Failure"))).setVisible(!events.isEmpty());

        CheckGroupSelector failureSelector = new CheckGroupSelector("failureSelector", failureGroup);
        failureSelector.setVisible(!events.isEmpty());
        add(failureSelector);

        ListView<String> failureView = new ListView<String>("failureView", events) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Check<>("failureCheck",
                        new Model<>(AuditLoggerName.buildEvent(
                                eventCategoryTO.getType(),
                                eventCategoryTO.getCategory(),
                                eventCategoryTO.getSubcategory(),
                                item.getModelObject(),
                                AuditElements.Result.FAILURE)),
                        failureGroup));
            }
        };
        failureGroup.add(failureView);
    }

    private List<String> getEvents(final EventCategoryTO eventCategoryTO) {
        final List<String> res;

        res = eventCategoryTO.getEvents();

        if (res.isEmpty()) {
            if ((AuditElements.EventCategoryType.PROPAGATION == eventCategoryTO.getType()
                    || AuditElements.EventCategoryType.PULL == eventCategoryTO.getType()
                    || AuditElements.EventCategoryType.PUSH == eventCategoryTO.getType())
                    && StringUtils.isEmpty(eventCategoryTO.getCategory())) {
                res.add(eventCategoryTO.getType().toString());
            } else if (AuditElements.EventCategoryType.TASK == eventCategoryTO.getType()
                    && StringUtils.isNotEmpty(eventCategoryTO.getCategory())) {
                res.add(eventCategoryTO.getCategory());
            }
        } else {
            Collections.sort(res);
        }

        return res;
    }

    /**
     * To be extended in order to add actions on events.
     *
     * @param event event.
     */
    protected abstract void onEventAction(final IEvent<?> event);

    @Override
    public void onEvent(final IEvent<?> event) {
        onEventAction(event);
    }
}
