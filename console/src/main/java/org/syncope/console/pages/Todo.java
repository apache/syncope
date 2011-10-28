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
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.web.client.RestClientException;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeSession;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.pages.Tasks.DatePropertyColumn;
import org.syncope.console.rest.TodoRestClient;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

public class Todo extends BasePage {

    private static final long serialVersionUID = -7122136682275797903L;

    @SpringBean
    private TodoRestClient restClient;

    private final ModalWindow editTodoWin;

    private static final int WIN_HEIGHT = 400;

    private static final int WIN_WIDTH = 600;

    @SpringBean
    private PreferenceManager prefMan;

    private WebMarkupContainer container;

    /**
     * Response flag set by the Modal Window after the operation
     * is completed.
     */
    private boolean operationResult = false;

    private int paginatorRows;

    public Todo(final PageParameters parameters) {
        super(parameters);

        container = new WebMarkupContainer("todoContainer");

        add(editTodoWin = new ModalWindow("editTodoWin"));

        paginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_TODO_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("taskId"),
                "taskId", "taskId"));
        columns.add(new PropertyColumn(new ResourceModel("key"),
                "key", "key"));
        columns.add(new PropertyColumn(new ResourceModel("description"),
                "description", "description"));
        columns.add(new DatePropertyColumn(new ResourceModel("createTime"),
                "createTime", "createTime", null));
        columns.add(new DatePropertyColumn(new ResourceModel("dueDate"),
                "dueDate", "dueDate", null));
        columns.add(new PropertyColumn(new ResourceModel("owner"),
                "owner", "owner"));
        columns.add(new AbstractColumn<WorkflowFormTO>(
                new ResourceModel("claim")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<WorkflowFormTO>> cellItem,
                    final String componentId,
                    final IModel<WorkflowFormTO> model) {

                final WorkflowFormTO formTO = model.getObject();
                AjaxLink claimLink = new IndicatingAjaxLink("link") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            restClient.claimForm(formTO.getTaskId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException scee) {
                            error(getString("error") + ":" + scee.getMessage());
                        }
                        target.add(feedbackPanel);
                        target.add(container);
                    }
                };

                claimLink.add(new Label("linkTitle", getString("claim")));

                Panel panel = new LinkPanel(componentId);
                panel.add(claimLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Todo", "claim"));

                cellItem.add(panel);
            }
        });
        columns.add(new AbstractColumn<WorkflowFormTO>(
                new ResourceModel("manage")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<WorkflowFormTO>> cellItem,
                    final String componentId,
                    final IModel<WorkflowFormTO> model) {

                final WorkflowFormTO formTO = model.getObject();
                Panel panel;
                if (SyncopeSession.get().getUserId().equals(
                        formTO.getOwner())) {

                    AjaxLink manageLink = new IndicatingAjaxLink("link") {

                        private static final long serialVersionUID =
                                -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            editTodoWin.setPageCreator(
                                    new ModalWindow.PageCreator() {

                                        @Override
                                        public Page createPage() {
                                            return new TodoModalPage(
                                                    Todo.this.getPageReference(),
                                                    editTodoWin, formTO);
                                        }
                                    });

                            editTodoWin.show(target);
                        }
                    };
                    manageLink.add(new Label("linkTitle", getString("manage")));

                    panel = new LinkPanel(componentId);
                    panel.add(manageLink);

                    MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                            xmlRolesReader.getAllAllowedRoles("Todo", "read"));

                } else {
                    panel = new EmptyPanel(componentId);
                }
                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable todoTable =
                new AjaxFallbackDefaultDataTable("todoTable", columns,
                new WorkflowFormProvider(), paginatorRows);

        container.add(todoTable);
        container.setOutputMarkupId(true);

        add(container);

        Form paginatorForm = new Form("paginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_TODO_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                todoTable.setItemsPerPage(paginatorRows);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);

        editTodoWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editTodoWin.setInitialHeight(WIN_HEIGHT);
        editTodoWin.setInitialWidth(WIN_WIDTH);
        editTodoWin.setCookieName("edit-todo-modal");

        setWindowClosedCallback(editTodoWin, container);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    private void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID =
                            8804221891699487139L;

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.add(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.add(feedbackPanel);
                            operationResult = false;
                        }
                    }
                });
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(final boolean operationResult) {
        this.operationResult = operationResult;
    }

    private class WorkflowFormProvider
            extends SortableDataProvider<WorkflowFormTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private SortableDataProviderComparator<WorkflowFormTO> comparator;

        public WorkflowFormProvider() {
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<WorkflowFormTO>(this);
        }

        @Override
        public Iterator<WorkflowFormTO> iterator(final int first,
                final int count) {

            List<WorkflowFormTO> list = getAllForms();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getAllForms().size();
        }

        @Override
        public IModel<WorkflowFormTO> model(
                final WorkflowFormTO configuration) {

            return new AbstractReadOnlyModel<WorkflowFormTO>() {

                private static final long serialVersionUID =
                        -2566070996511906708L;

                @Override
                public WorkflowFormTO getObject() {
                    return configuration;
                }
            };
        }

        private List<WorkflowFormTO> getAllForms() {
            List<WorkflowFormTO> list = null;

            try {
                list = restClient.getForms();
            } catch (RestClientException rce) {
                throw rce;
            }
            return list;
        }
    }
}
