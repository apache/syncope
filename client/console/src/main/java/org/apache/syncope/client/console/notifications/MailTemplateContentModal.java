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

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;

public class MailTemplateContentModal extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = 2053048734388383021L;

    private final MailTemplateContentTO content;

    public MailTemplateContentModal(
            final BaseModal<Serializable> modal,
            final MailTemplateContentTO content,
            final PageReference pageRef) {
        super(modal, pageRef);
        this.content = content;

        TextArea<String> templateDefArea = new TextArea<>("template", new PropertyModel<String>(content, "content"));
        templateDefArea.setMarkupId("template").setOutputMarkupPlaceholderTag(true);
        add(templateDefArea);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnLoadHeaderItem.forScript(
                "CodeMirror.fromTextArea(document.getElementById('template'), {"
                + "  lineNumbers: true, "
                + "  autoRefresh: true"
                + "}).on('change', updateTextArea);"));
    }

    @Override
    public MailTemplateContentTO getItem() {
        return this.content;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            new NotificationRestClient().updateTemplateFormat(
                    content.getKey(), content.getContent(), content.getFormat());
            info(getString(Constants.OPERATION_SUCCEEDED));
            modal.show(false);
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating template for {}", content.getKey(), e);
            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
        }
        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
    }

    public static class MailTemplateContentTO extends AbstractBaseBean {

        private static final long serialVersionUID = -1756961687134322845L;

        private final String key;

        private String content;

        private final MailTemplateFormat format;

        public MailTemplateContentTO(final String key, final MailTemplateFormat format) {
            this.key = key;
            this.format = format;
        }

        public String getKey() {
            return key;
        }

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = content;
        }

        public MailTemplateFormat getFormat() {
            return format;
        }
    }
}
