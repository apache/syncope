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
package org.apache.syncope.client.console.status;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.StatusPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.PasswordPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ChangePasswordModal extends AbstractModalPanel<AnyWrapper<UserTO>> {

    private static final long serialVersionUID = 6257389301592059194L;

    @SpringBean
    protected UserRestClient userRestClient;

    protected final IModel<List<StatusBean>> statusModel;

    protected final UserWrapper wrapper;

    public ChangePasswordModal(
            final BaseModal<AnyWrapper<UserTO>> baseModal,
            final UserWrapper wrapper,
            final PageReference pageRefer) {

        super(baseModal, pageRefer);
        this.wrapper = wrapper;

        PasswordPanel passwordPanel = new PasswordPanel("passwordPanel", wrapper, false, false);
        passwordPanel.setOutputMarkupId(true);
        add(passwordPanel);

        statusModel = new ListModel<>(new ArrayList<>());
        StatusPanel statusPanel = new StatusPanel("status", wrapper.getInnerObject(), statusModel, pageRefer);
        statusPanel.setCheckAvailability(ListViewPanel.CheckAvailability.AVAILABLE);
        add(statusPanel.setRenderBodyOnly(true));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        UserTO inner = wrapper.getInnerObject();

        try {
            if (StringUtils.isBlank(inner.getPassword()) || statusModel.getObject().isEmpty()) {
                SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
            } else {
                List<String> resources = new ArrayList<>();
                boolean isOnSyncope = false;
                for (StatusBean sb : statusModel.getObject()) {
                    if (sb.getResource().equals(Constants.SYNCOPE)) {
                        isOnSyncope = true;
                    } else {
                        resources.add(sb.getResource());
                    }
                }

                UserUR req = new UserUR.Builder(inner.getKey()).
                        password(new PasswordPatch.Builder().
                                value(inner.getPassword()).onSyncope(isOnSyncope).resources(resources).build()).
                        build();

                userRestClient.update(inner.getETagValue(), req);
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                modal.show(false);
                modal.close(target);
            }
        } catch (Exception e) {
            LOG.error("While updating password for user {}", inner, e);
            SyncopeConsoleSession.get().onException(e);
        }
        super.onSubmit(target);
    }
}
