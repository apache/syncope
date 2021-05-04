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

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

public class ApplicationModalPanel extends AbstractModalPanel<ApplicationTO> {

    private static final long serialVersionUID = 4575264480736377795L;

    private final ApplicationTO application;

    private final boolean create;

    public ApplicationModalPanel(
            final ApplicationTO application,
            final boolean create,
            final BaseModal<ApplicationTO> modal,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.application = application;
        this.create = create;

        modal.setFormModel(application);

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                Constants.KEY_FIELD_NAME,
                new PropertyModel<>(application, Constants.KEY_FIELD_NAME), false);
        key.setReadOnly(!create);
        key.setRequired(true);
        add(key);

        AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME,
                new PropertyModel<>(application, Constants.DESCRIPTION_FIELD_NAME), false);
        description.setRequired(false);
        add(description);
    }

    @Override
    public ApplicationTO getItem() {
        return application;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            if (create) {
                ApplicationRestClient.create(application);
            } else {
                ApplicationRestClient.update(application);
            }
            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating/updating application", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
