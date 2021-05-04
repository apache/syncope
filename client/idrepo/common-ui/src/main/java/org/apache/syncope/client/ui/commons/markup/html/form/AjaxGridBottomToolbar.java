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
import java.util.Optional;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IFormVisitorParticipant;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.model.PropertyModel;
import org.wicketstuff.egrid.column.AbstractEditablePropertyColumn;
import org.wicketstuff.egrid.column.EditableCellPanel;
import org.wicketstuff.egrid.component.EditableDataTable;
import org.wicketstuff.egrid.component.EditableGridSubmitLink;
import org.wicketstuff.egrid.toolbar.AbstractEditableGridToolbar;

public abstract class AjaxGridBottomToolbar<T, S> extends AbstractEditableGridToolbar {

    private static final long serialVersionUID = 1L;

    protected static final String CELL_ID = "cell";

    protected static final String CELLS_ID = "cells";

    protected T newRow = null;

    public AjaxGridBottomToolbar(final EditableDataTable<?, ?> table) {
        super(table);

        newRow = newRowInstance();

        MarkupContainer td = new WebMarkupContainer("td");
        td.add(new AttributeModifier("colspan", table.getColumns().size() - 1));
        AddToolBarForm addToolBarForm = new AddToolBarForm("addToolbarForm");
        td.add(addToolBarForm);
        add(td);
        add(newAddButton(addToolBarForm));
    }

    protected abstract T newRowInstance();

    protected abstract void onAdd(AjaxRequestTarget target, T newRow);

    protected void onError(final AjaxRequestTarget target) {
    }

    protected class AddToolBarForm extends Form<T> implements IFormVisitorParticipant {

        protected static final long serialVersionUID = 1L;

        public AddToolBarForm(final String id) {
            super(id);
            add(newEditorComponents());
        }

        @Override
        public boolean processChildren() {
            return Optional.ofNullable(getRootForm().findSubmitter()).
                    map(submitter -> submitter.getForm() == this).
                    orElse(false);
        }
    }

    protected Component newAddButton(final WebMarkupContainer encapsulatingContainer) {
        return new EditableGridSubmitLink("add", encapsulatingContainer) {

            protected static final long serialVersionUID = 1L;

            @Override
            protected void onSuccess(final AjaxRequestTarget target) {
                onAdd(target, newRow);
                newRow = newRowInstance();
                target.add(getTable());
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                AjaxGridBottomToolbar.this.onError(target);
            }
        };
    }

    protected Loop newEditorComponents() {
        List<AbstractEditablePropertyColumn<T, S>> columns = getEditableColumns();
        return new Loop(CELLS_ID, columns.size()) {

            protected static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final LoopItem item) {
                addEditorComponent(item, getEditorColumn(columns, item.getIndex()));
            }
        };
    }

    protected void addEditorComponent(final LoopItem item, final AbstractEditablePropertyColumn<T, S> toolBarCell) {
        item.add(newCell(toolBarCell));
    }

    @SuppressWarnings("unchecked")
    protected List<AbstractEditablePropertyColumn<T, S>> getEditableColumns() {
        List<AbstractEditablePropertyColumn<T, S>> columns = new ArrayList<>();
        getTable().getColumns().stream().
                filter(AbstractEditablePropertyColumn.class::isInstance).
                map(AbstractEditablePropertyColumn.class::cast).
                forEach(columns::add);
        return columns;
    }

    protected Component newCell(final AbstractEditablePropertyColumn<T, S> editableGridColumn) {
        EditableCellPanel panel = editableGridColumn.getEditableCellPanel(CELL_ID);
        FormComponent<?> editorComponent = panel.getEditableComponent();
        editorComponent.setDefaultModel(new PropertyModel<T>(newRow, editableGridColumn.getPropertyExpression()));
        return panel;
    }

    protected AbstractEditablePropertyColumn<T, S> getEditorColumn(
            final List<AbstractEditablePropertyColumn<T, S>> editorColumn, final int index) {

        return editorColumn.get(index);
    }
}
