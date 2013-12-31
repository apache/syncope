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

import java.util.Collection;
import java.util.List;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.AbstractBasePage;
import org.apache.syncope.console.pages.BulkActionModalPage;
import org.apache.syncope.console.pages.panels.AbstractSearchResultPanel.EventDataWrapper;
import org.apache.syncope.console.rest.BaseRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.extensions.markup.html.repeater.data.table.CheckGroupColumn;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;

public class AjaxDataTablePanel<T, S> extends DataTablePanel<T, S> {

    private static final long serialVersionUID = -7264400471578272966L;

    public AjaxDataTablePanel(
            final String id,
            final List<IColumn<T, S>> columns,
            final ISortableDataProvider<T, S> dataProvider,
            final int rowsPerPage,
            final Collection<ActionLink.ActionType> actions,
            final BaseRestClient bulkActionExecutor,
            final String itemIdField,
            final String pageId,
            final PageReference pageRef) {

        super(id);

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

        Form<T> bulkActionForm = new Form<T>("groupForm");
        fragment.add(bulkActionForm);

        group = new CheckGroup<T>("checkgroup", model);
        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // triggers AJAX form submit
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
                                group.getModelObject(),
                                columns,
                                actions,
                                bulkActionExecutor,
                                itemIdField,
                                pageId);
                    }
                });

                bulkModalWin.show(target);
            }
        });
    }
}
