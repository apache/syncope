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
package org.apache.syncope.client.console.notifications;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.TemplateRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;

public class TemplateModal<T extends EntityTO, F> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 2053048734388383021L;

    private final T templateTO;

    private final TemplateRestClient<T, F> restClient;

    public TemplateModal(
            final BaseModal<T> modal,
            final TemplateRestClient<T, F> restClient,
            final T templateTO,
            final PageReference pageRef) {
        super(modal, pageRef);
        this.restClient = restClient;
        this.templateTO = templateTO;

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                Constants.KEY_FIELD_NAME,
                new PropertyModel<>(templateTO, Constants.KEY_FIELD_NAME), false);
        key.setOutputMarkupPlaceholderTag(true);
        add(key.setRenderBodyOnly(true));
    }

    @Override
    public T getItem() {
        return this.templateTO;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            restClient.createTemplate(templateTO);
            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (SyncopeClientException e) {
            LOG.error("While creating template for {}", templateTO.getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
