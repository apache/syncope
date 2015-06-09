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
import org.apache.syncope.client.console.pages.ProvisioningModalPage;
import org.apache.syncope.client.console.pages.ResourceModalPage;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.LinkPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources WebPage.
 */
public class Resources extends Panel {

    private static final long serialVersionUID = -3789252860990261728L;

    protected static final Logger LOG = LoggerFactory.getLogger(Resources.class);

    private static final int WIN_HEIGHT = 600;

    private static final int WIN_WIDTH = 1100;

    @SpringBean
    private PreferenceManager prefMan;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    private final ModalWindow createResourceWin;

    private final ModalWindow editResourceWin;

    private final int resourcePaginatorRows;

    private WebMarkupContainer resourceContainer;

    private final ModalWindow editConnectorWin;

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

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    private final PageReference pageRef;

    public Resources(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        createResourceWin = new ModalWindow("createResourceWin");
        add(createResourceWin);

        editResourceWin = new ModalWindow("editResourceWin");
        add(editResourceWin);

        editConnectorWin = new ModalWindow("editConnectorWin");
        add(editConnectorWin);

        statusmodal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        statusmodal.setInitialHeight(STATUS_MODAL_WIN_HEIGHT);
        statusmodal.setInitialWidth(STATUS_MODAL_WIN_WIDTH);
        statusmodal.setCookieName("status-modal");
        add(statusmodal);

        resourcePaginatorRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_RESOURCES_PAGINATOR_ROWS);

