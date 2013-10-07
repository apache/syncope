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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.console.commons.ActionTableCheckGroup;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionDataTablePanel<T, S> extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ActionDataTablePanel.class);

    private static final long serialVersionUID = -8826989026203543957L;

    private final CheckGroup<T> group;

    private final Form bulkActionForm;

    private final ActionLinksPanel actionPanel;

    private final AjaxFallbackDefaultDataTable<T, S> dataTable;

    private final PageReference pageRef;

    private final String CANCEL = "cancel";

    public ActionDataTablePanel(
            final String id,
            final List<IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider,
            final int rowsPerPage,
            final PageReference pageRef) {

        super(id);

        this.pageRef = pageRef;

        bulkActionForm = new Form("groupForm");
        add(bulkActionForm);

        group = new ActionTableCheckGroup<T>("checkgroup", new ArrayList<T>()) {

            private static final long serialVersionUID = -8667764190925075389L;

            @Override
            public boolean isCheckable(T element) {
                return isElementEnabled(element);
            }
        };
        bulkActionForm.add(group);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // ignore
            }
        });

        columns.add(0, new CheckGroupColumn<T, S>(group));
        dataTable = new AjaxFallbackDefaultDataTable<T, S>("dataTable", columns, dataProvider, rowsPerPage);
        group.add(dataTable);

        final WebMarkupContainer actionPanelContainer = new WebMarkupContainer("actionPanelContainer");
        bulkActionForm.add(actionPanelContainer);

        actionPanel = new ActionLinksPanel("actions", new Model(), pageRef);
        actionPanelContainer.add(actionPanel);

        if (dataTable.getRowCount() == 0) {
            actionPanelContainer.add(new AttributeModifier("style", "display: none"));
        }

        bulkActionForm.add(new ClearIndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL), pageRef) {

            private static final long serialVersionUID = -2341391430136818025L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                // ignore
            }
        }.setVisible(false).setEnabled(false));
    }

    public final void setCurrentPage(final long page) {
        dataTable.setCurrentPage(page);
    }

    public final long getRowCount() {
        return dataTable.getRowCount();
    }

    public final long getCurrentPage() {
        return dataTable.getCurrentPage();
    }

    public final long getPageCount() {
        return dataTable.getPageCount();
    }

    public void setItemsPerPage(final int resourcePaginatorRows) {
        dataTable.setItemsPerPage(resourcePaginatorRows);
    }

    public void addAction(final ActionLink action, final ActionType type, final String pageId) {
        actionPanel.add(action, type, pageId);
    }

    public void addAction(final ActionLink action, final ActionType type, final String pageId, final String actionId) {
        actionPanel.add(action, type, pageId, actionId);
    }

    public void addAction(final ActionLink action, final ActionType type, final String pageId, final boolean enabled) {
        actionPanel.add(action, type, pageId, enabled);
    }

    public void addCancelButton(final ModalWindow window) {

        final AjaxButton cancel = new ClearIndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL), pageRef) {

            private static final long serialVersionUID = -2341391430136818025L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };

        cancel.setDefaultFormProcessing(false);
        bulkActionForm.addOrReplace(cancel);
    }

    public Collection<T> getModelObject() {
        return group.getModelObject();
    }

    public boolean isElementEnabled(final T element) {
        return true;
    }
}
