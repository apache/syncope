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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.console.wizards.any.DerAttrs;
import org.apache.syncope.client.console.wizards.any.PlainAttrs;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.ResourceModel;

public class RealmWizardBuilder extends BaseAjaxWizardBuilder<RealmTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected static final class RealmWrapper extends AnyWrapper<RealmTO> {

        private static final long serialVersionUID = 4347624290770782509L;

        public RealmWrapper(final RealmTO attributable) {
            super(attributable);
        }
    }

    protected final RealmRestClient realmRestClient;

    protected RealmTO parent;

    public RealmWizardBuilder(final RealmRestClient realmRestClient, final PageReference pageRef) {
        super(new RealmTO(), pageRef);
        this.realmRestClient = realmRestClient;
    }

    public void setParent(final RealmTO parent) {
        this.parent = parent;
    }

    @Override
    protected Serializable onApplyInternal(final RealmTO modelObject) {
        return parent == null
                ? realmRestClient.update(modelObject)
                : realmRestClient.create(parent.getFullPath(), modelObject);
    }

    @Override
    protected WizardModel buildModelSteps(final RealmTO modelObject, final WizardModel wizardModel) {
        Optional.ofNullable(parent).ifPresent(p -> modelObject.getAnyTypeClasses().addAll(p.getAnyTypeClasses()));

        RealmDetails details = new RealmDetails("details", modelObject);
        details.add(new AttributeAppender("style", "overflow-x:hidden;"));

        wizardModel.add(new Realm(details));

        RealmWrapper wrapper = new RealmWrapper(modelObject);

        wizardModel.add(new PlainAttrs(wrapper, mode, modelObject.getAnyTypeClasses(), List.of()) {

            private static final long serialVersionUID = 8167894751609598306L;

            @Override
            public PageReference getPageReference() {
                return pageRef;
            }
        });

        wizardModel.add(new DerAttrs(wrapper, modelObject.getAnyTypeClasses(), List.of()) {

            private static final long serialVersionUID = 4298394879912549771L;

            @Override
            public PageReference getPageReference() {
                return pageRef;
            }
        });

        wizardModel.add(new Placeholder(wrapper));

        return wizardModel;
    }

    protected static class Realm extends WizardStep {

        private static final long serialVersionUID = -2123790676338327104L;

        protected Realm(final RealmDetails details) {
            add(details);
        }
    }

    protected static class Placeholder extends WizardStep implements ICondition {

        private static final long serialVersionUID = -1633642371208520029L;

        protected final RealmWrapper wrapper;

        protected Placeholder(final RealmWrapper wrapper) {
            this.wrapper = wrapper;
            setTitleModel(new ResourceModel("noAttributes"));
        }

        @Override
        public boolean evaluate() {
            // display this step only if no AnyTypeClasses are selected for this Realm
            return wrapper.getInnerObject().getAnyTypeClasses().isEmpty();
        }
    }
}
