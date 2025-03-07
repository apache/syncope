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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.IndicatorAjaxLink;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobActionPanel extends WizardMgtPanel<Serializable> {

    private static final long serialVersionUID = 6645135178773151224L;

    protected static final Logger LOG = LoggerFactory.getLogger(JobActionPanel.class);

    @SpringBean
    protected NotificationRestClient notificationRestClient;

    @SpringBean
    protected ReportRestClient reportRestClient;

    @SpringBean
    protected TaskRestClient taskRestClient;

    public JobActionPanel(
            final String id,
            final JobTO jobTO,
            final boolean showNotRunning,
            final Component container) {

        super(id, true);
        setOutputMarkupId(true);

        Fragment controls;
        if (jobTO.isRunning()) {
            controls = new Fragment("controls", "runningFragment", this);
            controls.add(new Label("status", Model.of()).add(new PopoverBehavior(
                    Model.of(),
                    Model.of("<pre>" + (jobTO.getStatus() == null ? StringUtils.EMPTY : jobTO.getStatus()) + "</pre>"),
                    new PopoverConfig().withAnimation(true).withHoverTrigger().withHtml(true).
                            withPlacement(TooltipConfig.Placement.left))));
            controls.add(new IndicatorAjaxLink<Void>("stop") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        switch (jobTO.getType()) {
                            case NOTIFICATION:
                                notificationRestClient.actionJob(JobAction.STOP);
                                break;

                            case REPORT:
                                reportRestClient.actionJob(jobTO.getRefKey(), JobAction.STOP);
                                break;

                            case TASK:
                                taskRestClient.actionJob(jobTO.getRefKey(), JobAction.STOP);
                                break;

                            default:
                        }
                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        send(container, Broadcast.EXACT, new JobActionPayload(target));
                    } catch (Exception e) {
                        LOG.error("While stopping {}", jobTO.getRefDesc(), e);
                        SyncopeConsoleSession.get().onException(e);
                    }
                    ((BasePage) getPage()).getNotificationPanel().refresh(target);
                }
            });
        } else {
            controls = new Fragment("controls", "notRunningFragment", this);
            controls.add(new IndicatorAjaxLink<Void>("start") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        switch (jobTO.getType()) {
                            case NOTIFICATION:
                                notificationRestClient.actionJob(JobAction.START);
                                break;

                            case REPORT:
                                reportRestClient.actionJob(jobTO.getRefKey(), JobAction.START);
                                break;

                            case TASK:
                                taskRestClient.actionJob(jobTO.getRefKey(), JobAction.START);
                                break;

                            default:
                        }
                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        send(container, Broadcast.EXACT, new JobActionPayload(target));
                    } catch (Exception e) {
                        LOG.error("While starting {}", jobTO.getRefDesc(), e);
                        SyncopeConsoleSession.get().onException(e);
                    }
                    ((BasePage) getPage()).getNotificationPanel().refresh(target);
                }
            });
            if (!showNotRunning) {
                controls.setOutputMarkupPlaceholderTag(true);
                controls.setVisible(false);
            }
        }
        addInnerObject(controls);
    }

    public static class JobActionPayload implements Serializable {

        private static final long serialVersionUID = -6798174303329212126L;

        private final AjaxRequestTarget target;

        public JobActionPayload(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
