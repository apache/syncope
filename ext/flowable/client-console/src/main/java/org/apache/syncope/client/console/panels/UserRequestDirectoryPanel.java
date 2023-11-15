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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.batch.BatchModal;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.UserRequestDirectoryPanel.UserRequestProvider;
import org.apache.syncope.client.console.panels.UserRequestsPanel.UserRequestSearchEvent;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ConfirmBehavior;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class UserRequestDirectoryPanel
        extends DirectoryPanel<UserRequest, UserRequest, UserRequestProvider, UserRequestRestClient> {

    private static final long serialVersionUID = -5346161040211617763L;

    private static final String PREF_USER_REQUEST_PAGINATOR_ROWS = "userrequest.paginator.rows";

    private String keyword;

    public UserRequestDirectoryPanel(
            final String id,
            final UserRequestRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        setFooterVisibility(false);

        initResultTable();

        IndicatingAjaxButton batchLink = new IndicatingAjaxButton("batchLink", resultTable.group.getForm()) {

            private static final long serialVersionUID = 382302811235019988L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                Collection<UserRequest> items = resultTable.group.getModelObject();
                if (!items.isEmpty()) {
                    BatchRequest batch = SyncopeConsoleSession.get().batch();
                    UserRequestService service = batch.getService(UserRequestService.class);
                    items.forEach(item -> service.cancelRequest(item.getExecutionId(), null));

                    Map<String, String> results = restClient.batch(batch);

                    resultTable.batchModal.header(new ResourceModel("batch"));
                    resultTable.batchModal.changeCloseButtonLabel(getString("cancel", null, "Cancel"), target);

                    target.add(resultTable.batchModal.setContent(new BatchModal<>(
                            resultTable.batchModal,
                            pageRef,
                            new ArrayList<>(items),
                            getColumns(),
                            results,
                            "executionId",
                            target)));

                    resultTable.batchModal.show(true);
                }
            }
        };
        batchLink.add(new ConfirmBehavior(batchLink, "confirmDelete"));
        ((WebMarkupContainer) resultTable.get("tablePanel")).addOrReplace(batchLink);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, FlowableEntitlement.USER_REQUEST_LIST);
    }

    @Override
    protected List<IColumn<UserRequest, String>> getColumns() {
        List<IColumn<UserRequest, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel("bpmnProcess"), "bpmnProcess", "bpmnProcess"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("startTime"), "startTime", "startTime"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("username"), "username"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("activityId"), "activityId"));

        return columns;
    }

    @Override
    public ActionsPanel<UserRequest> getActions(final IModel<UserRequest> model) {
        ActionsPanel<UserRequest> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequest ignore) {
                try {
                    restClient.cancelRequest(model.getObject().getExecutionId(), null);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                    UserRequestDirectoryPanel.this.getTogglePanel().close(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While canceling execution {}", model.getObject().getExecutionId(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, FlowableEntitlement.USER_REQUEST_CANCEL, true);

        return panel;
    }

    @Override
    protected UserRequestProvider dataProvider() {
        return new UserRequestProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_USER_REQUEST_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof UserRequestSearchEvent) {
            UserRequestSearchEvent payload = UserRequestSearchEvent.class.cast(event.getPayload());
            keyword = payload.getKeyword();

            updateResultTable(payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    protected final class UserRequestProvider extends DirectoryDataProvider<UserRequest> {

        private static final long serialVersionUID = -1392420250782313734L;

        public UserRequestProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("startTime", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<UserRequest> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.listRequests(
                    keyword, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return restClient.countRequests(keyword);
        }

        @Override
        public IModel<UserRequest> model(final UserRequest request) {
            return Model.of(request);
        }
    }
}
