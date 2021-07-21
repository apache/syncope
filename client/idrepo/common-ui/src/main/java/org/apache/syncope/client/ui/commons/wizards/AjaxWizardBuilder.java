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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AjaxWizardBuilder<T extends Serializable> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5241745929825564456L;

    protected static final Logger LOG = LoggerFactory.getLogger(AjaxWizardBuilder.class);

    protected AjaxWizard.Mode mode = AjaxWizard.Mode.CREATE;

    protected final List<Component> outerObjects = new ArrayList<>();

    /**
     * Construct.
     *
     * @param defaultItem default item.
     * @param pageRef Caller page reference.
     */
    public AjaxWizardBuilder(final T defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);
    }

    public AjaxWizardBuilder<T> addOuterObject(final Component... childs) {
        outerObjects.addAll(List.of(childs));
        return this;
    }

    @Override
    public AjaxWizard<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        AjaxWizard<T> wizard = build(id, mode);
        for (int i = 1; i < index; i++) {
            wizard.getWizardModel().next();
        }
        return wizard;
    }

    /**
     * Build the wizard with a default wizard id.
     *
     * @param mode wizard mode.
     * @return wizard.
     */
    public AjaxWizard<T> build(final AjaxWizard.Mode mode) {
        return build(AbstractWizardMgtPanel.WIZARD_ID, mode);
    }

    /**
     * Build the wizard.
     *
     * @param id component id.
     * @param mode wizard mode.
     * @return wizard.
     */
    public AjaxWizard<T> build(final String id, final AjaxWizard.Mode mode) {
        this.mode = mode;

        // get the specified item if available
        T modelObj = newModelObject();

        return new AjaxWizard<>(id, modelObj, buildModelSteps(modelObj, new WizardModel()), mode, pageRef) {

            private static final long serialVersionUID = 7770507663760640735L;

            @Override
            protected void onCancelInternal() {
                AjaxWizardBuilder.this.onCancelInternal(modelObj);
            }

            @Override
            protected Pair<Serializable, Serializable> onApplyInternal(final AjaxRequestTarget target) {
                Serializable res = AjaxWizardBuilder.this.onApplyInternal(modelObj);

                Serializable payload;
                switch (mode) {
                    case CREATE:
                        payload = getCreateCustomPayloadEvent(res, target);
                        break;
                    case EDIT:
                    case TEMPLATE:
                        payload = getEditCustomPayloadEvent(res, target);
                        break;
                    default:
                        payload = null;
                }

                return Pair.of(payload, res);
            }

            @Override
            protected long getMaxWaitTimeInSeconds() {
                return AjaxWizardBuilder.this.getMaxWaitTimeInSeconds();
            }

            @Override
            protected void sendError(final Exception exception) {
                BaseSession.class.cast(Session.get()).onException(exception);
            }

            @Override
            protected void sendWarning(final String message) {
                AjaxWizardBuilder.this.sendWarning(message);
            }

            @Override
            protected Future<Pair<Serializable, Serializable>> execute(
                final Callable<Pair<Serializable, Serializable>> future) {
                return AjaxWizardBuilder.this.execute(future);
            }
        }.setEventSink(eventSink).addOuterObject(outerObjects);
    }

    protected abstract WizardModel buildModelSteps(T modelObject, WizardModel wizardModel);

    /**
     * Override to send custom events after create.
     *
     * @param afterObject after applied changes object.
     * @param target ajax request target
     * @return payload to be sent.
     */
    protected Serializable getCreateCustomPayloadEvent(final Serializable afterObject, final AjaxRequestTarget target) {
        return null;
    }

    protected abstract long getMaxWaitTimeInSeconds();

    protected abstract void sendError(Exception exception);

    protected abstract void sendWarning(String message);

    protected abstract Future<Pair<Serializable, Serializable>> execute(
            Callable<Pair<Serializable, Serializable>> future);

    /**
     * Override to send custom events after edit.
     *
     * @param afterObject after applied changes object.
     * @param target ajax request target
     * @return payload to be sent.
     */
    protected Serializable getEditCustomPayloadEvent(final Serializable afterObject, final AjaxRequestTarget target) {
        return null;
    }
}
