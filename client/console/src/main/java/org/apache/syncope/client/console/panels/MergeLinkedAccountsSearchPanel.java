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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.search.AnySelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.panels.search.UserSelectionDirectoryPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.patch.LinkedAccountPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MergeLinkedAccountsSearchPanel extends WizardStep implements ICondition {
    private static final long serialVersionUID = 1221037007528732347L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(MergeLinkedAccountsSearchPanel.class);

    private final WebMarkupContainer ownerContainer;

    private final UserSearchPanel userSearchPanel;

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private final UserSelectionDirectoryPanel userDirectoryPanel;

    private final Fragment userSearchFragment;

    private UserTO originalUserTO;

    public MergeLinkedAccountsSearchPanel(final UserTO userTO, final PageReference pageRef) {
        super();
        setOutputMarkupId(true);

        setTitleModel(new ResourceModel("mergeLinkedAccounts.searchUser"));
        this.originalUserTO = userTO;

        ownerContainer = new WebMarkupContainer("ownerContainer");
        ownerContainer.setOutputMarkupId(true);
        add(ownerContainer);

        userSearchFragment = new Fragment("search", "userSearchFragment", this);
        userSearchPanel = UserSearchPanel.class.cast(new UserSearchPanel.Builder(
            new ListModel<>(new ArrayList<>())).required(false).enableSearch(MergeLinkedAccountsSearchPanel.this).
            build("usersearch"));
        userSearchFragment.add(userSearchPanel);

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(AnyTypeKind.USER.name());
        userDirectoryPanel = UserSelectionDirectoryPanel.class.cast(new UserSelectionDirectoryPanel.Builder(
            anyTypeClassRestClient.list(anyTypeTO.getClasses()), anyTypeTO.getKey(), pageRef).
            build("searchResult"));

        userSearchFragment.add(userDirectoryPanel);
        ownerContainer.add(userSearchFragment);
    }

    private void mergeAccounts(final UserTO mergingUserTO) throws Exception {
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(originalUserTO.getUsername());

        mergingUserTO.getLinkedAccounts().forEach(acct -> {
            LinkedAccountTO linkedAccount =
                new LinkedAccountTO.Builder(acct.getResource(), acct.getConnObjectKeyValue())
                    .password(acct.getPassword())
                    .suspended(acct.isSuspended())
                    .username(acct.getUsername())
                    .build();
            linkedAccount.getPlainAttrs().addAll(acct.getPlainAttrs());
            linkedAccount.getPrivileges().addAll(acct.getPrivileges());
            LinkedAccountPatch patch = new LinkedAccountPatch.Builder().linkedAccountTO(linkedAccount)
                .operation(PatchOperation.ADD_REPLACE)
                .build();
            userPatch.getLinkedAccounts().add(patch);
        });

        BatchRequest batchRequest = SyncopeConsoleSession.get().batch();

        // Delete merging user
        BatchRequestItem deleteRequest = new BatchRequestItem();
        deleteRequest.setMethod(HttpMethod.DELETE);
        deleteRequest.setRequestURI("/users/" + mergingUserTO.getKey());
        deleteRequest.getHeaders().put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON));
        batchRequest.getItems().add(deleteRequest);

        // Update user with linked accounts
        String updateUserPayload = MAPPER.writeValueAsString(userPatch);
        BatchRequestItem updateUser = new BatchRequestItem();
        updateUser.setMethod(HttpMethod.PATCH);
        updateUser.setRequestURI("/users/" + originalUserTO.getUsername());
        updateUser.setHeaders(new HashMap<>());
        updateUser.getHeaders().put(RESTHeaders.PREFER,
            Collections.singletonList(Preference.RETURN_NO_CONTENT.toString()));
        updateUser.getHeaders().put(HttpHeaders.ACCEPT,
            Collections.singletonList(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_TYPE,
            Collections.singletonList(MediaType.APPLICATION_JSON));
        updateUser.getHeaders().put(HttpHeaders.CONTENT_LENGTH,
            Collections.singletonList(updateUserPayload.length()));
        updateUser.setContent(updateUserPayload);
        batchRequest.getItems().add(updateUser);
        
        Map<String, String> batchResponse = new UserRestClient().batch(batchRequest);
        batchResponse.forEach((key, value) -> {
            if (!value.equalsIgnoreCase("success")) {
                throw new IllegalArgumentException("Unable to report a success operation status for " + key);
            }
        });
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            final AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
            final String fiql = "username!~" + this.originalUserTO.getUsername() + ';'
                + SearchUtils.buildFIQL(userSearchPanel.getModel().getObject(),
                SyncopeClient.getUserSearchConditionBuilder());
            userDirectoryPanel.search(fiql, target);
        } else if (event.getPayload() instanceof AnySelectionDirectoryPanel.ItemSelection) {
            try {
                AnySelectionDirectoryPanel.ItemSelection payload =
                    (AnySelectionDirectoryPanel.ItemSelection) event.getPayload();
                final AnyTO sel = payload.getSelection();
                UserTO mergingUserTO = new UserRestClient().read(sel.getKey());
                payload.getTarget().add(ownerContainer);
                mergeAccounts(mergingUserTO);
                displaySuccess();
            } catch (Exception e) {
                LOG.error("Wizard error on finish", e);
                displayError(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName() : e.getMessage());
            }
        }
    }

    private void displayError(final String message) {
        LOG.error(message);
        SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target.isPresent()) {
            ((BasePage) getPage()).getNotificationPanel().refresh(target.get());
        }
    }

    private void displaySuccess() {
        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target.isPresent()) {
            ((BasePage) getPage()).getNotificationPanel().refresh(target.get());
        }
    }

    @Override
    public boolean evaluate() {
        return SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
            isActionAuthorized(this, RENDER);
    }
}
