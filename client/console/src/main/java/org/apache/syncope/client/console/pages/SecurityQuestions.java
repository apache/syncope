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

import org.apache.syncope.client.console.panels.SecurityQuestionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SecurityQuestions extends BasePage {

    private static final long serialVersionUID = 931085006718655535L;

    private final SecurityQuestionsPanel securityQuestionsPanel;

    private final BaseModal<SecurityQuestionTO> securityQuestionModal;

    public SecurityQuestions(final PageParameters parameters) {
        super(parameters);

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.add(new Label("header", getString("header_title")));
        content.setOutputMarkupId(true);
        add(content);

        securityQuestionModal = new BaseModal<>("securityQuestionModal");
        addWindowWindowClosedCallback(securityQuestionModal);
        add(securityQuestionModal);

        securityQuestionsPanel = new SecurityQuestionsPanel("securityQuestionPanel", getPageReference());
        securityQuestionsPanel.setOutputMarkupId(true);

        content.add(securityQuestionsPanel);
    }

    private void addWindowWindowClosedCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(securityQuestionsPanel);
                modal.show(false);

                ((AbstractBasePage) getPage()).getNotificationPanel().refresh(target);
            }
        }
        );
    }
}
