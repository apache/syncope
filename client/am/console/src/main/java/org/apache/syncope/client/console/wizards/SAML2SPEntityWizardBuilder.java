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
package org.apache.syncope.client.console.wizards;

import jakarta.ws.rs.core.MediaType;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.syncope.client.console.rest.SAML2SPEntityRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;

public class SAML2SPEntityWizardBuilder extends SAML2EntityWizardBuilder<SAML2SPEntityTO> {

    private static final long serialVersionUID = 2400472385439304277L;

    protected final SAML2SPEntityRestClient saml2SPEntityRestClient;

    public SAML2SPEntityWizardBuilder(
            final SAML2SPEntityTO defaultItem,
            final SAML2SPEntityRestClient saml2SPEntityRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.saml2SPEntityRestClient = saml2SPEntityRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final SAML2SPEntityTO modelObject) {
        if (modelObject.getMetadata() != null) {
            modelObject.setMetadata(Base64.getEncoder().encodeToString(
                    modelObject.getMetadata().getBytes(StandardCharsets.UTF_8)));
        }
        saml2SPEntityRestClient.set(modelObject);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SAML2SPEntityTO modelObject, final WizardModel wizardModel) {
        if (modelObject.getMetadata() != null) {
            modelObject.setMetadata(new String(Base64.getDecoder().decode(
                    modelObject.getMetadata()), StandardCharsets.UTF_8));
        }

        wizardModel.add(new Profile(modelObject, mode == AjaxWizard.Mode.CREATE));
        wizardModel.add(new Metadata(modelObject, pageRef));
        wizardModel.add(new BinaryPem(modelObject, "keystore", pageRef));
        return wizardModel;
    }

    protected static class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        Profile(final SAML2SPEntityTO entity, final boolean isNew) {
            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(entity, Constants.KEY_FIELD_NAME));
            key.addRequiredLabel();
            key.setEnabled(isNew);
            add(key);
        }
    }

    protected class BinaryPem extends Pem {

        private static final long serialVersionUID = 1L;

        public BinaryPem(final SAML2SPEntityTO entity, final String property, final PageReference pageRef) {
            super(property);

            add(new BinaryFieldPanel(
                    "content",
                    "",
                    new PropertyModel<>(entity, property),
                    MediaType.APPLICATION_OCTET_STREAM,
                    entity.getKey()) {

                private static final long serialVersionUID = -3268213909514986831L;

                @Override
                protected PageReference getPageReference() {
                    return pageRef;
                }
            });
        }
    }
}
