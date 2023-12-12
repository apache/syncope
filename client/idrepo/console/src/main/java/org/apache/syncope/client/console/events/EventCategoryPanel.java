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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.events.SelectedEventsPanel.EventSelectionChanged;
import org.apache.syncope.client.console.events.SelectedEventsPanel.InspectSelectedEvent;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.audit.EventCategory;
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

    protected static class ChangeCategoryEvent {

        protected final AjaxRequestTarget target;

        protected final Panel changedPanel;

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

    protected static List<String> filter(
            final List<EventCategory> categories,
            final EventCategoryType type) {

        return categories.stream().
                filter(c -> type == c.getType() && StringUtils.isNotEmpty(c.getCategory())).
                map(EventCategory::getCategory).
                distinct().
                sorted().
                collect(Collectors.toList());
    }

    protected static List<String> filter(
            final List<EventCategory> categories,
            final EventCategoryType type,
            final String category) {

        return categories.stream().
                filter(c -> type == c.getType() && StringUtils.equals(category, c.getCategory())).
                map(EventCategory::getCategory).
                distinct().
                sorted().
                collect(Collectors.toList());
    }

    private final List<EventCategory> eventCategories;

    private final EventCategory eventCategory = new EventCategory();

    private final WebMarkupContainer categoryContainer;

    private final WebMarkupContainer eventsContainer;

    private final SelectedEventsPanel selectedEventsPanel;

    private final AjaxDropDownChoicePanel<EventCategoryType> type;

    private final AjaxDropDownChoicePanel<String> category;

    private final AjaxDropDownChoicePanel<String> subcategory;

    private final AjaxTextFieldPanel custom;

    private final ActionsPanel<EventCategory> actionsPanel;

    private final IModel<List<String>> model;

    public EventCategoryPanel(
            final String id,
            final List<EventCategory> eventCategories,
            final IModel<List<String>> model) {

        super(id);

        this.model = model;
        selectedEventsPanel = new SelectedEventsPanel("selectedEventsPanel", model);
        add(selectedEventsPanel);

        this.eventCategories = eventCategories;

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
                new PropertyModel<>(eventCategory, "type"),
                false);
        type.setChoices(eventCategories.stream().
                map(EventCategory::getType).distinct().
                sorted(Comparator.comparing(EventCategoryType::name)).
                collect(Collectors.toList()));
        type.setChoiceRenderer(new IChoiceRenderer<>() {

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
                    final String id,
                    final IModel<? extends List<? extends EventCategoryType>> choices) {

                return choices.getObject().stream().filter(object -> object.name().equals(id)).findAny().orElse(null);
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
                new PropertyModel<>(eventCategory, "category"),
                false);
        category.setChoices(filter(eventCategories, type.getModelObject()));
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
                new PropertyModel<>(eventCategory, "subcategory"),
                false);
        subcategory.setChoices(filter(eventCategories, type.getModelObject(), category.getModelObject()));
        categoryContainer.add(subcategory);

        subcategory.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306812L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(EventCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, subcategory));
            }
        });

        categoryContainer.add(new Label("customLabel", new ResourceModel("custom", "custom")).setVisible(false));

        custom = new AjaxTextFieldPanel("custom", "custom", new Model<>(null));
        custom.setVisible(false);
        custom.setEnabled(false);

        categoryContainer.add(custom.hideLabel());

        actionsPanel = new ActionsPanel<>("customActions", null);
        actionsPanel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final EventCategory ignore) {
                if (StringUtils.isNotBlank(custom.getModelObject())) {
                    Pair<EventCategory, AuditElements.Result> parsed =
                            AuditLoggerName.parseEventCategory(custom.getModelObject());

                    AuditLoggerName auditLoggerName = new AuditLoggerName(
                            parsed.getLeft().getType(),
                            null,
                            null,
                            parsed.getLeft().getEvents().isEmpty()
                            ? StringUtils.EMPTY : parsed.getLeft().getEvents().iterator().next(),
                            parsed.getRight());

                    custom.setModelObject(StringUtils.EMPTY);
                    send(EventCategoryPanel.this.getPage(), Broadcast.BREADTH, new EventSelectionChanged(
                            target,
                            Set.of(auditLoggerName),
                            Set.of()));
                    target.add(categoryContainer);
                }
            }
        }, ActionLink.ActionType.CREATE, StringUtils.EMPTY).hideLabel();
        actionsPanel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435521L;

            @Override
            public void onClick(final AjaxRequestTarget target, final EventCategory ignore) {
                if (StringUtils.isNotBlank(custom.getModelObject())) {
                    Pair<EventCategory, AuditElements.Result> parsed =
                            AuditLoggerName.parseEventCategory(custom.getModelObject());

                    AuditLoggerName auditLoggerName = new AuditLoggerName(
                            parsed.getLeft().getType(),
                            null,
                            null,
                            parsed.getLeft().getEvents().isEmpty()
                            ? StringUtils.EMPTY : parsed.getLeft().getEvents().iterator().next(),
                            parsed.getRight());

                    custom.setModelObject(StringUtils.EMPTY);
                    send(EventCategoryPanel.this.getPage(), Broadcast.BREADTH, new EventSelectionChanged(
                            target,
                            Set.of(),
                            Set.of(auditLoggerName)));
                    target.add(categoryContainer);
                }
            }
        }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();

        categoryContainer.add(actionsPanel);

        actionsPanel.setVisible(false);
        actionsPanel.setEnabled(false);
        actionsPanel.setMarkupId("inline-actions");

        eventsContainer.add(new EventSelectionPanel("eventsPanel", eventCategory, model) {

            private static final long serialVersionUID = 3513194801190026082L;

            @Override
            protected void onEventAction(final IEvent<?> event) {
                EventCategoryPanel.this.onEventAction(event);
            }
        });
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ChangeCategoryEvent) {
            // update objects ....
            eventCategory.getEvents().clear();

            ChangeCategoryEvent change = (ChangeCategoryEvent) event.getPayload();

            switch (change.getChangedPanel().getId()) {
                case "type":
                    eventCategory.setType(type.getModelObject());
                    eventCategory.setCategory(null);
                    eventCategory.setSubcategory(null);
                    if (type.getModelObject() == EventCategoryType.CUSTOM
                            || type.getModelObject() == EventCategoryType.WA) {

                        category.setChoices(List.of());
                        subcategory.setChoices(List.of());
                        category.setEnabled(false);
                        subcategory.setEnabled(false);
                        custom.setVisible(true);
                        custom.setEnabled(true);
                        actionsPanel.setVisible(true);
                        actionsPanel.setEnabled(true);
                    } else {
                        category.setChoices(filter(eventCategories, type.getModelObject()));
                        subcategory.setChoices(List.of());
                        category.setEnabled(true);
                        subcategory.setEnabled(true);
                        custom.setVisible(false);
                        custom.setEnabled(false);
                        actionsPanel.setVisible(false);
                        actionsPanel.setEnabled(false);
                    }
                    change.getTarget().add(categoryContainer);
                    break;

                case "category":
                    subcategory.setChoices(
                            filter(eventCategories, type.getModelObject(), category.getModelObject()));
                    eventCategory.setCategory(category.getModelObject());
                    eventCategory.setSubcategory(null);
                    change.getTarget().add(categoryContainer);
                    break;

                default:
                    eventCategory.setSubcategory(subcategory.getModelObject());
                    break;
            }

            updateEventsContainer(change.getTarget());
        } else if (event.getPayload() instanceof InspectSelectedEvent) {
            // update objects ....
            eventCategory.getEvents().clear();

            InspectSelectedEvent inspectSelectedEvent = (InspectSelectedEvent) event.getPayload();

            Pair<EventCategory, AuditElements.Result> categoryEvent =
                    AuditLoggerName.parseEventCategory(inspectSelectedEvent.getEvent());

            eventCategory.setType(categoryEvent.getLeft().getType());
            category.setChoices(filter(eventCategories, type.getModelObject()));

            eventCategory.setCategory(categoryEvent.getLeft().getCategory());
            subcategory.setChoices(filter(eventCategories, type.getModelObject(), category.getModelObject()));

            eventCategory.setSubcategory(categoryEvent.getLeft().getSubcategory());

            if (categoryEvent.getLeft().getType() == EventCategoryType.CUSTOM
                    || categoryEvent.getLeft().getType() == EventCategoryType.WA) {

                custom.setModelObject(AuditLoggerName.buildEvent(
                        categoryEvent.getLeft().getType(),
                        categoryEvent.getLeft().getCategory(),
                        categoryEvent.getLeft().getSubcategory(),
                        categoryEvent.getLeft().getEvents().isEmpty()
                        ? StringUtils.EMPTY : categoryEvent.getLeft().getEvents().iterator().next(),
                        categoryEvent.getRight()));

                category.setEnabled(false);
                subcategory.setEnabled(false);
                custom.setVisible(true);
                custom.setEnabled(true);
                actionsPanel.setVisible(true);
                actionsPanel.setEnabled(true);
            } else {
                category.setEnabled(true);
                subcategory.setEnabled(true);
                custom.setVisible(false);
                custom.setEnabled(false);
                actionsPanel.setVisible(false);
                actionsPanel.setEnabled(false);
            }

            inspectSelectedEvent.getTarget().add(categoryContainer);
            updateEventsContainer(inspectSelectedEvent.getTarget());
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

    protected abstract List<String> getListAuthRoles();

    protected void authorizeList() {
        getListAuthRoles().forEach(r -> MetaDataRoleAuthorizationStrategy.authorize(selectedEventsPanel, RENDER, r));
    }

    protected abstract List<String> getChangeAuthRoles();

    protected void authorizeChanges() {
        getChangeAuthRoles().forEach(r -> {
            MetaDataRoleAuthorizationStrategy.authorize(categoryContainer, RENDER, r);
            MetaDataRoleAuthorizationStrategy.authorize(eventsContainer, RENDER, r);
        });
    }

    protected void updateEventsContainer(final AjaxRequestTarget target) {
        for (Iterator<EventCategory> itor = eventCategories.iterator();
                itor.hasNext() && eventCategory.getEvents().isEmpty();) {

            EventCategory ec = itor.next();
            if (eventCategory.getType() == ec.getType()
                    && StringUtils.equals(eventCategory.getCategory(), ec.getCategory())
                    && StringUtils.equals(eventCategory.getSubcategory(), ec.getSubcategory())) {

                eventCategory.getEvents().addAll(ec.getEvents());
            }
        }

        eventsContainer.addOrReplace(new EventSelectionPanel("eventsPanel", eventCategory, model) {

            private static final long serialVersionUID = 3513194801190026082L;

            @Override
            public void onEventAction(final IEvent<?> event) {
                EventCategoryPanel.this.onEventAction(event);
            }
        });
        target.add(eventsContainer);
    }
}
