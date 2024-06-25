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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxGridFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ImplementationTO;
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
    protected MIMETypesLoader mimeTypesLoader;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    protected final IModel<List<String>> validators = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdRepoImplementationType.ATTR_VALUE_VALIDATOR).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final IModel<List<String>> dropdownValueProviders = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdRepoImplementationType.DROPDOWN_VALUE_PROVIDER).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final PlainSchemaTO schemaTO;

    protected final AjaxDropDownChoicePanel<String> validator;

    protected final AjaxDropDownChoicePanel<AttrSchemaType> type;

    protected final AjaxTextFieldPanel conversionPattern;

    protected final WebMarkupContainer conversionParams;

    protected final WebMarkupContainer enumParams;

    protected final AjaxGridFieldPanel<String, String> enumValues;

    protected final AjaxDropDownChoicePanel<String> dropdownValueProvider;

    protected final WebMarkupContainer dropdownParams;

    protected final AjaxTextFieldPanel secretKey;

    protected final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm;

    protected final AjaxCheckBoxPanel transparentEncryption;

    protected final WebMarkupContainer encryptedParams;

    protected final AjaxTextFieldPanel mimeType;

    protected final WebMarkupContainer binaryParams;

    public PlainSchemaDetails(final String id, final PlainSchemaTO schemaTO) {
        super(id, schemaTO);
        this.schemaTO = schemaTO;

        type = new AjaxDropDownChoicePanel<>("type", getString("type"), new PropertyModel<>(schemaTO, "type"));

        boolean isCreate = schemaTO == null || schemaTO.getKey() == null || schemaTO.getKey().isEmpty();

        type.setChoices(List.of(AttrSchemaType.values()));
        type.setEnabled(isCreate);
        type.addRequiredLabel();
        add(type);

        // long, double, date
        conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<>(schemaTO, "conversionPattern"));
        add(conversionPattern);

        conversionParams = new WebMarkupContainer("conversionParams");
        conversionParams.setOutputMarkupPlaceholderTag(true);
        conversionParams.add(conversionPattern);
        add(conversionParams);

        WebMarkupContainer typeParams = new WebMarkupContainer("typeParams");
        typeParams.setOutputMarkupPlaceholderTag(true);

        // enum
        enumValues = new AjaxGridFieldPanel<>(
                "enumValues", "enumValues", new PropertyModel<>(schemaTO, "enumValues"));

        enumParams = new WebMarkupContainer("enumParams");
        enumParams.setOutputMarkupPlaceholderTag(true);
        enumParams.add(enumValues);
        typeParams.add(enumParams);

        // dropdown
        dropdownValueProvider = new AjaxDropDownChoicePanel<>("dropdownValueProvider",
                getString("dropdownValueProvider"), new PropertyModel<>(schemaTO, "dropdownValueProvider"));
        dropdownValueProvider.setOutputMarkupId(true);
        ((DropDownChoice) dropdownValueProvider.getField()).setNullValid(true);
        dropdownValueProvider.setChoices(dropdownValueProviders.getObject());
        dropdownValueProvider.setRequired(true);

        dropdownParams = new WebMarkupContainer("dropdownParams");
        dropdownParams.setOutputMarkupPlaceholderTag(true);
        dropdownParams.add(dropdownValueProvider);
        typeParams.add(dropdownParams);

        // encrypted
        secretKey = new AjaxTextFieldPanel("secretKey",
                getString("secretKey"), new PropertyModel<>(schemaTO, "secretKey"));

        cipherAlgorithm = new AjaxDropDownChoicePanel<>(
                "cipherAlgorithm", getString("cipherAlgorithm"),
                new PropertyModel<>(schemaTO, "cipherAlgorithm"));
        cipherAlgorithm.setChoices(List.of(CipherAlgorithm.values()));

        transparentEncryption = new AjaxCheckBoxPanel(
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

        encryptedParams = new WebMarkupContainer("encryptedParams");
        encryptedParams.setOutputMarkupPlaceholderTag(true);
        encryptedParams.add(secretKey);
        encryptedParams.add(cipherAlgorithm);
        encryptedParams.add(transparentEncryption);

        typeParams.add(encryptedParams);

        // binary
        mimeType = new AjaxTextFieldPanel("mimeType",
                getString("mimeType"), new PropertyModel<>(schemaTO, "mimeType"));

        binaryParams = new WebMarkupContainer("binaryParams");
        binaryParams.setOutputMarkupPlaceholderTag(true);
        binaryParams.add(mimeType);
        typeParams.add(binaryParams);
        add(typeParams);

        // show or hide
        showHide();

        type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                PlainSchemaDetails.this.showHide();
                target.add(conversionParams);
                target.add(typeParams);
                target.add(dropdownValueProvider);
                target.add(validator);
            }
        });

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

    private void showHide() {
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
            enumValues.setModelObject(new HashMap<>());

            dropdownParams.setVisible(false);
            dropdownValueProvider.setModelObject(null);
            if (dropdownValueProvider.isRequired()) {
                dropdownValueProvider.removeRequiredLabel();
            }

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
            enumValues.setModelObject(schemaTO.getEnumValues());

            dropdownParams.setVisible(false);
            dropdownValueProvider.setModelObject(null);
            if (dropdownValueProvider.isRequired()) {
                dropdownValueProvider.removeRequiredLabel();
            }

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
        } else if (AttrSchemaType.Dropdown.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            enumValues.setModelObject(schemaTO.getEnumValues());

            dropdownParams.setVisible(true);
            if (!dropdownValueProvider.isRequired()) {
                dropdownValueProvider.addRequiredLabel();
            }

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
            enumValues.setModelObject(new HashMap<>());

            dropdownParams.setVisible(false);
            dropdownValueProvider.setModelObject(null);
            if (dropdownValueProvider.isRequired()) {
                dropdownValueProvider.removeRequiredLabel();
            }

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
            enumValues.setModelObject(new HashMap<>());

            dropdownParams.setVisible(false);
            dropdownValueProvider.setModelObject(null);
            if (dropdownValueProvider.isRequired()) {
                dropdownValueProvider.removeRequiredLabel();
            }

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

            schemaTO.setValidator("BinaryValidator");
        } else {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            enumValues.setModelObject(new HashMap<>());

            dropdownParams.setVisible(false);
            dropdownValueProvider.setModelObject(null);
            if (dropdownValueProvider.isRequired()) {
                dropdownValueProvider.removeRequiredLabel();
            }

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
            schemaTO.setValidator(null);
        }
    }
}
