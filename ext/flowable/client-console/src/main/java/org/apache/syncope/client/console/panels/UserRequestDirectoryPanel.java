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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.UserRequestDirectoryPanel.UserRequestProvider;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class UserRequestDirectoryPanel
        extends DirectoryPanel<UserRequest, UserRequest, UserRequestProvider, UserRequestRestClient> {

    private static final long serialVersionUID = -5346161040211617763L;

    private static final String PREF_USER_REQUEST_PAGINATOR_ROWS = "userrequest.paginator.rows";

    public UserRequestDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        restClient = new UserRequestRestClient();

        initResultTable();

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
        final ActionsPanel<UserRequest> panel = super.getActions(model);

        panel.add(new ActionLink<UserRequest>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequest ignore) {
                try {
                    UserRequestRestClient.cancelRequest(model.getObject().getExecutionId(), null);
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

    protected static class UserRequestProvider extends DirectoryDataProvider<UserRequest> {

        private static final long serialVersionUID = -1392420250782313734L;

        public UserRequestProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("startTime", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<UserRequest> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return UserRequestRestClient.getUserRequests((page < 0 ? 0 : page) + 1,
                paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return UserRequestRestClient.countUserRequests();
        }

        @Override
        public IModel<UserRequest> model(final UserRequest request) {
            return new IModel<UserRequest>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public UserRequest getObject() {
                    return request;
                }
            };
        }
    }
}
