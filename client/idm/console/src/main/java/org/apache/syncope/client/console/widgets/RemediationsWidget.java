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
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.Remediations;
import org.apache.syncope.client.console.rest.RemediationRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
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

@ExtWidget(priority = 0)
public class RemediationsWidget extends ExtAlertWidget<RemediationTO> {

    private static final long serialVersionUID = 1817429725840355068L;

    @SpringBean
    protected RemediationRestClient remediationRestClient;

    protected final List<RemediationTO> lastRemediations = new ArrayList<>();

    public RemediationsWidget(final String id, final PageReference pageRef) {
        super(id, pageRef);
        setOutputMarkupId(true);

        latestAlertsList.add(new IndicatorAjaxTimerBehavior(Duration.of(30, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                if (!latestAlerts.getObject().equals(lastRemediations)) {
                    refreshLatestAlerts(target);
                }
            }
        });
    }

    public final void refreshLatestAlerts(final AjaxRequestTarget target) {
        latestAlerts.getObject().clear();
        latestAlerts.getObject().addAll(lastRemediations);

        long latestAlertSize = getLatestAlertsSize();
        linkAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(linkAlertsNumber);

        headerAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(headerAlertsNumber);

        target.add(latestAlertsList);

        lastRemediations.clear();
        lastRemediations.addAll(latestAlerts.getObject());
    }

    @Override
    protected long getLatestAlertsSize() {
        return SyncopeConsoleSession.get().owns(IdMEntitlement.REMEDIATION_LIST)
                && SyncopeConsoleSession.get().owns(IdMEntitlement.REMEDIATION_READ)
                ? remediationRestClient.countRemediations()
                : 0L;
    }

    @Override
    protected IModel<List<RemediationTO>> getLatestAlerts() {
        return new ListModel<>() {

            private static final long serialVersionUID = 541491929575585613L;

            @Override
            public List<RemediationTO> getObject() {
                List<RemediationTO> updatedRemediations;
                if (SyncopeConsoleSession.get().owns(IdMEntitlement.REMEDIATION_LIST)
                        && SyncopeConsoleSession.get().owns(IdMEntitlement.REMEDIATION_READ)) {

                    updatedRemediations = remediationRestClient.getRemediations(
                            1, MAX_SIZE, new SortParam<>("instant", true));
                } else {
                    updatedRemediations = List.of();
                }

                return updatedRemediations;
            }
        };
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        BookmarkablePageLink<Remediations> remediations = BookmarkablePageLinkBuilder.build(linkid, Remediations.class);
        MetaDataRoleAuthorizationStrategy.authorize(remediations, WebPage.ENABLE, IdMEntitlement.REMEDIATION_LIST);
        return remediations;
    }

    @Override
    protected Icon getIcon(final String iconid) {
        return new Icon(iconid, FontAwesome5IconType.medkit_s);
    }
}
