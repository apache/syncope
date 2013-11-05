/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.console.pages.panels;

import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class SelectedEventsPanel extends Panel {

    private static final long serialVersionUID = -4832450230348213500L;

    private final WebMarkupContainer selectionContainer;

    private ListView<String> selectedEvents;

    private final IModel<List<String>> model;

    public SelectedEventsPanel(final String id, final IModel<List<String>> model) {
        super(id);

        this.model = model;

        selectionContainer = new WebMarkupContainer("selectionContainer");
        selectionContainer.setOutputMarkupId(true);
        add(selectionContainer);

        selectedEvents = new ListView<String>("selectedEvents", model.getObject()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label("selectedEvent", new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };

        selectionContainer.add(selectedEvents);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof EventSelectionChanged) {
            final EventSelectionChanged eventSelectionChanged = (EventSelectionChanged) event.getPayload();

            for (String toBeRemoved : eventSelectionChanged.getToBeRemoved()) {
                model.getObject().remove(toBeRemoved);
            }

            for (String toBeAdded : eventSelectionChanged.getToBeAdded()) {
                if (!model.getObject().contains(toBeAdded)) {
                    model.getObject().add(toBeAdded);
                }
            }

            eventSelectionChanged.getTarget().add(selectionContainer);
        }
    }

    public static class EventSelectionChanged {

        private final AjaxRequestTarget target;

        private final Set<String> toBeRemoved;

        private final Set<String> toBeAdded;

        public EventSelectionChanged(
                final AjaxRequestTarget target,
                final Set<String> toBeAdded,
                final Set<String> toBeRemoved) {
            this.target = target;
            this.toBeAdded = toBeAdded;
            this.toBeRemoved = toBeRemoved;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public Set<String> getToBeRemoved() {
            return toBeRemoved;
        }

        public Set<String> getToBeAdded() {
            return toBeAdded;
        }
    }
}
