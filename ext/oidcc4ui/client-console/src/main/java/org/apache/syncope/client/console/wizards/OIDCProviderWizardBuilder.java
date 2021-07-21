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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.panels.OIDCProvidersDirectoryPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.OIDCProviderRestClient;
import org.apache.syncope.client.console.wizards.mapping.ItemTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.mapping.JEXLTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.mapping.OIDCProviderMappingPanel;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.types.OIDCClientImplementationType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class OIDCProviderWizardBuilder extends AjaxWizardBuilder<OIDCC4UIProviderTO> {

    private static final long serialVersionUID = -3310772400714122768L;

    private final OIDCProvidersDirectoryPanel directoryPanel;

    private final IModel<List<String>> opActions = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return ImplementationRestClient.list(OIDCClientImplementationType.OP_ACTIONS).stream().
                map(EntityTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    public OIDCProviderWizardBuilder(
            final OIDCProvidersDirectoryPanel directoryPanel,
            final OIDCC4UIProviderTO defaultItem,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.directoryPanel = directoryPanel;
    }

    @Override
    protected Serializable onApplyInternal(final OIDCC4UIProviderTO modelObject) {
        if (modelObject.getKey() == null) {
            if (modelObject.getHasDiscovery()) {
                OIDCProviderRestClient.createFromDiscovery(modelObject);
            } else {
                OIDCProviderRestClient.create(modelObject);
            }

        } else {
            OIDCProviderRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final OIDCC4UIProviderTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new OP(modelObject));
        if (modelObject.getKey() == null) {
            wizardModel.add(new OPContinue(modelObject));
        } else {
            wizardModel.add(new OPContinue(modelObject, true));
        }

        Mapping mapping = new Mapping();
        mapping.setOutputMarkupId(true);

        ItemTransformersTogglePanel mapItemTransformers = new ItemTransformersTogglePanel(mapping, pageRef);
        addOuterObject(mapItemTransformers);
        JEXLTransformersTogglePanel jexlTransformers = new JEXLTransformersTogglePanel(mapping, pageRef);
        addOuterObject(jexlTransformers);
        mapping.add(new OIDCProviderMappingPanel("mapping", modelObject, mapItemTransformers, jexlTransformers));

        wizardModel.add(mapping);

        return wizardModel;
    }

    @Override
    protected long getMaxWaitTimeInSeconds() {
        return SyncopeWebApplication.get().getMaxWaitTimeInSeconds();
    }

    @Override
    protected void sendError(final Exception exception) {
        SyncopeConsoleSession.get().onException(exception);
    }

    @Override
    protected void sendWarning(final String message) {
        SyncopeConsoleSession.get().warn(message);
    }

    @Override
    protected Future<Pair<Serializable, Serializable>> execute(
            final Callable<Pair<Serializable, Serializable>> future) {
        return SyncopeConsoleSession.get().execute(future);
    }

    public class OP extends WizardStep {

        private static final long serialVersionUID = 7127421283216134900L;

        public OP(final OIDCC4UIProviderTO opTO) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    "name", "name", new PropertyModel<>(opTO, "name"), false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            AjaxTextFieldPanel clientID = new AjaxTextFieldPanel(
                    "clientID", "clientID", new PropertyModel<>(opTO, "clientID"), false);
            clientID.addRequiredLabel();
            clientID.setEnabled(true);
            add(clientID);

            AjaxTextFieldPanel clientSecret = new AjaxTextFieldPanel(
                    "clientSecret", "clientSecret", new PropertyModel<>(opTO, "clientSecret"), false);
            clientSecret.addRequiredLabel();
            clientSecret.setEnabled(true);
            add(clientSecret);

            AjaxCheckBoxPanel createUnmatching = new AjaxCheckBoxPanel(
                    "createUnmatching", "createUnmatching", new PropertyModel<>(opTO, "createUnmatching"),
                    false);
            add(createUnmatching);

            AjaxCheckBoxPanel selfRegUnmatching = new AjaxCheckBoxPanel(
                    "selfRegUnmatching", "selfRegUnmatching", new PropertyModel<>(opTO, "selfRegUnmatching"),
                    false);
            add(selfRegUnmatching);

            AjaxCheckBoxPanel updateMatching = new AjaxCheckBoxPanel(
                    "updateMatching", "updateMatching", new PropertyModel<>(opTO, "updateMatching"), false);
            add(updateMatching);

            AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                    setName(new StringResourceModel("actions", directoryPanel).getString()).
                    setAllowMoveAll(true).setAllowOrder(true).
                    build("actions",
                            new PropertyModel<>(opTO, "actions"),
                            new ListModel<>(opActions.getObject()));
            actions.setOutputMarkupId(true);
            add(actions);
        }
    }

    public static class OPContinue extends WizardStep {

        private static final long serialVersionUID = -7087008312629522790L;

        public OPContinue(final OIDCC4UIProviderTO opTO) {
            final WebMarkupContainer content = new WebMarkupContainer("content");
            this.setOutputMarkupId(true);
            content.setOutputMarkupId(true);
            add(content);

            UrlValidator urlValidator = new UrlValidator();
            final AjaxTextFieldPanel issuer = new AjaxTextFieldPanel(
                    "issuer", "issuer", new PropertyModel<>(opTO, "issuer"));
            issuer.addValidator(urlValidator);
            issuer.addRequiredLabel();
            content.add(issuer);

            final AjaxCheckBoxPanel hasDiscovery = new AjaxCheckBoxPanel(
                    "hasDiscovery", "hasDiscovery", new PropertyModel<>(opTO, "hasDiscovery"));
            content.add(hasDiscovery);

            final AjaxTextFieldPanel authorizationEndpoint = new AjaxTextFieldPanel("authorizationEndpoint",
                    "authorizationEndpoint", new PropertyModel<>(opTO, "authorizationEndpoint"));
            authorizationEndpoint.addRequiredLabel();
            authorizationEndpoint.addValidator(urlValidator);
            content.add(authorizationEndpoint);

            final AjaxTextFieldPanel userinfoEndpoint = new AjaxTextFieldPanel("userinfoEndpoint",
                    "userinfoEndpoint", new PropertyModel<>(opTO, "userinfoEndpoint"));
            userinfoEndpoint.addValidator(urlValidator);
            content.add(userinfoEndpoint);

            final AjaxTextFieldPanel tokenEndpoint = new AjaxTextFieldPanel("tokenEndpoint",
                    "tokenEndpoint", new PropertyModel<>(opTO, "tokenEndpoint"));
            tokenEndpoint.addRequiredLabel();
            tokenEndpoint.addValidator(urlValidator);
            content.add(tokenEndpoint);

            final AjaxTextFieldPanel jwksUri = new AjaxTextFieldPanel("jwksUri",
                    "jwksUri", new PropertyModel<>(opTO, "jwksUri"));
            jwksUri.addRequiredLabel();
            jwksUri.addValidator(urlValidator);
            content.add(jwksUri);

            final AjaxTextFieldPanel endSessionEndpoint = new AjaxTextFieldPanel("endSessionEndpoint",
                    "endSessionEndpoint", new PropertyModel<>(opTO, "endSessionEndpoint"));
            endSessionEndpoint.addValidator(urlValidator);
            content.add(endSessionEndpoint);

            final WebMarkupContainer visibleParam = new WebMarkupContainer("visibleParams");
            visibleParam.setOutputMarkupPlaceholderTag(true);
            visibleParam.add(authorizationEndpoint);
            visibleParam.add(userinfoEndpoint);
            visibleParam.add(tokenEndpoint);
            visibleParam.add(jwksUri);
            visibleParam.add(endSessionEndpoint);
            content.add(visibleParam);

            showHide(hasDiscovery, visibleParam);

            hasDiscovery.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    showHide(hasDiscovery, visibleParam);
                    target.add(visibleParam);
                }
            });
        }

        public OPContinue(final OIDCC4UIProviderTO opTO, final boolean readOnly) {
            WebMarkupContainer content = new WebMarkupContainer("content");
            this.setOutputMarkupId(true);
            content.setOutputMarkupId(true);
            add(content);

            final AjaxTextFieldPanel issuer = new AjaxTextFieldPanel(
                    "issuer", "issuer", new PropertyModel<>(opTO, "issuer"));
            issuer.setReadOnly(readOnly);
            content.add(issuer);

            final AjaxCheckBoxPanel hasDiscovery = new AjaxCheckBoxPanel(
                    "hasDiscovery", "hasDiscovery", new PropertyModel<>(opTO, "hasDiscovery"));
            hasDiscovery.setReadOnly(readOnly);
            content.add(hasDiscovery);

            final AjaxTextFieldPanel authorizationEndpoint = new AjaxTextFieldPanel("authorizationEndpoint",
                    "authorizationEndpoint", new PropertyModel<>(opTO, "authorizationEndpoint"));
            authorizationEndpoint.setReadOnly(readOnly);
            content.add(authorizationEndpoint);

            final AjaxTextFieldPanel userinfoEndpoint = new AjaxTextFieldPanel("userinfoEndpoint",
                    "userinfoEndpoint", new PropertyModel<>(opTO, "userinfoEndpoint"));
            userinfoEndpoint.setReadOnly(readOnly);
            content.add(userinfoEndpoint);

            final AjaxTextFieldPanel tokenEndpoint = new AjaxTextFieldPanel("tokenEndpoint",
                    "tokenEndpoint", new PropertyModel<>(opTO, "tokenEndpoint"));
            tokenEndpoint.setReadOnly(readOnly);
            content.add(tokenEndpoint);

            final AjaxTextFieldPanel jwksUri = new AjaxTextFieldPanel("jwksUri",
                    "jwksUri", new PropertyModel<>(opTO, "jwksUri"));
            jwksUri.setReadOnly(readOnly);
            content.add(jwksUri);

            final AjaxTextFieldPanel endSessionEndpoint = new AjaxTextFieldPanel("endSessionEndpoint",
                    "endSessionEndpoint", new PropertyModel<>(opTO, "endSessionEndpoint"));
            endSessionEndpoint.setReadOnly(readOnly);
            content.add(endSessionEndpoint);

            final WebMarkupContainer visibleParam = new WebMarkupContainer("visibleParams");
            visibleParam.setOutputMarkupPlaceholderTag(true);
            visibleParam.add(authorizationEndpoint);
            visibleParam.add(userinfoEndpoint);
            visibleParam.add(tokenEndpoint);
            visibleParam.add(jwksUri);
            visibleParam.add(endSessionEndpoint);
            content.add(visibleParam);
        }
    }

    private static void showHide(final AjaxCheckBoxPanel hasDiscovery, final WebMarkupContainer visibleParams) {
        if (hasDiscovery.getField().getValue().equals("false")) {
            visibleParams.setVisible(true);
        } else {
            visibleParams.setVisible(false);
        }
    }

    /**
     * Mapping definition step.
     */
    private static final class Mapping extends WizardStep {

        private static final long serialVersionUID = 3454904947720856253L;

        Mapping() {
            setTitleModel(Model.of("Mapping"));
            setSummaryModel(Model.of(StringUtils.EMPTY));
        }
    }
}
