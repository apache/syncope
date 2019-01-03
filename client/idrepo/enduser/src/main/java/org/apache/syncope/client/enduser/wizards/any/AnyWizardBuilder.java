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
import org.apache.syncope.client.ui.commons.wizards.any.AbstractAnyWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AnyWizardBuilder extends AbstractAnyWizardBuilder<UserTO> {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final List<String> anyTypeClasses;

    protected UserFormLayoutInfo formLayoutInfo;

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
        if (formLayoutInfo.isDerAttrs() && mode != AjaxWizard.Mode.TEMPLATE) {
            wizardModel.add(new DerAttrs(modelObject, anyTypeClasses, formLayoutInfo.getWhichDerAttrs()));
        }
        if (formLayoutInfo.isVirAttrs()) {
            wizardModel.add(new VirAttrs(
                    modelObject, mode, anyTypeClasses, formLayoutInfo.getWhichVirAttrs()));
        }

        if (formLayoutInfo.isResources()) {
            wizardModel.add(new Resources(modelObject));
        }

        return wizardModel;
    }

    @Override
    protected long getMaxWaitTimeInSeconds() {
        return SyncopeWebApplication.get().getMaxWaitTimeInSeconds();
    }

    @Override
    protected void sendError(final String message) {
        SyncopeEnduserSession.get().error(message);
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
}
