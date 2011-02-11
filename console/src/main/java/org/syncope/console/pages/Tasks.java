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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
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
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.Utility;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Tasks page.
 */
public class Tasks extends BasePage {

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 500;

    @SpringBean
    private TaskRestClient restClient;

    @SpringBean
    private Utility utility;

    private int paginatorRows;

    private WebMarkupContainer container;

    /**
     * Response flag set by the Modal Window after the operation is completed:
     * TRUE if the operation succedes, FALSE otherwise
     */
    private boolean operationResult = false;

    private ModalWindow window;

    public Tasks(final PageParameters parameters) {
        super(parameters);

        add(window = new ModalWindow("taskWin"));

        paginatorRows = utility.getPaginatorRowsToDisplay(
                Constants.CONF_TASKS_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("id")),
                "id", "id"));

        columns.add(new PropertyColumn(new Model(getString("accountId")),
                "accountId", "accountId"));

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "open"))) {

            public void populateItem(Item<ICellPopulator<TaskTO>> cellItem,
                    String componentId, IModel<TaskTO> model) {
                final TaskTO taskTO = model.getObject();

                AjaxLink viewLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            public Page createPage() {
                                TaskModalPage modalPage = new TaskModalPage(
                                        Tasks.this, window, taskTO);
                                return modalPage;
                            }
                        });

                        window.show(target);

                    }
                };
                EditLinkPanel panel = new EditLinkPanel(componentId,
                        model);
                panel.add(viewLink);

                String allowedRoles = null;

                allowedRoles = xmlRolesReader.getAllAllowedRoles("Tasks",
                        "read");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "execute"))) {

            public void populateItem(Item<ICellPopulator<TaskTO>> cellItem,
                    String componentId, IModel<TaskTO> model) {
                final TaskTO taskTO = model.getObject();

                AjaxLink executeLink = new IndicatingAjaxLink("link") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        boolean res = false;

                        try {
                            res = restClient.startTaskExecution(taskTO.getId());
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }

                        if (res) {
                            getSession().info(getString("operation_succeded"));
                        }

                        target.addComponent(getPage().get("feedback"));
                        target.addComponent(container);
                    }
                };

                executeLink.add(new Label("linkTitle", getString("execute")));

                LinkPanel panel = new LinkPanel(componentId);
                panel.add(executeLink);

                String allowedRoles = null;

                allowedRoles = xmlRolesReader.getAllAllowedRoles("Tasks",
                        "execute");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<TaskTO>(new Model<String>(getString(
                "delete"))) {

            public void populateItem(Item<ICellPopulator<TaskTO>> cellItem,
                    String componentId, IModel<TaskTO> model) {
                final TaskTO taskTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        try {
                            restClient.deleteTask(taskTO.getId());
                        } catch (SyncopeClientCompositeErrorException scce) {
                            error(scce.getMessage());
                        }
                        target.addComponent(container);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };
                DeleteLinkPanel panel = new DeleteLinkPanel(componentId,
                        model);
                panel.add(deleteLink);

                String allowedRoles = null;

                allowedRoles = xmlRolesReader.getAllAllowedRoles("Tasks",
                        "delete");

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedRoles);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new TasksProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(feedbackPanel);
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
                new PropertyModel(this, "paginatorRows"), utility.
                paginatorRowsChooser());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                utility.updatePaginatorRows(Constants.CONF_TASKS_PAGINATOR_ROWS,
                        paginatorRows);

                table.setRowsPerPage(paginatorRows);

                target.addComponent(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    class TasksProvider extends SortableDataProvider<TaskTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        public TasksProvider() {
            //Default sorting
            setSort("id", true);
        }

        @Override
        public Iterator<TaskTO> iterator(int first, int count) {
            List<TaskTO> list = getTasksListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getTasksListDB().size();
        }

        @Override
        public IModel<TaskTO> model(final TaskTO task) {
            return new AbstractReadOnlyModel<TaskTO>() {

                @Override
                public TaskTO getObject() {
                    return task;
                }
            };
        }

        public List<TaskTO> getTasksListDB() {
            List<TaskTO> list = restClient.getAllTasks();

            //Mock task for test purpose
        /*if(list == null || list.size() == 0) {
            list = new ArrayList<TaskTO>();

            TaskTO task1 = new TaskTO();
            task1.setAccountId("luisAccount");
            task1.setId(1L);
            task1.setResource("testResource");

            List executions = new ArrayList<TaskExecutionTO>();

            TaskExecutionTO taskExecution = new TaskExecutionTO();
            taskExecution.setStartDate(new Date());
            taskExecution.setEndDate(new Date());
            taskExecution.setMessage("Prova");
            taskExecution.setStatus(TaskExecutionStatus.CREATED);
            executions.add(taskExecution);

            task1.setExecutions(executions);

            list.add(task1);
            }*/

            return list;
        }

        class SortableDataProviderComparator implements
                Comparator<TaskTO>, Serializable {

            public int compare(final TaskTO o1,
                    final TaskTO o2) {
                PropertyModel<Comparable> model1 =
                        new PropertyModel<Comparable>(o1,
                        getSort().getProperty());
                PropertyModel<Comparable> model2 =
                        new PropertyModel<Comparable>(o2,
                        getSort().getProperty());

                int result = 1;

                if (model1.getObject() == null && model2.getObject() == null) {
                    result = 0;
                } else if (model1.getObject() == null) {
                    result = 1;
                } else if (model2.getObject() == null) {
                    result = -1;
                } else {
                    result = ((Comparable) model1.getObject()).compareTo(
                            model2.getObject());
                }

                result = getSort().isAscending() ? result : -result;

                return result;
            }
        }
    }
}
