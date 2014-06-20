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

import java.util.List;
import org.apache.syncope.common.to.AbstractTaskTO;
import org.apache.syncope.common.to.AbstractSyncTaskTO;
import org.apache.syncope.common.to.PushTaskTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.PushTaskModalPage;
import org.apache.syncope.console.pages.SyncTaskModalPage;
import org.apache.syncope.console.pages.Tasks;
import org.apache.syncope.console.pages.Tasks.TasksProvider;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.http.WebResponse;

public abstract class AbstractSyncTasksPanel<T extends AbstractSyncTaskTO> extends AbstractTasks {

    private static final long serialVersionUID = -8674781241465369244L;

    private int paginatorRows;

    protected WebMarkupContainer container;

    protected ModalWindow window;

    protected AjaxDataTablePanel<AbstractTaskTO, String> table;

    private final Class<T> reference;

    public AbstractSyncTasksPanel(final String id, final PageReference pageRef, final Class<T> reference) {
        super(id, pageRef);

        this.reference = reference;

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        window = new ModalWindow("taskWin");
        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setCookieName(VIEW_TASK_WIN_COOKIE_NAME);
        add(window);

        ((Tasks) pageRef.getPage()).setWindowClosedCallback(window, container);

        paginatorRows = prefMan.getPaginatorRows(getWebRequest(), Constants.PREF_SYNC_TASKS_PAGINATOR_ROWS);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(container);
        }
    }

    protected void initTasksTable() {

        table = Tasks.updateTaskTable(
                getColumns(),
                new TasksProvider<T>(restClient, paginatorRows, getId(), this.reference),
                container,
                0,
                this.pageRef,
                restClient);

        Form paginatorForm = new Form("PaginatorForm");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(), Constants.PREF_SYNC_TASKS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));

                table = Tasks.updateTaskTable(
                        getColumns(),
                        new TasksProvider<T>(restClient, paginatorRows, getId(), reference),
                        container,
                        table == null ? 0 : (int) table.getCurrentPage(),
                        pageRef,
                        restClient);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        container.add(paginatorForm);

        // create new task
        AjaxLink<Void> createLink = new ClearIndicatingAjaxLink<Void>("createLink", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return reference.equals(SyncTaskTO.class) ? new SyncTaskModalPage(window, new SyncTaskTO(),
                                pageRef) : new PushTaskModalPage(window, new PushTaskTO(), pageRef);
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createLink, RENDER, xmlRolesReader.getAllAllowedRoles(TASKS, "create"));

        add(createLink);

    }

    protected abstract List<IColumn<AbstractTaskTO, String>> getColumns();
}
