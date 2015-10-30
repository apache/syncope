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

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AjaxWizardBuilder<T extends Serializable> {

    private static final long serialVersionUID = 1L;

    private final String id;

    private final PageReference pageRef;

    private T defaultItem;

    private T item;

    /**
     * Construct.
     *
     * @param id The component id
     * @param defaultItem default item.
     * @param pageRef Caller page reference.
     */
    public AjaxWizardBuilder(final String id, final T defaultItem, final PageReference pageRef) {
        this.id = id;
        this.defaultItem = defaultItem;
        this.pageRef = pageRef;
    }

    public AjaxWizard<T> build(final int index) {
        final AjaxWizard<T> wizard = build(index > 0);
        for (int i = 1; i < index; i++) {
            wizard.getWizardModel().next();
        }
        return wizard;
    }

    public AjaxWizard<T> build(final boolean edit) {
        final T modelObject = getItem();
        this.item = null;

        return new AjaxWizard<T>(id, modelObject, buildModelSteps(modelObject, new WizardModel()), pageRef, edit) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onCancelInternal() {
                AjaxWizardBuilder.this.onCancelInternal(getItem());
            }

            @Override
            protected void onApplyInternal() {
                AjaxWizardBuilder.this.onApplyInternal(getItem());
            }
        };
    }

    protected abstract WizardModel buildModelSteps(final T modelObject, final WizardModel wizardModel);

    protected abstract void onCancelInternal(T modelObject);

    protected abstract void onApplyInternal(T modelObject);

    protected T getDefaultItem() {
        return defaultItem;
    }

    private T getItem() {
        return item == null ? SerializationUtils.clone(defaultItem) : item;
    }

    /**
     * Replaces the default value provided with the constructor and nullify working item object.
     *
     * @param item new value.
     * @return the current wizard factory instance.
     */
    public AjaxWizardBuilder<T> setItem(final T item) {
        this.defaultItem = item;
        this.item = null;
        return this;
    }
}
