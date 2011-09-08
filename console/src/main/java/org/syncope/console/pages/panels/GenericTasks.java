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
package org.syncope.console.pages.panels;

import java.util.ArrayList;
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.pages.BasePage;
import org.syncope.console.pages.GTaskModalPage;
import org.syncope.console.pages.Tasks.DatePropertyColumn;
import org.syncope.console.pages.Tasks.TasksProvider;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

public class GenericTasks extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            GenericTasks.class);

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

    public GenericTasks(String id, IModel<?> model) {
        super(id, model);
    }

    public GenericTasks(String id) {
        super(id);
        add(window = new ModalWindow("taskWin"));

        paginatorRows = prefMan.getPaginatorRows(
                getWebRequest(),
                Constants.PREF_TASKS_PAGINATOR_ROWS);

        List<IColumn<SchedTaskTO>> columns =
                new ArrayList<IColumn<SchedTaskTO>>();

        columns.add(new PropertyColumn(
                new ResourceModel("id"), "id", "id"));

        columns.add(new PropertyColumn(
                new ResourceModel("class"), "jobClassName", "jobClassName"));

        columns.add(new DatePropertyColumn(
                new ResourceModel("lastExec"), "lastExec", "lastExec", null));

        columns.add(new DatePropertyColumn(
                new ResourceModel("nextExec"), "nextExec", "nextExec", null));

        columns.add(new AbstractColumn<SchedTaskTO>(
                new ResourceModel("detail")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<SchedTaskTO>> cellItem,
                    final String componentId,
                    final IModel<SchedTaskTO> model) {

                final SchedTaskTO taskTO = model.getObject();

                AjaxLink viewLink = new IndicatingAjaxLink("editLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID =
                                    -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new GTaskModalPage(
                                        (BasePage) getPage(), window, taskTO);
                            }
                        });

                        window.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(viewLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Tasks", "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<SchedTaskTO>(
                new ResourceModel("execute")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<SchedTaskTO>> cellItem,
                    final String componentId,
                    final IModel<SchedTaskTO> model) {

                final SchedTaskTO taskTO = model.getObject();

                AjaxLink executeLink = new IndicatingAjaxLink("link") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

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

        columns.add(new AbstractColumn<SchedTaskTO>(
                new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<SchedTaskTO>> cellItem,
                    final String componentId,
                    final IModel<SchedTaskTO> model) {

                final SchedTaskTO taskTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

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

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles("Tasks", "delete"));

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable<SchedTaskTO> table =
                new AjaxFallbackDefaultDataTable<SchedTaskTO>(
                "datatable", columns, new TasksProvider(
                restClient, paginatorRows, getId(), SchedTaskTO.class),
                paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID = 8804221891699487139L;

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

            private static final long serialVersionUID = -1107858522700306810L;

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

        // create new user
        AjaxLink createLink = new IndicatingAjaxLink("createLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new GTaskModalPage((BasePage) getPage(), window,
                                new SchedTaskTO());
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createLink, RENDER,
                xmlRolesReader.getAllAllowedRoles("Tasks", "create"));

        add(createLink);
    }
}
