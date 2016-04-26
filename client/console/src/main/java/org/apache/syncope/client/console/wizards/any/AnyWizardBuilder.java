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

import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.layout.AbstractAnyFormLayout;
import org.apache.syncope.client.console.layout.AnyForm;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AnyWizardBuilder<A extends AnyTO> extends AjaxWizardBuilder<AnyHandler<A>> {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final AnyObjectRestClient anyObjectRestClient = new AnyObjectRestClient();

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

        super(new AnyHandler<>(anyTO), pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    /**
     * Construct.
     *
     * @param handler any handler
     * @param anyTypeClasses any type classes
     * @param formLayoutInfo form layout info
     * @param pageRef caller page reference.
     */
    public AnyWizardBuilder(
            final AnyHandler<A> handler,
            final List<String> anyTypeClasses,
            final AbstractAnyFormLayout<A, ? extends AnyForm<A>> formLayoutInfo,
            final PageReference pageRef) {

        super(handler, pageRef);
        this.anyTypeClasses = anyTypeClasses;
        this.formLayoutInfo = formLayoutInfo;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyHandler<A> modelObject, final WizardModel wizardModel) {
        // optional details panel step
        addOptionalDetailsPanel(modelObject, wizardModel);

        if ((this instanceof GroupWizardBuilder)
                && (modelObject.getInnerObject() instanceof GroupTO)
                && (formLayoutInfo instanceof GroupFormLayoutInfo)) {

            GroupFormLayoutInfo groupFormLayoutInfo = GroupFormLayoutInfo.class.cast(formLayoutInfo);
            if (groupFormLayoutInfo.isOwnership()) {
                wizardModel.add(new Ownership(GroupHandler.class.cast(modelObject), pageRef));
            }
            if (groupFormLayoutInfo.isDynamicMemberships()) {
                wizardModel.add(new DynamicMemberships(GroupHandler.class.cast(modelObject)));
            }
        }

        if (formLayoutInfo.isAuxClasses()) {
            wizardModel.add(new AuxClasses(modelObject.getInnerObject(), anyTypeClasses));
        }

        // attributes panel steps
        if (formLayoutInfo.isPlainAttrs()) {
            wizardModel.add(new PlainAttrs(
                    modelObject.getInnerObject(),
                    null,
                    Mode.ADMIN,
                    anyTypeClasses,
                    formLayoutInfo.getWhichPlainAttrs()));
        }
        if (formLayoutInfo.isDerAttrs()) {
            wizardModel.add(new DerAttrs(
                    modelObject.getInnerObject(), anyTypeClasses, formLayoutInfo.getWhichDerAttrs()));
        }
        if (formLayoutInfo.isVirAttrs()) {
            wizardModel.add(new VirAttrs(
                    modelObject.getInnerObject(), anyTypeClasses, formLayoutInfo.getWhichVirAttrs()));
        }

        // role panel step (just available for users)
        if ((this instanceof UserWizardBuilder)
                && (modelObject.getInnerObject() instanceof UserTO)
                && (formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRoles()) {

            wizardModel.add(new Roles(UserTO.class.cast(modelObject.getInnerObject())));
        }

        // relationship panel step (available for users and any objects)
        if (((formLayoutInfo instanceof UserFormLayoutInfo)
                && UserFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())
                || ((formLayoutInfo instanceof AnyObjectFormLayoutInfo)
                && AnyObjectFormLayoutInfo.class.cast(formLayoutInfo).isRelationships())) {

            wizardModel.add(new Relationships(modelObject.getInnerObject(), pageRef));
        }

        // resource panel step
        if (formLayoutInfo.isResources()) {
            wizardModel.add(new Resources(modelObject.getInnerObject()));
        }

        return wizardModel;
    }

    protected AnyWizardBuilder<A> addOptionalDetailsPanel(
            final AnyHandler<A> modelObject, final WizardModel wizardModel) {

        if (modelObject.getInnerObject().getKey() != null) {
            wizardModel.add(new Details<>(
                    modelObject, new ListModel<>(Collections.<StatusBean>emptyList()), true, pageRef));
        }
        return this;
    }
}
