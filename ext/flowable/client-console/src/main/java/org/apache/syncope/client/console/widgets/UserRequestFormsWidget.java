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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.UserRequests;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtWidget(priority = 10)
public class UserRequestFormsWidget extends ExtAlertWidget<UserRequestForm> {

    private static final long serialVersionUID = 7667120094526529934L;

    @SpringBean
    protected UserRequestRestClient userRequestRestClient;

    protected final List<UserRequestForm> lastForms = new ArrayList<>();

    public UserRequestFormsWidget(final String id, final PageReference pageRef) {
        super(id, pageRef);
        setOutputMarkupId(true);

        latestAlertsList.add(new IndicatorAjaxTimerBehavior(Duration.of(30, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                if (!latestAlerts.getObject().equals(lastForms)) {
                    refreshLatestAlerts(target);
                }
            }
        });
    }

    public final void refreshLatestAlerts(final AjaxRequestTarget target) {
        latestAlerts.getObject().clear();
        latestAlerts.getObject().addAll(lastForms);

        long latestAlertSize = getLatestAlertsSize();
        linkAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(linkAlertsNumber);

        headerAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(headerAlertsNumber);

        target.add(latestAlertsList);

        lastForms.clear();
        lastForms.addAll(latestAlerts.getObject());
    }

    @Override
    protected long getLatestAlertsSize() {
        return SyncopeConsoleSession.get().owns(FlowableEntitlement.USER_REQUEST_FORM_LIST)
                ? userRequestRestClient.countForms(null)
                : 0L;
    }

    @Override
    protected IModel<List<UserRequestForm>> getLatestAlerts() {
        return new ListModel<>() {

            private static final long serialVersionUID = -2583290457773357445L;

            @Override
            public List<UserRequestForm> getObject() {
                List<UserRequestForm> updatedForms;
                if (SyncopeConsoleSession.get().owns(FlowableEntitlement.USER_REQUEST_FORM_LIST)) {
                    updatedForms = userRequestRestClient.listForms(
                            null, 1, MAX_SIZE, new SortParam<>("createTime", true));
                } else {
                    updatedForms = Collections.emptyList();
                }

                return updatedForms;
            }
        };
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
}
