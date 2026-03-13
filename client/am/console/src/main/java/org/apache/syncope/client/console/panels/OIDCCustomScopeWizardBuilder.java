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
import java.util.HashSet;
import java.util.Optional;
import org.apache.syncope.client.console.panels.OIDCCustomScopeDirectoryPanel.CustomScope;
import org.apache.syncope.client.console.rest.OIDCOpEntityRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class OIDCCustomScopeWizardBuilder extends BaseAjaxWizardBuilder<CustomScope> {

    private static final long serialVersionUID = 6268620772839923063L;

    protected final OIDCOpEntityRestClient restClient;

    public OIDCCustomScopeWizardBuilder(
            final CustomScope customScope,
            final OIDCOpEntityRestClient restClient,
            final PageReference pageRef) {

        super(customScope, pageRef);
        this.restClient = restClient;
    }

    @Override
    protected Serializable onApplyInternal(final CustomScope modelObject) {
        OIDCOpEntityTO oidcOpEntity = Optional.ofNullable(restClient.get().get()).orElseGet(() -> new OIDCOpEntityTO());

        Optional.ofNullable(getOriginalItem().getKey()).ifPresent(oidcOpEntity.getCustomScopes()::remove);

        oidcOpEntity.getCustomScopes().put(modelObject.getKey(), new HashSet<>(modelObject.getClaims()));

        restClient.set(oidcOpEntity);

        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final CustomScope modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        return wizardModel;
    }

    protected class Profile extends WizardStep {

        private static final long serialVersionUID = 746342514949780968L;

        public Profile(final CustomScope modelObject) {
            AjaxTextFieldPanel scope = new AjaxTextFieldPanel(
                    "scope", "scope", new PropertyModel<>(modelObject, "scope"), false);
            add(scope.setReadOnly(modelObject.getKey() != null).setRequired(true));

            AjaxTextFieldPanel claims = new AjaxTextFieldPanel("panel", "claims", Model.of());
            add(new MultiFieldPanel.Builder<String>(
                    new PropertyModel<>(modelObject, "claims")).build(
                    "claims",
                    "claims",
                    claims).setRequired(true));
        }
    }
}
