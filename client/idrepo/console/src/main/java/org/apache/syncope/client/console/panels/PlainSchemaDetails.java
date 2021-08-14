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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.commons.PropertyList;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;

public class PlainSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 5378100729213456451L;

    @SpringBean
    private MIMETypesLoader mimeTypesLoader;

    private final MultiFieldPanel<String> enumerationValues;

    private final MultiFieldPanel<String> enumerationKeys;

    private final AjaxDropDownChoicePanel<String> validator;

    private final AjaxDropDownChoicePanel<AttrSchemaType> type;

    public PlainSchemaDetails(final String id, final PlainSchemaTO schemaTO) {
        super(id, schemaTO);

        type = new AjaxDropDownChoicePanel<>("type", getString("type"), new PropertyModel<>(schemaTO, "type"));

        boolean isCreate = schemaTO == null || schemaTO.getKey() == null || schemaTO.getKey().isEmpty();

        type.setChoices(List.of(AttrSchemaType.values()));
        type.setEnabled(isCreate);
        type.addRequiredLabel();
        add(type);

        // long, double, date
        AjaxTextFieldPanel conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<>(schemaTO, "conversionPattern"));
        add(conversionPattern);

        WebMarkupContainer conversionParams = new WebMarkupContainer("conversionParams");
        conversionParams.setOutputMarkupPlaceholderTag(true);
        conversionParams.add(conversionPattern);
        add(conversionParams);

        WebMarkupContainer typeParams = new WebMarkupContainer("typeParams");
        typeParams.setOutputMarkupPlaceholderTag(true);

        // enum
        AjaxTextFieldPanel enumerationValuesPanel = new AjaxTextFieldPanel("panel",
                "enumerationValues", new Model<>(null));

        enumerationValues = new MultiFieldPanel.Builder<>(
                new PropertyModel<List<String>>(schemaTO, "enumerationValues") {

            private static final long serialVersionUID = -4953564762272833993L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<>() {

                    @Override
                    public String getValues() {
                        return schemaTO.getEnumerationValues();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        schemaTO.setEnumerationValues(getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                schemaTO.setEnumerationValues(PropertyList.getEnumValuesAsString(object));
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
                        return schemaTO.getEnumerationKeys();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        schemaTO.setEnumerationKeys(PropertyList.getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                schemaTO.setEnumerationKeys(PropertyList.getEnumValuesAsString(object));
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
                new AjaxTextFieldPanel("panel", "enumerationKeys", new Model<>(null)));

        WebMarkupContainer enumParams = new WebMarkupContainer("enumParams");
        enumParams.setOutputMarkupPlaceholderTag(true);
        enumParams.add(enumerationValues);
        enumParams.add(enumerationKeys);
        typeParams.add(enumParams);

        // encrypted
        AjaxTextFieldPanel secretKey = new AjaxTextFieldPanel("secretKey",
                getString("secretKey"), new PropertyModel<>(schemaTO, "secretKey"));

        AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm = new AjaxDropDownChoicePanel<>(
                "cipherAlgorithm", getString("cipherAlgorithm"),
                new PropertyModel<>(schemaTO, "cipherAlgorithm"));
        cipherAlgorithm.setChoices(List.of(CipherAlgorithm.values()));

        AjaxCheckBoxPanel transparentEncryption = new AjaxCheckBoxPanel(
                "transparentEncryption", "transparentEncryption", new Model<Boolean>() {

            private static final long serialVersionUID = 5636572627689425575L;

            @Override
            public Boolean getObject() {
                return SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN.equals(schemaTO.getConversionPattern());
            }

            @Override
            public void setObject(final Boolean object) {
                schemaTO.setConversionPattern(BooleanUtils.isTrue(object)
                        ? SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN
                        : null);
            }
        }, true);

        WebMarkupContainer encryptedParams = new WebMarkupContainer("encryptedParams");
        encryptedParams.setOutputMarkupPlaceholderTag(true);
        encryptedParams.add(secretKey);
        encryptedParams.add(cipherAlgorithm);
        encryptedParams.add(transparentEncryption);

        typeParams.add(encryptedParams);

        // binary
        AjaxTextFieldPanel mimeType = new AjaxTextFieldPanel("mimeType",
                getString("mimeType"), new PropertyModel<>(schemaTO, "mimeType"));

        WebMarkupContainer binaryParams = new WebMarkupContainer("binaryParams");
        binaryParams.setOutputMarkupPlaceholderTag(true);
        binaryParams.add(mimeType);
        typeParams.add(binaryParams);
        add(typeParams);

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
                target.add(validator);
            }
        });

        IModel<List<String>> validators = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return ImplementationRestClient.list(IdRepoImplementationType.VALIDATOR).stream().
                        map(EntityTO::getKey).sorted().collect(Collectors.toList());
            }
        };
        validator = new AjaxDropDownChoicePanel<>("validator",
                getString("validator"), new PropertyModel<>(schemaTO, "validator"));
        validator.setOutputMarkupId(true);
        ((DropDownChoice) validator.getField()).setNullValid(true);
        validator.setChoices(validators.getObject());
        add(validator);

        AutoCompleteTextField<String> mandatoryCondition = new AutoCompleteTextField<String>(
                "mandatoryCondition", new PropertyModel<>(schemaTO, "mandatoryCondition")) {

            private static final long serialVersionUID = -2428903969518079100L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                List<String> choices = new ArrayList<>();

                if (Strings.isEmpty(input)) {
                    choices = List.of();
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
        add(mandatoryCondition);

        add(Constants.getJEXLPopover(this, TooltipConfig.Placement.right));

        add(new AjaxCheckBoxPanel(
                "multivalue", getString("multivalue"), new PropertyModel<>(schemaTO, "multivalue")));

        add(new AjaxCheckBoxPanel(
                "readonly", getString("readonly"), new PropertyModel<>(schemaTO, "readonly")));

        add(new AjaxCheckBoxPanel("uniqueConstraint",
                getString("uniqueConstraint"), new PropertyModel<>(schemaTO, "uniqueConstraint")).
                setEnabled(isCreate));
    }

    private void showHide(final PlainSchemaTO schema, final AjaxDropDownChoicePanel<AttrSchemaType> type,
            final WebMarkupContainer conversionParams, final AjaxTextFieldPanel conversionPattern,
            final WebMarkupContainer enumParams, final AjaxTextFieldPanel enumerationValuesPanel,
            final MultiFieldPanel<String> enumerationValues, final MultiFieldPanel<String> enumerationKeys,
            final WebMarkupContainer encryptedParams,
            final AjaxTextFieldPanel secretKey, final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm,
            final WebMarkupContainer binaryParams, final AjaxTextFieldPanel mimeType) {

        int typeOrdinal = -1;
        try {
            typeOrdinal = Integer.parseInt(type.getField().getValue());
        } catch (NumberFormatException e) {
            LOG.error("Invalid value found: {}", type.getField().getValue(), e);
        }
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
            enumerationValues.setModelObject(PropertyList.getEnumValuesAsList(schema.getEnumerationValues()));
            enumerationKeys.setModelObject(PropertyList.getEnumValuesAsList(schema.getEnumerationKeys()));

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
            mimeType.setChoices(mimeTypesLoader.getMimeTypes());

            schema.setValidator("BinaryValidator");
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

        if (type.isEnabled() && AttrSchemaType.Binary.ordinal() != typeOrdinal) {
            schema.setValidator(null);
        }
    }
}
