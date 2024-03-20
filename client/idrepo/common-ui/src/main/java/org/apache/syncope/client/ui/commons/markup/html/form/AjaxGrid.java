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
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.egrid.column.EditableActionsColumn;
import org.wicketstuff.egrid.column.panel.ActionsPanel;
import org.wicketstuff.egrid.component.EditableDataTable;
import org.wicketstuff.egrid.provider.IEditableDataProvider;
import org.wicketstuff.egrid.toolbar.HeadersToolbar;
import org.wicketstuff.egrid.toolbar.NavigationToolbar;

public class AjaxGrid<K, V, S> extends Panel {

    private static final long serialVersionUID = 9101893623114754751L;

    protected static class NonValidatingForm<T> extends Form<T> {

        private static final long serialVersionUID = 8313183098058102408L;

        public NonValidatingForm(final String id) {
            super(id);
        }

        @Override
        public void process(final IFormSubmitter submittingComponent) {
            delegateSubmit(submittingComponent);
        }
    }

    protected EditableDataTable<Pair<K, V>, S> dataTable;

    public AjaxGrid(
            final String id,
            final List<? extends IColumn<Pair<K, V>, S>> columns,
            final IEditableDataProvider<Pair<K, V>, S> dataProvider,
            final long rowsPerPage) {

        super(id);

        List<IColumn<Pair<K, V>, S>> newCols = new ArrayList<>();
        newCols.addAll(columns);
        newCols.add(new AjaxGridActionsColumn<>(new Model<>("Actions")));

        dataTable = new EditableDataTable<>("dataTable", newCols, dataProvider, rowsPerPage);
        dataTable.setOutputMarkupId(true);

        dataTable.addTopToolbar(new NavigationToolbar(dataTable));
        if (displayHeader()) {
            dataTable.addTopToolbar(new HeadersToolbar<>(dataTable, dataProvider));
        }
        if (displayAdd()) {
            dataTable.addBottomToolbar(newAddBottomToolbar(dataTable, dataProvider));
        }

        Form<Pair<K, V>> form = new NonValidatingForm<>("form");
        form.setOutputMarkupId(true);
        form.add(dataTable);
        add(form);
    }

    public AjaxGrid<K, V, S> setTableBodyCss(final String cssStyle) {
        dataTable.setTableBodyCss(cssStyle);
        return this;
    }

    public AjaxGrid<K, V, S> setTableCss(final String cssStyle) {
        dataTable.add(AttributeModifier.replace("class", cssStyle));
        return this;
    }

    protected AjaxGridBottomToolbar<Pair<K, V>, S> newAddBottomToolbar(
            final EditableDataTable<Pair<K, V>, S> dataTable,
            final IEditableDataProvider<Pair<K, V>, S> dataProvider) {

        return new AjaxGridBottomToolbar<>(dataTable) {

            protected static final long serialVersionUID = 1L;

            @Override
            protected Pair<K, V> newRowInstance() {
                return new MutablePair<>();
            }

            @Override
            protected void onAdd(final AjaxRequestTarget target, final Pair<K, V> newRow) {
                dataProvider.add(newRow);
                target.add(dataTable);
                AjaxGrid.this.onAdd(target, newRow);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                super.onError(target);
                AjaxGrid.this.onError(target);
            }
        };
    }

    protected boolean allowDelete(final Item<Pair<K, V>> rowItem) {
        return true;
    }

    protected boolean displayHeader() {
        return true;
    }

    protected boolean displayAdd() {
        return true;
    }

    protected void onCancel(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
    }

    protected void onDelete(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
    }

    protected void onSave(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
    }

    protected void onError(final AjaxRequestTarget target) {
    }

    protected void onAdd(final AjaxRequestTarget target, final Pair<K, V> newRow) {
    }

    protected class AjaxGridActionsColumn<P, S> extends EditableActionsColumn<Pair<K, V>, S> {

        private static final long serialVersionUID = 7409805339768145855L;

        public AjaxGridActionsColumn(final IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(
                final Item<ICellPopulator<Pair<K, V>>> item,
                final String componentId,
                final IModel<Pair<K, V>> rowModel) {

            @SuppressWarnings("unchecked")
            Item<Pair<K, V>> rowItem = item.findParent(Item.class);
            item.add(new AjaxGridActionsPanel<>(componentId, rowItem));
        }

        @Override
        protected void onError(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
            AjaxGrid.this.onError(target);
        }

        @Override
        protected void onSave(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
            AjaxGrid.this.onSave(target, rowModel);
        }

        @Override
        protected void onDelete(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
            AjaxGrid.this.onDelete(target, rowModel);
        }

        @Override
        protected void onCancel(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
            AjaxGrid.this.onCancel(target, rowModel);
        }

        @Override
        protected boolean allowDelete(final Item<Pair<K, V>> rowItem) {
            return AjaxGrid.this.allowDelete(rowItem);
        }
    }

    protected class AjaxGridActionsPanel<T> extends ActionsPanel<Pair<K, V>> {

        private static final long serialVersionUID = -1239486389000098745L;

        private final Item<Pair<K, V>> rowItem;

        @SuppressWarnings("unchecked")
        public AjaxGridActionsPanel(final String id, final Item<Pair<K, V>> item) {
            super(id, item);

            this.rowItem = item;
        }

        @Override
        protected void onEdit(final AjaxRequestTarget target) {
            super.onEdit(target);

            rowItem.setMetaData(EditableDataTable.EDITING, true);
            send(getPage(), Broadcast.BREADTH, rowItem);
            target.add(rowItem);
        }

        @Override
        protected void onSave(final AjaxRequestTarget target) {
            super.onSave(target);

            rowItem.setMetaData(EditableDataTable.EDITING, false);
            send(getPage(), Broadcast.BREADTH, rowItem);
            target.add(rowItem);

            AjaxGrid.this.onSave(target, rowItem.getModel());
        }

        @Override
        protected void onError(final AjaxRequestTarget target) {
            target.add(rowItem);

            AjaxGrid.this.onError(target);
        }

        @Override
        protected void onCancel(final AjaxRequestTarget target) {
            super.onCancel(target);

            rowItem.setMetaData(EditableDataTable.EDITING, false);
            send(getPage(), Broadcast.BREADTH, rowItem);
            target.add(rowItem);

            AjaxGrid.this.onCancel(target, rowItem.getModel());
        }

        @Override
        protected void onDelete(final AjaxRequestTarget target) {
            super.onDelete(target);

            dataTable.getDataProvider().remove(rowItem.getModelObject());
            target.add(dataTable);

            AjaxGrid.this.onDelete(target, rowItem.getModel());
        }
    }
}
