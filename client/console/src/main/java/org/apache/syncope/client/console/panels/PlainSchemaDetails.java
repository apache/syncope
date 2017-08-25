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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.PropertyList;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

public class PlainSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 5378100729213456451L;

    private static final MIMETypesLoader MIME_TYPES_LOADER = (MIMETypesLoader) SyncopeConsoleApplication.get().
            getServletContext().getAttribute(ConsoleInitializer.MIMETYPES_LOADER);

    private final MultiFieldPanel<String> enumerationValues;

    private final MultiFieldPanel<String> enumerationKeys;

    public PlainSchemaDetails(
            final String id,
            final PageReference pageReference,
            final AbstractSchemaTO schemaTO) {
        super(id, pageReference, schemaTO);

        final AjaxDropDownChoicePanel<AttrSchemaType> type = new AjaxDropDownChoicePanel<>(
                "type", getString("type"), new PropertyModel<>(schemaTO, "type"));

        type.setChoices(Arrays.asList(AttrSchemaType.values()));
        type.setEnabled(schemaTO == null || schemaTO.getKey() == null || schemaTO.getKey().isEmpty());
        type.addRequiredLabel();

        schemaForm.add(type);

        // long, double, date
        final AjaxTextFieldPanel conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<>(schemaTO, "conversionPattern"));

        schemaForm.add(conversionPattern);

        final WebMarkupContainer conversionParams = new WebMarkupContainer("conversionParams");
        conversionParams.setOutputMarkupPlaceholderTag(true);
        conversionParams.add(conversionPattern);

        schemaForm.add(conversionParams);

        final WebMarkupContainer typeParams = new WebMarkupContainer("typeParams");

        typeParams.setOutputMarkupPlaceholderTag(true);

        // enum
        final AjaxTextFieldPanel enumerationValuesPanel = new AjaxTextFieldPanel("panel",
                "enumerationValues", new Model<>(null));

        enumerationValues = new MultiFieldPanel.Builder<String>(
                new PropertyModel<List<String>>(schemaTO, "enumerationValues") {

            private static final long serialVersionUID = -4953564762272833993L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<PlainSchemaTO>() {

                    @Override
                    public String getValues() {
                        return PlainSchemaTO.class.cast(schemaTO).getEnumerationValues();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        PlainSchemaTO.class.cast(schemaTO).setEnumerationValues(getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                PlainSchemaTO.class.cast(schemaTO).setEnumerationValues(PropertyList.getEnumValuesAsString(object));
            }
        }) {

            private static final long serialVersionUID = -8752965211744734798L;

            @Override
            protected String newModelObject() {
                return StringUtils.EMPTY;
            }

        }.build(
                "enumerationValues",
                "enumerationValues",
                enumerationValuesPanel);

        enumerationKeys = new MultiFieldPanel.Builder<String>(
                new PropertyModel<List<String>>(schemaTO, "enumerationKeys") {

            private static final long serialVersionUID = -4953564762272833993L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<PlainSchemaTO>() {

                    @Override
                    public String getValues() {
                        return PlainSchemaTO.class.cast(schemaTO).getEnumerationKeys();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        PlainSchemaTO.class.cast(schemaTO).setEnumerationKeys(PropertyList.getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                PlainSchemaTO.class.cast(schemaTO).setEnumerationKeys(PropertyList.getEnumValuesAsString(object));
            }
        }) {

            private static final long serialVersionUID = -8752965211744734798L;

            @Override
            protected String newModelObject() {
                return StringUtils.EMPTY;
            }

        }.build(
                "enumerationKeys",
                "enumerationKeys",
                new AjaxTextFieldPanel("panel", "enumerationKeys", new Model<String>()));

        final WebMarkupContainer enumParams = new WebMarkupContainer("enumParams");
        enumParams.setOutputMarkupPlaceholderTag(true);
        enumParams.add(enumerationValues);
        enumParams.add(enumerationKeys);
        typeParams.add(enumParams);

        // encrypted
        final AjaxTextFieldPanel secretKey = new AjaxTextFieldPanel("secretKey",
                getString("secretKey"), new PropertyModel<>(schemaTO, "secretKey"));

        final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm = new AjaxDropDownChoicePanel<>(
                "cipherAlgorithm", getString("cipherAlgorithm"),
                new PropertyModel<>(schemaTO, "cipherAlgorithm"));

        cipherAlgorithm.setChoices(Arrays.asList(CipherAlgorithm.values()));

        final WebMarkupContainer encryptedParams = new WebMarkupContainer("encryptedParams");
        encryptedParams.setOutputMarkupPlaceholderTag(true);
        encryptedParams.add(secretKey);
        encryptedParams.add(cipherAlgorithm);

        typeParams.add(encryptedParams);

        // binary
        final AjaxTextFieldPanel mimeType = new AjaxTextFieldPanel("mimeType",
                getString("mimeType"), new PropertyModel<>(schemaTO, "mimeType"));

        final WebMarkupContainer binaryParams = new WebMarkupContainer("binaryParams");
        binaryParams.setOutputMarkupPlaceholderTag(true);
        binaryParams.add(mimeType);
        typeParams.add(binaryParams);
        schemaForm.add(typeParams);

        // show or hide
        showHide(schemaTO, type,
                conversionParams, conversionPattern,
                enumParams, enumerationValuesPanel, enumerationValues, enumerationKeys,
                encryptedParams, secretKey, cipherAlgorithm,
                binaryParams, mimeType);

        type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                PlainSchemaDetails.this.showHide(schemaTO, type,
                        conversionParams, conversionPattern,
                        enumParams, enumerationValuesPanel, enumerationValues, enumerationKeys,
                        encryptedParams, secretKey, cipherAlgorithm,
                        binaryParams, mimeType);
                target.add(conversionParams);
                target.add(typeParams);
            }
        }
        );

        IModel<List<String>> validatorsList = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getValidators());
            }
        };
        final AjaxDropDownChoicePanel<String> validatorClass = new AjaxDropDownChoicePanel<>("validatorClass",
                getString("validatorClass"), new PropertyModel<>(schemaTO, "validatorClass"));
        ((DropDownChoice) validatorClass.getField()).setNullValid(true);
        validatorClass.setChoices(validatorsList.getObject());
        schemaForm.add(validatorClass);

        AutoCompleteTextField<String> mandatoryCondition = new AutoCompleteTextField<String>("mandatoryCondition") {

            private static final long serialVersionUID = -2428903969518079100L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                List<String> choices = new ArrayList<>();

                if (Strings.isEmpty(input)) {
                    choices = Collections.emptyList();
                } else if ("true".startsWith(input.toLowerCase())) {
                    choices.add("true");
                } else if ("false".startsWith(input.toLowerCase())) {
                    choices.add("false");
                }

                return choices.iterator();
            }
        };
        mandatoryCondition.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        schemaForm.add(mandatoryCondition);

        schemaForm.add(Constants.getJEXLPopover(this, TooltipConfig.Placement.right));

        schemaForm.add(new AjaxCheckBoxPanel(
                "multivalue", getString("multivalue"), new PropertyModel<>(schemaTO, "multivalue")));

        schemaForm.add(new AjaxCheckBoxPanel(
                "readonly", getString("readonly"), new PropertyModel<>(schemaTO, "readonly")));

        schemaForm.add(new AjaxCheckBoxPanel("uniqueConstraint",
                getString("uniqueConstraint"), new PropertyModel<>(schemaTO, "uniqueConstraint")));

    }

    private void showHide(final AbstractSchemaTO schema, final AjaxDropDownChoicePanel<AttrSchemaType> type,
            final WebMarkupContainer conversionParams, final AjaxTextFieldPanel conversionPattern,
            final WebMarkupContainer enumParams, final AjaxTextFieldPanel enumerationValuesPanel,
            final MultiFieldPanel<String> enumerationValues, final MultiFieldPanel<String> enumerationKeys,
            final WebMarkupContainer encryptedParams,
            final AjaxTextFieldPanel secretKey, final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm,
            final WebMarkupContainer binaryParams, final AjaxTextFieldPanel mimeType) {

        final int typeOrdinal = Integer.parseInt(type.getField().getValue());
        if (AttrSchemaType.Long.ordinal() == typeOrdinal
                || AttrSchemaType.Double.ordinal() == typeOrdinal
                || AttrSchemaType.Date.ordinal() == typeOrdinal) {

            conversionParams.setVisible(true);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(null));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(null));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);

            binaryParams.setVisible(false);
            mimeType.setModelObject(null);
            mimeType.setChoices(null);
        } else if (AttrSchemaType.Enum.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(true);
            if (!enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.addRequiredLabel();
            }
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(((PlainSchemaTO) schema).
                    getEnumerationValues()));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(((PlainSchemaTO) schema).
                    getEnumerationKeys()));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);

            binaryParams.setVisible(false);
            mimeType.setModelObject(null);
            mimeType.setChoices(null);
        } else if (AttrSchemaType.Encrypted.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(null));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(null));

            encryptedParams.setVisible(true);
            if (!secretKey.isRequired()) {
                secretKey.addRequiredLabel();
            }
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.addRequiredLabel();
            }

            binaryParams.setVisible(false);
            mimeType.setModelObject(null);
            mimeType.setChoices(null);
        } else if (AttrSchemaType.Binary.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(null));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(null));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);

            binaryParams.setVisible(true);
            mimeType.setChoices(MIME_TYPES_LOADER.getMimeTypes());
        } else {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(null));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(null));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);

            binaryParams.setVisible(false);
            mimeType.setModelObject(null);
            mimeType.setChoices(null);
        }
    }
}
