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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.validation.ValidationError;

public class SchemaTypeWizardBuilder extends BaseAjaxWizardBuilder<SchemaTO> {

    private static final long serialVersionUID = -3893521796674873644L;

    protected final SchemaType schemaType;

    protected final SchemaRestClient schemaRestClient;

    protected final ListModel<MutablePair<Locale, String>> translations = new ListModel<>(new ArrayList<>());

    public SchemaTypeWizardBuilder(
            final SchemaTO schemaTO,
            final SchemaRestClient schemaRestClient,
            final PageReference pageRef) {

        super(schemaTO, pageRef);

        this.schemaType = SchemaType.fromToClass(schemaTO.getClass());
        this.schemaRestClient = schemaRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final SchemaTO modelObject) {
        modelObject.getLabels().clear();
        modelObject.getLabels().putAll(translations.getObject().stream().
                filter(Objects::nonNull).
                filter(translation -> translation.getKey() != null).
                filter(translation -> translation.getValue() != null).
                collect(Collectors.toMap(MutablePair::getKey, MutablePair::getValue)));

        if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
            schemaRestClient.create(schemaType, modelObject);
        } else {
            schemaRestClient.update(schemaType, modelObject);
        }

        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final SchemaTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new Labels(modelObject));
        return wizardModel;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = 382498949020534783L;

        public Details(final SchemaTO modelObject) {
            AjaxDropDownChoicePanel<SchemaType> kind =
                    new AjaxDropDownChoicePanel<>("kind", getString("kind"), new Model<>());
            kind.setChoices(List.of(SchemaType.values()));
            kind.setOutputMarkupId(true);
            kind.setModelObject(schemaType);
            kind.setEnabled(false);
            add(kind);

            Panel detailsPanel;
            switch (schemaType) {
                case DERIVED:
                    detailsPanel = new DerSchemaDetails("details", (DerSchemaTO) modelObject);
                    break;

                case VIRTUAL:
                    detailsPanel = SyncopeWebApplication.get().getVirSchemaDetailsPanelProvider().
                            get("details", (VirSchemaTO) modelObject);
                    break;

                case PLAIN:
                default:
                    detailsPanel = new PlainSchemaDetails("details", (PlainSchemaTO) modelObject);
            }
            add(detailsPanel.setOutputMarkupId(true));
        }
    }

    public class Labels extends WizardStep {

        private static final long serialVersionUID = -3130973642912822270L;

        public Labels(final SchemaTO modelObject) {
            setTitleModel(new ResourceModel("translations"));
            setOutputMarkupId(true);

            translations.getObject().clear();
            modelObject.getLabels().forEach(
                    (locale, display) -> translations.getObject().add(MutablePair.of(locale, display)));

            ListView<MutablePair<Locale, String>> labels = new ListView<>("labels", translations) {

                private static final long serialVersionUID = -8746795666847966508L;

                @Override
                protected void populateItem(final ListItem<MutablePair<Locale, String>> item) {
                    MutablePair<Locale, String> entry = item.getModelObject();

                    AjaxTextFieldPanel locale = new AjaxTextFieldPanel("locale", "locale", new Model<>(), true);
                    locale.getField().setModel(new IModel<>() {

                        private static final long serialVersionUID = 1500045101360533133L;

                        @Override
                        public String getObject() {
                            return entry.getLeft() == null ? null : entry.getLeft().toString();
                        }

                        @Override
                        public void setObject(final String object) {
                            entry.setLeft(LocaleUtils.toLocale(object));
                        }
                    });
                    locale.setRequired(true).hideLabel();
                    locale.setChoices(SyncopeConsoleSession.get().getSupportedLocales().stream().
                            map(Objects::toString).collect(Collectors.toList()));
                    locale.addValidator(validatable -> {
                        try {
                            LocaleUtils.toLocale(validatable.getValue());
                        } catch (Exception e) {
                            LOG.error("Invalid Locale: {}", validatable.getValue(), e);
                            validatable.error(new ValidationError("Invalid Locale: " + validatable.getValue()));

                            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(t -> t.add(Labels.this));
                        }
                    });
                    item.add(locale);

                    AjaxTextFieldPanel display = new AjaxTextFieldPanel("display", "display", new Model<>());
                    display.getField().setModel(new IModel<>() {

                        private static final long serialVersionUID = 1500045101360533133L;

                        @Override
                        public String getObject() {
                            return entry.getRight();
                        }

                        @Override
                        public void setObject(final String object) {
                            entry.setRight(object);
                        }
                    });
                    display.setRequired(true).hideLabel();
                    item.add(display);

                    ActionsPanel<Serializable> actions = new ActionsPanel<>("toRemove", null);
                    actions.add(new ActionLink<>() {

                        private static final long serialVersionUID = -3722207913631435501L;

                        @Override
                        public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                            translations.getObject().remove(item.getIndex());

                            item.getParent().removeAll();
                            target.add(Labels.this);
                        }
                    }, ActionLink.ActionType.DELETE, IdRepoEntitlement.SCHEMA_UPDATE, true).hideLabel();
                    item.add(actions);
                }
            };
            add(labels.setReuseItems(true));

            IndicatingAjaxButton addLabel = new IndicatingAjaxButton("addLabel") {

                private static final long serialVersionUID = -4804368561204623354L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    translations.getObject().add(MutablePair.of(null, null));
                    target.add(Labels.this);
                }
            };
            add(addLabel.setDefaultFormProcessing(false));
        }
    }
}
