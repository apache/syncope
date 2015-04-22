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
package org.apache.syncope.client.console.pages;

import java.util.ArrayList;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.panels.AccountInformationPanel;
import org.apache.syncope.client.console.panels.MembershipsPanel;
import org.apache.syncope.client.console.panels.ResourcesPanel;
import org.apache.syncope.client.console.panels.StatusPanel;
import org.apache.syncope.common.lib.AttributableOperations;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with User form.
 */
public class EditUserModalPage extends UserModalPage {

    private static final long serialVersionUID = -6479209496805705739L;

    protected Form form;

    private final UserTO initialUserTO;

    private StatusPanel statusPanel;

    public EditUserModalPage(final PageReference pageRef, final ModalWindow window, final UserTO userTO) {
        super(pageRef, window, userTO, Mode.ADMIN, true);

        this.initialUserTO = SerializationUtils.clone(userTO);

        form = setupEditPanel();

        // add resource assignment details in case of update
        if (userTO.getKey() != 0) {
            form.addOrReplace(new Label("pwdChangeInfo", new ResourceModel("pwdChangeInfo")));

            statusPanel = new StatusPanel("statuspanel", userTO, new ArrayList<StatusBean>(), getPageReference());
            statusPanel.setOutputMarkupId(true);
            MetaDataRoleAuthorizationStrategy.authorize(
                    statusPanel, RENDER, xmlRolesReader.getEntitlement("Resources", "getConnectorObject"));
            form.addOrReplace(statusPanel);

            form.addOrReplace(new AccountInformationPanel("accountinformation", userTO));

            form.addOrReplace(new ResourcesPanel.Builder("resources").attributableTO(userTO).
                    statusPanel(statusPanel).build());

            form.addOrReplace(new MembershipsPanel("memberships", userTO, mode, statusPanel, getPageReference()));
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void submitAction(final AjaxRequestTarget target, final Form form) {
        final UserTO updatedUserTO = (UserTO) form.getModelObject();

        if (updatedUserTO.getKey() == 0) {
            userTO = userRestClient.create(updatedUserTO, storePassword.getModelObject());
        } else {
            final UserMod userMod = AttributableOperations.diff(updatedUserTO, initialUserTO);

            if (statusPanel != null) {
                userMod.setPwdPropRequest(statusPanel.getStatusMod());
            }

            // update user just if it is changed
            if (!userMod.isEmpty()) {
                userTO = userRestClient.update(initialUserTO.getETagValue(), userMod);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void closeAction(final AjaxRequestTarget target, final Form form) {
        setResponsePage(new ResultStatusModalPage.Builder(window, userTO).mode(mode).build());
    }
}
