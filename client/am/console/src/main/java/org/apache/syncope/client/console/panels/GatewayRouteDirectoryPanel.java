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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.GatewayRouteDirectoryPanel.GatewayRouteProvider;
import org.apache.syncope.client.console.rest.GatewayRouteRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class GatewayRouteDirectoryPanel
        extends DirectoryPanel<GatewayRouteTO, GatewayRouteTO, GatewayRouteProvider, GatewayRouteRestClient> {

    private static final long serialVersionUID = -2334397933375604015L;

    public GatewayRouteDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, pageRef);
        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        restClient = new GatewayRouteRestClient();

        addNewItemPanelBuilder(new GatewayRouteWizardBuilder(new GatewayRouteTO(), pageRef), true);
        initResultTable();

        utilityAjaxLink = new AjaxLink<GatewayRouteTO>("utility") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    GatewayRouteRestClient.push();
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While pushing to SRA", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        };
        initialFragment.addOrReplace(utilityAjaxLink);
        utilityAjaxLink.add(utilityIcon);
        utilityIcon.add(new AttributeModifier("class", "fa fa-fast-forward"));
        enableUtilityButton();
    }

    @Override
    protected List<IColumn<GatewayRouteTO, String>> getColumns() {
        List<IColumn<GatewayRouteTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(new StringResourceModel("key", this), "key"));
        columns.add(new PropertyColumn<>(new StringResourceModel("name", this), "name", "name"));
        columns.add(new PropertyColumn<>(new StringResourceModel("order", this), "order", "order"));
        columns.add(new PropertyColumn<>(new StringResourceModel("target", this), "target", "target"));
        columns.add(new PropertyColumn<>(new StringResourceModel("status", this), "status", "status"));

        return columns;
    }

    @Override
    protected ActionsPanel<GatewayRouteTO> getActions(final IModel<GatewayRouteTO> model) {
        ActionsPanel<GatewayRouteTO> panel = super.getActions(model);

        panel.add(new ActionLink<GatewayRouteTO>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GatewayRouteTO ignore) {
                send(GatewayRouteDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                GatewayRouteRestClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.GATEWAY_ROUTE_UPDATE);

        panel.add(new ActionLink<GatewayRouteTO>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GatewayRouteTO ignore) {
                GatewayRouteTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(GatewayRouteDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(clone, target));
            }
        }, ActionLink.ActionType.CLONE, AMEntitlement.GATEWAY_ROUTE_CREATE);

        panel.add(new ActionLink<GatewayRouteTO>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GatewayRouteTO ignore) {
                GatewayRouteTO route = model.getObject();
                try {
                    GatewayRouteRestClient.delete(route.getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", route.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.GATEWAY_ROUTE_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected GatewayRouteProvider dataProvider() {
        return new GatewayRouteProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_GATEWAYROUTE_PAGINATOR_ROWS;
    }

    protected static final class GatewayRouteProvider extends DirectoryDataProvider<GatewayRouteTO> {

        private static final long serialVersionUID = 5282134321828253058L;

        private final SortableDataProviderComparator<GatewayRouteTO> comparator;

        public GatewayRouteProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends GatewayRouteTO> iterator(final long first, final long count) {
            List<GatewayRouteTO> list = GatewayRouteRestClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return GatewayRouteRestClient.list().size();
        }

        @Override
        public IModel<GatewayRouteTO> model(final GatewayRouteTO route) {
            return new CompoundPropertyModel<>(route);
        }
    }
}
