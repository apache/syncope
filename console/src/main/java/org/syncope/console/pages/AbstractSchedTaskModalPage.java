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
package org.syncope.console.pages;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.StringUtils;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.DateFormatROModel;
import org.syncope.console.markup.html.CrontabContainer;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public abstract class AbstractSchedTaskModalPage extends TaskModalPage {

    private static final long serialVersionUID = 2892005971093059242L;

    protected CrontabContainer crontab;

    public AbstractSchedTaskModalPage(
            final ModalWindow window,
            final SchedTaskTO taskTO,
            final PageReference callerPageRef) {

        super(taskTO);

        crontab = new CrontabContainer("crontab",
                new PropertyModel<String>(taskTO, "cronExpression"),
                taskTO.getCronExpression());
        form.add(crontab);

        final AjaxTextFieldPanel lastExec = new AjaxTextFieldPanel(
                "lastExec", getString("lastExec"), new DateFormatROModel(
                new PropertyModel<String>(taskTO, "lastExec")), false);
        lastExec.setEnabled(false);
        profile.add(lastExec);

        final AjaxTextFieldPanel nextExec = new AjaxTextFieldPanel(
                "nextExec", getString("nextExec"), new DateFormatROModel(
                new PropertyModel<String>(taskTO, "nextExec")), false);
        nextExec.setEnabled(false);
        profile.add(nextExec);

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("apply")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                SchedTaskTO taskTO = (SchedTaskTO) form.getModelObject();
                taskTO.setCronExpression(
                        !StringUtils.hasText(taskTO.getCronExpression())
                        ? null : crontab.getCronExpression());

                try {
                    if (taskTO.getId() > 0) {
                        if (taskTO instanceof SyncTaskTO) {
                            taskRestClient.updateSyncTask((SyncTaskTO) taskTO);
                        } else {
                            taskRestClient.updateSchedTask(taskTO);
                        }
                    } else {
                        if (taskTO instanceof SyncTaskTO) {
                            taskRestClient.createSyncTask((SyncTaskTO) taskTO);
                        } else {
                            taskRestClient.createSchedTask(taskTO);
                        }
                    }

                    ((BasePage) callerPageRef.getPage()).setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    LOG.error("While creating or updating task", e);
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };


        if (taskTO.getId() > 0) {
            MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Tasks", "update"));
        } else {
            MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER,
                    xmlRolesReader.getAllAllowedRoles("Tasks", "create"));
        }

        form.add(submit);
    }
}
