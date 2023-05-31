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
package org.apache.syncope.client.console.authprofiles;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.Model;

public abstract class AuthProfileWizardBuilder<T extends BaseBean> extends BaseAjaxWizardBuilder<T> {

    private static final long serialVersionUID = 1L;

    protected final StepModel<T> model;

    public AuthProfileWizardBuilder(final T defaultItem, final StepModel<T> model, final PageReference pageRef) {
        super(defaultItem, pageRef);
        this.model = model;
    }

    @Override
    protected WizardModel buildModelSteps(final T modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Step(modelObject));
        return wizardModel;
    }

    protected static class StepModel<T extends BaseBean> extends Model<T> {

        private static final long serialVersionUID = 1L;

        private T initialModelObject;

        public void setInitialModelObject(final T initialModelObject) {
            this.initialModelObject = SerializationUtils.clone(initialModelObject);
        }

        public T getInitialModelObject() {
            return initialModelObject;
        }
    }

    protected class Step extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        Step(final T modelObject) {
            model.setObject(modelObject);
            model.setInitialModelObject(modelObject);
            add(new BeanPanel<>("bean", model, pageRef).setRenderBodyOnly(true));
        }
    }
}
