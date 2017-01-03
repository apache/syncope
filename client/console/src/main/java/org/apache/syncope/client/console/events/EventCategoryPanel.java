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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.events.SelectedEventsPanel.EventSelectionChanged;
import org.apache.syncope.client.console.events.SelectedEventsPanel.InspectSelectedEvent;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public abstract class EventCategoryPanel extends Panel {

    private static final long serialVersionUID = 6429053774964787734L;

    private final List<EventCategoryTO> eventCategoryTOs;

    private final EventCategoryTO eventCategoryTO = new EventCategoryTO();

    private final WebMarkupContainer categoryContainer;

    private final WebMarkupContainer eventsContainer;

    private final SelectedEventsPanel selectedEventsPanel;

    private final AjaxDropDownChoicePanel<EventCategoryType> type;

    private final AjaxDropDownChoicePanel<String> category;

    private final AjaxDropDownChoicePanel<String> subcategory;

    private final AjaxTextFieldPanel custom;

    private final ActionLinksPanel<EventCategoryTO> actionLinksPanel;

    private final IModel<List<String>> model;

    public EventCategoryPanel(
            final String id,
            final List<EventCategoryTO> eventCategoryTOs,
            final IModel<List<String>> model) {

        super(id);

        this.model = model;
        selectedEventsPanel = new SelectedEventsPanel("selectedEventsPanel", model);
        add(selectedEventsPanel);

        this.eventCategoryTOs = eventCategoryTOs;

        categoryContainer = new WebMarkupContainer("categoryContainer");
        categoryContainer.setOutputMarkupId(true);
        add(categoryContainer);

        eventsContainer = new WebMarkupContainer("eventsContainer");
        eventsContainer.setOutputMarkupId(true);
        add(eventsContainer);

        authorizeList();
        authorizeChanges();

        type = new AjaxDropDownChoicePanel<>(
                "type",
                "type",
                new PropertyModel<EventCategoryType>(eventCategoryTO, "type"),
                false);
        type.setChoices(Arrays.asList(EventCategoryType.values()));
        type.setStyleSheet("ui-widget-content ui-corner-all");
        type.setChoiceRenderer(new IChoiceRenderer<EventCategoryType>() {

            private static final long serialVersionUID = 2317134950949778735L;

            @Override
            public String getDisplayValue(final EventCategoryType eventCategoryType) {
                return eventCategoryType.name();
            }

            @Override
            public String getIdValue(final EventCategoryType eventCategoryType, final int i) {
                return eventCategoryType.name();
            }

            @Override
            public EventCategoryType getObject(
                    final String id, final IModel<? extends List<? extends EventCategoryType>> choices) {
                return IterableUtils.find(choices.getObject(), new Predicate<EventCategoryType>() {

                    @Override
                    public boolean evaluate(final EventCategoryType object) {
                        return object.name().equals(id);
                    }
                });
            }
        });
        categoryContainer.add(type);

        type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(EventCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, type));
            }
        });

        category = new AjaxDropDownChoicePanel<>(
                "category",
                "category",
                new PropertyModel<String>(eventCategoryTO, "category"),
                false);
        category.setChoices(filter(eventCategoryTOs, type.getModelObject()));
        categoryContainer.add(category);

        category.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306811L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(EventCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, category));
            }
        });

        subcategory = new AjaxDropDownChoicePanel<>(
                "subcategory",
                "subcategory",
                new PropertyModel<String>(eventCategoryTO, "subcategory"),
                false);
        subcategory.setChoices(filter(eventCategoryTOs, type.getModelObject(), category.getModelObject()));
        categoryContainer.add(subcategory);

        subcategory.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306812L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(EventCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, subcategory));
            }
        });

        categoryContainer.add(new Label("customLabel", new ResourceModel("custom", "custom")).setVisible(false));

        custom = new AjaxTextFieldPanel("custom", "custom", new Model<String>(null));
        custom.setVisible(false);
        custom.setEnabled(false);

        categoryContainer.add(custom.hideLabel());

        actionLinksPanel = ActionLinksPanel.<EventCategoryTO>builder().
                add(new ActionLink<EventCategoryTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final EventCategoryTO modelObject) {
                        if (StringUtils.isNotBlank(custom.getModelObject())) {
                            Map.Entry<EventCategoryTO, AuditElements.Result> parsed =
                                    AuditLoggerName.parseEventCategory(custom.getModelObject());

                            String eventString = AuditLoggerName.buildEvent(
                                    parsed.getKey().getType(),
                                    null,
                                    null,
                                    parsed.getKey().getEvents().isEmpty()
                                    ? StringUtils.EMPTY : parsed.getKey().getEvents().iterator().next(),
                                    parsed.getValue());

                            custom.setModelObject(StringUtils.EMPTY);
                            send(EventCategoryPanel.this.getPage(), Broadcast.BREADTH, new EventSelectionChanged(
                                    target,
                                    Collections.<String>singleton(eventString),
                                    Collections.<String>emptySet()));
                            target.add(categoryContainer);
                        }
                    }
                }, ActionLink.ActionType.CREATE).
                add(new ActionLink<EventCategoryTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final EventCategoryTO modelObject) {
                        if (StringUtils.isNotBlank(custom.getModelObject())) {
                            Pair<EventCategoryTO, AuditElements.Result> parsed =
                                    AuditLoggerName.parseEventCategory(custom.getModelObject());

                            String eventString = AuditLoggerName.buildEvent(
                                    parsed.getKey().getType(),
                                    null,
                                    null,
                                    parsed.getKey().getEvents().isEmpty()
                                    ? StringUtils.EMPTY : parsed.getKey().getEvents().iterator().next(),
                                    parsed.getValue());

                            custom.setModelObject(StringUtils.EMPTY);
                            send(EventCategoryPanel.this.getPage(), Broadcast.BREADTH, new EventSelectionChanged(
                                    target,
                                    Collections.<String>singleton(eventString),
                                    Collections.<String>emptySet()));
                            target.add(categoryContainer);
                        }
                    }
                }, ActionLink.ActionType.CREATE).
                add(new ActionLink<EventCategoryTO>() {

                    private static final long serialVersionUID = -3722207913631435521L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final EventCategoryTO modelObject) {
                        if (StringUtils.isNotBlank(custom.getModelObject())) {
                            Pair<EventCategoryTO, AuditElements.Result> parsed =
                                    AuditLoggerName.parseEventCategory(custom.getModelObject());

                            String eventString = AuditLoggerName.buildEvent(
                                    parsed.getKey().getType(),
                                    null,
                                    null,
                                    parsed.getKey().getEvents().isEmpty()
                                    ? StringUtils.EMPTY : parsed.getKey().getEvents().iterator().next(),
                                    parsed.getValue());

                            custom.setModelObject(StringUtils.EMPTY);
                            send(EventCategoryPanel.this.getPage(), Broadcast.BREADTH, new EventSelectionChanged(
                                    target,
                                    Collections.<String>emptySet(),
                                    Collections.<String>singleton(eventString)));
                            target.add(categoryContainer);
                        }
                    }
                }, ActionLink.ActionType.DELETE).build("customActions");

        categoryContainer.add(actionLinksPanel);

        actionLinksPanel.setVisible(false);
        actionLinksPanel.setEnabled(false);

        eventsContainer.add(new EventSelectionPanel("eventsPanel", eventCategoryTO, model) {

            private static final long serialVersionUID = 3513194801190026082L;

            @Override
            protected void onEventAction(final IEvent<?> event) {
                EventCategoryPanel.this.onEventAction(event);
            }
        });
    }

    private List<String> filter(final List<EventCategoryTO> eventCategoryTOs, final EventCategoryType type) {
        Set<String> res = new HashSet<>();

        for (EventCategoryTO eventCategory : eventCategoryTOs) {
            if (type == eventCategory.getType() && StringUtils.isNotEmpty(eventCategory.getCategory())) {
                res.add(eventCategory.getCategory());
            }
        }

        List<String> filtered = new ArrayList<>(res);
        Collections.sort(filtered);
        return filtered;
    }

    private List<String> filter(
            final List<EventCategoryTO> eventCategoryTOs, final EventCategoryType type, final String category) {

        Set<String> res = new HashSet<>();

        for (EventCategoryTO eventCategory : eventCategoryTOs) {
            if (type == eventCategory.getType() && StringUtils.equals(category, eventCategory.getCategory())
                    && StringUtils.isNotEmpty(eventCategory.getSubcategory())) {
                res.add(eventCategory.getSubcategory());
            }
        }

        List<String> filtered = new ArrayList<>(res);
        Collections.sort(filtered);
        return filtered;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ChangeCategoryEvent) {
            // update objects ....
            eventCategoryTO.getEvents().clear();

            final ChangeCategoryEvent change = (ChangeCategoryEvent) event.getPayload();

            final Panel changedPanel = change.getChangedPanel();
            if (null != changedPanel.getId()) {
                switch (changedPanel.getId()) {
                    case "type":
                        eventCategoryTO.setType(type.getModelObject());
                        eventCategoryTO.setCategory(null);
                        eventCategoryTO.setSubcategory(null);
                        if (type.getModelObject() == EventCategoryType.CUSTOM) {
                            category.setChoices(Collections.<String>emptyList());
                            subcategory.setChoices(Collections.<String>emptyList());
                            category.setEnabled(false);
                            subcategory.setEnabled(false);
                            custom.setVisible(true);
                            custom.setEnabled(true);
                            actionLinksPanel.setVisible(true);
                            actionLinksPanel.setEnabled(true);
                        } else {
                            category.setChoices(filter(eventCategoryTOs, type.getModelObject()));
                            subcategory.setChoices(Collections.<String>emptyList());
                            category.setEnabled(true);
                            subcategory.setEnabled(true);
                            custom.setVisible(false);
                            custom.setEnabled(false);
                            actionLinksPanel.setVisible(false);
                            actionLinksPanel.setEnabled(false);
                        }
                        change.getTarget().add(categoryContainer);
                        break;

                    case "category":
                        subcategory.setChoices(
                                filter(eventCategoryTOs, type.getModelObject(), category.getModelObject()));
                        eventCategoryTO.setCategory(category.getModelObject());
                        eventCategoryTO.setSubcategory(null);
                        change.getTarget().add(categoryContainer);
                        break;

                    default:
                        eventCategoryTO.setSubcategory(subcategory.getModelObject());
                        break;
                }
            }

            updateEventsContainer(change.getTarget());
        } else if (event.getPayload() instanceof InspectSelectedEvent) {
            // update objects ....
            eventCategoryTO.getEvents().clear();

            final InspectSelectedEvent inspectSelectedEvent = (InspectSelectedEvent) event.getPayload();

            final Map.Entry<EventCategoryTO, AuditElements.Result> categoryEvent = AuditLoggerName.parseEventCategory(
                    inspectSelectedEvent.getEvent());

            eventCategoryTO.setType(categoryEvent.getKey().getType());
            category.setChoices(filter(eventCategoryTOs, type.getModelObject()));

            eventCategoryTO.setCategory(categoryEvent.getKey().getCategory());
            subcategory.setChoices(filter(eventCategoryTOs, type.getModelObject(), category.getModelObject()));

            eventCategoryTO.setSubcategory(categoryEvent.getKey().getSubcategory());

            if (categoryEvent.getKey().getType() == EventCategoryType.CUSTOM) {
                custom.setModelObject(AuditLoggerName.buildEvent(
                        categoryEvent.getKey().getType(),
                        categoryEvent.getKey().getCategory(),
                        categoryEvent.getKey().getSubcategory(),
                        categoryEvent.getKey().getEvents().isEmpty()
                        ? StringUtils.EMPTY : categoryEvent.getKey().getEvents().iterator().next(),
                        categoryEvent.getValue()));

                category.setEnabled(false);
                subcategory.setEnabled(false);
                custom.setVisible(true);
                custom.setEnabled(true);
                actionLinksPanel.setVisible(true);
                actionLinksPanel.setEnabled(true);
            } else {
                category.setEnabled(true);
                subcategory.setEnabled(true);
                custom.setVisible(false);
                custom.setEnabled(false);
                actionLinksPanel.setVisible(false);
                actionLinksPanel.setEnabled(false);
            }

            inspectSelectedEvent.getTarget().add(categoryContainer);
            updateEventsContainer(inspectSelectedEvent.getTarget());
        }
    }

    private void setEvents() {
        final Iterator<EventCategoryTO> itor = eventCategoryTOs.iterator();
        while (itor.hasNext() && eventCategoryTO.getEvents().isEmpty()) {
            final EventCategoryTO eventCategory = itor.next();
            if (eventCategory.getType() == eventCategoryTO.getType()
                    && StringUtils.equals(eventCategory.getCategory(), eventCategoryTO.getCategory())
                    && StringUtils.equals(eventCategory.getSubcategory(), eventCategoryTO.getSubcategory())) {
                eventCategoryTO.getEvents().addAll(eventCategory.getEvents());

            }
        }
    }

    private static class ChangeCategoryEvent {

        private final AjaxRequestTarget target;

        private final Panel changedPanel;

        ChangeCategoryEvent(final AjaxRequestTarget target, final Panel changedPanel) {
            this.target = target;
            this.changedPanel = changedPanel;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Panel getChangedPanel() {
            return changedPanel;
        }
    }

    /**
     * To be extended in order to add actions on events.
     *
     * @param event event.
     */
    protected void onEventAction(final IEvent<?> event) {
        // nothing by default
    }

    private void authorizeList() {
        for (String role : getListAuthRoles()) {
            MetaDataRoleAuthorizationStrategy.authorize(selectedEventsPanel, RENDER, role);
        }
    }

    private void authorizeChanges() {
        for (String role : getChangeAuthRoles()) {
            MetaDataRoleAuthorizationStrategy.authorize(categoryContainer, RENDER, role);
            MetaDataRoleAuthorizationStrategy.authorize(eventsContainer, RENDER, role);
        }
    }

    private void updateEventsContainer(final AjaxRequestTarget target) {
        setEvents();

        eventsContainer.addOrReplace(new EventSelectionPanel("eventsPanel", eventCategoryTO, model) {

            private static final long serialVersionUID = 3513194801190026082L;

            @Override
            public void onEventAction(final IEvent<?> event) {
                EventCategoryPanel.this.onEventAction(event);
            }
        });
        target.add(eventsContainer);
    }

    protected abstract List<String> getListAuthRoles();

    protected abstract List<String> getChangeAuthRoles();
}
