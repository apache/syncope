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
package org.apache.syncope.client.console.wizards;

import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.html.form.Form;

public class AjaxWizardButtonBar extends WizardButtonBar {

    private static final long serialVersionUID = 5641095671558703391L;

    public AjaxWizardButtonBar(final String id, final AjaxWizard<?> wizard, final boolean edit) {
        super(id, wizard);

        addOrReplace(new AjaxWizardButton("next", wizard, "next") {

            private static final long serialVersionUID = 1773811852118436784L;

            @Override
            protected void onClick(final AjaxRequestTarget target, final Form<?> form) {
                IWizardModel wizardModel = getWizardModel();
                IWizardStep step = wizardModel.getActiveStep();

                // let the step apply any state
                step.applyState();

                // if the step completed after applying the state, move the model onward
                if (step.isComplete()) {
                    wizardModel.next();
                } else {
                    error(getLocalizer().getString(
                            "org.apache.wicket.extensions.wizard.NextButton.step.did.not.complete", this));
                }

                target.add(wizard);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                super.onError(target, form);
                NotificationPanel.class.cast(wizard.getFeedbackPanel()).refresh(target);
            }

            @Override
            public final boolean isEnabled() {
                return super.isEnabled() && getWizardModel().isNextAvailable();
            }
        });

        addOrReplace(new AjaxWizardButton("previous", wizard, "prev", false) {

            private static final long serialVersionUID = 5704878742768853867L;

            @Override
            protected void onClick(final AjaxRequestTarget target, final Form<?> form) {
                getWizardModel().previous();
                wizard.modelChanged();
                target.add(wizard);
            }

            @Override
            public final boolean isEnabled() {
                return super.isEnabled() && getWizardModel().isPreviousAvailable();
            }
        });

        addOrReplace(new AjaxWizardButton("cancel", wizard, "cancel", false) {

            private static final long serialVersionUID = 5704878742768853867L;

            @Override
            protected void onClick(final AjaxRequestTarget target, final Form<?> form) {
                getWizardModel().cancel();
                target.add(wizard);
            }

            @Override
            public final boolean isEnabled() {
                return true;
            }
        });

        addOrReplace(new AjaxWizardButton("finish", wizard, "finish") {

            private static final long serialVersionUID = 1773811852118436784L;

            @Override
            protected void onClick(final AjaxRequestTarget target, final Form<?> form) {
                getWizardModel().finish();
                target.add(wizard);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                super.onError(target, form);
                NotificationPanel.class.cast(wizard.getFeedbackPanel()).refresh(target);
            }

            @Override
            public final boolean isEnabled() {
                if (edit) {
                    return true;
                } else {
                    final IWizardStep activeStep = getWizardModel().getActiveStep();
                    return (activeStep != null) && getWizardModel().isLastStep(activeStep) && super.isEnabled();
                }
            }
        });
    }

    @Override
    public final MarkupContainer addOrReplace(final Component... childs) {
        return super.addOrReplace(childs);
    }
}
