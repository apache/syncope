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
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;

public abstract class AjaxWizard<T extends Serializable> extends Wizard {

    private static final long serialVersionUID = 1L;

    private final PageReference pageRef;

    private T item;

    private final boolean edit;

    private NotificationPanel feedbackPanel;

    /**
     * Construct.
     *
     * @param id The component id.
     * @param item model object.
     * @param model
     * @param pageRef Caller page reference.
     * @param edit <tt>true</tt> if edit mode.
     */
    public AjaxWizard(
            final String id, final T item, final WizardModel model, final PageReference pageRef, final boolean edit) {
        super(id);
        this.item = item;
        this.pageRef = pageRef;
        this.edit = edit;

        setOutputMarkupId(true);

        setDefaultModel(new CompoundPropertyModel<AjaxWizard<T>>(this));

        init(model);
    }

    @Override
    protected Component newButtonBar(final String id) {
        return new AjaxWizardButtonBar(id, this, this.edit);
    }

    @Override
    protected Component newFeedbackPanel(final String id) {
        if (feedbackPanel == null) {
            feedbackPanel = new NotificationPanel(id);
        }
        return feedbackPanel;
    }

    public NotificationPanel getFeedbackPanel() {
        return feedbackPanel;
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

    /**
     * Replaces the default value provided with the constructor.
     *
     * @param item new value.
     * @return the current wizard instance.
     */
    public AjaxWizard<T> setItem(final T item) {
        this.item = item;
        return this;
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

    public static class NewItemActionEvent<T> extends NewItemEvent<T> {

        private int index = 0;

        public NewItemActionEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

        public NewItemActionEvent(final T item, final int index, final AjaxRequestTarget target) {
            super(item, target);
            this.index = index;
        }

        public int getIndex() {
            return index;
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
}
