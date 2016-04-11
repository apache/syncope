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

import java.io.Serializable;

import java.util.Collections;
import java.util.List;

import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AnyWizardBuilder<T extends AnyTO> extends AjaxWizardBuilder<AnyHandler<T>>
        implements Serializable {

    private static final long serialVersionUID = -2480279868319546243L;

    protected final AnyObjectRestClient anyObjectRestClient = new AnyObjectRestClient();

    protected final List<String> anyTypeClasses;

    /**
     * Construct.
     *
     * @param anyTO any
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public AnyWizardBuilder(final T anyTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(new AnyHandler<>(anyTO), pageRef);
        this.anyTypeClasses = anyTypeClasses;
    }

    /**
     * Construct.
     *
     * @param handler any handler
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public AnyWizardBuilder(
            final AnyHandler<T> handler,
            final List<String> anyTypeClasses,
            final PageReference pageRef) {
        super(handler, pageRef);
        this.anyTypeClasses = anyTypeClasses;
    }

    @Override
    protected WizardModel buildModelSteps(final AnyHandler<T> modelObject, final WizardModel wizardModel) {
        final String[] clazzes = anyTypeClasses.toArray(new String[] {});
        // optional details panel step
        addOptionalDetailsPanel(modelObject, wizardModel);

        if ((this instanceof GroupWizardBuilder) && (modelObject.getInnerObject() instanceof GroupTO)) {
            wizardModel.add(new Ownership(GroupHandler.class.cast(modelObject), pageRef));
            wizardModel.add(new DynamicMemberships(GroupHandler.class.cast(modelObject)));
        }

        wizardModel.add(new AuxClasses(modelObject.getInnerObject(), clazzes));

        // attributes panel steps
        wizardModel.add(new PlainAttrs(modelObject.getInnerObject(), null, Mode.ADMIN, clazzes));
        wizardModel.add(new DerAttrs(modelObject.getInnerObject(), clazzes));
        wizardModel.add(new VirAttrs(modelObject.getInnerObject(), clazzes));

        // role panel step (jst available for users)
        if ((this instanceof UserWizardBuilder) && (modelObject.getInnerObject() instanceof UserTO)) {
            wizardModel.add(new Roles(UserTO.class.cast(modelObject.getInnerObject())));
        }

        // relationship panel step (jst available for users)
        if (!(this instanceof GroupWizardBuilder)) {
            wizardModel.add(new Relationships(modelObject.getInnerObject(), pageRef));
        }

        // resource panel step
        wizardModel.add(new Resources(modelObject.getInnerObject()));
        return wizardModel;
    }

    protected AnyWizardBuilder<T> addOptionalDetailsPanel(
            final AnyHandler<T> modelObject, final WizardModel wizardModel) {
        if (modelObject.getInnerObject().getKey() != null && modelObject.getInnerObject().getKey() > 0) {
            wizardModel.add(new Details<>(
                    modelObject, new ListModel<>(Collections.<StatusBean>emptyList()), pageRef, true));
        }
        return this;
    }
}
