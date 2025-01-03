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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.SubmitableModalPanel;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.exception.CaptchaNotMatchingException;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AjaxWizard<T extends Serializable> extends Wizard
        implements SubmitableModalPanel, WizardModalPanel<T> {

    private static final long serialVersionUID = -1272120742876833520L;

    private final List<Component> outerObjects = new ArrayList<>();

    public enum Mode {
        CREATE,
        EDIT,
        TEMPLATE,
        READONLY,
        EDIT_APPROVAL;

    }

    protected static final Logger LOG = LoggerFactory.getLogger(AjaxWizard.class);

    private T item;

    private final Mode mode;

    private IEventSink eventSink;

    private final PageReference pageRef;

    /**
     * Construct.
     *
     * @param id The component id
     * @param item model object
     * @param model wizard model
     * @param mode mode
     * @param pageRef caller page reference.
     */
    public AjaxWizard(
            final String id,
            final T item,
            final WizardModel model,
            final Mode mode,
            final PageReference pageRef) {

        super(id);
        this.item = item;
        this.mode = mode;
        this.pageRef = pageRef;

        if (mode == Mode.READONLY) {
            model.setCancelVisible(false);
        }

        add(new ListView<>("outerObjectsRepeater", outerObjects) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }

        });

        setOutputMarkupId(true);
        setDefaultModel(new CompoundPropertyModel<>(this));
        init(model);
    }

    /**
     * Add object outside the main container.
     * Use this method just to be not influenced by specific inner object css'.
     * Be sure to provide {@code outer} as id.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    public final AjaxWizard<T> addOuterObject(final List<Component> childs) {
        outerObjects.addAll(childs);
        return this;
    }

    public AjaxWizard<T> setEventSink(final IEventSink eventSink) {
        this.eventSink = eventSink;
        return this;
    }

    @Override
    protected void init(final IWizardModel wizardModel) {
        super.init(wizardModel);
        getForm().remove(FEEDBACK_ID);

        if (mode == Mode.READONLY) {
            Iterator<IWizardStep> iter = wizardModel.stepIterator();
            while (iter.hasNext()) {
                WizardStep.class.cast(iter.next()).setEnabled(false);
            }
        }
    }

    @Override
    protected Component newButtonBar(final String id) {
        return new AjaxWizardMgtButtonBar<>(id, this, mode);
    }

    protected abstract void onCancelInternal();

    protected abstract void sendError(Exception exception);

    protected abstract void sendWarning(String message);

    protected abstract Future<Pair<Serializable, Serializable>> execute(
            Callable<Pair<Serializable, Serializable>> future);

    /**
     * Apply operation
     *
     * @param target request target
     * @return a pair of payload (maybe null) and resulting object.
     */
    protected abstract Pair<Serializable, Serializable> onApplyInternal(AjaxRequestTarget target);

    protected abstract long getMaxWaitTimeInSeconds();

    @Override
    public final void onCancel() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).orElse(null);
        try {
            onCancelInternal();
            if (eventSink == null) {
                send(AjaxWizard.this, Broadcast.BUBBLE, new NewItemCancelEvent<>(item, target));
            } else {
                send(eventSink, Broadcast.EXACT, new NewItemCancelEvent<>(item, target));
            }
        } catch (Exception e) {
            LOG.warn("Wizard error on cancel", e);
            sendError(e);
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

    @Override
    public final void onFinish() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).orElse(null);
        try {
            final Serializable res = onApply(target);
            if (eventSink == null) {
                send(this, Broadcast.BUBBLE, new NewItemFinishEvent<>(item, target).setResult(res));
            } else {
                send(eventSink, Broadcast.EXACT, new NewItemFinishEvent<>(item, target).setResult(res));
            }
        } catch (TimeoutException te) {
            LOG.warn("Operation took too long", te);
            if (eventSink == null) {
                send(this, Broadcast.BUBBLE, new NewItemCancelEvent<>(item, target));
            } else {
                send(eventSink, Broadcast.EXACT, new NewItemCancelEvent<>(item, target));
            }
            sendWarning(getString("timeout"));
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        } catch (CaptchaNotMatchingException ce) {
            LOG.error("Wizard error on finish: captcha not matching", ce);
            sendError(new WicketRuntimeException(getString(Constants.CAPTCHA_ERROR)));
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        } catch (Exception e) {
            LOG.error("Wizard error on finish", e);
            sendError(e);
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

    @Override
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

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            onApply(target);
        } catch (TimeoutException te) {
            LOG.warn("Operation took too long", te);
            send(eventSink, Broadcast.EXACT, new NewItemCancelEvent<>(item, target));
            sendWarning(getString("timeout"));
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

    @Override
    public void onError(final AjaxRequestTarget target) {
        ((BaseWebPage) getPage()).getNotificationPanel().refresh(target);
    }

    private Serializable onApply(final AjaxRequestTarget target) throws TimeoutException {
        try {
            Future<Pair<Serializable, Serializable>> executor = execute(new ApplyFuture(target));

            Pair<Serializable, Serializable> res = executor.get(getMaxWaitTimeInSeconds(), TimeUnit.SECONDS);

            if (res.getLeft() != null) {
                send(pageRef.getPage(), Broadcast.BUBBLE, res.getLeft());
            }

            return res.getRight();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof CaptchaNotMatchingException captchaNotMatchingException) {
                throw captchaNotMatchingException;
            }
            throw new WicketRuntimeException(e);
        }
    }

    public abstract static class NewItemEvent<T extends Serializable> {

        private final T item;

        private IModel<String> titleModel;

        private final AjaxRequestTarget target;

        private WizardModalPanel<?> modalPanel;

        public NewItemEvent(final T item, final AjaxRequestTarget target) {
            this.item = item;
            this.target = target;
        }

        public T getItem() {
            return item;
        }

        public Optional<AjaxRequestTarget> getTarget() {
            return Optional.ofNullable(target);
        }

        public WizardModalPanel<?> getModalPanel() {
            return modalPanel;
        }

        public NewItemEvent<T> forceModalPanel(final WizardModalPanel<?> modalPanel) {
            this.modalPanel = modalPanel;
            return this;
        }

        public IModel<String> getTitleModel() {
            return titleModel;
        }

        public NewItemEvent<T> setTitleModel(final IModel<String> titleModel) {
            this.titleModel = titleModel;
            return this;
        }

        public abstract String getEventDescription();
    }

    public static class NewItemActionEvent<T extends Serializable> extends NewItemEvent<T> {

        private static final String EVENT_DESCRIPTION = "new";

        private int index;

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

        @Override
        public String getEventDescription() {
            return NewItemActionEvent.EVENT_DESCRIPTION;
        }
    }

    public static class EditItemActionEvent<T extends Serializable> extends NewItemActionEvent<T> {

        private static final String EVENT_DESCRIPTION = "edit";

        public EditItemActionEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

        public EditItemActionEvent(final T item, final int index, final AjaxRequestTarget target) {
            super(item, index, target);
        }

        @Override
        public String getEventDescription() {
            return EditItemActionEvent.EVENT_DESCRIPTION;
        }
    }

    public static class NewItemCancelEvent<T extends Serializable> extends NewItemEvent<T> {

        private static final String EVENT_DESCRIPTION = "cancel";

        public NewItemCancelEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

        @Override
        public String getEventDescription() {
            return NewItemCancelEvent.EVENT_DESCRIPTION;
        }
    }

    public static class NewItemFinishEvent<T extends Serializable> extends NewItemEvent<T> {

        private static final String EVENT_DESCRIPTION = "finish";

        private Serializable result;

        public NewItemFinishEvent(final T item, final AjaxRequestTarget target) {
            super(item, target);
        }

        @Override
        public String getEventDescription() {
            return NewItemFinishEvent.EVENT_DESCRIPTION;
        }

        public NewItemFinishEvent<T> setResult(final Serializable result) {
            this.result = result;
            return this;
        }

        public Serializable getResult() {
            return result;
        }
    }

    private class ApplyFuture implements Callable<Pair<Serializable, Serializable>>, Serializable {

        private static final long serialVersionUID = -4657123322652656848L;

        private final AjaxRequestTarget target;

        private final Application application;

        private final RequestCycle requestCycle;

        private final Session session;

        ApplyFuture(final AjaxRequestTarget target) {
            this.target = target;
            this.application = Application.get();
            this.requestCycle = RequestCycle.get();
            this.session = Session.exists() ? Session.get() : null;
        }

        @Override
        public Pair<Serializable, Serializable> call() {
            try {
                ThreadContext.setApplication(this.application);
                ThreadContext.setRequestCycle(this.requestCycle);
                ThreadContext.setSession(this.session);
                return AjaxWizard.this.onApplyInternal(this.target);
            } finally {
                ThreadContext.detach();
            }
        }
    }
}
