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
package org.apache.syncope.client.console.wizards.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.MergeLinkedAccountsWizardModel;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MergeLinkedAccountsResourcesPanel;
import org.apache.syncope.client.console.panels.MergeLinkedAccountsReviewPanel;
import org.apache.syncope.client.console.panels.MergeLinkedAccountsSearchPanel;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.patch.LinkedAccountPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MergeLinkedAccountsWizardBuilder extends AjaxWizardBuilder<UserTO> implements IEventSink {
    private static final long serialVersionUID = -9142332740863374891L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Panel parentPanel;

    private final BaseModal modal;

    private MergeLinkedAccountsWizardModel model;

    public MergeLinkedAccountsWizardBuilder(final IModel<UserTO> model, final PageReference pageRef,
                                            final Panel parentPanel, final BaseModal modal) {
        super(model.getObject(), pageRef);
        this.parentPanel = parentPanel;
        this.modal = modal;
    }

    @Override
    protected WizardModel buildModelSteps(final UserTO modelObject, final WizardModel wizardModel) {
        this.model = new MergeLinkedAccountsWizardModel(modelObject);
        wizardModel.add(new MergeLinkedAccountsSearchPanel(model, getPageReference()));
        wizardModel.add(new MergeLinkedAccountsResourcesPanel(model, getPageReference()));
        wizardModel.add(new MergeLinkedAccountsReviewPanel(model, getPageReference()));
        return wizardModel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
            AjaxRequestTarget target1 = ((AjaxWizard.NewItemCancelEvent) event.getPayload()).getTarget();
            modal.close(target1);
        }
        if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
            AjaxRequestTarget target1 = ((AjaxWizard.NewItemFinishEvent) event.getPayload()).getTarget();
            try {
                mergeAccounts();
                this.parentPanel.info(this.parentPanel.getString(Constants.OPERATION_SUCCEEDED));
                ((BasePage) this.parentPanel.getPage()).getNotificationPanel().refresh(target1);
                modal.close(target1);
            } catch (Exception e) {
                this.parentPanel.error(this.parentPanel.getString(Constants.ERROR) + ": " + e.getMessage());
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target1);
            }
        }
    }

    private void mergeAccounts() throws Exception {
        final UserTO mergingUserTO = this.model.getMergingUser();
        final ResourceRestClient resourceRestClient = new ResourceRestClient();

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(this.model.getBaseUser().getUsername());

        // Move linked accounts into the target/base user as linked accounts
        mergingUserTO.getLinkedAccounts().forEach(acct -> {
            LinkedAccountTO linkedAccount =
                new LinkedAccountTO.Builder(acct.getResource(), acct.getConnObjectKeyValue())
                    .password(acct.getPassword())
                    .suspended(acct.isSuspended())
                    .username(acct.getUsername())
                    .build();
            linkedAccount.getPlainAttrs().addAll(acct.getPlainAttrs());
            linkedAccount.getPrivileges().addAll(acct.getPrivileges());
            LinkedAccountPatch patch = new LinkedAccountPatch.Builder()
                .linkedAccountTO(linkedAccount)
                .operation(PatchOperation.ADD_REPLACE)
                .build();
            userPatch.getLinkedAccounts().add(patch);
        });

        // Move merging user's resources into the target/base user as a linked account
        mergingUserTO.getResources().forEach(resource -> {
            String connObjectKeyValue = resourceRestClient.getConnObjectKeyValue(resource,
                mergingUserTO.getType(), mergingUserTO.getKey());
            LinkedAccountTO linkedAccount =
                new LinkedAccountTO.Builder(resource, connObjectKeyValue)
                    .build();
            linkedAccount.getPlainAttrs().addAll(mergingUserTO.getPlainAttrs());
            linkedAccount.getPrivileges().addAll(mergingUserTO.getPrivileges());
            LinkedAccountPatch patch = new LinkedAccountPatch.Builder()
                .linkedAccountTO(linkedAccount)
                .operation(PatchOperation.ADD_REPLACE)
                .build();
            userPatch.getLinkedAccounts().add(patch);
        });

        // Move merging user into target/base user as a linked account
        String connObjectKeyValue = resourceRestClient.getConnObjectKeyValue(
            this.model.getResource().getKey(),
            mergingUserTO.getType(), mergingUserTO.getKey());
        LinkedAccountTO linkedAccount =
            new LinkedAccountTO.Builder(this.model.getResource().getKey(), connObjectKeyValue)
                .password(mergingUserTO.getPassword())
                .suspended(mergingUserTO.isSuspended())
                .username(mergingUserTO.getUsername())
                .build();
        linkedAccount.getPlainAttrs().addAll(mergingUserTO.getPlainAttrs());
        linkedAccount.getPrivileges().addAll(mergingUserTO.getPrivileges());
        LinkedAccountPatch patch = new LinkedAccountPatch.Builder().linkedAccountTO(linkedAccount)
            .operation(PatchOperation.ADD_REPLACE)
            .build();
        userPatch.getLinkedAccounts().add(patch);

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
        updateUser.setRequestURI("/users/" + this.model.getBaseUser().getUsername());
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

}
