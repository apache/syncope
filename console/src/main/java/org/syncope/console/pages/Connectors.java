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
import org.apache.wicket.PageParameters;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Connectors WebPage.
 */
public class Connectors extends BasePage {

    private static final int WIN_HEIGHT = 400;

    private static final int WIN_WIDTH = 400;

    @SpringBean
    private ConnectorRestClient restClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createConnectorWin;

    private final ModalWindow editConnectorWin;

    private WebMarkupContainer container;

    /**
     * Response flag set by the Modal Window after the operation is completed.
     */
    private boolean operationResult = false;

    private int paginatorRows;

    public Connectors(PageParameters parameters) {
        super(parameters);

        add(createConnectorWin = new ModalWindow("createConnectorWin"));
        add(editConnectorWin = new ModalWindow("editConnectorWin"));

        paginatorRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_CONNECTORS_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model(getString("id")),
                "id", "id"));

        columns.add(new PropertyColumn(new Model(getString("name")),
                "connectorName", "connectorName"));

        columns.add(new PropertyColumn(new Model(getString("displayName")),
                "displayName", "displayName"));

        columns.add(new PropertyColumn(new Model(getString("version")),
                "version", "version"));

        columns.add(new PropertyColumn(new Model(getString("bundleName")),
                "bundleName", "bundleName"));

        columns.add(new AbstractColumn<ConnInstanceTO>(new Model<String>(
                getString("edit"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConnInstanceTO>> cellItem,
                    final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                final AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editConnectorWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    @Override
                                    public Page createPage() {
                                        return new ConnectorModalPage(
                                                Connectors.this,
                                                editConnectorWin, connectorTO,
                                                false);
                                    }
                                });

                        editConnectorWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Connectors", "read"));

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<ConnInstanceTO>(
                new Model<String>(getString("delete"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConnInstanceTO>> cellItem,
                    final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        try {

                            if (!checkDeleteIsForbidden(connectorTO)) {
                                restClient.delete(connectorTO.getId());
                                info(getString("operation_succeded"));
                            } else {
                                error(getString("delete_error"));
                            }

                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString("operation_error"));

                            LOG.error("While deleting connector "
                                    + connectorTO.getId(), e);
                        }

                        target.addComponent(container);
                        target.addComponent(feedbackPanel);
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);
                panel.add(deleteLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        xmlRolesReader.getAllAllowedRoles(
                        "Connectors", "delete"));

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("datatable", columns,
                new ConnectorsProvider(), paginatorRows);

        container = new WebMarkupContainer("container");
        container.add(table);
        container.setOutputMarkupId(true);

        add(container);

        setWindowClosedCallback(createConnectorWin, container);
        setWindowClosedCallback(editConnectorWin, container);

        createConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConnectorWin.setInitialHeight(WIN_HEIGHT);
        createConnectorWin.setInitialWidth(WIN_WIDTH);
        createConnectorWin.setPageMapName("create-conn-modal");
        createConnectorWin.setCookieName("create-conn-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setInitialHeight(WIN_HEIGHT);
        editConnectorWin.setInitialWidth(WIN_WIDTH);
        editConnectorWin.setPageMapName("edit-conn-modal");
        editConnectorWin.setCookieName("edit-conn-modal");

        AjaxLink createConnectorLink = new IndicatingAjaxLink(
                "createConnectorLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        ConnectorModalPage form = new ConnectorModalPage(
                                Connectors.this, editConnectorWin,
                                new ConnInstanceTO(), true);
                        return form;
                    }
                });

                createConnectorWin.show(target);
            }
        };

        String allowedRoles = xmlRolesReader.getAllAllowedRoles(
                "Connectors", "create");
        MetaDataRoleAuthorizationStrategy.authorize(createConnectorLink, ENABLE,
                allowedRoles);

        add(createConnectorLink);

        Form paginatorForm = new Form("PaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "paginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_CONNECTORS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                table.setRowsPerPage(paginatorRows);

                target.addComponent(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    /**
     * Check if the delete action is forbidden
     * @param ConnectorInstanceTO object to check
     * @return true if the action is forbidden, false otherwise
     */
    public boolean checkDeleteIsForbidden(ConnInstanceTO connectorTO) {

        boolean forbidden = false;

        for (ResourceTO resourceTO : resourceRestClient.getAllResources()) {
            if (resourceTO.getConnectorId().equals(connectorTO.getId())) {
                forbidden = true;
            }
        }

        return forbidden;
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param current window
     * @param container to refresh
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

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
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    class ConnectorsProvider extends SortableDataProvider<ConnInstanceTO> {

        private SortableDataProviderComparator<ConnInstanceTO> comparator;

        public ConnectorsProvider() {
            //Default sorting
            setSort("id", true);
            comparator =
                    new SortableDataProviderComparator<ConnInstanceTO>(
                    this);
        }

        @Override
        public Iterator<ConnInstanceTO> iterator(int first, int count) {
            List<ConnInstanceTO> list = getConnectorsListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getConnectorsListDB().size();
        }

        @Override
        public IModel<ConnInstanceTO> model(
                final ConnInstanceTO connector) {

            return new AbstractReadOnlyModel<ConnInstanceTO>() {

                @Override
                public ConnInstanceTO getObject() {
                    return connector;
                }
            };
        }

        public List<ConnInstanceTO> getConnectorsListDB() {
            return restClient.getAllConnectors();
        }
    }
}
