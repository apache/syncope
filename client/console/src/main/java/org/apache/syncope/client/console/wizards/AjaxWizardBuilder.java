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

import java.io.Serializable;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AjaxWizardBuilder<T extends Serializable> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5241745929825564456L;

    /**
     * Construct.
     *
     * @param id The component id
     * @param defaultItem default item.
     * @param pageRef Caller page reference.
     */
    public AjaxWizardBuilder(final String id, final T defaultItem, final PageReference pageRef) {
        super(id, defaultItem, pageRef);
    }

    @Override
    public AjaxWizard<T> build(final int index, final AjaxWizard.Mode mode) {
        final AjaxWizard<T> wizard = build(mode);
        for (int i = 1; i < index; i++) {
            wizard.getWizardModel().next();
        }
        return wizard;
    }

    public AjaxWizard<T> build(final AjaxWizard.Mode mode) {
        // ge the specified item if available
        final T modelObject = newModelObject();

        return new AjaxWizard<T>(id, modelObject, buildModelSteps(modelObject, new WizardModel()), mode) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onCancelInternal() {
                AjaxWizardBuilder.this.onCancelInternal(modelObject);
            }

            @Override
            protected Serializable onApplyInternal() {
                return AjaxWizardBuilder.this.onApplyInternal(modelObject);
            }
        };
    }

    protected abstract WizardModel buildModelSteps(final T modelObject, final WizardModel wizardModel);
}
