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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.syncope.client.console.rest.SAML2IdPEntityRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.TextEditorPanel;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.PropertyModel;

public class SAML2IdPEntityWizardBuilder extends SAML2EntityWizardBuilder<SAML2IdPEntityTO> {

    private static final long serialVersionUID = -8013493490328546125L;

    protected final SAML2IdPEntityRestClient saml2IdPEntityRestClient;

    public SAML2IdPEntityWizardBuilder(
            final SAML2IdPEntityTO defaultItem,
            final SAML2IdPEntityRestClient saml2IdPEntityRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.saml2IdPEntityRestClient = saml2IdPEntityRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final SAML2IdPEntityTO modelObject) {
        if (modelObject.getMetadata() != null) {
            modelObject.setMetadata(Base64.getEncoder().encodeToString(
                    modelObject.getMetadata().getBytes(StandardCharsets.UTF_8)));
        }
        if (modelObject.getSigningCertificate() != null) {
            modelObject.setSigningCertificate(Base64.getEncoder().encodeToString(
                    modelObject.getSigningCertificate().getBytes(StandardCharsets.UTF_8)));
        }
        if (modelObject.getSigningKey() != null) {
            modelObject.setSigningKey(Base64.getEncoder().encodeToString(
                    modelObject.getSigningKey().getBytes(StandardCharsets.UTF_8)));
        }
        if (modelObject.getEncryptionCertificate() != null) {
            modelObject.setEncryptionCertificate(Base64.getEncoder().encodeToString(
                    modelObject.getEncryptionCertificate().getBytes(StandardCharsets.UTF_8)));
        }
        if (modelObject.getEncryptionKey() != null) {
            modelObject.setEncryptionKey(Base64.getEncoder().encodeToString(
                    modelObject.getEncryptionKey().getBytes(StandardCharsets.UTF_8)));
        }
        saml2IdPEntityRestClient.set(modelObject);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SAML2IdPEntityTO modelObject, final WizardModel wizardModel) {
        if (modelObject.getMetadata() != null) {
            modelObject.setMetadata(new String(Base64.getDecoder().decode(
                    modelObject.getMetadata()), StandardCharsets.UTF_8));
        }
        if (modelObject.getSigningCertificate() != null) {
            modelObject.setSigningCertificate(new String(Base64.getDecoder().decode(
                    modelObject.getSigningCertificate()), StandardCharsets.UTF_8));
        }
        if (modelObject.getSigningKey() != null) {
            modelObject.setSigningKey(new String(Base64.getDecoder().decode(
                    modelObject.getSigningKey()), StandardCharsets.UTF_8));
        }
        if (modelObject.getEncryptionCertificate() != null) {
            modelObject.setEncryptionCertificate(new String(Base64.getDecoder().decode(
                    modelObject.getEncryptionCertificate()), StandardCharsets.UTF_8));
        }
        if (modelObject.getEncryptionKey() != null) {
            modelObject.setEncryptionKey(new String(Base64.getDecoder().decode(
                    modelObject.getEncryptionKey()), StandardCharsets.UTF_8));
        }

        wizardModel.add(new Metadata(modelObject, pageRef));
        wizardModel.add(new TextPem(modelObject, "signingCertificate", pageRef));
        wizardModel.add(new TextPem(modelObject, "signingKey", pageRef));
        wizardModel.add(new TextPem(modelObject, "encryptionCertificate", pageRef));
        wizardModel.add(new TextPem(modelObject, "encryptionKey", pageRef));
        return wizardModel;
    }

    protected class TextPem extends Pem {

        private static final long serialVersionUID = 1L;

        public TextPem(final SAML2IdPEntityTO entity, final String property, final PageReference pageRef) {
            super(property);

            add(new TextEditorPanel(null, new PropertyModel<>(entity, property), false, pageRef));
        }
    }
}
