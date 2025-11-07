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
package org.apache.syncope.client.console.widgets;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.UserRequests;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtWidget(priority = 10)
public class UserRequestFormsWidget extends ExtAlertWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    @SpringBean
    protected UserRequestRestClient userRequestRestClient;

    protected final boolean authorized;

    public UserRequestFormsWidget(final String id, final PageReference pageRef) {
        super(id, pageRef);
        authorized = SyncopeConsoleSession.get().owns(FlowableEntitlement.USER_REQUEST_FORM_LIST);
    }

    @Override
    protected long getLatestAlertsSize() {
        return authorized ? userRequestRestClient.countForms(null) : 0L;
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        BookmarkablePageLink<UserRequests> userRequests = BookmarkablePageLinkBuilder.build(linkid, UserRequests.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                userRequests, WebPage.ENABLE, FlowableEntitlement.USER_REQUEST_FORM_LIST);
        return userRequests;
    }

    @Override
    protected Icon getIcon(final String iconid) {
        return new Icon(iconid, FontAwesome5IconType.handshake_r);
    }

    public void refresh(final AjaxRequestTarget target) {
        long latestAlterts = getLatestAlertsSize();
        if (!String.valueOf(latestAlterts).equals(linkAlertsNumber.getDefaultModelObjectAsString())) {
            linkAlertsNumber.setDefaultModelObject(latestAlterts);
            target.add(linkAlertsNumber);

            headerAlertsNumber.setDefaultModelObject(latestAlterts);
            target.add(headerAlertsNumber);
        }
    }
}