        setupResources();
    }

    private void setupResources() {
        List<IColumn<ResourceTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<ResourceTO, String>(new StringResourceModel("key", this, null), "key", "key"));

        columns.add(new AbstractColumn<ResourceTO, String>(
                new StringResourceModel("connector", this, null, "connector")) {

                    private static final long serialVersionUID = 8263694778917279290L;

                    @Override
                    public void populateItem(final Item<ICellPopulator<ResourceTO>> cellItem, final String componentId,
                            final IModel<ResourceTO> rowModel) {

                        final AjaxLink<String> editLink = new ClearIndicatingAjaxLink<String>("link", pageRef) {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            protected void onClickInternal(final AjaxRequestTarget target) {

                                editConnectorWin.setPageCreator(new ModalWindow.PageCreator() {

                                    private static final long serialVersionUID = -7834632442532690940L;

                                    @Override
                                    public Page createPage() {
                                        return new ConnectorModalPage(Resources.this.pageRef,
                                                editConnectorWin,
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

                        MetaDataRoleAuthorizationStrategy.authorize(editConnPanel, ENABLE, Entitlement.CONNECTOR_READ);
                    }
                });

        columns.add(new AbstractColumn<ResourceTO, String>(
                new StringResourceModel("propagationPrimary", this, null)) {

                    private static final long serialVersionUID = -3503023501954863131L;

                    @Override
                    public void populateItem(final Item<ICellPopulator<ResourceTO>> item,
                            final String componentId, final IModel<ResourceTO> model) {

                        item.add(new Label(componentId, ""));
                        item.add(new AttributeModifier("class", new Model<>(
                                                Boolean.toString(model.getObject().isPropagationPrimary()))));
                    }

                    @Override
                    public String getCssClass() {
                        return "narrowcolumn";
                    }
                });

        columns.add(new PropertyColumn<ResourceTO, String>(new StringResourceModel(
                "propagationPriority", this, null), "propagationPriority", "propagationPriority") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return "narrowcolumn";
                    }
                });

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

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, pageRef);
                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        statusmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ProvisioningModalPage<>(
                                        pageRef, statusmodal, model.getObject(), UserTO.class);
                            }
                        });

                        statusmodal.show(target);
                    }
                }, ActionLink.ActionType.MANAGE_USERS, "Resources");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        statusmodal.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ProvisioningModalPage<>(
                                        pageRef, statusmodal, model.getObject(), GroupTO.class);
                            }
                        });

                        statusmodal.show(target);
                    }
                }, ActionLink.ActionType.MANAGE_GROUPS, "Resources");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        resourceTO.setUsyncToken(null);
                        resourceTO.setRsyncToken(null);
                        try {
                            resourceRestClient.update(resourceTO);
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ":" + e.getMessage());

                            LOG.error("While resetting sync token from " + resourceTO.getKey(), e);
                        }

                        ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
                        target.add(resourceContainer);
                    }
                }, ActionLink.ActionType.RESET, "Resources");

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        editResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ResourceModalPage(pageRef, editResourceWin, resourceTO, false);
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
                            resourceRestClient.delete(resourceTO.getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (SyncopeClientException e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());

                            LOG.error("While deleting resource " + resourceTO.getKey(), e);
                        }

                        ((BasePage) pageRef.getPage()).getFeedbackPanel().refresh(target);
                        target.add(resourceContainer);
                    }
                }, ActionLink.ActionType.DELETE, "Resources");

                cellItem.add(panel);
            }
        });

        final AjaxDataTablePanel<ResourceTO, String> table = new AjaxDataTablePanel<>(
                "resourceDatatable",
                columns,
                (ISortableDataProvider<ResourceTO, String>) new ResourcesProvider(),
                resourcePaginatorRows,
                Arrays.asList(new ActionLink.ActionType[] { ActionLink.ActionType.DELETE }),
                resourceRestClient,
                "key",
                "Resources",
                pageRef);

        resourceContainer = new WebMarkupContainer("resourceContainer");
        resourceContainer.add(table);
        resourceContainer.setOutputMarkupId(true);

        add(resourceContainer);

        ((BasePage) pageRef.getPage()).setWindowClosedCallback(createResourceWin, resourceContainer);
        ((BasePage) pageRef.getPage()).setWindowClosedCallback(editResourceWin, resourceContainer);

        createResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createResourceWin.setInitialHeight(WIN_HEIGHT);
        createResourceWin.setInitialWidth(WIN_WIDTH);
        createResourceWin.setCookieName("create-res-modal");

        editResourceWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editResourceWin.setInitialHeight(WIN_HEIGHT);
        editResourceWin.setInitialWidth(WIN_WIDTH);
        editResourceWin.setCookieName("edit-res-modal");

        editConnectorWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editConnectorWin.setInitialHeight(WIN_HEIGHT);
        editConnectorWin.setInitialWidth(WIN_WIDTH);
        editConnectorWin.setCookieName("edit-conn-modal");

        AjaxLink<Void> createResourceLink
                = new ClearIndicatingAjaxLink<Void>("createResourceLink", pageRef) {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    protected void onClickInternal(final AjaxRequestTarget target) {
                        createResourceWin.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                final ResourceModalPage windows = new ResourceModalPage(Resources.this.pageRef,
                                        editResourceWin, new ResourceTO(), true);
                                return windows;
                            }
                        });

                        createResourceWin.show(target);
                    }
                };

        MetaDataRoleAuthorizationStrategy.authorize(createResourceLink, ENABLE, Entitlement.RESOURCE_CREATE);

        add(createResourceLink);

        @SuppressWarnings("rawtypes")
        final Form paginatorForm = new Form("resourcePaginatorForm");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final DropDownChoice rowsChooser = new DropDownChoice("rowsChooser", new PropertyModel(this,
                "resourcePaginatorRows"), prefMan.getPaginatorChoices());

        rowsChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), Constants.PREF_RESOURCES_PAGINATOR_ROWS,
                        String.valueOf(resourcePaginatorRows));

                table.setItemsPerPage(resourcePaginatorRows);
                target.add(resourceContainer);
            }
        });

        paginatorForm.add(rowsChooser);
        add(paginatorForm);
    }

    class ResourcesProvider extends SortableDataProvider<ResourceTO, String> {

        private static final long serialVersionUID = -9055916672926643975L;

        private final SortableDataProviderComparator<ResourceTO> comparator;

        public ResourcesProvider() {
            super();
            //Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ResourceTO> iterator(final long first, final long count) {
            List<ResourceTO> list = resourceRestClient.getAll();

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return resourceRestClient.getAll().size();
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
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AbstractSearchResultPanel.EventDataWrapper) {
            ((AbstractSearchResultPanel.EventDataWrapper) event.getPayload()).getTarget().add(resourceContainer);
        }
    }
}
