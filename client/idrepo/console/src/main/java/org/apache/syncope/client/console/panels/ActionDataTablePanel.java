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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.client.console.commons.ActionTableCheckGroup;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;

public class ActionDataTablePanel<T extends Serializable, S> extends DataTablePanel<T, S> {

    private static final long serialVersionUID = -8826989026203543957L;

    private static final String CANCEL = "cancel";

    private final Form<T> batchForm;

    private final ActionsPanel<Serializable> actionPanel;

    public ActionDataTablePanel(
            final String id,
            final List<IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider,
            final int rowsPerPage) {

        super(id);

        batchForm = new Form<>("groupForm");
        add(batchForm);

        group = new ActionTableCheckGroup<>("checkgroup", model) {

            private static final long serialVersionUID = -8667764190925075389L;

            @Override
            public boolean isCheckable(final T element) {
                return isElementEnabled(element);
            }
        };
        group.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // triggers AJAX form submit
            }
        });
        batchForm.add(group);

        columns.addFirst(new CheckGroupColumn<>(group));
        dataTable = new AjaxFallbackDataTable<>("dataTable", columns, dataProvider, rowsPerPage, this);
        group.add(dataTable);

        final WebMarkupContainer actionPanelContainer = new WebMarkupContainer("actionPanelContainer");
        batchForm.add(actionPanelContainer);

        actionPanel = new ActionsPanel<>("actions", null);
        actionPanelContainer.add(actionPanel);

        if (dataTable.getRowCount() == 0) {
            actionPanelContainer.add(new AttributeModifier("style", "display: none"));
        }

        batchForm.add(new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -2341391430136818025L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // ignore
            }
        }.setVisible(false).setEnabled(false));
    }

    public void addAction(final ActionLink<Serializable> action, final ActionType type, final String entitlements) {
        actionPanel.add(action, type, entitlements);
    }

    public void addCancelButton(final BaseModal<?> modal) {
        AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                modal.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        batchForm.addOrReplace(cancel);
    }

    public Collection<T> getModelObject() {
        return group.getModelObject();
    }

    public boolean isElementEnabled(final T element) {
        return true;
    }
}
