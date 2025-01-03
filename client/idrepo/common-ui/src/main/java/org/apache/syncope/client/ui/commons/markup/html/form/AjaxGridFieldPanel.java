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
package org.apache.syncope.client.ui.commons.markup.html.form;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.wicketstuff.egrid.column.AbstractEditablePropertyColumn;
import org.wicketstuff.egrid.column.RequiredTextFieldColumn;
import org.wicketstuff.egrid.provider.EditableListDataProvider;

public class AjaxGridFieldPanel<K, V> extends Panel {

    private static final long serialVersionUID = 7589570522964677729L;

    protected final AjaxGrid<K, V, String> grid;

    protected final IModel<Map<K, V>> model;

    public AjaxGridFieldPanel(final String id, final String name, final IModel<Map<K, V>> model) {
        super(id, model);

        add(new Label(AbstractFieldPanel.LABEL, new ResourceModel(name, name)));

        grid = new AjaxGrid<>(
                "grid",
                getColumns(),
                new EditableListDataProvider<>(model.getObject().entrySet().stream().
                        map(e -> MutablePair.of(e.getKey(), e.getValue())).collect(Collectors.toList()), ""),
                10) {

            private static final long serialVersionUID = -1315456128897492459L;

            @Override
            protected boolean displayHeader() {
                return false;
            }

            @Override
            protected void onAdd(final AjaxRequestTarget target, final Pair<K, V> newRow) {
                model.getObject().put(newRow.getLeft(), newRow.getRight());
            }

            @Override
            protected void onDelete(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
                model.getObject().remove(rowModel.getObject().getLeft());
            }

            @Override
            protected void onSave(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
                model.getObject().put(rowModel.getObject().getLeft(), rowModel.getObject().getRight());
            }
        };
        add(grid);

        this.model = model;
    }

    public AjaxGridFieldPanel<K, V> hideLabel() {
        Optional.ofNullable(get(AbstractFieldPanel.LABEL)).ifPresent(label -> label.setVisible(false));

        return this;
    }

    private List<AbstractEditablePropertyColumn<Pair<K, V>, String>> getColumns() {
        List<AbstractEditablePropertyColumn<Pair<K, V>, String>> columns = new ArrayList<>();
        columns.add(new RequiredTextFieldColumn<>(Model.of(), "left"));
        columns.add(new RequiredTextFieldColumn<>(Model.of(), "right"));
        return columns;
    }

    public AjaxGrid<K, V, String> getGrid() {
        return grid;
    }

    public AjaxGridFieldPanel<K, V> setModelObject(final Map<K, V> object) {
        model.setObject(object);
        return this;
    }
}
