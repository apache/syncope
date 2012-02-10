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
import org.syncope.console.wicket.markup.html.form.ActionLink;
import org.syncope.console.wicket.markup.html.form.ActionLinksPanel;

/**
 * Resources WebPage.
 */
public class Resources extends BasePage {

    private static final long serialVersionUID = -3789252860990261728L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createResourceWin;

    private final ModalWindow editResourceWin;

    private final ModalWindow createConnectorWin;

    private final ModalWindow editConnectorWin;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 900;

    private WebMarkupContainer resourceContainer;

    private WebMarkupContainer connectorContainer;

    private int resourcePaginatorRows;

    private int connectorPaginatorRows;

    public Resources(final PageParameters parameters) {
        super(parameters);

        add(createResourceWin = new ModalWindow("createResourceWin"));
        add(editResourceWin = new ModalWindow("editResourceWin"));
        add(createConnectorWin = new ModalWindow("createConnectorWin"));
        add(editConnectorWin = new ModalWindow("editConnectorWin"));

        add(feedbackPanel);

        resourcePaginatorRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_RESOURCES_PAGINATOR_ROWS);
        connectorPaginatorRows = prefMan.getPaginatorRows(
                getRequest(),
                Constants.PREF_CONNECTORS_PAGINATOR_ROWS);

        setupResources();
        setupConnectors();
    }

    private void setupResources() {
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(
                new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn(
                new ResourceModel("propagationPrimary"),
                "propagationPrimary", "propagationPrimary"));
        columns.add(new PropertyColumn(
                new ResourceModel("propagationPriority"),
                "propagationPriority", "propagationPriority"));

        columns.add(new AbstractColumn<ResourceTO>(new ResourceModel("actions",
                "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ResourceTO>> cellItem,
                    final String componentId,
                    final IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID =
                                    -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage(
                                        Resources.this.getPageReference(),
                                        editResourceWin, resourceTO, false);
                                return form;
                            }
                        });

                        editResourceWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Resources", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {

                            resourceRestClient.delete(resourceTO.getName());
                            info(getString("operation_succeded"));

                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString("operation_error"));

                            LOG.error("While deleting resource "
                                    + resourceTO.getName(), e);
                        }

                        target.add(feedbackPanel);
                        target.add(resourceContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Resources", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("resourceDatatable", columns,
                new ResourcesProvider(), resourcePaginatorRows);

        resourceContainer = new WebMarkupContainer("resourceContainer");
        resourceContainer.add(table);
        resourceContainer.setOutputMarkupId(true);

        add(resourceContainer);

        setWindowClosedCallback(createResourceWin, resourceContainer);
        setWindowClosedCallback(editResourceWin, resourceContainer);

        createResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createResourceWin.setInitialHeight(WIN_HEIGHT);
        createResourceWin.setInitialWidth(WIN_WIDTH);
        createResourceWin.setCookieName("create-res-modal");

        editResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editResourceWin.setInitialHeight(WIN_HEIGHT);
        editResourceWin.setInitialWidth(WIN_WIDTH);
        editResourceWin.setCookieName("edit-res-modal");

        add(new IndicatingAjaxLink("createResourceLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(AjaxRequestTarget target) {

                createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID =
                            -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        final ResourceModalPage windows = new ResourceModalPage(
                                Resources.this.getPageReference(),
                                editResourceWin,
                                new ResourceTO(), true);
                        return windows;
                    }
                });

                createResourceWin.show(target);
            }
        });

        final Form paginatorForm = new Form("resourcePaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "resourcePaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(),
                        Constants.PREF_RESOURCES_PAGINATOR_ROWS,
                        String.valueOf(resourcePaginatorRows));

                table.setItemsPerPage(resourcePaginatorRows);
                target.add(resourceContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    private void setupConnectors() {
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
                new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConnInstanceTO>> cellItem,
                    final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                final ActionLinksPanel panel =
                        new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editConnectorWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID =
                                            -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ConnectorModalPage(
                                                Resources.this.getPageReference(),
                                                editConnectorWin, connectorTO);
                                    }
                                });

                        editConnectorWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Connectors", "read");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            connectorRestClient.delete(connectorTO.getId());
                            info(getString("operation_succeded"));
                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString("operation_error"));

                            LOG.error("While deleting connector "
                                    + connectorTO.getId(), e);
                        }

                        target.add(connectorContainer);
                        target.add(feedbackPanel);
                    }
                }, ActionLink.ActionType.DELETE, "Connectors", "delete");

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable table =
                new AjaxFallbackDefaultDataTable("connectorDatatable", columns,
                new ConnectorsProvider(), connectorPaginatorRows);

        connectorContainer = new WebMarkupContainer("connectorContainer");
        connectorContainer.add(table);
        connectorContainer.setOutputMarkupId(true);

        add(connectorContainer);

        setWindowClosedCallback(createConnectorWin, connectorContainer);
        setWindowClosedCallback(editConnectorWin, connectorContainer);

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
                                Resources.this.getPageReference(),
                                editConnectorWin, new ConnInstanceTO());
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

        Form paginatorForm = new Form("connectorPaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "connectorPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                prefMan.set(getRequest(),
                        getResponse(),
                        Constants.PREF_CONNECTORS_PAGINATOR_ROWS,
                        String.valueOf(connectorPaginatorRows));
                table.setItemsPerPage(connectorPaginatorRows);

                target.add(connectorContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    class ResourcesProvider extends SortableDataProvider<ResourceTO> {

        private static final long serialVersionUID = -9055916672926643975L;

        private SortableDataProviderComparator<ResourceTO> comparator;

        public ResourcesProvider() {
            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator =
                    new SortableDataProviderComparator<ResourceTO>(this);
        }

        @Override
        public Iterator<ResourceTO> iterator(final int first, final int count) {
            List<ResourceTO> list = getResourcesListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getResourcesListDB().size();
        }

        @Override
        public IModel<ResourceTO> model(final ResourceTO resource) {
            return new AbstractReadOnlyModel<ResourceTO>() {

                private static final long serialVersionUID = 8952474152465381634L;

                @Override
                public ResourceTO getObject() {
                    return resource;
                }
            };
        }

        public List<ResourceTO> getResourcesListDB() {
            return resourceRestClient.getAllResources();
        }
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
            return connectorRestClient.getAllConnectors();
        }
    }
}
