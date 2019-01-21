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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.PropertyList;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ParametersCreateWizardSchemaStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    @SpringBean
    private MIMETypesLoader mimeTypesLoader;

    private final ImplementationRestClient implRestClient = new ImplementationRestClient();

    private final MultiFieldPanel<String> enumerationValues;

    private final MultiFieldPanel<String> enumerationKeys;

    private final AjaxDropDownChoicePanel<String> validator;

    public ParametersCreateWizardSchemaStep(final ParametersCreateWizardPanel.ParametersForm modelObject) {
        modelObject.getPlainSchemaTO().setMandatoryCondition("false");

        final WebMarkupContainer content = new WebMarkupContainer("content");
        this.setOutputMarkupId(true);
        content.setOutputMarkupId(true);
        add(content);

        final AjaxDropDownChoicePanel<AttrSchemaType> type = new AjaxDropDownChoicePanel<>(
                "type", getString("type"), new PropertyModel<>(modelObject.getPlainSchemaTO(), "type"));
        type.setChoices(Arrays.asList(AttrSchemaType.values()));
        content.add(type);

        // long, double, date
        final AjaxTextFieldPanel conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<String>(
                modelObject.getPlainSchemaTO(), "conversionPattern"));
        content.add(conversionPattern);

        final WebMarkupContainer conversionParams = new WebMarkupContainer("conversionParams");
        conversionParams.setOutputMarkupPlaceholderTag(true);
        conversionParams.add(conversionPattern);
        content.add(conversionParams);

        final WebMarkupContainer typeParams = new WebMarkupContainer("typeParams");
        typeParams.setOutputMarkupPlaceholderTag(true);

        // enum
        final AjaxTextFieldPanel enumerationValuesPanel = new AjaxTextFieldPanel("panel", "enumerationValues",
                new Model<>(null));

        enumerationValues = new MultiFieldPanel.Builder<String>(
                new PropertyModel<List<String>>(modelObject.getPlainSchemaTO(), "enumerationValues") {

            private static final long serialVersionUID = -4953564762272833993L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<PlainSchemaTO>() {

                    @Override
                    public String getValues() {
                        return modelObject.getPlainSchemaTO().getEnumerationValues();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        modelObject.getPlainSchemaTO().setEnumerationValues(getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                modelObject.getPlainSchemaTO().setEnumerationValues(PropertyList.getEnumValuesAsString(object));
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
                new PropertyModel<List<String>>(modelObject.getPlainSchemaTO(), "enumerationKeys") {

            private static final long serialVersionUID = -4953564762272833993L;

            @Override
            public PropertyList<PlainSchemaTO> getObject() {
                return new PropertyList<PlainSchemaTO>() {

                    @Override
                    public String getValues() {
                        return modelObject.getPlainSchemaTO().getEnumerationKeys();
                    }

                    @Override
                    public void setValues(final List<String> list) {
                        modelObject.getPlainSchemaTO().setEnumerationKeys(PropertyList.getEnumValuesAsString(list));
                    }
                };
            }

            @Override
            public void setObject(final List<String> object) {
                modelObject.getPlainSchemaTO().setEnumerationKeys(PropertyList.getEnumValuesAsString(object));
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
                getString("secretKey"), new PropertyModel<>(modelObject.getPlainSchemaTO(), "secretKey"));

        final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm = new AjaxDropDownChoicePanel<>(
                "cipherAlgorithm", getString("cipherAlgorithm"),
                new PropertyModel<>(modelObject.getPlainSchemaTO(), "cipherAlgorithm"));

        cipherAlgorithm.setChoices(Arrays.asList(CipherAlgorithm.values()));

        final WebMarkupContainer encryptedParams = new WebMarkupContainer("encryptedParams");
        encryptedParams.setOutputMarkupPlaceholderTag(true);
        encryptedParams.add(secretKey);
        encryptedParams.add(cipherAlgorithm);

        typeParams.add(encryptedParams);

        // binary
        final AjaxTextFieldPanel mimeType = new AjaxTextFieldPanel("mimeType",
                getString("mimeType"), new PropertyModel<>(modelObject.getPlainSchemaTO(), "mimeType"));

        final WebMarkupContainer binaryParams = new WebMarkupContainer("binaryParams");
        binaryParams.setOutputMarkupPlaceholderTag(true);
        binaryParams.add(mimeType);
        typeParams.add(binaryParams);
        content.add(typeParams);

        // show or hide
        showHide(modelObject.getPlainSchemaTO(), type,
                conversionParams, conversionPattern,
                enumParams, enumerationValuesPanel, enumerationValues, enumerationKeys,
                encryptedParams, secretKey, cipherAlgorithm,
                binaryParams, mimeType);

        type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ParametersCreateWizardSchemaStep.this.showHide(modelObject.getPlainSchemaTO(), type,
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
                return implRestClient.list(ImplementationType.VALIDATOR).stream().
                        map(EntityTO::getKey).sorted().collect(Collectors.toList());
            }
        };
        validator = new AjaxDropDownChoicePanel<>("validator",
                getString("validator"), new PropertyModel<>(modelObject.getPlainSchemaTO(), "validator"));
        validator.setOutputMarkupId(true);
        ((DropDownChoice) validator.getField()).setNullValid(true);
        validator.setChoices(validators.getObject());
        content.add(validator);

        content.add(new AjaxCheckBoxPanel("multivalue", getString("multivalue"),
                new PropertyModel<>(modelObject.getPlainSchemaTO(), "multivalue")));
    }

    private void showHide(final PlainSchemaTO schema, final AjaxDropDownChoicePanel<AttrSchemaType> type,
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

            schema.setValidator(null);
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

            schema.setValidator(null);
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
            schema.setValidator(null);
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
            schema.setValidator(null);
        }
    }
}
