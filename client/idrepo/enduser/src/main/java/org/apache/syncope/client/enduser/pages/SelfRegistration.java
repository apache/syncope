/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.pages;

import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.panels.UserSelfFormPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SelfRegistration extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    private static final String SELF_REGISTRATION = "page.selfRegistration";

    public SelfRegistration(final PageParameters parameters) {
        super(parameters, SELF_REGISTRATION);
        
        setDomain(parameters);
        disableSidebar();

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        UserSelfFormPanel selfRegistrationPanel = new UserSelfFormPanel(
                "selfRegistrationPanel",
                buildNewUserTO(parameters),
                buildNewUserTO(parameters),
                SyncopeEnduserSession.get().getService(SyncopeService.class).platform().getUserClasses(),
                buildFormLayout(),
                getPageReference());
        selfRegistrationPanel.setOutputMarkupId(true);
        content.add(selfRegistrationPanel);
    }

    private UserFormLayoutInfo buildFormLayout() {
        UserFormLayoutInfo customlayoutInfo = SyncopeEnduserApplication.get().getCustomFormLayout();
        return customlayoutInfo != null ? customlayoutInfo : new UserFormLayoutInfo();
    }

    private static UserTO buildNewUserTO(final PageParameters parameters) {
        UserTO userTO = new UserTO();

        if (parameters != null) {
            if (!parameters.get("saml2SPUserAttrs").isNull()) {
                SyncopeEnduserApplication.extractAttrsFromExt(parameters.get("saml2SPUserAttrs").toString(), userTO);
            } else if (!parameters.get("oidcClientUserAttrs").isNull()) {
                SyncopeEnduserApplication.extractAttrsFromExt(parameters.get("oidcClientUserAttrs").toString(), userTO);
            }
        }
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        return userTO;
    }
}
