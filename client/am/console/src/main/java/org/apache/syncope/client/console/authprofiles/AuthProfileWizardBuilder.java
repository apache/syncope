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

import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.AuthProfileRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.Model;

public abstract class AuthProfileWizardBuilder<T extends BaseBean> extends BaseAjaxWizardBuilder<T> {

    private static final long serialVersionUID = -723891643398238220L;

    protected final StepModel<T> model;

    protected final List<String> excluded;

    protected final ServiceOps serviceOps;

    protected final AuthProfileRestClient authProfileRestClient;

    public AuthProfileWizardBuilder(
            final T defaultItem,
            final StepModel<T> model,
            final List<String> excluded,
            final ServiceOps serviceOps,
            final AuthProfileRestClient authProfileRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.model = model;
        this.excluded = excluded;
        this.serviceOps = serviceOps;
        this.authProfileRestClient = authProfileRestClient;
    }

    @Override
    protected WizardModel buildModelSteps(final T modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Step(modelObject));
        if (modelObject instanceof WAConsentDecision consentDecision) {
            wizardModel.add(new ConsentAttributes(consentDecision));
        }
        return wizardModel;
    }

    protected static class StepModel<T extends BaseBean> extends Model<T> {

        private static final long serialVersionUID = -3300650579312254364L;

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
            add(new BeanPanel<>("bean", model, pageRef, excluded == null ? null : excluded.toArray(String[]::new)).
                    setRenderBodyOnly(true));
        }
    }

    protected class ConsentAttributes extends WizardStep {

        private static final long serialVersionUID = -4865650799450548351L;

        ConsentAttributes(final WAConsentDecision consentDecision) {
            String attributes = "{}";
            try {
                attributes = authProfileRestClient.readConsentAttributes(
                        serviceOps.get(NetworkService.Type.WA),
                        consentDecision.getPrincipal(),
                        consentDecision.getId());
            } catch (Exception e) {
                LOG.error("While attempting to fetch consent attributes for principal {} and id {}",
                        consentDecision.getPrincipal(), consentDecision.getId(), e);
            }
            add(new JsonEditorPanel(null, Model.of(attributes), true, pageRef));
        }

        @Override
        public String getTitle() {
            return getString("attributes");
        }
    }
}
