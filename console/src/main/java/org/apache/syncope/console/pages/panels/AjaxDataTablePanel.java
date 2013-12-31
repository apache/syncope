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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.AbstractBasePage;
import org.apache.syncope.console.pages.BulkActionModalPage;
import org.apache.syncope.console.pages.panels.AbstractSearchResultPanel.EventDataWrapper;
import org.apache.syncope.console.rest.BaseRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxDataTablePanel<T, S> extends Panel {

    private static final long serialVersionUID = -8826989026203543957L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AjaxDataTablePanel.class);

    private final CheckGroup<T> group;

    private final Form<?> bulkActionForm;

    protected final AjaxFallbackDefaultDataTable<T, S> dataTable;

    protected IModel<Collection<T>> model;

    public AjaxDataTablePanel(
            final String id,
            final List<IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider,
            final int rowsPerPage,
            final Collection<ActionLink.ActionType> actions,
            final BaseRestClient bulkActionExecutor,
            final String itemIdFiled,
            final String pageId,
            final PageReference pageRef) {

        super(id);

        model = new IModel<Collection<T>>() {

            private static final long serialVersionUID = 4886729136344643465L;

            private Collection<T> values = new HashSet<T>();

            @Override
            public Collection<T> getObject() {
                // Someone or something call this method to change the model: this is not the right behavior.
                // Return a copy of the model object in order to avoid SYNCOPE-465
                return new HashSet<T>(values);
            }

            @Override
            public void setObject(final Collection<T> selected) {
                final Collection<T> all = getGroupModelObjects();
                values.removeAll(all);
                values.addAll(selected);
            }

            @Override
            public void detach() {
            }
        };

        final ModalWindow bulkModalWin = new ModalWindow("bulkModal");
        bulkModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        bulkModalWin.setInitialHeight(600);
        bulkModalWin.setInitialWidth(900);
        bulkModalWin.setCookieName("bulk-modal");
        add(bulkModalWin);

        bulkModalWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487149L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                final EventDataWrapper data = new EventDataWrapper();
                data.setTarget(target);
                data.setRows(rowsPerPage);

                send(pageRef.getPage(), Broadcast.BREADTH, data);

                final AbstractBasePage page = (AbstractBasePage) pageRef.getPage();

                if (page.isModalResult()) {
                    // reset modal result
                    page.setModalResult(false);
                    // set operation succeeded
                    getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                    // refresh feedback panel
                    target.add(page.getFeedbackPanel());
                }
            }
        });

        Fragment fragment = new Fragment("tablePanel", "bulkAvailable", this);
        add(fragment);

        bulkActionForm = new Form("groupForm");
        fragment.add(bulkActionForm);

        group = new CheckGroup<T>("checkgroup", model);
        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        bulkActionForm.add(group);

        columns.add(0, new CheckGroupColumn<T, S>(group));
        dataTable = new AjaxFallbackDefaultDataTable<T, S>("dataTable", columns, dataProvider, rowsPerPage);
        group.add(dataTable);

        fragment.add(new ClearIndicatingAjaxButton("bulkActionLink", bulkActionForm, pageRef) {

            private static final long serialVersionUID = 382302811235019988L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                bulkModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690941L;

                    @Override
                    public Page createPage() {
                        return new BulkActionModalPage<T, S>(
                                bulkModalWin,
                                new ArrayList<T>(group.getModelObject()),
                                columns,
                                actions,
                                bulkActionExecutor,
                                itemIdFiled,
                                pageId);
                    }
                });

                bulkModalWin.show(target);
            }
        });
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

    protected Collection<T> getGroupModelObjects() {
        final Set<T> res = new HashSet<T>();

        final Component rows = group.get("dataTable:body:rows");
        if (rows instanceof DataGridView) {
            @SuppressWarnings("unchecked")
            final Iterator<Item<T>> iter = ((DataGridView<T>) rows).getItems();

            while (iter.hasNext()) {
                res.add(iter.next().getModelObject());
            }
        }
        return res;
    }
}
