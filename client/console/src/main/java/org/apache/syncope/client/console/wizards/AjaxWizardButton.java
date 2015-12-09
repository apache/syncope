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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;

public abstract class AjaxWizardButton extends AjaxButton {

    private static final long serialVersionUID = -9147736051493629209L;

    private final IWizard wizard;

    public AjaxWizardButton(
            final String id,
            final IWizard wizard,
            final Form<?> form,
            final String labelResourceKey,
            final boolean formprocessing) {
        super(id, form);
        this.setLabel(new ResourceModel(labelResourceKey, labelResourceKey));
        this.wizard = wizard;
        setDefaultFormProcessing(formprocessing);
    }

    public AjaxWizardButton(final String id, final IWizard wizard, final String labelResourceKey) {
        this(id, wizard, null, labelResourceKey, true);
    }

    public AjaxWizardButton(
            final String id, final IWizard wizard, final String labelResourceKey, final boolean formprocessing) {
        this(id, wizard, null, labelResourceKey, formprocessing);
    }

    protected final IWizard getWizard() {
        return wizard;
    }

    protected final IWizardModel getWizardModel() {
        return getWizard().getWizardModel();
    }

    @Override
    protected final void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        onClick(target, form);
    }

    protected abstract void onClick(final AjaxRequestTarget target, final Form<?> form);
}
