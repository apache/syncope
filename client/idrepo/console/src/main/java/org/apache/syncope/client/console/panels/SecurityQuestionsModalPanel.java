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
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SecurityQuestionsModalPanel extends AbstractModalPanel<SecurityQuestionTO> {

    private static final long serialVersionUID = 4024126489500665435L;

    @SpringBean
    protected SecurityQuestionRestClient securityQuestionRestClient;

    protected final SecurityQuestionTO securityQuestionTO;

    public SecurityQuestionsModalPanel(
            final BaseModal<SecurityQuestionTO> modal,
            final SecurityQuestionTO securityQuestionTO,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.securityQuestionTO = securityQuestionTO;
        add(new SecurityQuestionDetailsPanel("securityQuestionDetailsPanel", getItem()));
    }

    @Override
    public final SecurityQuestionTO getItem() {
        return this.securityQuestionTO;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            if (securityQuestionTO.getKey() == null) {
                securityQuestionRestClient.create(securityQuestionTO);
            } else {
                securityQuestionRestClient.update(securityQuestionTO);
            }

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating or updating {}", securityQuestionTO, e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
