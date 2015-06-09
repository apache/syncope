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
package org.apache.syncope.client.console.panels;

import static org.apache.wicket.Component.ENABLE;
import static org.apache.wicket.Component.RENDER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.pages.ConnectorModalPage;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources WebPage.
 */
public class Connectors extends Panel {

    private static final long serialVersionUID = -3789252860990261728L;

    protected static final Logger LOG = LoggerFactory.getLogger(Connectors.class);

    private static final int WIN_HEIGHT = 600;

    private static final int WIN_WIDTH = 1100;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createConnectorWin;

    private final ModalWindow editConnectorWin;

    private final int connectorPaginatorRows;

    private WebMarkupContainer connectorContainer;

    /**
     * Modal window to be used for user status management.
     */
    protected final ModalWindow statusmodal = new ModalWindow("statusModal");

    /**
     * Schemas to be shown modal window height.
     */
    private static final int STATUS_MODAL_WIN_HEIGHT = 500;

    /**
     * Schemas to be shown modal window width.
     */
    private static final int STATUS_MODAL_WIN_WIDTH = 700;

    private final PageReference pageRef;

    public Connectors(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        createConnectorWin = new ModalWindow("createConnectorWin");
        add(createConnectorWin);

        editConnectorWin = new ModalWindow("editConnectorWin");
        add(editConnectorWin);

        statusmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        statusmodal.setInitialHeight(STATUS_MODAL_WIN_HEIGHT);
        statusmodal.setInitialWidth(STATUS_MODAL_WIN_WIDTH);
        statusmodal.setCookieName("status-modal");
        add(statusmodal);

        AjaxLink<Void> reloadLink = new ClearIndicatingAjaxLink<Void>("reloadLink", pageRef) {

            private static final long serialVersionUID = 3109256773218160485L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                try {
                    connectorRestClient.reload();
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
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
        MetaDataRoleAuthorizationStrategy.authorize(reloadLink, ENABLE, Entitlement.CONNECTOR_RELOAD);
        add(reloadLink);

        connectorPaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_CONNECTORS_PAGINATOR_ROWS);

        setupConnectors();
    }

    private void setupConnectors() {
        List<IColumn<ConnInstanceTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<ConnInstanceTO, String>(
                new StringResourceModel("key", this, null), "key", "key"));
        columns.add(new PropertyColumn<ConnInstanceTO, String>(
                new StringResourceModel("name", this, null), "connectorName", "connectorName"));
        columns.add(new PropertyColumn<ConnInstanceTO, String>(
                new StringResourceModel("displayName", this, null), "displayName", "displayName"));
        columns.add(new PropertyColumn<ConnInstanceTO, String>(
                new StringResourceModel("bundleName", this, null), "bundleName", "bundleName"));
        columns.add(new PropertyColumn<ConnInstanceTO, String>(
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

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ConnectorModalPage(Connectors.this.pageRef,
                                        editConnectorWin,
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
                            connectorRestClient.delete(connectorTO.getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());

                            LOG.error("While deleting connector " + connectorTO.getKey(), e);
                        }

                        target.add(connectorContainer);
                        ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, "Connectors");

                cellItem.add(panel);
            }
        });

        final AjaxDataTablePanel<ConnInstanceTO, String> table = new AjaxDataTablePanel<>(
                "connectorDatatable",
                columns,
                (ISortableDataProvider<ConnInstanceTO, String>) new ConnectorsProvider(),
                connectorPaginatorRows,
                Arrays.asList(new ActionLink.ActionType[] { ActionLink.ActionType.DELETE }),
                connectorRestClient,
                "key",
                "Connectors",
                pageRef);

        connectorContainer = new WebMarkupContainer("connectorContainer");
        connectorContainer.add(table);
        connectorContainer.setOutputMarkupId(true);

        MetaDataRoleAuthorizationStrategy.authorize(connectorContainer, RENDER, Entitlement.CONNECTOR_LIST);

        add(connectorContainer);

        ((BasePage) pageRef.getPage()).setWindowClosedCallback(createConnectorWin, connectorContainer);
        ((BasePage) pageRef.getPage()).setWindowClosedCallback(editConnectorWin, connectorContainer);

        createConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createConnectorWin.setInitialHeight(WIN_HEIGHT);
        createConnectorWin.setInitialWidth(WIN_WIDTH);
        createConnectorWin.setCookieName("create-conn-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setInitialHeight(WIN_HEIGHT);
        editConnectorWin.setInitialWidth(WIN_WIDTH);
        editConnectorWin.setCookieName("edit-conn-modal");

        AjaxLink<Void> createConnectorLink
                = new ClearIndicatingAjaxLink<Void>("createConnectorLink", pageRef) {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    protected void onClickInternal(final AjaxRequestTarget target) {
                        createConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                ConnectorModalPage form = new ConnectorModalPage(
                                        pageRef, editConnectorWin, new ConnInstanceTO());
                                return form;
                            }
                        });

                        createConnectorWin.show(target);
                    }
                };

        MetaDataRoleAuthorizationStrategy.authorize(createConnectorLink, ENABLE, Entitlement.CONNECTOR_CREATE);

        add(createConnectorLink);

        @SuppressWarnings("rawtypes")
        Form paginatorForm = new Form("connectorPaginatorForm");

        MetaDataRoleAuthorizationStrategy.authorize(paginatorForm, RENDER, Entitlement.CONNECTOR_LIST);

        final DropDownChoice<Integer> rowsChooser = new DropDownChoice<>(
                "rowsChooser",
                new PropertyModel<Integer>(this,
                        "connectorPaginatorRows"),
                prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_CONNECTORS_PAGINATOR_ROWS,
                        String.valueOf(connectorPaginatorRows));
                table.setItemsPerPage(connectorPaginatorRows);

                target.add(connectorContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    private class ConnectorsProvider extends SortableDataProvider<ConnInstanceTO, String> {

        private static final long serialVersionUID = 4445909568349448518L;

        private final SortableDataProviderComparator<ConnInstanceTO> comparator;

        public ConnectorsProvider() {
            super();
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ConnInstanceTO> iterator(final long first, final long count) {
            List<ConnInstanceTO> list = connectorRestClient.getAllConnectors();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return connectorRestClient.getAllConnectors().size();
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
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(connectorContainer);
        }
    }
}
