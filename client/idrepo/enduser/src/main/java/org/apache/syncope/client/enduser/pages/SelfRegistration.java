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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.enduser.panels.UserSelfFormPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SelfRegistration extends BaseNoSidebarPage {

    private static final long serialVersionUID = -1100228004207271270L;

    private static final String SELF_REGISTRATION = "page.selfRegistration";

    public static final String NEW_USER_PARAM = "newUser";

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public SelfRegistration(final PageParameters parameters) {
        super(parameters, SELF_REGISTRATION);

        setDomain(parameters);
        disableSidebarAndNavbar();

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        UserSelfFormPanel selfRegistrationPanel = new UserSelfFormPanel(
                "selfRegistrationPanel",
                buildNewUserTO(parameters),
                buildNewUserTO(parameters),
                SyncopeEnduserSession.get().getAnonymousClient().platform().getUserClasses(),
                buildFormLayout(),
                getPageReference());
        selfRegistrationPanel.setOutputMarkupId(true);
        content.add(selfRegistrationPanel);
    }

    private UserFormLayoutInfo buildFormLayout() {
        UserFormLayoutInfo customlayoutInfo = SyncopeWebApplication.get().getCustomFormLayout();
        return customlayoutInfo != null ? customlayoutInfo : new UserFormLayoutInfo();
    }

    private static UserTO buildNewUserTO(final PageParameters parameters) {
        UserTO userTO = null;
        if (parameters != null) {
            if (!parameters.get(NEW_USER_PARAM).isNull()) {
                try {
                    userTO = MAPPER.readValue(parameters.get(NEW_USER_PARAM).toString(), UserTO.class);
                } catch (JsonProcessingException e) {
                    LOG.error("While reading user data from social registration", e);
                }
            }
        }
        if (userTO == null) {
            userTO = new UserTO();
        }
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        return userTO;
    }
}
