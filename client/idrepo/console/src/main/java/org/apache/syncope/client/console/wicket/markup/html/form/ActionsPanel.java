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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * This empty class must exist because there not seems to be alternative to provide specialized HTML for edit links.
 *
 * @param <T> model object type.
 */
public final class ActionsPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = 322966537010107771L;

    private final List<Action<T>> actions = new ArrayList<>();

    private IModel<T> model;

    public ActionsPanel(final String componentId, final IModel<T> model) {
        super(componentId, model);
        setOutputMarkupId(true);
        this.model = model;

        add(new ListView<>("actionRepeater", actions) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Action<T>> item) {
                item.add(new ActionPanel<>(ActionsPanel.this.model, item.getModelObject()));
            }

        }.setRenderBodyOnly(true));
    }

    public Action<T> add(
            final ActionLink<T> link,
            final ActionLink.ActionType type,
            final String entitlements) {

        return add(link, type, entitlements, false);
    }

    public Action<T> add(
            final ActionLink<T> link,
            final ActionLink.ActionType type,
            final String entitlements,
            final boolean onConfirm) {

        Action<T> action = new Action<>(link, type);
        action.setEntitlements(entitlements);
        action.setOnConfirm(onConfirm);
        actions.add(action);
        return action;
    }

    public Action<T> add(final Action<T> action) {
        actions.add(action);
        return action;
    }

    public Action<T> add(final int index, final Action<T> action) {
        actions.add(index, action);
        return action;
    }

    public Action<T> set(final int index, final Action<T> action) {
        actions.set(index, action);
        return action;
    }

    public List<Action<T>> getActions() {
        return actions;
    }

    public ActionsPanel<T> clone(final String componentId, final IModel<T> model) {
        ActionsPanel<T> panel = new ActionsPanel<>(componentId, model);
        panel.actions.addAll(actions);
        return panel;
    }

    /**
     * Use this with toggle panels.
     *
     * @param componentId Component Id.
     * @param model Model.
     * @return Actions panel.
     */
    public ActionsPanel<T> cloneWithLabels(final String componentId, final IModel<T> model) {
        ActionsPanel<T> panel = new ActionsPanel<>(componentId, model);
        actions.forEach(action -> panel.actions.add(action.showLabel()));
        return panel;
    }

    public boolean isEmpty() {
        return this.actions.isEmpty();
    }
}
