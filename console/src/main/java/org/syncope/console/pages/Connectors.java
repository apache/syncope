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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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

    private static final int WIN_WIDTH = 600;

    private static final long serialVersionUID = 4983541555239865907L;

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
                getRequest(),
                Constants.PREF_CONNECTORS_PAGINATOR_ROWS);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new ResourceModel("id"),
                "id", "id"));

        columns.add(new PropertyColumn(new ResourceModel("name"),
                "connectorName", "connectorName"));

        columns.add(new PropertyColumn(new ResourceModel("displayName"),
                "displayName", "displayName"));

        columns.add(new PropertyColumn(new ResourceModel("version"),
                "version", "version"));

        columns.add(new PropertyColumn(new ResourceModel("bundleName"),
                "bundleName", "bundleName"));

        columns.add(new AbstractColumn<ConnInstanceTO>(
                new ResourceModel("edit")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConnInstanceTO>> cellItem,
                    final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                final AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editConnectorWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ConnectorModalPage(
                                                Connectors.this.getPageReference(),
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
                new ResourceModel("delete")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConnInstanceTO>> cellItem,
                    final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

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

                        target.add(container);
                        target.add(feedbackPanel);
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
        createConnectorWin.setCookieName("create-conn-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setInitialHeight(WIN_HEIGHT);
        editConnectorWin.setInitialWidth(WIN_WIDTH);
        editConnectorWin.setCookieName("edit-conn-modal");

        AjaxLink createConnectorLink = new IndicatingAjaxLink(
                "createConnectorLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {

                createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        ConnectorModalPage form = new ConnectorModalPage(
                                Connectors.this.getPageReference(),
                                editConnectorWin, new ConnInstanceTO(), true);
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

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_CONNECTORS_PAGINATOR_ROWS,
                        String.valueOf(paginatorRows));
                table.setItemsPerPage(paginatorRows);

                target.add(container);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    /**
     * Check if the delete action is forbidden.
     *
     * @param connectorTO object to check
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
    private void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    private static final long serialVersionUID =
                            8804221891699487139L;

                    @Override
                    public void onClose(AjaxRequestTarget target) {
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

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    class ConnectorsProvider extends SortableDataProvider<ConnInstanceTO> {

        private static final long serialVersionUID = 4445909568349448518L;

        private SortableDataProviderComparator<ConnInstanceTO> comparator;

        public ConnectorsProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
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

                private static final long serialVersionUID =
                        -6033068018293569398L;

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
