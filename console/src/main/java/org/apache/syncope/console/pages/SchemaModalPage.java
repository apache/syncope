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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.JexlHelpUtil;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.string.Strings;

/**
 * Modal window with Schema form.
 */
public class SchemaModalPage extends AbstractSchemaModalPage<SchemaTO> {

    private static final long serialVersionUID = -5991561277287424057L;

    public SchemaModalPage(final AttributableType kind) {
        super(kind);
    }

    @Override
    public void setSchemaModalPage(final PageReference pageRef, final ModalWindow window,
            final SchemaTO schemaTO, final boolean createFlag) {

        final SchemaTO schema = schemaTO == null
                ? new SchemaTO()
                : schemaTO;

        final Form<SchemaTO> schemaForm = new Form<SchemaTO>(FORM);

        schemaForm.setModel(new CompoundPropertyModel<SchemaTO>(schema));
        schemaForm.setOutputMarkupId(true);

        final AjaxTextFieldPanel name =
                new AjaxTextFieldPanel("name", getString("name"), new PropertyModel<String>(schema, "name"));
        name.addRequiredLabel();
        name.setEnabled(createFlag);
        schemaForm.add(name);

        final AjaxDropDownChoicePanel<AttributeSchemaType> type = new AjaxDropDownChoicePanel<AttributeSchemaType>(
                "type", getString("type"), new PropertyModel<AttributeSchemaType>(schema, "type"));
        type.setChoices(Arrays.asList(AttributeSchemaType.values()));
        type.addRequiredLabel();
        schemaForm.add(type);

        // -- long, double, date
        final AjaxTextFieldPanel conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<String>(schema, "conversionPattern"));
        schemaForm.add(conversionPattern);

        final WebMarkupContainer conversionParams = new WebMarkupContainer("conversionParams");
        conversionParams.setOutputMarkupPlaceholderTag(true);
        conversionParams.add(conversionPattern);
        schemaForm.add(conversionParams);

        // -- enum
        final AjaxTextFieldPanel enumerationValuesPanel =
                new AjaxTextFieldPanel("panel", "enumerationValues", new Model<String>(null));
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final MultiFieldPanel<String> enumerationValues = new MultiFieldPanel<String>("enumerationValues",
                new Model(),
                enumerationValuesPanel);
        enumerationValues.setModelObject(getEnumValuesAsList(schema.getEnumerationValues()));

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final MultiFieldPanel<String> enumerationKeys = new MultiFieldPanel<String>("enumerationKeys",
                new Model(),
                new AjaxTextFieldPanel("panel", "enumerationKeys", new Model<String>(null)));
        enumerationKeys.setModelObject(getEnumValuesAsList(schema.getEnumerationKeys()));

        final WebMarkupContainer enumParams = new WebMarkupContainer("enumParams");
        enumParams.setOutputMarkupPlaceholderTag(true);
        enumParams.add(enumerationValues);
        enumParams.add(enumerationKeys);
        schemaForm.add(enumParams);

        // -- encrypted
        final AjaxTextFieldPanel secretKey = new AjaxTextFieldPanel("secretKey",
                getString("secretKey"), new PropertyModel<String>(schema, "secretKey"));

        final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm = new AjaxDropDownChoicePanel<CipherAlgorithm>(
                "cipherAlgorithm", getString("cipherAlgorithm"),
                new PropertyModel<CipherAlgorithm>(schema, "cipherAlgorithm"));
        cipherAlgorithm.setChoices(Arrays.asList(CipherAlgorithm.values()));

        final WebMarkupContainer encryptedParams = new WebMarkupContainer("encryptedParams");
        encryptedParams.setOutputMarkupPlaceholderTag(true);
        encryptedParams.add(secretKey);
        encryptedParams.add(cipherAlgorithm);
        schemaForm.add(encryptedParams);

