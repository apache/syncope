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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.AbstractSearchResultPanel;
import org.apache.syncope.console.pages.panels.AjaxDataTablePanel;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.console.wicket.markup.html.form.LinkPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Resources WebPage.
 */
public class Resources extends BasePage {

    private static final long serialVersionUID = -3789252860990261728L;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createResourceWin;

    private final ModalWindow editResourceWin;

    private final ModalWindow createConnectorWin;

    private final ModalWindow editConnectorWin;

    private static final int WIN_HEIGHT = 600;

    private static final int WIN_WIDTH = 1100;

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

        AjaxLink<Void> reloadLink = new ClearIndicatingAjaxLink<Void>("reloadLink", getPageReference()) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.reload();
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
                target.add(feedbackPanel);
                target.add(connectorContainer);
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                final AjaxCallListener ajaxCallListener = new AjaxCallListener() {

                    private static final long serialVersionUID = 7160235486520935153L;

                    @Override
                    public CharSequence getPrecondition(final Component component) {
                        return "if (!confirm('" + getString("confirmReloadConnectors") + "')) "
                                + "{return false;} else {return true;}";
                    }
                };
                attributes.getAjaxCallListeners().add(ajaxCallListener);
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(reloadLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Connectors", "reload"));
        add(reloadLink);

        resourcePaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_RESOURCES_PAGINATOR_ROWS);
        connectorPaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_CONNECTORS_PAGINATOR_ROWS);

        setupResources();
        setupConnectors();
    }

    private void setupResources() {
        List<IColumn<ResourceTO, String>> columns = new ArrayList<IColumn<ResourceTO, String>>();

        columns.add(new PropertyColumn(new StringResourceModel("name", this, null), "name", "name"));

        columns.add(new AbstractColumn<ResourceTO, String>(
                new StringResourceModel("connector", this, null, "connector")) {

            private static final long serialVersionUID = 8263694778917279290L;

            @Override
            public void populateItem(final Item<ICellPopulator<ResourceTO>> cellItem, final String componentId,
                    final IModel<ResourceTO> rowModel) {

                final AjaxLink<String> editLink = new ClearIndicatingAjaxLink<String>("link", getPageReference()) {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    protected void onClickInternal(final AjaxRequestTarget target) {

                        editConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ConnectorModalPage(Resources.this.getPageReference(), editConnectorWin,
                                        connectorRestClient.read(rowModel.getObject().getConnectorId()));
                            }
                        });

                        editConnectorWin.show(target);
                    }
                };
                editLink.add(new Label("linkTitle", rowModel.getObject().getConnectorDisplayName()));

                LinkPanel editConnPanel = new LinkPanel(componentId);
                editConnPanel.add(editLink);

                cellItem.add(editConnPanel);

                MetaDataRoleAuthorizationStrategy.authorize(editConnPanel, ENABLE, xmlRolesReader.getAllAllowedRoles(
                        "Connectors", "read"));
            }
        });

        columns.add(new PropertyColumn(new StringResourceModel(
                "propagationPrimary", this, null), "propagationPrimary", "propagationPrimary"));
        columns.add(new PropertyColumn(new StringResourceModel(
                "propagationPriority", this, null), "propagationPriority", "propagationPriority"));

        columns.add(new AbstractColumn<ResourceTO, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<ResourceTO>> cellItem, final String componentId,
                    final IModel<ResourceTO> model) {
                final ResourceTO resourceTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        editResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                ResourceModalPage form = new ResourceModalPage(Resources.this.getPageReference(),
                                        editResourceWin, resourceTO, false);
                                return form;
                            }
                        });

                        editResourceWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Resources");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {

                            resourceRestClient.delete(resourceTO.getName());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString(Constants.ERROR) + ":" + e.getMessage());

                            LOG.error("While deleting resource " + resourceTO.getName(), e);
                        }

                        target.add(feedbackPanel);
                        target.add(resourceContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Resources");

                cellItem.add(panel);
            }
        });

        final AjaxDataTablePanel<ResourceTO, String> table = new AjaxDataTablePanel<ResourceTO, String>(
                "resourceDatatable",
                columns,
                (ISortableDataProvider<ResourceTO, String>) new ResourcesProvider(),
                resourcePaginatorRows,
                Arrays.asList(new ActionLink.ActionType[] {ActionLink.ActionType.DELETE}),
                resourceRestClient,
                "name",
                "Resources",
                getPageReference());

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

        AjaxLink createResourceLink = new ClearIndicatingAjaxLink("createResourceLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        final ResourceModalPage windows = new ResourceModalPage(Resources.this.getPageReference(),
                                editResourceWin, new ResourceTO(), true);
                        return windows;
                    }
                });

                createResourceWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createResourceLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Resources", "create"));

        add(createResourceLink);

        final Form paginatorForm = new Form("resourcePaginatorForm");

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "resourcePaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_RESOURCES_PAGINATOR_ROWS, String
                        .valueOf(resourcePaginatorRows));

                table.setItemsPerPage(resourcePaginatorRows);
                target.add(resourceContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    private void setupConnectors() {
        List<IColumn<ConnInstanceTO, String>> columns = new ArrayList<IColumn<ConnInstanceTO, String>>();

        columns.add(new PropertyColumn(
                new StringResourceModel("id", this, null), "id", "id"));
        columns.add(new PropertyColumn(
                new StringResourceModel("name", this, null), "connectorName", "connectorName"));
        columns.add(new PropertyColumn(
                new StringResourceModel("displayName", this, null), "displayName", "displayName"));
        columns.add(new PropertyColumn(
                new StringResourceModel("bundleName", this, null), "bundleName", "bundleName"));
        columns.add(new PropertyColumn(
                new StringResourceModel("version", this, null), "version", "version"));
        columns.add(new AbstractColumn<ConnInstanceTO, String>(new StringResourceModel("actions", this, null, "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<ConnInstanceTO>> cellItem, final String componentId,
                    final IModel<ConnInstanceTO> model) {

                final ConnInstanceTO connectorTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ConnectorModalPage(Resources.this.getPageReference(), editConnectorWin,
                                        connectorTO);
                            }
                        });

                        editConnectorWin.show(target);
                    }
                }, ActionLink.ActionType.EDIT, "Connectors");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            connectorRestClient.delete(connectorTO.getId());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientCompositeErrorException e) {
                            error(getString(Constants.ERROR) + ":" + e.getMessage());

                            LOG.error("While deleting connector " + connectorTO.getId(), e);
                        }

                        target.add(connectorContainer);
                        target.add(feedbackPanel);
                    }
                }, ActionLink.ActionType.DELETE, "Connectors");

                cellItem.add(panel);
            }
        });

        final AjaxDataTablePanel<ConnInstanceTO, String> table = new AjaxDataTablePanel<ConnInstanceTO, String>(
                "connectorDatatable",
                columns,
                (ISortableDataProvider<ConnInstanceTO, String>) new ConnectorsProvider(),
                connectorPaginatorRows,
                Arrays.asList(new ActionLink.ActionType[] {ActionLink.ActionType.DELETE}),
                connectorRestClient,
                "id",
                "Connectors",
                getPageReference());

        connectorContainer = new WebMarkupContainer("connectorContainer");
        connectorContainer.add(table);
        connectorContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(connectorContainer, RENDER, xmlRolesReader.getAllAllowedRoles(
                "Connectors", "list"));

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

        AjaxLink createConnectorLink = new ClearIndicatingAjaxLink("createConnectorLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        ConnectorModalPage form = new ConnectorModalPage(Resources.this.getPageReference(),
                                editConnectorWin, new ConnInstanceTO());
                        return form;
                    }
                });

                createConnectorWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createConnectorLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                "Connectors", "create"));

        add(createConnectorLink);

        Form paginatorForm = new Form("connectorPaginatorForm");

        MetaDataRoleAuthorizationStrategy.authorize(paginatorForm, RENDER, xmlRolesReader.getAllAllowedRoles(
                "Connectors", "list"));

        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "connectorPaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_CONNECTORS_PAGINATOR_ROWS, String
                        .valueOf(connectorPaginatorRows));
                table.setItemsPerPage(connectorPaginatorRows);

                target.add(connectorContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    class ResourcesProvider extends SortableDataProvider<ResourceTO, String> {

        private static final long serialVersionUID = -9055916672926643975L;

        private SortableDataProviderComparator<ResourceTO> comparator;

        public ResourcesProvider() {
            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<ResourceTO>(this);
        }

        @Override
        public Iterator<ResourceTO> iterator(final long first, final long count) {
            List<ResourceTO> list = getResourcesListDB();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
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

    private class ConnectorsProvider extends SortableDataProvider<ConnInstanceTO, String> {

        private static final long serialVersionUID = 4445909568349448518L;

        private SortableDataProviderComparator<ConnInstanceTO> comparator;

        public ConnectorsProvider() {
            //Default sorting
            setSort("id", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<ConnInstanceTO>(this);
        }

        @Override
        public Iterator<ConnInstanceTO> iterator(long first, long count) {
            List<ConnInstanceTO> list = getConnectorsListDB();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return getConnectorsListDB().size();
        }

        @Override
        public IModel<ConnInstanceTO> model(final ConnInstanceTO connector) {

            return new AbstractReadOnlyModel<ConnInstanceTO>() {

                private static final long serialVersionUID = -6033068018293569398L;

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

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(resourceContainer);
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(connectorContainer);
        }
    }
}