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
package org.apache.syncope.client.ui.commons.wizards;

import java.io.Serializable;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
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

public class AjaxWizardMgtButtonBar<T extends Serializable> extends WizardButtonBar {

    private static final long serialVersionUID = 7453943437344127136L;

    private final AjaxWizard.Mode mode;

    protected boolean completed = false;

    public AjaxWizardMgtButtonBar(final String id, final AjaxWizard<T> wizard, final AjaxWizard.Mode mode) {
        super(id, wizard);
        this.mode = mode;
        wizard.setOutputMarkupId(true);
    }

    @Override
    public MarkupContainer add(final Component... childs) {
        for (Component component : childs) {
            if (component instanceof final WizardButton components) {
                ajaxify(components);
            }
        }
        return super.add(childs);
    }

    private void ajaxify(final WizardButton button) {
        button.add(new AjaxFormSubmitBehavior("click") {

            private static final long serialVersionUID = 18163421824742L;

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
                ((BaseWebPage) getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                tag.put("type", "button");
            }
        });
    }

    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
    }

    @Override
    protected FinishButton newFinishButton(final String id, final IWizard wizard) {
        return new FinishButton(id, wizard) {

            private static final long serialVersionUID = 864248301720764819L;

            @Override
            public boolean isEnabled() {
                switch (mode) {
                    case EDIT:
                    case TEMPLATE:
                        return true;
                    case READONLY:
                        return false;
                    default:
                        if (!completed) {
                            final IWizardStep activeStep = getWizardModel().getActiveStep();
                            completed = (activeStep != null)
                                    && getWizardModel().isLastStep(activeStep)
                                    && super.isEnabled();
                        }
                        return completed;
                }
            }

            @Override
            public boolean isVisible() {
                switch (mode) {
                    case READONLY:
                        return false;
                    default:
                        return true;
                }
            }
        };
    }
}
