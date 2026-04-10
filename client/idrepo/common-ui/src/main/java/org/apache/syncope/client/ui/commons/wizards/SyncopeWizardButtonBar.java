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

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButton;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.ComponentTag;

public abstract class SyncopeWizardButtonBar extends WizardButtonBar {

    private static final long serialVersionUID = -7619496626807763617L;

    protected abstract class SyncopeWizardAjaxFormSubmitBehavior extends AjaxFormSubmitBehavior {

        private static final long serialVersionUID = 5919882857610793575L;

        protected final WizardButton button;

        protected SyncopeWizardAjaxFormSubmitBehavior(final WizardButton button) {
            super(Constants.ON_CLICK);
            this.button = button;
        }

        protected abstract NotificationPanel getNotificationPanel();

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
            getNotificationPanel().refresh(target);
        }

        @Override
        protected void onComponentTag(final ComponentTag tag) {
            tag.put("type", "button");
        }
    }

    public SyncopeWizardButtonBar(final String id, final Wizard wizard) {
        super(id, wizard);
        wizard.setOutputMarkupId(true);
    }

    protected abstract AjaxFormSubmitBehavior newAjaxFormSubmitBehavior(WizardButton button);

    @Override
    public MarkupContainer add(final Component... childs) {
        for (Component component : childs) {
            if (component instanceof final WizardButton button) {
                button.add(newAjaxFormSubmitBehavior(button));
            }
        }
        return super.add(childs);
    }
}
