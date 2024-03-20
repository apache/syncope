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
import org.wicketstuff.egrid.column.panel.EditablePanel;
import org.wicketstuff.egrid.component.EditableDataTable;
import org.wicketstuff.egrid.component.EditableTableSubmitLink;
import org.wicketstuff.egrid.toolbar.AbstractEditableToolbar;

public abstract class AjaxGridBottomToolbar<T, S> extends AbstractEditableToolbar {

    private static final long serialVersionUID = -8122881502408032823L;

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

        private static final long serialVersionUID = 4496461574118470280L;

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
        return new EditableTableSubmitLink("add", encapsulatingContainer) {

            private static final long serialVersionUID = 4073423722572857137L;

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

            private static final long serialVersionUID = -7579369655436866236L;

            @Override
            protected void populateItem(final LoopItem item) {
                AbstractEditablePropertyColumn<T, S> editableColumn = columns.get(item.getIndex());

                EditablePanel panel = editableColumn.createEditablePanel(CELL_ID);
                FormComponent<?> editorComponent = panel.getEditableComponent();
                editorComponent.setDefaultModel(new PropertyModel<T>(newRow, editableColumn.getPropertyExpression()));

                item.add(panel);
            }
        };
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
}