        showHide(schema, type,
                conversionParams, conversionPattern,
                enumParams, enumerationValuesPanel, enumerationValues, enumerationKeys,
                encryptedParams, secretKey, cipherAlgorithm);
        type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                SchemaModalPage.this.showHide(schema, type,
                        conversionParams, conversionPattern,
                        enumParams, enumerationValuesPanel, enumerationValues, enumerationKeys,
                        encryptedParams, secretKey, cipherAlgorithm);
                target.add(schemaForm);
            }
        });

        final IModel<List<String>> validatorsList = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return schemaRestClient.getAllValidatorClasses();
            }
        };
        final AjaxDropDownChoicePanel<String> validatorClass = new AjaxDropDownChoicePanel<String>("validatorClass",
                getString("validatorClass"), new PropertyModel<String>(schema, "validatorClass"));
        ((DropDownChoice) validatorClass.getField()).setNullValid(true);
        validatorClass.setChoices(validatorsList.getObject());
        schemaForm.add(validatorClass);

        final AutoCompleteTextField<String> mandatoryCondition =
                new AutoCompleteTextField<String>("mandatoryCondition") {

                    private static final long serialVersionUID = -2428903969518079100L;

                    @Override
                    protected Iterator<String> getChoices(final String input) {
                        List<String> choices = new ArrayList<String>();

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
        mandatoryCondition.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });
        schemaForm.add(mandatoryCondition);

        final WebMarkupContainer pwdJexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");

        final AjaxLink<Void> pwdQuestionMarkJexlHelp = JexlHelpUtil.getAjaxLink(pwdJexlHelp, "questionMarkJexlHelp");
        schemaForm.add(pwdQuestionMarkJexlHelp);
        pwdQuestionMarkJexlHelp.add(pwdJexlHelp);

        final AjaxCheckBoxPanel multivalue = new AjaxCheckBoxPanel("multivalue", getString("multivalue"),
                new PropertyModel<Boolean>(schema, "multivalue"));
        schemaForm.add(multivalue);

        final AjaxCheckBoxPanel readonly = new AjaxCheckBoxPanel("readonly", getString("readonly"),
                new PropertyModel<Boolean>(schema, "readonly"));
        schemaForm.add(readonly);

        final AjaxCheckBoxPanel uniqueConstraint = new AjaxCheckBoxPanel("uniqueConstraint",
                getString("uniqueConstraint"), new PropertyModel<Boolean>(schema, "uniqueConstraint"));
        schemaForm.add(uniqueConstraint);

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final SchemaTO schemaTO = (SchemaTO) form.getDefaultModelObject();

                schemaTO.setEnumerationValues(getEnumValuesAsString(enumerationValues.getView().getModelObject()));
                schemaTO.setEnumerationKeys(getEnumValuesAsString(enumerationKeys.getView().getModelObject()));

                if (schemaTO.isMultivalue() && schemaTO.isUniqueConstraint()) {
                    error(getString("multivalueAndUniqueConstr.validation"));
                    feedbackPanel.refresh(target);
                    return;
                }

                try {
                    if (createFlag) {
                        schemaRestClient.createSchema(kind, schemaTO);
                    } else {
                        schemaRestClient.updateSchema(kind, schemaTO);
                    }
                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    window.close(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
        schemaForm.add(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };
        cancel.setDefaultFormProcessing(false);
        schemaForm.add(cancel);

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Schema", "create")
                : xmlRolesReader.getAllAllowedRoles("Schema", "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        add(schemaForm);
    }

    private void showHide(final SchemaTO schema, final AjaxDropDownChoicePanel<AttributeSchemaType> type,
            final WebMarkupContainer conversionParams, final AjaxTextFieldPanel conversionPattern,
            final WebMarkupContainer enumParams, final AjaxTextFieldPanel enumerationValuesPanel,
            final MultiFieldPanel<String> enumerationValues, final MultiFieldPanel<String> enumerationKeys,
            final WebMarkupContainer encryptedParams,
            final AjaxTextFieldPanel secretKey, final AjaxDropDownChoicePanel<CipherAlgorithm> cipherAlgorithm) {

        final int typeOrdinal = Integer.parseInt(type.getField().getValue());
        if (AttributeSchemaType.Long.ordinal() == typeOrdinal || AttributeSchemaType.Double.ordinal() == typeOrdinal
                || AttributeSchemaType.Date.ordinal() == typeOrdinal) {

            conversionParams.setVisible(true);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(getEnumValuesAsList(null));
            enumerationKeys.setModelObject(getEnumValuesAsList(null));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);
        } else if (AttributeSchemaType.Enum.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(true);
            if (!enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.addRequiredLabel();
            }
            enumerationValues.setModelObject(getEnumValuesAsList(schema.getEnumerationValues()));
            enumerationKeys.setModelObject(getEnumValuesAsList(schema.getEnumerationKeys()));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);
        } else if (AttributeSchemaType.Encrypted.ordinal() == typeOrdinal) {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(getEnumValuesAsList(null));
            enumerationKeys.setModelObject(getEnumValuesAsList(null));

            encryptedParams.setVisible(true);
            if (!secretKey.isRequired()) {
                secretKey.addRequiredLabel();
            }
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.addRequiredLabel();
            }
        } else {
            conversionParams.setVisible(false);
            conversionPattern.setModelObject(null);

            enumParams.setVisible(false);
            if (enumerationValuesPanel.isRequired()) {
                enumerationValuesPanel.removeRequiredLabel();
            }
            enumerationValues.setModelObject(getEnumValuesAsList(null));
            enumerationKeys.setModelObject(getEnumValuesAsList(null));

            encryptedParams.setVisible(false);
            if (secretKey.isRequired()) {
                secretKey.removeRequiredLabel();
            }
            secretKey.setModelObject(null);
            if (cipherAlgorithm.isRequired()) {
                cipherAlgorithm.removeRequiredLabel();
            }
            cipherAlgorithm.setModelObject(null);
        }
    }

    private String getEnumValuesAsString(final List<String> enumerationValues) {
        final StringBuilder builder = new StringBuilder();

        for (String str : enumerationValues) {
            if (StringUtils.isNotBlank(str)) {
                if (builder.length() > 0) {
                    builder.append(SyncopeConstants.ENUM_VALUES_SEPARATOR);
                }

                builder.append(str.trim());
            }
        }

        return builder.toString();
    }

    private List<String> getEnumValuesAsList(final String enumerationValues) {
        final List<String> values = new ArrayList<String>();

        if (StringUtils.isNotBlank(enumerationValues)) {
            for (String value : enumerationValues.split(SyncopeConstants.ENUM_VALUES_SEPARATOR)) {
                values.add(value.trim());
            }
        } else {
            values.add(StringUtils.EMPTY);
        }

        return values;
    }
}
