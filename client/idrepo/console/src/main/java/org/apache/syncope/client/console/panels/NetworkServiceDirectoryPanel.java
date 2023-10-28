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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.NetworkServiceDirectoryPanel.NetworkServiceProvider;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class NetworkServiceDirectoryPanel extends DirectoryPanel<
        NetworkService, NetworkService, NetworkServiceProvider, SyncopeRestClient> {

    private static final long serialVersionUID = 1868839768348072635L;

    @SpringBean
    protected ServiceOps serviceOps;

    private final NetworkService.Type type;

    public NetworkServiceDirectoryPanel(
            final String id,
            final NetworkService.Type type,
            final SyncopeRestClient syncopeRestClient,
            final PageReference pageRef) {
        super(id, syncopeRestClient, pageRef, true);
        this.type = type;

        NetworkService service = new NetworkService();
        service.setType(type);

        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        initResultTable();
    }

    @Override
    protected List<IColumn<NetworkService, String>> getColumns() {
        List<IColumn<NetworkService, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(new StringResourceModel("address", this), "address", "address"));

        return columns;
    }

    @Override
    protected ActionsPanel<NetworkService> getActions(final IModel<NetworkService> model) {
        ActionsPanel<NetworkService> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final NetworkService ignore) {
                try {
                    serviceOps.unregister(model.getObject());
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getAddress(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.KEYMASTER, true);

        return panel;
    }

    @Override
    protected NetworkServiceProvider dataProvider() {
        return new NetworkServiceProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_NETWORK_SERVICE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class NetworkServiceProvider extends DirectoryDataProvider<NetworkService> {

        private static final long serialVersionUID = 8594921866993979224L;

        public NetworkServiceProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("address", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<NetworkService> iterator(final long first, final long count) {
            List<NetworkService> list = serviceOps.list(type);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return serviceOps.list(type).size();
        }

        @Override
        public IModel<NetworkService> model(final NetworkService service) {
            return new IModel<>() {

                private static final long serialVersionUID = 999513782683391483L;

                @Override
                public NetworkService getObject() {
                    return service;
                }
            };
        }
    }
}
