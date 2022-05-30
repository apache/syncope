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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.core.util.lang.PropertyResolverConverter;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class PolicyModalPanelBuilder<T extends PolicyTO> extends AbstractModalPanelBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private static class BackOffParamsModel<N extends Number> implements IModel<N> {

        private static final long serialVersionUID = 28839546672164L;

        private final PropertyModel<String> backOffParamsModel;

        private final int index;

        BackOffParamsModel(final PropertyModel<String> backOffParamsModel, final int index) {
            this.backOffParamsModel = backOffParamsModel;
            this.index = index;
        }

        @SuppressWarnings("unchecked")
        @Override
        public N getObject() {
            String[] split = backOffParamsModel.getObject().split(";");
            if (index >= split.length) {
                return null;
            }

            return index == 2
                    ? (N) Double.valueOf(backOffParamsModel.getObject().split(";")[index])
                    : (N) Long.valueOf(backOffParamsModel.getObject().split(";")[index]);
        }

        @Override
        public void setObject(final N object) {
            String[] split = backOffParamsModel.getObject().split(";");
            if (index < split.length) {
                split[index] = object.toString();
                backOffParamsModel.setObject(Arrays.stream(split).collect(Collectors.joining(";")));
            }
        }
    }

    private final BaseModal<T> modal;

    private final PolicyType type;

    public PolicyModalPanelBuilder(
            final PolicyType type, final T policyTO, final BaseModal<T> modal, final PageReference pageRef) {

        super(policyTO, pageRef);
        this.type = type;
        this.modal = modal;
    }

    @Override
    public WizardModalPanel<T> build(final String id, final int index, final AjaxWizard.Mode mode) {
        return new Profile(newModelObject(), modal, pageRef);
    }

    private class Profile extends AbstractModalPanel<T> {

        private static final long serialVersionUID = -3043839139187792810L;

        private final T policyTO;

        private final LoadableDetachableModel<List<String>> resources = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getResourceProvider().get();
            }
        };

        Profile(final T policyTO, final BaseModal<T> modal, final PageReference pageRef) {
            super(modal, pageRef);
            modal.setFormModel(policyTO);

            this.policyTO = policyTO;

            List<Component> fields = new ArrayList<>();

            fields.add(new AjaxTextFieldPanel("field", Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(policyTO, Constants.NAME_FIELD_NAME), false).setRequired(true));

            switch (type) {
                case ACCOUNT:
                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                            "field",
                            "maxAuthenticationAttempts",
                            Integer.class,
                            new PropertyModel<>(policyTO, "maxAuthenticationAttempts")));

                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "propagateSuspension",
                            new PropertyModel<>(policyTO, "propagateSuspension"),
                            false));

                    fields.add(new AjaxPalettePanel.Builder<String>().setName("passthroughResources").build(
                            "field",
                            new PropertyModel<>(policyTO, "passthroughResources"),
                            new ListModel<>(resources.getObject())));
                    break;

                case PASSWORD:
                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                            "field",
                            "historyLength",
                            Integer.class,
                            new PropertyModel<>(policyTO, "historyLength")));

                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "allowNullPassword",
                            new PropertyModel<>(policyTO, "allowNullPassword"),
                            false));
                    break;

                case PROPAGATION:
                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                            "field",
                            "maxAttempts",
                            Integer.class,
                            new PropertyModel<>(policyTO, "maxAttempts")));
                    AjaxDropDownChoicePanel<Serializable> backOffStrategy = new AjaxDropDownChoicePanel<>(
                            "field",
                            "backOffStrategy",
                            new PropertyModel<>(policyTO, "backOffStrategy")).
                            setChoices(List.of((Serializable[]) BackOffStrategy.values()));
                    fields.add(backOffStrategy);

                    PropertyModel<String> backOffParamsModel = new PropertyModel<>(policyTO, "backOffParams");

                    AjaxSpinnerFieldPanel<Long> initialInterval = new AjaxSpinnerFieldPanel.Builder<Long>().
                            min(1L).build(
                            "field",
                            "initialInterval",
                            Long.class,
                            new BackOffParamsModel<>(backOffParamsModel, 0));
                    fields.add(initialInterval.setOutputMarkupPlaceholderTag(true));
                    AjaxSpinnerFieldPanel<Long> maxInterval = new AjaxSpinnerFieldPanel.Builder<Long>().
                            min(1L).build(
                            "field",
                            "maxInterval",
                            Long.class,
                            new BackOffParamsModel<>(backOffParamsModel, 1));
                    fields.add(maxInterval.setOutputMarkupPlaceholderTag(true).setVisible(false));
                    AjaxSpinnerFieldPanel<Double> multiplier = new AjaxSpinnerFieldPanel.Builder<Double>().
                            min(1D).build(
                            "field",
                            "multiplier",
                            Double.class,
                            new BackOffParamsModel<>(backOffParamsModel, 2));
                    fields.add(multiplier.setOutputMarkupPlaceholderTag(true).setVisible(false));

                    showHide(backOffStrategy, initialInterval, maxInterval, multiplier);

                    backOffStrategy.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = -1107858522700306810L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            BackOffStrategy strategy = (BackOffStrategy) backOffStrategy.getField().getModelObject();
                            backOffParamsModel.setObject(strategy.getDefaultBackOffParams());

                            showHide(backOffStrategy, initialInterval, maxInterval, multiplier);

                            target.add(initialInterval);
                            target.add(maxInterval);
                            target.add(multiplier);
                        }
                    });
                    break;

                case PULL:
                case PUSH:
                    fields.add(new AjaxDropDownChoicePanel<>(
                            "field",
                            "conflictResolutionAction",
                            new PropertyModel<>(policyTO, "conflictResolutionAction")).
                            setChoices(List.of((Serializable[]) ConflictResolutionAction.values())));
                    break;

                case ACCESS:
                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                            "field",
                            "order",
                            Integer.class,
                            new PropertyModel<>(policyTO, "order")));
                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "enabled",
                            new PropertyModel<>(policyTO, "enabled"),
                            false));
                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "ssoEnabled",
                            new PropertyModel<>(policyTO, "ssoEnabled"),
                            false));
                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "requireAllAttributes",
                            new PropertyModel<>(policyTO, "requireAllAttributes"),
                            false));
                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "caseInsensitive",
                            new PropertyModel<>(policyTO, "caseInsensitive"),
                            false));
                    AjaxTextFieldPanel unauthorizedRedirectUrl = new AjaxTextFieldPanel(
                            "field",
                            "unauthorizedRedirectUrl",
                            new IModel<>() {

                        @Override
                        public String getObject() {
                            return Optional.ofNullable(
                                    (URI) PropertyResolver.getValue("unauthorizedRedirectUrl", policyTO)).
                                    map(URI::toASCIIString).orElse(null);
                        }

                        @Override
                        public void setObject(final String object) {
                            PropertyResolverConverter prc = new PropertyResolverConverter(
                                    Application.get().getConverterLocator(),
                                    SyncopeConsoleSession.get().getLocale());
                            PropertyResolver.setValue(
                                    "unauthorizedRedirectUrl",
                                    policyTO,
                                    Optional.ofNullable(object).map(URI::create).orElse(null),
                                    prc);
                        }
                    }, false);
                    unauthorizedRedirectUrl.getField().add(new UrlValidator(new String[] { "http", "https" }));
                    fields.add(unauthorizedRedirectUrl);
                    break;

                case ATTR_RELEASE:
                    fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                            "field",
                            "order",
                            Integer.class,
                            new PropertyModel<>(policyTO, "order")));
                    fields.add(new AjaxCheckBoxPanel(
                            "field",
                            "status",
                            new PropertyModel<>(policyTO, "status"),
                            false));
                    break;

                case AUTH:
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

        private void showHide(
                final AjaxDropDownChoicePanel<Serializable> backOffStrategy,
                final AjaxSpinnerFieldPanel<Long> initialInterval,
                final AjaxSpinnerFieldPanel<Long> maxInterval,
                final AjaxSpinnerFieldPanel<Double> multiplier) {

            BackOffStrategy strategy = (BackOffStrategy) backOffStrategy.getField().getModelObject();

            switch (strategy) {
                case EXPONENTIAL:
                    initialInterval.addLabel("initialInterval");
                    maxInterval.setVisible(true);
                    multiplier.setVisible(true);
                    break;

                case RANDOM:
                    initialInterval.addLabel("initialInterval");
                    maxInterval.setVisible(true);
                    multiplier.setVisible(true);
                    break;

                case FIXED:
                default:
                    initialInterval.addLabel("period");
                    maxInterval.setVisible(false);
                    multiplier.setVisible(false);
            }
        }

        @Override
        public void onSubmit(final AjaxRequestTarget target) {
            try {
                if (policyTO.getKey() == null) {
                    PolicyRestClient.create(type, policyTO);
                } else {
                    PolicyRestClient.update(type, policyTO);
                }
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                Profile.this.modal.close(target);
            } catch (Exception e) {
                LOG.error("While creating/updating policy", e);
                SyncopeConsoleSession.get().onException(e);
            }
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
