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
package org.apache.syncope.client.console.wizards;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButton;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.ComponentTag;

public class AjaxWizardMgtButtonBar extends WizardButtonBar {

    private static final long serialVersionUID = 7453943437344127136L;

    private final boolean edit;

    private final AjaxWizard wizard;

    public AjaxWizardMgtButtonBar(final String id, final AjaxWizard wizard, final boolean edit) {
        super(id, wizard);
        this.edit = edit;
        this.wizard = wizard;
        wizard.setOutputMarkupId(true);
    }

    @Override
    public MarkupContainer add(final Component... childs) {
        for (Component component : childs) {
            if (component instanceof WizardButton) {
                ajaxify((WizardButton) component);
            }
        }
        return super.add(childs);
    }

    private void ajaxify(final WizardButton button) {
        button.add(new AjaxFormSubmitBehavior("click") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                AjaxWizardMgtButtonBar.this.updateAjaxAttributes(attributes);
            }

            @Override
            public boolean getDefaultProcessing() {
                return button.getDefaultFormProcessing();
            }

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                target.add(findParent(Wizard.class));

                button.onSubmit();
            }

            @Override
            protected void onAfterSubmit(final AjaxRequestTarget target) {
                button.onAfterSubmit();
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                target.add(findParent(Wizard.class));
                button.onError();
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                tag.put("type", "button");
            }
        });
    }

    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
    }

    protected FinishButton newFinishButton(final String id, final IWizard wizard) {
        return new FinishButton(id, wizard) {

            private static final long serialVersionUID = 864248301720764819L;

            @Override
            public final boolean isEnabled() {
                if (edit) {
                    return true;
                } else {
                    final IWizardStep activeStep = getWizardModel().getActiveStep();
                    return (activeStep != null) && getWizardModel().isLastStep(activeStep) && super.isEnabled();
                }
            }
        };
    }
}
