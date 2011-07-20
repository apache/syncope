/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Tasks page.
 */
public class PropagationTasks extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            PropagationTasks.class);

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    @SpringBean
    private TaskRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private int paginatorRows;

    private WebMarkupContainer container;

    /**
     * Response flag set by the Modal Window after the operation is completed:
     * TRUE if the operation succedes, FALSE otherwise
     */
    private boolean operationResult = false;

    private ModalWindow window;

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    public PropagationTasks(
            final String id) {

        super(id);

        add(window = new ModalWindow("taskWin"));

        paginatorRows = prefMan.getPaginatorRows(
                getWebRequest(),
                Constants.PREF_TASKS_PAGINATOR_ROWS);

        List<IColumn<TaskTO>> columns = new ArrayList<IColumn<TaskTO>>();

        columns.add(new PropertyColumn(
                new Model(getString("id")), "id", "id"));

        columns.add(new PropertyColumn(
                new Model(getString("resource")), "resource", "resource"));

        columns.add(new PropertyColumn(
                new Model(getString("accountId")), "accountId", "accountId"));

        columns.add(new PropertyColumn(
                new Model(getString("propagationMode")),
                "propagationMode", "propagationMode"));

        columns.add(new PropertyColumn(
                new Model(getString("resourceOperationType")),
                "resourceOperationType", "resourceOperationType"));

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "detail"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskTO>> cellItem,
                    final String componentId,
                    final IModel<TaskTO> model) {

                final TaskTO taskTO = model.getObject();

                AjaxLink viewLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            @Override
                            public Page createPage() {
                                return new PTaskModalPage(
                                        (BasePage) getPage(), window, taskTO);
                            }
                        });

                        window.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId,
                        model);
                panel.add(viewLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Tasks",
                        "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "execute"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskTO>> cellItem,
                    final String componentId,
                    final IModel<TaskTO> model) {

                final TaskTO taskTO = model.getObject();

                AjaxLink executeLink = new IndicatingAjaxLink("link") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.startExecution(taskTO.getId());
                            getSession().info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        target.addComponent(getPage().get("feedback"));
                        target.addComponent(container);
                    }
                };

                executeLink.add(new Label("linkTitle", getString("execute")));

                LinkPanel panel = new LinkPanel(componentId);
                panel.add(executeLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Tasks", "execute"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "delete"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<TaskTO>> cellItem,
                    final String componentId,
                    final IModel<TaskTO> model) {

                final TaskTO taskTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.delete(taskTO.getId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.addComponent(container);
                        target.addComponent(getPage().get("feedback"));
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                String allowedRoles = xmlRolesReader.getAllAllowedRoles("Tasks",
                        "delete");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<TaskTO> table =
                new AjaxFallbackDefaultDataTable<TaskTO>(
                "datatable", columns, new TasksProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(getPage().get("feedback"));
                            operationResult = false;
                        }
                    }
                });

        window.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        window.setInitialHeight(WIN_HEIGHT);
        window.setInitialWidth(WIN_WIDTH);
        window.setPageMapName("view-task-win");
        window.setCookieName("view-task-win");

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequest(), (WebResponse) getResponse(),
                        Constants.PREF_TASKS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));

                table.setRowsPerPage(paginatorRows);

                target.addComponent(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    private class TasksProvider extends SortableDataProvider<TaskTO> {

        private SortableDataProviderComparator<TaskTO> comparator;

        public TasksProvider() {
            super();
            //Default sorting
            setSort("id", true);
            comparator = new SortableDataProviderComparator<TaskTO>(this);
        }

        @Override
        public Iterator<PropagationTaskTO> iterator(int first, int count) {
            final List<PropagationTaskTO> tasks =
                    restClient.listPropagationTasks(
                    (first / paginatorRows) + 1, count);
            Collections.sort(tasks, comparator);
            return tasks.iterator();
        }

        @Override
        public int size() {
            return restClient.count(getId());
        }

        @Override
        public IModel<TaskTO> model(final TaskTO object) {
            return new CompoundPropertyModel<TaskTO>(object);
        }
    }
}
