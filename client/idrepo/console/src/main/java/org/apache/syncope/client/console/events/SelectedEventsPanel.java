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
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class SelectedEventsPanel extends Panel {

    private static final long serialVersionUID = -4832450230348213500L;

    private final WebMarkupContainer selectionContainer;

    private final IModel<List<String>> model;

    public SelectedEventsPanel(final String id, final IModel<List<String>> model) {
        super(id);

        this.model = model;

        selectionContainer = new WebMarkupContainer("selectionContainer");
        selectionContainer.setOutputMarkupId(true);
        add(selectionContainer);

        ListMultipleChoice<String> selectedEvents =
                new ListMultipleChoice<>("selectedEvents", new ListModel<>(), model) {

            private static final long serialVersionUID = 1226677544225737338L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.remove("size");
                tag.remove("multiple");
                tag.put("size", 5);
            }
        };
        selectedEvents.setMaxRows(5);
        selectedEvents.setChoiceRenderer(new IChoiceRenderer<>() {

            private static final long serialVersionUID = -4288397951948436434L;

            @Override
            public Object getDisplayValue(final String object) {
                return object;
            }

            @Override
            public String getIdValue(final String object, final int index) {
                return object;
            }

            @Override
            public String getObject(final String id, final IModel<? extends List<? extends String>> choices) {
                return id;
            }
        });
        selectedEvents.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(SelectedEventsPanel.this.getPage(),
                        Broadcast.BREADTH,
                        new InspectSelectedEvent(target, selectedEvents.getModelValue()));
            }
        });
        selectionContainer.add(selectedEvents);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof final EventSelectionChanged eventSelectionChanged) {

            eventSelectionChanged.getToBeRemoved().
                    forEach(toBeRemoved -> model.getObject().remove(toBeRemoved.toString()));

            eventSelectionChanged.getToBeAdded().stream().
                    filter(toBeAdded -> !model.getObject().contains(toBeAdded.toString())).
                    forEach(toBeAdded -> model.getObject().add(toBeAdded.toString()));

            Collections.sort(model.getObject());
            eventSelectionChanged.getTarget().add(selectionContainer);
        }
    }

    public static class InspectSelectedEvent {

        private final AjaxRequestTarget target;

        private final String event;

        public InspectSelectedEvent(final AjaxRequestTarget target, final String event) {
            this.target = target;
            this.event = event;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getEvent() {
            return event;
        }
    }

    public static class EventSelectionChanged {

        private final AjaxRequestTarget target;

        private final Set<OpEvent> toBeRemoved;

        private final Set<OpEvent> toBeAdded;

        public EventSelectionChanged(
                final AjaxRequestTarget target,
                final Set<OpEvent> toBeAdded,
                final Set<OpEvent> toBeRemoved) {

            this.target = target;
            this.toBeAdded = toBeAdded;
            this.toBeRemoved = toBeRemoved;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Set<OpEvent> getToBeRemoved() {
            return toBeRemoved;
        }

        public Set<OpEvent> getToBeAdded() {
            return toBeAdded;
        }
    }
}
