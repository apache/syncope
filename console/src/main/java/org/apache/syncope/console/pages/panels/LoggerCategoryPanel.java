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
package org.apache.syncope.console.pages.panels;

import static org.apache.wicket.Component.RENDER;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.types.AuditElements.EventCategoryType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggerCategoryPanel extends Panel {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LoggerCategoryPanel.class);

    private static final long serialVersionUID = 6429053774964787734L;

    private final List<EventCategoryTO> eventCategoryTOs;

    private final EventCategoryTO eventCategoryTO = new EventCategoryTO();

    private final WebMarkupContainer categoryContainer;

    private final WebMarkupContainer eventsContainer;

    private final SelectedEventsPanel selectedEventsPanel;

    private final AjaxDropDownChoicePanel<EventCategoryType> type;

    private final AjaxDropDownChoicePanel<String> category;

    private final AjaxDropDownChoicePanel<String> subcategory;

    private final IModel<List<String>> model;

    public LoggerCategoryPanel(
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

        categoryContainer.add(new Label("typeLabel", new ResourceModel("type", "type")));

        type = new AjaxDropDownChoicePanel<EventCategoryType>(
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
        });
        categoryContainer.add(type);

        type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(LoggerCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, type));
            }
        });

        categoryContainer.add(new Label("categoryLabel", new ResourceModel("category", "category")));

        category = new AjaxDropDownChoicePanel<String>(
                "category",
                "category",
                new PropertyModel<String>(eventCategoryTO, "category"),
                false);
        category.setChoices(filter(eventCategoryTOs, type.getModelObject()));
        category.setStyleSheet("ui-widget-content ui-corner-all");
        categoryContainer.add(category);

        category.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306811L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(LoggerCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, category));
            }
        });

        categoryContainer.add(new Label("subcategoryLabel", new ResourceModel("subcategory", "subcategory")));

        subcategory = new AjaxDropDownChoicePanel<String>(
                "subcategory",
                "subcategory",
                new PropertyModel<String>(eventCategoryTO, "subcategory"),
                false);
        subcategory.setChoices(filter(eventCategoryTOs, type.getModelObject(), category.getModelObject()));
        subcategory.setStyleSheet("ui-widget-content ui-corner-all");
        categoryContainer.add(subcategory);

        subcategory.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306812L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(LoggerCategoryPanel.this, Broadcast.EXACT, new ChangeCategoryEvent(target, subcategory));
            }
        });

        eventsContainer.add(new EventSelectionPanel("eventsPanel", eventCategoryTO, model) {

            private static final long serialVersionUID = 3513194801190026082L;

            @Override
            public void onEvent(final IEvent<?> event) {
                onEventAction(event);
            }
        });
    }

    private List<String> filter(
            final List<EventCategoryTO> eventCategoryTOs, final EventCategoryType type) {
        final Set<String> res = new HashSet<String>();

        for (EventCategoryTO eventCategory : eventCategoryTOs) {
            if (type == eventCategory.getType() && StringUtils.isNotEmpty(eventCategory.getCategory())) {
                res.add(eventCategory.getCategory());
            }
        }

        final List<String> filtered = new ArrayList<String>(res);
        Collections.sort(filtered);
        return filtered;
    }

    private List<String> filter(
            final List<EventCategoryTO> eventCategoryTOs, final EventCategoryType type, final String category) {
        final Set<String> res = new HashSet<String>();

        for (EventCategoryTO eventCategory : eventCategoryTOs) {
            if (type == eventCategory.getType() && StringUtils.equals(category, eventCategory.getCategory())
                    && StringUtils.isNotEmpty(eventCategory.getSubcategory())) {
                res.add(eventCategory.getSubcategory());
            }
        }

        final List<String> filtered = new ArrayList<String>(res);
        Collections.sort(filtered);
        return filtered;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ChangeCategoryEvent) {
            // update objects ....
            eventCategoryTO.getEvents().clear();

            final ChangeCategoryEvent change = (ChangeCategoryEvent) event.getPayload();

            final Panel changedPanel = change.getChangedPanel();
            if ("type".equals(changedPanel.getId())) {
                category.setChoices(filter(eventCategoryTOs, type.getModelObject()));
                subcategory.setChoices(Collections.<String>emptyList());
                eventCategoryTO.setType(type.getModelObject());
                eventCategoryTO.setCategory(null);
                eventCategoryTO.setSubcategory(null);
                change.getTarget().add(categoryContainer);
            } else if ("category".equals(changedPanel.getId())) {
                subcategory.setChoices(filter(eventCategoryTOs, type.getModelObject(), category.getModelObject()));
                eventCategoryTO.setCategory(category.getModelObject());
                eventCategoryTO.setSubcategory(null);
                change.getTarget().add(categoryContainer);
            } else {
                eventCategoryTO.setSubcategory(subcategory.getModelObject());
            }

            setEvents();

            eventsContainer.addOrReplace(new EventSelectionPanel("eventsPanel", eventCategoryTO, model) {

                private static final long serialVersionUID = 3513194801190026082L;

                @Override
                public void onEvent(final IEvent<?> event) {
                    onEventAction(event);
                }
            });
            change.getTarget().add(eventsContainer);
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

    private class ChangeCategoryEvent {

        private final AjaxRequestTarget target;

        private final Panel changedPanel;

        public ChangeCategoryEvent(final AjaxRequestTarget target, final Panel changedPanel) {
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
        for (String role : getListRoles()) {
            MetaDataRoleAuthorizationStrategy.authorize(selectedEventsPanel, RENDER, role);
        }
    }

    private void authorizeChanges() {
        for (String role : getChangeRoles()) {
            MetaDataRoleAuthorizationStrategy.authorize(categoryContainer, RENDER, role);
            MetaDataRoleAuthorizationStrategy.authorize(eventsContainer, RENDER, role);
        }
    }

    protected abstract String[] getListRoles();

    protected abstract String[] getChangeRoles();
}
