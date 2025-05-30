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
package org.apache.syncope.client.enduser.pages;

import java.util.List;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class ReauthLogin extends Login {

    private static final long serialVersionUID = -218346357365913190L;

    public ReauthLogin(final PageParameters parameters) {
        super(parameters);

        SyncopeEnduserSession session = SyncopeEnduserSession.get();

        usernameField.setEnabled(false);
        usernameField.setModelObject(session.getSelfTO().getUsername());
        usernameField.modelChanged();

        languageSelect.setEnabled(false);
        languageSelect.setModelObject(session.getLocale());
        languageSelect.modelChanged();

        domainSelect.setEnabled(false);
        domainSelect.setModelObject(session.getDomain());
        domainSelect.modelChanged();

        selfPwdReset.setVisible(false);
        selfRegistration.setVisible(false);
    }

    @Override
    protected List<BaseSSOLoginFormPanel> getSSOLoginFormPanels() {
        List<BaseSSOLoginFormPanel> panels = super.getSSOLoginFormPanels();
        panels.forEach(panel -> panel.setReauth(true));
        return panels;
    }

    @Override
    protected void onAuthenticateSuccess(final AjaxRequestTarget target) {
        SyncopeEnduserSession.get().setLastReauth();
        super.onAuthenticateSuccess(target);
    }
}
