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
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.PasswordPanel;
import org.apache.syncope.client.console.wizards.any.StatusPanel;
import org.apache.syncope.client.console.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class ChangePasswordModal extends AbstractModalPanel<AnyWrapper<UserTO>> {

    private static final long serialVersionUID = 6257389301592059194L;

    private final UserRestClient userRestClient = new UserRestClient();

    private final IModel<List<StatusBean>> statusModel;

    private final UserWrapper wrapper;

    public ChangePasswordModal(
            final BaseModal<AnyWrapper<UserTO>> baseModal,
            final PageReference pageReference,
            final UserWrapper wrapper) {
        super(baseModal, pageReference);

        this.wrapper = wrapper;

        final PasswordPanel passwordPanel = new PasswordPanel("passwordPanel", wrapper, false);
        passwordPanel.setOutputMarkupId(true);
        add(passwordPanel);

        statusModel = new ListModel<>(new ArrayList<StatusBean>());
        StatusPanel statusPanel = new StatusPanel("status", wrapper.getInnerObject(), statusModel, pageReference);
        statusPanel.setCheckAvailability(ListViewPanel.CheckAvailability.AVAILABLE);
        add(statusPanel.setRenderBodyOnly(true));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        final UserTO inner = wrapper.getInnerObject();

        try {
            if (StringUtils.isBlank(inner.getPassword()) || statusModel.getObject().isEmpty()) {
                SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
            } else {
                final List<String> resources = new ArrayList<String>();
                boolean isOnSyncope = false;
                for (StatusBean sb : statusModel.getObject()) {
                    if (sb.getResourceName().equals(Constants.SYNCOPE)) {
                        isOnSyncope = true;
                    } else {
                        resources.add(sb.getResourceName());
                    }
                }

                final UserPatch patch = new UserPatch();
                patch.setKey(inner.getKey());

                PasswordPatch passwordPatch = new PasswordPatch.Builder().
                        value(inner.getPassword()).onSyncope(isOnSyncope).resources(resources).build();
                patch.setPassword(passwordPatch);

                userRestClient.update(inner.getETagValue(), patch);
                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                modal.show(false);
                modal.close(target);
            }
        } catch (Exception e) {
            LOG.error("While updating password for user {}", inner, e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName()
                    : e.getMessage());
        }
        super.onSubmit(target, form);
    }
}
