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
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.request.cycle.RequestCycle;

public abstract class AjaxWizard<T extends Serializable> extends Wizard {

    private static final long serialVersionUID = 1L;

    private final PageReference pageRef;

    private final T item;

    /**
     * Construct.
     *
     * @param id The component id
     * @param item
     * @param pageRef Caller page reference.
     */
    public AjaxWizard(final String id, final T item, final PageReference pageRef) {
        super(id);
        this.item = item;
        this.pageRef = pageRef;
        setOutputMarkupId(true);
    }

    @Override
    protected Component newButtonBar(final String id) {
        return new AjaxWizardButtonBar(id, this);
    }

    protected abstract void onCancelInternal();

    protected abstract void onApplyInternal();

    /**
     * @see org.apache.wicket.extensions.wizard.Wizard#onCancel()
     */
    @Override
    public final void onCancel() {
        onCancelInternal();
        send(pageRef.getPage(), Broadcast.DEPTH,
                new NewItemCancelEvent<T>(item, RequestCycle.get().find(AjaxRequestTarget.class)));
    }

    /**
     * @see org.apache.wicket.extensions.wizard.Wizard#onFinish()
     */
    @Override
    public final void onFinish() {
        onApplyInternal();
        send(pageRef.getPage(), Broadcast.DEPTH,
                new NewItemFinishEvent<T>(item, RequestCycle.get().find(AjaxRequestTarget.class)));
    }

    public T getItem() {
        return item;
    }

    public abstract static class NewItemEvent<T> {
        private final T item;

        private final AjaxRequestTarget target;

        public NewItemEvent(final T item, final AjaxRequestTarget target) {
            this.item = item;
            this.target = target;
        }

        public T getItem() {
            return item;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }

    public static class NewItemCancelEvent<T> extends NewItemEvent<T> {

        public NewItemCancelEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

    }

    public static class NewItemFinishEvent<T> extends NewItemEvent<T> {

        public NewItemFinishEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

    }

    /**
     *
     * @return
     */
    @Override
    public AjaxWizard<T> clone() {
        return SerializationUtils.clone(this);
    }
}
