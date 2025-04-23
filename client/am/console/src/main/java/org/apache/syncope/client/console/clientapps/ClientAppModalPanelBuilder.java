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

import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.ClientAppRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.OIDCScopeConstants;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.LogoutType;
import org.apache.syncope.common.lib.types.OIDCApplicationType;
import org.apache.syncope.common.lib.types.OIDCClientAuthenticationMethod;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionAlg;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;
import org.apache.syncope.common.lib.types.OIDCTokenSigningAlg;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.XmlSecAlgorithm;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
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

    protected final IModel<Map<String, String>> accessPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.ACCESS).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> attrReleasePolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.ATTR_RELEASE).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> authPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.AUTH).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final IModel<Map<String, String>> ticketExpirationPolicies = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected Map<String, String> load() {
            return policyRestClient.list(PolicyType.TICKET_EXPIRATION).stream().
                    collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        }
    };

    protected final BaseModal<T> modal;

    protected final ClientAppType type;

    protected final PolicyRestClient policyRestClient;

    protected final ClientAppRestClient clientAppRestClient;

    protected final RealmRestClient realmRestClient;

    public ClientAppModalPanelBuilder(
            final ClientAppType type,
            final T defaultItem,
            final BaseModal<T> modal,
            final PolicyRestClient policyRestClient,
            final ClientAppRestClient clientAppRestClient,
            final RealmRestClient realmRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.type = type;
        this.modal = modal;
        this.policyRestClient = policyRestClient;
        this.clientAppRestClient = clientAppRestClient;
        this.realmRestClient = realmRestClient;
    }

    @Override
    public WizardModalPanel<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return new Profile(newModelObject(), modal, pageRef);
    }

    private class Profile extends AbstractModalPanel<T> {

        private static final long serialVersionUID = 7647959917047450318L;

        private final T clientAppTO;

        Profile(final T clientAppTO, final BaseModal<T> modal, final PageReference pageRef) {
            super(modal, pageRef);
            modal.setFormModel(clientAppTO);

            this.clientAppTO = clientAppTO;

            List<Component> fields = new ArrayList<>();

            boolean fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);
            AjaxSearchFieldPanel realm = new AjaxSearchFieldPanel(
                    "field", "realm", new PropertyModel<>(clientAppTO, "realm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return realmRestClient.search(fullRealmsTree
                            ? RealmsUtils.buildBaseQuery()
                            : RealmsUtils.buildKeywordQuery(input)).getResult().stream().
                            map(RealmTO::getFullPath).iterator();
                }
            };
            fields.add(realm.setOutputMarkupId(true));

            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "field", Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(clientAppTO, Constants.NAME_FIELD_NAME), false);
            fields.add(name.setRequired(true));

            if (clientAppTO.getClientAppId() == null) {
                Stream.of(ClientAppType.values()).map(clientAppRestClient::list).flatMap(List::stream).
                        max(Comparator.comparing(ClientAppTO::getClientAppId)).
                        ifPresent(app -> clientAppTO.setClientAppId(app.getClientAppId() + 1));
            }
            fields.add(new AjaxNumberFieldPanel.Builder<Long>().build(
                    "field", "clientAppId", Long.class,
                    new PropertyModel<>(clientAppTO, "clientAppId")).setRequired(true));

            fields.add(new AjaxNumberFieldPanel.Builder<Integer>().build(
                    "field", "evaluationOrder", Integer.class,
                    new PropertyModel<>(clientAppTO, "evaluationOrder")).setRequired(true));

            fields.add(new AjaxTextFieldPanel(
                    "field", Constants.DESCRIPTION_FIELD_NAME,
                    new PropertyModel<>(clientAppTO, Constants.DESCRIPTION_FIELD_NAME), false));

            fields.add(new AjaxTextFieldPanel(
                    "field", "logo",
                    new PropertyModel<>(clientAppTO, "logo"), false));

            fields.add(new AjaxTextFieldPanel(
                    "field", "theme",
                    new PropertyModel<>(clientAppTO, "theme"), false));

            AjaxTextFieldPanel informationUrl = new AjaxTextFieldPanel(
                    "field", "informationUrl",
                    new PropertyModel<>(clientAppTO, "informationUrl"), false);
            informationUrl.addValidator(new UrlValidator());
            fields.add(informationUrl);

            AjaxTextFieldPanel privacyUrl = new AjaxTextFieldPanel(
                    "field", "privacyUrl",
                    new PropertyModel<>(clientAppTO, "privacyUrl"), false);
            privacyUrl.addValidator(new UrlValidator());
            fields.add(privacyUrl);

            AjaxDropDownChoicePanel<String> accessPolicy = new AjaxDropDownChoicePanel<>(
                    "field", "accessPolicy", new PropertyModel<>(clientAppTO, "accessPolicy"), false);
            accessPolicy.setChoiceRenderer(new PolicyRenderer(accessPolicies.getObject()));
            accessPolicy.setChoices(new ArrayList<>(accessPolicies.getObject().keySet()));
            ((AbstractSingleSelectChoice<?>) accessPolicy.getField()).setNullValid(true);
            fields.add(accessPolicy);

            AjaxDropDownChoicePanel<String> attrReleasePolicy = new AjaxDropDownChoicePanel<>(
                    "field", "attrReleasePolicy", new PropertyModel<>(clientAppTO, "attrReleasePolicy"), false);
            attrReleasePolicy.setChoiceRenderer(new PolicyRenderer(attrReleasePolicies.getObject()));
            attrReleasePolicy.setChoices(new ArrayList<>(attrReleasePolicies.getObject().keySet()));
            ((AbstractSingleSelectChoice<?>) attrReleasePolicy.getField()).setNullValid(true);
            fields.add(attrReleasePolicy);

            AjaxDropDownChoicePanel<String> authPolicy = new AjaxDropDownChoicePanel<>(
                    "field", "authPolicy", new PropertyModel<>(clientAppTO, "authPolicy"), false);
            authPolicy.setChoiceRenderer(new PolicyRenderer(authPolicies.getObject()));
            authPolicy.setChoices(new ArrayList<>(authPolicies.getObject().keySet()));
            fields.add(authPolicy);

            AjaxDropDownChoicePanel<String> ticketExpirationPolicy = new AjaxDropDownChoicePanel<>(
                    "field", "ticketExpirationPolicy",
                    new PropertyModel<>(clientAppTO, "ticketExpirationPolicy"), false);
            ticketExpirationPolicy.setChoiceRenderer(new PolicyRenderer(ticketExpirationPolicies.getObject()));
            ticketExpirationPolicy.setChoices(new ArrayList<>(ticketExpirationPolicies.getObject().keySet()));
            ((AbstractSingleSelectChoice<?>) ticketExpirationPolicy.getField()).setNullValid(true);
            fields.add(ticketExpirationPolicy);

            AjaxDropDownChoicePanel<LogoutType> logoutType = new AjaxDropDownChoicePanel<>(
                    "field", "logoutType", new PropertyModel<>(clientAppTO, "logoutType"), false);
            logoutType.setChoices(List.of(LogoutType.values()));
            fields.add(logoutType.setRequired(true));

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

                        private static final long serialVersionUID = -6139318907146065915L;

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
                    clientSecret.setChoices(List.of(RandomStringUtils.secure().nextNumeric(15)));
                    fields.add(clientSecret);

                    AjaxTextFieldPanel idTokenIssuer = new AjaxTextFieldPanel(
                            "field", "idTokenIssuer", new PropertyModel<>(clientAppTO, "idTokenIssuer"), false);
                    fields.add(idTokenIssuer);

                    AjaxCheckBoxPanel signIdToken = new AjaxCheckBoxPanel(
                            "field", "signIdToken", new PropertyModel<>(clientAppTO, "signIdToken"));
                    fields.add(signIdToken);
                    AjaxDropDownChoicePanel<OIDCTokenSigningAlg> idTokenSigningAlg = new AjaxDropDownChoicePanel<>(
                            "field", "idTokenSigningAlg", new PropertyModel<>(clientAppTO, "idTokenSigningAlg"), false);
                    idTokenSigningAlg.setChoices(List.of(OIDCTokenSigningAlg.values()));
                    fields.add(idTokenSigningAlg.addRequiredLabel());

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "encryptIdToken", new PropertyModel<>(clientAppTO, "encryptIdToken")));
                    AjaxDropDownChoicePanel<OIDCTokenEncryptionAlg> idTokenEncryptionAlg =
                            new AjaxDropDownChoicePanel<>(
                                    "field",
                                    "idTokenEncryptionAlg",
                                    new PropertyModel<>(clientAppTO, "idTokenEncryptionAlg"),
                                    false);
                    idTokenEncryptionAlg.setChoices(List.of(OIDCTokenEncryptionAlg.values()));
                    fields.add(idTokenEncryptionAlg.addRequiredLabel());
                    AjaxDropDownChoicePanel<OIDCTokenEncryptionEncoding> idTokenEncryptionEncoding =
                            new AjaxDropDownChoicePanel<>(
                                    "field",
                                    "idTokenEncryptionEncoding",
                                    new PropertyModel<>(clientAppTO, "idTokenEncryptionEncoding"),
                                    false);
                    idTokenEncryptionEncoding.setChoices(List.of(OIDCTokenEncryptionEncoding.values()));
                    fields.add(idTokenEncryptionEncoding);

                    AjaxDropDownChoicePanel<OIDCTokenSigningAlg> userInfoSigningAlg = new AjaxDropDownChoicePanel<>(
                            "field",
                            "userInfoSigningAlg",
                            new PropertyModel<>(clientAppTO, "userInfoSigningAlg"),
                            false);
                    userInfoSigningAlg.setChoices(List.of(OIDCTokenSigningAlg.values()));
                    fields.add(userInfoSigningAlg);
                    AjaxDropDownChoicePanel<OIDCTokenEncryptionAlg> userInfoEncryptedResponseAlg =
                            new AjaxDropDownChoicePanel<>(
                                    "field",
                                    "userInfoEncryptedResponseAlg",
                                    new PropertyModel<>(clientAppTO, "userInfoEncryptedResponseAlg"),
                                    false);
                    userInfoEncryptedResponseAlg.setChoices(List.of(OIDCTokenEncryptionAlg.values()));
                    fields.add(userInfoEncryptedResponseAlg);
                    AjaxDropDownChoicePanel<OIDCTokenEncryptionEncoding> userInfoEncryptedResponseEncoding =
                            new AjaxDropDownChoicePanel<>(
                                    "field",
                                    "userInfoEncryptedResponseEncoding",
                                    new PropertyModel<>(clientAppTO, "userInfoEncryptedResponseEncoding"),
                                    false);
                    userInfoEncryptedResponseEncoding.setChoices(List.of(OIDCTokenEncryptionEncoding.values()));
                    fields.add(userInfoEncryptedResponseEncoding);

                    fields.add(new AjaxCheckBoxPanel(
                            "field", "jwtAccessToken", new PropertyModel<>(clientAppTO, "jwtAccessToken")));
                    fields.add(new AjaxCheckBoxPanel(
                            "field", "bypassApprovalPrompt", new PropertyModel<>(clientAppTO, "bypassApprovalPrompt")));
                    fields.add(new AjaxCheckBoxPanel(
                            "field", "generateRefreshToken", new PropertyModel<>(clientAppTO, "generateRefreshToken")));

                    AjaxDropDownChoicePanel<OIDCSubjectType> subjectType = new AjaxDropDownChoicePanel<>(
                            "field", "subjectType", new PropertyModel<>(clientAppTO, "subjectType"), false);
                    subjectType.setChoices(List.of(OIDCSubjectType.values()));
                    fields.add(subjectType.addRequiredLabel().setEnabled(true));

                    AjaxDropDownChoicePanel<OIDCApplicationType> applicationType = new AjaxDropDownChoicePanel<>(
                            "field", "applicationType", new PropertyModel<>(clientAppTO, "applicationType"), false);
                    applicationType.setChoices(List.of(OIDCApplicationType.values()));
                    fields.add(applicationType.addRequiredLabel().setEnabled(true));

                    AjaxTextFieldPanel redirectUri = new AjaxTextFieldPanel("panel", "redirectUris", new Model<>());
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

                    AutoCompleteSettings scopesSettings = new AutoCompleteSettings();
                    scopesSettings.setShowCompleteListOnFocusGain(true);
                    scopesSettings.setShowListOnEmptyInput(true);
                    AjaxSearchFieldPanel scopes = new AjaxSearchFieldPanel(
                            "panel", "scopes", new PropertyModel<>(clientAppTO, "scopes"), scopesSettings) {

                        private static final long serialVersionUID = 7160878678968866138L;

                        @Override
                        protected Iterator<String> getChoices(final String input) {
                            List<String> choices = new ArrayList<>(OIDCScopeConstants.ALL_STANDARD_SCOPES);
                            choices.add(OIDCScopeConstants.SYNCOPE);
                            return choices.iterator();
                        }
                    };
                    fields.add(new MultiFieldPanel.Builder<String>(
                            new PropertyModel<>(clientAppTO, "scopes")).build(
                            "field",
                            "scopes",
                            scopes));

                    AjaxTextFieldPanel logoutUri = new AjaxTextFieldPanel(
                            "field", "logoutUri", new PropertyModel<>(clientAppTO, "logoutUri"), false);
                    logoutUri.addValidator(new UrlValidator());
                    fields.add(logoutUri);

                    BinaryFieldPanel jwks = new BinaryFieldPanel(
                            "field",
                            "jwks",
                            new Model<>() {

                        private static final long serialVersionUID = 7666049400663637482L;

                        @Override
                        public String getObject() {
                            OIDCRPClientAppTO oidcRPCA = (OIDCRPClientAppTO) clientAppTO;
                            return StringUtils.isBlank(oidcRPCA.getJwks())
                                    ? null
                                    : Base64.getEncoder().encodeToString(
                                            oidcRPCA.getJwks().getBytes(StandardCharsets.UTF_8));
                        }

                        @Override
                        public void setObject(final String object) {
                            OIDCRPClientAppTO oidcRPCA = (OIDCRPClientAppTO) clientAppTO;
                            if (StringUtils.isBlank(object)) {
                                oidcRPCA.setJwks(null);
                            } else {
                                oidcRPCA.setJwks(
                                        new String(Base64.getDecoder().decode(object), StandardCharsets.UTF_8));
                            }
                        }
                    },
                            MediaType.APPLICATION_JSON,
                            "client-jwks");
                    fields.add(jwks);

                    AjaxTextFieldPanel jwksUri = new AjaxTextFieldPanel(
                            "field", "jwksUri", new PropertyModel<>(clientAppTO, "jwksUri"), false);
                    jwksUri.addValidator(new UrlValidator());
                    fields.add(jwksUri);

                    AjaxDropDownChoicePanel<OIDCClientAuthenticationMethod> tokenEndpointAuthenticationMethod =
                            new AjaxDropDownChoicePanel<>(
                                    "field",
                                    "tokenEndpointAuthenticationMethod",
                                    new PropertyModel<>(clientAppTO, "tokenEndpointAuthenticationMethod"),
                                    false);
                    tokenEndpointAuthenticationMethod.setChoices(List.of(OIDCClientAuthenticationMethod.values()));
                    fields.add(tokenEndpointAuthenticationMethod);
                    break;

                case SAML2SP:
                    AjaxTextFieldPanel entityId = new AjaxTextFieldPanel(
                            "field", "entityId", new PropertyModel<>(clientAppTO, "entityId"), false);
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

                    fields.add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
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
                    clientAppRestClient.create(type, clientAppTO);
                } else {
                    clientAppRestClient.update(type, clientAppTO);
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
