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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;

public class MailTemplateModal extends AbstractModalPanel<MailTemplateTO> {

    private static final long serialVersionUID = 2053048734388383021L;

    private final MailTemplateTO mailTemplateTO;

    public MailTemplateModal(
            final BaseModal<MailTemplateTO> modal,
            final MailTemplateTO mailTemplateTO,
            final PageReference pageRef) {
        super(modal, pageRef);
        this.mailTemplateTO = mailTemplateTO;

        final AjaxTextFieldPanel key
                = new AjaxTextFieldPanel("key", "key", new PropertyModel<String>(mailTemplateTO, "key"), false);
        key.setOutputMarkupPlaceholderTag(true);
        add(key.setRenderBodyOnly(true));
    }

    @Override
    public MailTemplateTO getItem() {
        return this.mailTemplateTO;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            new NotificationRestClient().createTemplate(mailTemplateTO);
            info(getString(Constants.OPERATION_SUCCEEDED));
            modal.show(false);
            modal.close(target);
        } catch (SyncopeClientException e) {
            LOG.error("While creating template for {}", mailTemplateTO.getKey(), e);
            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
        }
        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
    }
}
