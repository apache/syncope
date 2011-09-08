/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.Arrays;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.StringUtils;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class SchedTaskModalPage extends TaskModalPage {

    private static final long serialVersionUID = 2892005971093059242L;

    protected WebMarkupContainer crontab;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public SchedTaskModalPage(
            final BasePage basePage,
            final ModalWindow window,
            final SchedTaskTO taskTO) {

        super(taskTO);

        crontab = new WebMarkupContainer("crontab");
        crontab.setOutputMarkupId(true);
        form.add(crontab);

        final AjaxTextFieldPanel lastExec = new AjaxTextFieldPanel(
                "lastExec", getString("lastExec"),
                new PropertyModel<String>(taskTO, "lastExec"), false);
        lastExec.setEnabled(false);
        profile.add(lastExec);

        final AjaxTextFieldPanel nextExec = new AjaxTextFieldPanel(
                "nextExec", getString("nextExec"),
                new PropertyModel<String>(taskTO, "nextExec"), false);
        nextExec.setEnabled(false);
        profile.add(nextExec);

        final DropDownChoice<String> cronTemplateChooser = new DropDownChoice(
                "cronTemplateChooser",
                new PropertyModel(taskTO, "cronExpression"),
                Arrays.asList(Tasks.CRON_TEMPLATES),
                new SelectChoiceRenderer()) {

            private static final long serialVersionUID = -5843424545478691442L;

            @Override
            protected CharSequence getDefaultChoice(Object selected) {
                return "<option value=\"\">"
                        + getString("chooseForTemplate")
                        + "</option>";
            }
        };

        final TextField seconds = new TextField(
                "seconds",
                new Model(getCronField(taskTO.getCronExpression(), 0)));
        crontab.add(seconds);

        final TextField minutes = new TextField(
                "minutes",
                new Model(getCronField(taskTO.getCronExpression(), 1)));
        crontab.add(minutes);

        final TextField hours = new TextField(
                "hours",
                new Model(getCronField(taskTO.getCronExpression(), 2)));
        crontab.add(hours);

        final TextField daysOfMonth = new TextField(
                "daysOfMonth",
                new Model(getCronField(taskTO.getCronExpression(), 3)));
        crontab.add(daysOfMonth);

        final TextField months = new TextField(
                "months",
                new Model(getCronField(taskTO.getCronExpression(), 4)));
        crontab.add(months);

        final TextField daysOfWeek = new TextField(
                "daysOfWeek",
                new Model(getCronField(taskTO.getCronExpression(), 5)));
        crontab.add(daysOfWeek);

        cronTemplateChooser.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        seconds.setModelObject(
                                getCronField(cronTemplateChooser, 0));
                        minutes.setModelObject(
                                getCronField(cronTemplateChooser, 1));
                        hours.setModelObject(
                                getCronField(cronTemplateChooser, 2));
                        daysOfMonth.setModelObject(
                                getCronField(cronTemplateChooser, 3));
                        months.setModelObject(
                                getCronField(cronTemplateChooser, 4));
                        daysOfWeek.setModelObject(
                                getCronField(cronTemplateChooser, 5));
                        target.addComponent(crontab);
                    }
                });

        crontab.add(cronTemplateChooser);

        final IndicatingAjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("apply")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                SchedTaskTO taskTO = (SchedTaskTO) form.getModelObject();
                if (!StringUtils.hasText(taskTO.getCronExpression())
                        || "UNSCHEDULE".equals(taskTO.getCronExpression())) {
                    taskTO.setCronExpression(null);
                } else {
                    taskTO.setCronExpression(getCron(
                            seconds, minutes, hours,
                            daysOfMonth, months, daysOfWeek));
                }

                try {
                    if (taskTO.getId() > 0) {
                        // update task
                        if (taskTO instanceof SyncTaskTO) {
                            taskTO = taskRestClient.updateSyncTask(
                                    (SyncTaskTO) taskTO);
                        } else {
                            taskTO = taskRestClient.updateSchedTask(taskTO);
                        }
                    } else {
                        if (taskTO instanceof SyncTaskTO) {
                            // create task
                            taskTO = taskRestClient.createSyncTask(
                                    (SyncTaskTO) taskTO);
                        } else {
                            taskTO = taskRestClient.createSchedTask(taskTO);
                        }
                    }
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    LOG.error("While creating or updating task", e);
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
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
