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
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import org.apache.syncope.client.console.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.console.layout.AnyForm;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;

public abstract class AnyWizardBuilder<A extends AnyTO> extends AjaxWizardBuilder<AnyWrapper<A>> {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final List<String> anyTypeClasses;

    protected AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo;

    /**
     * Construct.
     *
     * @param anyTO any
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final A anyTO,
            final List<String> anyTypeClasses,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo,
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
            final AnyWrapper<A> wrapper,
            final List<String> anyTypeClasses,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo,
            final PageReference pageRef) {

        super(wrapper, pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyWrapper<A> modelObject, final WizardModel wizardModel) {
        // optional details panel step
        final Details<A> details = addOptionalDetailsPanel(modelObject);
        if (details != null) {
            wizardModel.add(details);
        }

        if ((this instanceof GroupWizardBuilder)
                && (modelObject.getInnerObject() instanceof GroupTO)
                && (formLayoutInfo instanceof GroupFormLayoutInfo)) {

            GroupFormLayoutInfo groupFormLayoutInfo = GroupFormLayoutInfo.class.cast(formLayoutInfo);
            if (groupFormLayoutInfo.isOwnership()) {
                wizardModel.add(new Ownership(GroupWrapper.class.cast(modelObject), pageRef));
            }
            if (groupFormLayoutInfo.isDynamicMemberships()) {
                wizardModel.add(new DynamicMemberships(GroupWrapper.class.cast(modelObject)));
            }
        }

        if (formLayoutInfo.isAuxClasses()) {
            wizardModel.add(new AuxClasses(modelObject, anyTypeClasses));
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
                    formLayoutInfo.getWhichPlainAttrs()));
        }
        if (formLayoutInfo.isDerAttrs() && mode != AjaxWizard.Mode.TEMPLATE) {
            wizardModel.add(new DerAttrs(
                    modelObject, anyTypeClasses, formLayoutInfo.getWhichDerAttrs()));
        }
        if (formLayoutInfo.isVirAttrs()) {
            wizardModel.add(new VirAttrs(
                    modelObject, mode, anyTypeClasses, formLayoutInfo.getWhichVirAttrs()));
        }

        // role panel step (just available for users)
        if ((this instanceof UserWizardBuilder)
                && (modelObject.getInnerObject() instanceof UserTO)
                && (formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRoles()) {

            wizardModel.add(new Roles(modelObject));
        }

        // relationship panel step (available for users and any objects)
        if (((formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())
                || ((formLayoutInfo instanceof AnyObjectFormLayoutInfo)
                && AnyObjectFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())) {

            wizardModel.add(new Relationships(modelObject, pageRef));
        }

        // resource panel step
        if (formLayoutInfo.isResources()) {
            wizardModel.add(new Resources(modelObject));
        }

        return wizardModel;
    }

    protected Details<A> addOptionalDetailsPanel(final AnyWrapper<A> modelObject) {
        if (modelObject.getInnerObject().getKey() == null) {
            return null;
        } else {
            return new Details<>(
                    modelObject,
                    mode == AjaxWizard.Mode.TEMPLATE,
                    true,
                    pageRef);
        }
    }
}
