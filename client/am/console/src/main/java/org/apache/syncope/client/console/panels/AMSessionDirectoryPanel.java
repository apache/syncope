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
import java.util.stream.Stream;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AMSessionDirectoryPanel.AMSessionProvider;
import org.apache.syncope.client.console.panels.AMSessionPanel.AMSessionSearchEvent;
import org.apache.syncope.client.console.rest.AMSessionRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.AMSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class AMSessionDirectoryPanel
        extends DirectoryPanel<AMSession, AMSession, AMSessionProvider, AMSessionRestClient> {

    private static final long serialVersionUID = 24083331272114L;

    private final String listEntitlement;

    private final String deleteEntitlement;

    private final BaseModal<String> viewModal;

    private String keyword;

    public AMSessionDirectoryPanel(
            final String id,
            final AMSessionRestClient restClient,
            final String listEntitlement,
            final String deleteEntitlement,
            final PageReference pageRef) {

        super(id, restClient, pageRef);

        this.listEntitlement = listEntitlement;
        this.deleteEntitlement = deleteEntitlement;
        this.restClient = restClient;

        disableCheckBoxes();

        viewModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        viewModal.size(Modal.Size.Extra_large);
        viewModal.setWindowClosedCallback(target -> viewModal.show(false));
        addOuterObject(viewModal);

        initResultTable();
    }

    @Override
    protected List<IColumn<AMSession, String>> getColumns() {
        List<IColumn<AMSession, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("authenticationDate"), "authenticationDate", "authenticationDate"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("principal"), "principal", "principal"));
        return columns;
    }

    @Override
    protected ActionsPanel<AMSession> getActions(final IModel<AMSession> model) {
        ActionsPanel<AMSession> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 22687128346032L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AMSession ignore) {
                viewModal.header(new ResourceModel("details"));
                target.add(viewModal.setContent(
                    new JsonEditorPanel(viewModal, Model.of(model.getObject().getJson()), true, pageRef)));
                viewModal.show(true);
            }
        }, ActionLink.ActionType.VIEW, listEntitlement);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4608353559809323466L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AMSession ignore) {
                AMSession session = model.getObject();
                try {
                    restClient.delete(session.getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", session.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, deleteEntitlement, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected AMSessionProvider dataProvider() {
        return new AMSessionProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_WASESSION_PAGINATOR_ROWS;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AMSessionSearchEvent) {
            AMSessionSearchEvent payload = AMSessionSearchEvent.class.cast(event.getPayload());
            keyword = payload.getKeyword();

            updateResultTable(payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    protected final class AMSessionProvider extends DirectoryDataProvider<AMSession> {

        private static final long serialVersionUID = 18002870965042L;

        private final SortableDataProviderComparator<AMSession> comparator;

        public AMSessionProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("authenticationDate", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        private Stream<AMSession> filtered() {
            Stream<AMSession> stream = restClient.list().stream();
            return keyword == null
                    ? stream
                    : stream.filter(s -> s.getJson().contains(keyword));
        }

        @Override
        public Iterator<? extends AMSession> iterator(final long first, final long count) {
            return filtered().skip(first).limit(count).sorted(comparator).iterator();
        }

        @Override
        public long size() {
            return filtered().count();
        }

        @Override
        public IModel<AMSession> model(final AMSession waSession) {
            return new CompoundPropertyModel<>(waSession);
        }
    }
}
