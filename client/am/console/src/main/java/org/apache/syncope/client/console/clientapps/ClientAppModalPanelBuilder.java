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
package org.apache.syncope.client.console.clientapps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class ClientAppModalPanelBuilder<T extends ClientAppTO> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final IModel<Map<String, String>> accessPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return PolicyRestClient.list(PolicyType.ACCESS).stream().
                collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName));
        }
    };

    private final IModel<Map<String, String>> attrReleasePolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return PolicyRestClient.list(PolicyType.ATTR_RELEASE).stream().
                collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName));
        }
    };

    private final IModel<Map<String, String>> authPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return PolicyRestClient.list(PolicyType.AUTH).stream().
                collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName));
        }
    };

    private final BaseModal<T> modal;

    private final ClientAppType type;

    public ClientAppModalPanelBuilder(
            final ClientAppType type, final T defaultItem, final BaseModal<T> modal, final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.type = type;
        this.modal = modal;
    }

    @Override
    public WizardModalPanel<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return new Profile(newModelObject(), modal, pageRef);
    }

    private class Profile extends AbstractModalPanel<T> {

        private static final long serialVersionUID = 1L;

        private final T clientAppTO;

        Profile(final T clientAppTO, final BaseModal<T> modal, final PageReference pageRef) {
            super(modal, pageRef);
            modal.setFormModel(clientAppTO);

            this.clientAppTO = clientAppTO;

            List<Component> fields = new ArrayList<>();

            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "field", Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(clientAppTO, Constants.NAME_FIELD_NAME), false);
            fields.add(name.setRequired(true));

            if (clientAppTO.getClientAppId() == null) {
                Stream.of(ClientAppType.values()).map(ClientAppRestClient::list).flatMap(List::stream).
                        max(Comparator.comparing(ClientAppTO::getClientAppId)).
                        ifPresent(app -> clientAppTO.setClientAppId(app.getClientAppId() + 1));
            }
            fields.add(new AjaxSpinnerFieldPanel.Builder<Long>().build(
                    "field", "clientAppId", Long.class,
                    new PropertyModel<>(clientAppTO, "clientAppId")).setRequired(true));

            fields.add(new AjaxTextFieldPanel(
                    "field", Constants.DESCRIPTION_FIELD_NAME,
                    new PropertyModel<>(clientAppTO, Constants.DESCRIPTION_FIELD_NAME), false));

            AjaxDropDownChoicePanel<String> accessPolicy = new AjaxDropDownChoicePanel<>(
                    "field", "accessPolicy", new PropertyModel<>(clientAppTO, "accessPolicy"), false);
            accessPolicy.setChoiceRenderer(new PolicyRenderer(accessPolicies));
            accessPolicy.setChoices(new ArrayList<>(accessPolicies.getObject().keySet()));
            ((AbstractSingleSelectChoice<?>) accessPolicy.getField()).setNullValid(true);
            fields.add(accessPolicy);

            AjaxDropDownChoicePanel<String> attrReleasePolicy = new AjaxDropDownChoicePanel<>(
                    "field", "attrReleasePolicy", new PropertyModel<>(clientAppTO, "attrReleasePolicy"), false);
            attrReleasePolicy.setChoiceRenderer(new PolicyRenderer(attrReleasePolicies));
            attrReleasePolicy.setChoices(new ArrayList<>(attrReleasePolicies.getObject().keySet()));
            ((AbstractSingleSelectChoice<?>) attrReleasePolicy.getField()).setNullValid(true);
            fields.add(attrReleasePolicy);

            AjaxDropDownChoicePanel<String> authPolicy = new AjaxDropDownChoicePanel<>(
                    "field", "authPolicy", new PropertyModel<>(clientAppTO, "authPolicy"), false);
            authPolicy.setChoiceRenderer(new PolicyRenderer(authPolicies));
            authPolicy.setChoices(new ArrayList<>(authPolicies.getObject().keySet()));
            authPolicy.setRequired(true);
            ((AbstractSingleSelectChoice<?>) authPolicy.getField()).setNullValid(true);
            fields.add(authPolicy);

            fields.add(new AjaxTextFieldPanel("field", "theme", new PropertyModel<>(clientAppTO, "theme"), false));

            switch (type) {
                case CASSP:
                    fields.add(new AjaxTextFieldPanel(
                            "field", "serviceId", new PropertyModel<>(clientAppTO, "serviceId"), false).
                            setRequired(true));
                    break;

                case OIDCRP:
                    AjaxTextFieldPanel clientId = new AjaxTextFieldPanel(
                            "field", "clientId", new PropertyModel<>(clientAppTO, "clientId"), false);
                    fields.add(clientId.setRequired(true));
                    name.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            if (StringUtils.isBlank(clientId.getModelObject())) {
                                clientId.setModelObject(name.getModelObject());
                                target.add(clientId);
                            }
                        }
                    });

                    AjaxTextFieldPanel clientSecret = new AjaxTextFieldPanel(
                            "field", "clientSecret", new PropertyModel<>(clientAppTO, "clientSecret"), false);
                    clientSecret.setChoices(List.of(RandomStringUtils.randomAlphanumeric(15)));
                    fields.add(clientSecret.setRequired(true));

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "signIdToken", new PropertyModel<>(clientAppTO, "signIdToken")));
                    fields.add(new AjaxCheckBoxPanel(
                        "field", "jwtAccessToken", new PropertyModel<>(clientAppTO, "jwtAccessToken")));

                    AjaxDropDownChoicePanel<OIDCSubjectType> subjectType = new AjaxDropDownChoicePanel<>(
                            "field", "subjectType", new PropertyModel<>(clientAppTO, "subjectType"), false);
                    subjectType.setChoices(List.of(OIDCSubjectType.values()));
                    subjectType.addRequiredLabel().setEnabled(true);
                    fields.add(subjectType);

                    AjaxTextFieldPanel redirectUri = new AjaxTextFieldPanel("panel", "redirectUris", new Model<>());
                    redirectUri.addValidator(new UrlValidator());
                    fields.add(new MultiFieldPanel.Builder<String>(
                            new PropertyModel<>(clientAppTO, "redirectUris")).build(
                            "field",
                            "redirectUris",
                            redirectUri));

                    fields.add(new AjaxPalettePanel.Builder<OIDCGrantType>().setName("supportedGrantTypes").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "supportedGrantTypes"),
                            new ListModel<>(List.of(OIDCGrantType.values()))));

                    fields.add(new AjaxPalettePanel.Builder<OIDCResponseType>().setName("supportedResponseTypes").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "supportedResponseTypes"),
                            new ListModel<>(List.of(OIDCResponseType.values()))));

                    AjaxTextFieldPanel logoutUri = new AjaxTextFieldPanel(
                            "field", "logoutUri", new PropertyModel<>(clientAppTO, "logoutUri"), false);
                    logoutUri.addValidator(new UrlValidator());
                    fields.add(logoutUri);
                    break;

                case SAML2SP:
                    AjaxTextFieldPanel entityId = new AjaxTextFieldPanel(
                            "field", "entityId", new PropertyModel<>(clientAppTO, "entityId"), false);
                    entityId.addValidator(new UrlValidator());
                    fields.add(entityId.setRequired(true));

                    fields.add(new AjaxTextFieldPanel("field", "metadataLocation",
                            new PropertyModel<>(clientAppTO, "metadataLocation"), false).setRequired(true));

                    fields.add(new AjaxTextFieldPanel(
                            "field", "metadataSignatureLocation",
                            new PropertyModel<>(clientAppTO, "metadataSignatureLocation"), false));

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "signAssertions", new PropertyModel<>(clientAppTO, "signAssertions")));

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "signResponses", new PropertyModel<>(clientAppTO, "signResponses")));

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "encryptionOptional", new PropertyModel<>(clientAppTO, "encryptionOptional")));

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "encryptAssertions", new PropertyModel<>(clientAppTO, "encryptAssertions")));

                    fields.add(new AjaxTextFieldPanel(
                            "field", "requiredAuthenticationContextClass",
                            new PropertyModel<>(clientAppTO, "requiredAuthenticationContextClass"), false));

                    AjaxDropDownChoicePanel<SAML2SPNameId> requiredNameIdFormat = new AjaxDropDownChoicePanel<>(
                            "field", "requiredNameIdFormat", new PropertyModel<>(clientAppTO, "requiredNameIdFormat"),
                            false);
                    requiredNameIdFormat.setChoices(List.of(SAML2SPNameId.values()));
                    requiredNameIdFormat.addRequiredLabel().setEnabled(true);
                    fields.add(requiredNameIdFormat);

                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().min(0).build(
                            "field", "skewAllowance", Integer.class,
                            new PropertyModel<>(clientAppTO, "skewAllowance")));

                    fields.add(new AjaxTextFieldPanel(
                            "field", "nameIdQualifier", new PropertyModel<>(clientAppTO, "nameIdQualifier"), false));

                    AjaxTextFieldPanel assertionAudience = new AjaxTextFieldPanel(
                            "panel", "assertionAudience", new Model<>());
                    assertionAudience.addValidator(new UrlValidator());
                    fields.add(new MultiFieldPanel.Builder<String>(
                            new PropertyModel<>(clientAppTO, "assertionAudiences")).build(
                            "field",
                            "assertionAudiences",
                            assertionAudience));

                    fields.add(new AjaxTextFieldPanel(
                            "field", "serviceProviderNameIdQualifier",
                            new PropertyModel<>(clientAppTO, "serviceProviderNameIdQualifier"), false));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("signingSignatureAlgorithms").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "signingSignatureAlgorithms"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("signingSignatureReferenceDigestMethods").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "signingSignatureReferenceDigestMethods"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("encryptionDataAlgorithms").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "encryptionDataAlgorithms"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("encryptionKeyAlgorithms").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "encryptionKeyAlgorithms"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("signingSignatureBlackListedAlgorithms").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "signingSignatureBlackListedAlgorithms"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));

                    fields.add(new AjaxPalettePanel.Builder<XmlSecAlgorithm>().
                            setName("encryptionBlackListedAlgorithms").build(
                            "field",
                            new PropertyModel<>(clientAppTO, "encryptionBlackListedAlgorithms"),
                            new ListModel<>(List.of(XmlSecAlgorithm.values()))));
                    break;

                default:
            }

            add(new ListView<>("fields", fields) {

                private static final long serialVersionUID = -9180479401817023838L;

                @Override
                protected void populateItem(final ListItem<Component> item) {
                    item.add(item.getModelObject());
                }
            });
        }

        @Override
        public void onSubmit(final AjaxRequestTarget target) {
            try {
                if (clientAppTO.getKey() == null) {
                    ClientAppRestClient.create(type, clientAppTO);
                } else {
                    ClientAppRestClient.update(type, clientAppTO);
                }
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                Profile.this.modal.close(target);
            } catch (Exception e) {
                LOG.error("While creating/updating clientApp", e);
                SyncopeConsoleSession.get().onException(e);
            }
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
