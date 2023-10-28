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
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.SRARouteDirectoryPanel.SRARouteProvider;
import org.apache.syncope.client.console.rest.SRARouteRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class SRARouteDirectoryPanel
        extends DirectoryPanel<SRARouteTO, SRARouteTO, SRARouteProvider, SRARouteRestClient> {

    private static final long serialVersionUID = -2334397933375604015L;

    public SRARouteDirectoryPanel(final String id, final SRARouteRestClient restClient, final PageReference pageRef) {
        super(id, restClient, pageRef);
        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addNewItemPanelBuilder(new SRARouteWizardBuilder(new SRARouteTO(), restClient, pageRef), true);
        initResultTable();
    }

    @Override
    protected List<IColumn<SRARouteTO, String>> getColumns() {
        List<IColumn<SRARouteTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));
        columns.add(new PropertyColumn<>(new StringResourceModel("target", this), "target", "target"));
        columns.add(new PropertyColumn<>(new StringResourceModel("type", this), "type", "type"));
        columns.add(new BooleanPropertyColumn<>(new StringResourceModel("logout", this), "logout", "logout"));
        columns.add(new BooleanPropertyColumn<>(new StringResourceModel("csrf", this), "csrf", "csrf"));
        columns.add(new PropertyColumn<>(new StringResourceModel("order", this), "order", "order"));

        return columns;
    }

    @Override
    protected ActionsPanel<SRARouteTO> getActions(final IModel<SRARouteTO> model) {
        ActionsPanel<SRARouteTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SRARouteTO ignore) {
                send(SRARouteDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                restClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.SRA_ROUTE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SRARouteTO ignore) {
                SRARouteTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(SRARouteDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(clone, target));
            }
        }, ActionLink.ActionType.CLONE, AMEntitlement.SRA_ROUTE_CREATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SRARouteTO ignore) {
                SRARouteTO route = model.getObject();
                try {
                    restClient.delete(route.getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", route.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.SRA_ROUTE_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected SRARouteProvider dataProvider() {
        return new SRARouteProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_GATEWAYROUTE_PAGINATOR_ROWS;
    }

    protected final class SRARouteProvider extends DirectoryDataProvider<SRARouteTO> {

        private static final long serialVersionUID = 5282134321828253058L;

        private final SortableDataProviderComparator<SRARouteTO> comparator;

        public SRARouteProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends SRARouteTO> iterator(final long first, final long count) {
            List<SRARouteTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SRARouteTO> model(final SRARouteTO route) {
            return new CompoundPropertyModel<>(route);
        }
    }
}
