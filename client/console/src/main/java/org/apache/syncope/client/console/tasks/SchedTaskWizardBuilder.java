/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.client.console.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.commons.DateFormatROModel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class SchedTaskWizardBuilder<T extends SchedTaskTO> extends AjaxWizardBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    /**
     * Construct.
     *
     * @param taskTO task
     * @param pageRef Caller page reference.
     */
    public SchedTaskWizardBuilder(final T taskTO, final PageReference pageRef) {
        super("wizard", taskTO, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final SchedTaskTO modelObject) {
        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final SchedTaskTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final IModel<List<String>> classNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(new TaskRestClient().getJobClasses());
            }
        };

        public Profile(final SchedTaskTO taskTO) {
            final AjaxTextFieldPanel name
                    = new AjaxTextFieldPanel("name", "name", new PropertyModel<String>(taskTO, "name"));
            name.setEnabled(true);
            add(name);

            final AjaxTextFieldPanel description = new AjaxTextFieldPanel("description", "description",
                    new PropertyModel<String>(taskTO, "description"));
            description.setEnabled(true);
            add(description);

            final AjaxDropDownChoicePanel<String> className = new AjaxDropDownChoicePanel<String>(
                    "jobDelegateClassName",
                    getString("jobDelegateClassName"),
                    new PropertyModel<String>(taskTO, "jobDelegateClassName"));

            className.setChoices(classNames.getObject());
            className.addRequiredLabel();
            className.setEnabled(taskTO.getKey() == null || taskTO.getKey() == 0L);
            className.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
            add(className);

            if (taskTO instanceof SyncTaskTO || taskTO instanceof PushTaskTO) {
                className.setEnabled(false).setVisible(false);
            }

            final AjaxTextFieldPanel lastExec = new AjaxTextFieldPanel("lastExec", getString("lastExec"),
                    new DateFormatROModel(new PropertyModel<String>(taskTO, "lastExec")));
            lastExec.setEnabled(false);
            add(lastExec);

            final AjaxTextFieldPanel nextExec = new AjaxTextFieldPanel("nextExec", getString("nextExec"),
                    new DateFormatROModel(new PropertyModel<String>(taskTO, "nextExec")));
            nextExec.setEnabled(false);
            add(nextExec);
//
//            final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(APPLY)) {
//
//                private static final long serialVersionUID = -958724007591692537L;
//
//                @Override
//                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
//                    SchedTaskTO taskTO = (SchedTaskTO) form.getModelObject();
//                    taskTO.setCronExpression(StringUtils.hasText(taskTO.getCronExpression())
//                            ? crontab.getCronExpression()
//                            : null);
//
//                    try {
//                        submitAction(taskTO);
//
//                        ((BasePage) pageRef.getPage()).setModalResult(true);
//
//                        window.close(target);
//                    } catch (SyncopeClientException e) {
//                        LOG.error("While creating or updating task", e);
//                        error(getString(Constants.ERROR) + ": " + e.getMessage());
//                        feedbackPanel.refresh(target);
//                    }
//                }
//
//                @Override
//                protected void onError(final AjaxRequestTarget target, final Form<?> form) {
//                    feedbackPanel.refresh(target);
//                }
//            };
//
//            final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {
//
//                private static final long serialVersionUID = -958724007591692537L;
//
//                @Override
//                protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
//                    window.close(target);
//                }
//            };
//
//            cancel.setDefaultFormProcessing(false);
//
//            if (taskTO.getId() > 0) {
//                MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, xmlRolesReader.getEntitlement(TASKS,
//                        "update"));
//            } else {
//                MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, xmlRolesReader.getEntitlement(TASKS,
//                        "create"));
//            }
//
//            form.add(submit);
//            form.add(cancel);
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final SchedTaskTO taskTO) {
            add(new CrontabPanel(
                    "schedule", new PropertyModel<String>(taskTO, "cronExpression"), taskTO.getCronExpression()));
        }

    }
}
