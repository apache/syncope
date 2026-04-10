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
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardButton;

public class AjaxWizardMgtButtonBar<T extends Serializable> extends SyncopeWizardButtonBar {

    private static final long serialVersionUID = 7453943437344127136L;

    private final AjaxWizard.Mode mode;

    protected boolean completed = false;

    public AjaxWizardMgtButtonBar(final String id, final AjaxWizard<T> wizard, final AjaxWizard.Mode mode) {
        super(id, wizard);
        this.mode = mode;
    }

    @Override
    protected AjaxFormSubmitBehavior newAjaxFormSubmitBehavior(final WizardButton button) {
        return new SyncopeWizardAjaxFormSubmitBehavior(button) {

            private static final long serialVersionUID = -5761581448568725878L;

            @Override
            protected NotificationPanel getNotificationPanel() {
                return ((BaseWebPage) getPage()).getNotificationPanel();
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                AjaxWizardMgtButtonBar.this.updateAjaxAttributes(attributes);
            }
        };
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
                            completed = activeStep != null
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
