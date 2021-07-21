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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.egrid.column.EditableGridActionsColumn;
import org.wicketstuff.egrid.column.EditableGridActionsPanel;
import org.wicketstuff.egrid.component.EditableDataTable;
import org.wicketstuff.egrid.model.GridOperationData;
import org.wicketstuff.egrid.model.OperationType;
import org.wicketstuff.egrid.provider.IEditableDataProvider;
import org.wicketstuff.egrid.toolbar.EditableGridHeadersToolbar;
import org.wicketstuff.egrid.toolbar.EditableGridNavigationToolbar;

public class AjaxGrid<K, V, S> extends Panel {

    private static final long serialVersionUID = 1L;

    protected static class NonValidatingForm<T> extends Form<T> {

        protected static final long serialVersionUID = 1L;

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

        dataTable = new EditableDataTable<>("dataTable", newCols, dataProvider, rowsPerPage, null) {

            protected static final long serialVersionUID = 1L;

            @Override
            protected void onError(final AjaxRequestTarget target) {
                AjaxGrid.this.onError(target);
            }

            @Override
            protected Item<Pair<K, V>> newRowItem(final String id, final int index, final IModel<Pair<K, V>> model) {
                return super.newRowItem(id, index, model);
            }
        };

        dataTable.setOutputMarkupId(true);

        dataTable.addTopToolbar(new EditableGridNavigationToolbar(dataTable));
        if (displayHeader()) {
            dataTable.addTopToolbar(new EditableGridHeadersToolbar<>(dataTable, dataProvider));
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

    protected EditableDataTable.RowItem<Pair<K, V>> newRowItem(
            final String id, final int index, final IModel<Pair<K, V>> model) {

        return new EditableDataTable.RowItem<>(id, index, model);
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

    protected void onCancel(final AjaxRequestTarget target) {
    }

    protected void onDelete(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
    }

    protected void onSave(final AjaxRequestTarget target, final IModel<Pair<K, V>> rowModel) {
    }

    protected void onError(final AjaxRequestTarget target) {
    }

    protected void onAdd(final AjaxRequestTarget target, final Pair<K, V> newRow) {
    }

    protected class AjaxGridActionsColumn<P, S> extends EditableGridActionsColumn<Pair<K, V>, S> {

        protected static final long serialVersionUID = 1L;

        public AjaxGridActionsColumn(final IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(
                final Item<ICellPopulator<Pair<K, V>>> item,
                final String componentId,
                final IModel<Pair<K, V>> rowModel) {

            item.add(new AjaxGridActionsPanel<>(componentId, item));
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
        protected void onCancel(final AjaxRequestTarget target) {
            AjaxGrid.this.onCancel(target);
        }

        @Override
        protected boolean allowDelete(final Item<Pair<K, V>> rowItem) {
            return AjaxGrid.this.allowDelete(rowItem);
        }
    }

    protected class AjaxGridActionsPanel<T> extends EditableGridActionsPanel<Pair<K, V>> {

        private final Item<Pair<K, V>> rowItem;

        @SuppressWarnings("unchecked")
        public AjaxGridActionsPanel(final String id, final Item<ICellPopulator<Pair<K, V>>> item) {
            super(id, item);

            rowItem = item.findParent(Item.class);
            addOrReplace(new AjaxLink<String>("delete") {

                @SuppressWarnings("unchecked")
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    EditableDataTable eventTarget = rowItem.findParent(EditableDataTable.class);
                    send(getPage(), Broadcast.BREADTH, new GridOperationData<>(
                            OperationType.DELETE, (T) rowItem.getDefaultModelObject(), eventTarget));
                    target.add(eventTarget);
                    onDelete(target);
                }

                @Override
                public boolean isVisible() {
                    return AjaxGridActionsPanel.this.allowDelete(rowItem);
                }
            });
        }

        @Override
        protected void onSave(final AjaxRequestTarget target) {
            AjaxGrid.this.onSave(target, rowItem.getModel());
        }

        @Override
        protected void onError(final AjaxRequestTarget target) {
            AjaxGrid.this.onError(target);
        }

        @Override
        protected void onCancel(final AjaxRequestTarget target) {
            AjaxGrid.this.onCancel(target);
        }

        @Override
        protected void onDelete(final AjaxRequestTarget target) {
            AjaxGrid.this.onDelete(target, rowItem.getModel());
        }
    }
}
