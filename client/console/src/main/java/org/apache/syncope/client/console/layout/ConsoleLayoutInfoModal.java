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
package org.apache.syncope.client.console.layout;

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;

public class ConsoleLayoutInfoModal extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -5110368813584745668L;

    private final ConsoleLayoutInfo consoleLayoutInfo;

    public ConsoleLayoutInfoModal(
            final BaseModal<Serializable> modal,
            final ConsoleLayoutInfo consoleLayoutInfo,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.consoleLayoutInfo = consoleLayoutInfo;

        TextArea<String> consoleLayoutInfoDefArea = new TextArea<>("consoleLayoutInfo", new PropertyModel<String>(
                consoleLayoutInfo, "content"));
        consoleLayoutInfoDefArea.setMarkupId("consoleLayoutInfo").setOutputMarkupPlaceholderTag(true);
        add(consoleLayoutInfoDefArea);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnLoadHeaderItem.forScript(
                "CodeMirror.fromTextArea(document.getElementById('consoleLayoutInfo'), {"
                + "  lineNumbers: true, "
                + "  lineWrapping: true, "
                + "  matchBrackets: true,"
                + "  autoCloseBrackets: true,"
                + "  autoRefresh: true"
                + "}).on('change', updateTextArea);"));
    }

    @Override
    public ConsoleLayoutInfo getItem() {
        return consoleLayoutInfo;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            new RoleRestClient().setConsoleLayoutInfo(
                    consoleLayoutInfo.getKey(), consoleLayoutInfo.getContent());
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
            modal.show(false);
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating onsole layout info for role {}", consoleLayoutInfo.getKey(), e);
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                    getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    public static class ConsoleLayoutInfo implements Serializable {

        private static final long serialVersionUID = 961267717148831831L;

        private final String key;

        private String content;

        public ConsoleLayoutInfo(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getContent() {
            return content;
        }

        public void setContent(final String content) {
            this.content = FormLayoutInfoUtils.defaultConsoleLayoutInfoIfEmpty(content, new AnyTypeRestClient().list());
        }
    }
}
