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
package org.apache.syncope.client.enduser.wizards.any;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.layout.UserFormLayoutInfo;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardMgtButtonBar;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractAnyWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AnyWizardBuilder extends AbstractAnyWizardBuilder<UserTO> {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final List<String> anyTypeClasses;

    protected UserFormLayoutInfo formLayoutInfo;

    protected Captcha captcha;

    /**
     * Construct.
     *
     * @param anyTO any
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final UserTO anyTO,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(new AnyWrapper<>(anyTO), pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    /**
     * Construct.
     *
     * @param wrapper any wrapper
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final UserWrapper wrapper,
            final List<String> anyTypeClasses,
            final UserFormLayoutInfo formLayoutInfo,
            final PageReference pageRef) {

        super(wrapper, pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyWrapper<UserTO> modelObject, final WizardModel wizardModel) {
        wizardModel.add(new UserDetails(
                UserWrapper.class.cast(modelObject),
                mode == AjaxWizard.Mode.TEMPLATE,
                modelObject.getInnerObject().getKey() != null,
                UserFormLayoutInfo.class.cast(formLayoutInfo).isPasswordManagement(),
                pageRef));

        if (formLayoutInfo.isAuxClasses()) {
            wizardModel.add(new EnduserAuxClasses(modelObject, anyTypeClasses));
        }

        if (formLayoutInfo.isGroups()) {
            wizardModel.add(new Groups(modelObject, mode == AjaxWizard.Mode.TEMPLATE));
        }

        // attributes panel steps
        if (formLayoutInfo.isPlainAttrs()) {
            wizardModel.add(new PlainAttrs(
                    modelObject,
                    null,
                    mode,
                    anyTypeClasses,
                    formLayoutInfo.getWhichPlainAttrs()) {

                private static final long serialVersionUID = 8167894751609598306L;

                @Override
                public PageReference getPageReference() {
                    return pageRef;
                }

            });
        }
        if (formLayoutInfo.isDerAttrs()) {
            wizardModel.add(new DerAttrs(modelObject, anyTypeClasses, formLayoutInfo.getWhichDerAttrs()));
        }
        if (formLayoutInfo.isVirAttrs()) {
            wizardModel.add(new VirAttrs(
                    modelObject, mode, anyTypeClasses, formLayoutInfo.getWhichVirAttrs()));
        }
        if (formLayoutInfo.isResources()) {
            wizardModel.add(new Resources(modelObject));
        }
        if (SyncopeWebApplication.get().isCaptchaEnabled()) {
            // add captcha
            captcha = new Captcha();
            captcha.setOutputMarkupId(true);
            wizardModel.add(captcha);
        }

        return wizardModel;
    }

    @Override
    protected long getMaxWaitTimeInSeconds() {
        return SyncopeWebApplication.get().getMaxWaitTimeInSeconds();
    }

    @Override
    protected void sendError(final Exception exception) {
        SyncopeEnduserSession.get().onException(exception);
    }

    @Override
    protected void sendWarning(final String message) {
        SyncopeEnduserSession.get().warn(message);
    }

    @Override
    protected Future<Pair<Serializable, Serializable>> execute(
            final Callable<Pair<Serializable, Serializable>> future) {
        return SyncopeEnduserSession.get().execute(future);
    }

    @Override
    public AjaxWizard<AnyWrapper<UserTO>> build(final String id, final AjaxWizard.Mode mode) {
        this.mode = mode;

        // get the specified item if available
        final AnyWrapper<UserTO> modelObject = newModelObject();

        return new AjaxWizard<AnyWrapper<UserTO>>(
                id, modelObject, buildModelSteps(modelObject, new WizardModel()), mode, true, this.pageRef) {

            private static final long serialVersionUID = 7770507663760640735L;

            @Override
            protected void onCancelInternal() {
                AnyWizardBuilder.this.onCancelInternal(modelObject);
            }

            @Override
            protected Pair<Serializable, Serializable> onApplyInternal(final AjaxRequestTarget target) {
                Serializable res = AnyWizardBuilder.this.onApplyInternal(modelObject);

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
                return AnyWizardBuilder.this.getMaxWaitTimeInSeconds();
            }

            @Override
            protected void sendError(final Exception exception) {
                SyncopeEnduserSession.get().onException(exception);
            }

            @Override
            protected void sendWarning(final String message) {
                AnyWizardBuilder.this.sendWarning(message);
            }

            @Override
            protected Future<Pair<Serializable, Serializable>> execute(
                    final Callable<Pair<Serializable, Serializable>> future) {
                return AnyWizardBuilder.this.execute(future);
            }

            @Override
            protected Component newButtonBar(final String id) {
                return new AjaxWizardMgtButtonBar<>(id, this, mode) {

                    private static final long serialVersionUID = -3041152400413815333L;

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

                            @Override
                            public void onClick() {
                                IWizardModel wizardModel = getWizardModel();
                                IWizardStep activeStep = wizardModel.getActiveStep();

                                // let the step apply any state
                                activeStep.applyState();

                                // if the step completed after applying the state, notify the wizard
                                if (activeStep.isComplete()
                                        && SyncopeWebApplication.get().isCaptchaEnabled()
                                        && !getWizardModel().isLastStep(activeStep)) {
                                    // go to last step
                                    getWizardModel().last();
                                } else if (activeStep.isComplete()) {
                                    getWizardModel().finish();
                                } else {
                                    error(getLocalizer().getString(
                                            "org.apache.wicket.extensions.wizard.FinishButton.step.did.not.complete",
                                            this));
                                }
                            }
                        };
                    }

                };
            }

        }.setEventSink(eventSink).addOuterObject(outerObjects);
    }
}
