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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.client.AbstractBaseBean;
import org.apache.syncope.client.to.SchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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
public class SchemaModalPage extends AbstractSchemaModalPage {

    private static final long serialVersionUID = -5991561277287424057L;

    public SchemaModalPage(AttributableType kind) {
        super(kind);
    }

    @Override
    public void setSchemaModalPage(final PageReference pageRef, final ModalWindow window,
            AbstractBaseBean schemaTO, final boolean createFlag) {
        final SchemaTO schema;
        if (schemaTO != null && schemaTO instanceof SchemaTO) {
            schema = (SchemaTO) schemaTO;
        } else {
            schema = new SchemaTO();
        }

        final Form schemaForm = new Form("form");

        schemaForm.setModel(new CompoundPropertyModel(schema));
        schemaForm.setOutputMarkupId(Boolean.TRUE);

        final AjaxTextFieldPanel name =
                new AjaxTextFieldPanel("name", getString("name"), new PropertyModel<String>(schema, "name"));

        name.addRequiredLabel();
        name.setEnabled(createFlag);

        final AjaxTextFieldPanel conversionPattern = new AjaxTextFieldPanel("conversionPattern",
                getString("conversionPattern"), new PropertyModel<String>(schema, "conversionPattern"));

        final IModel<List<String>> validatorsList = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return restClient.getAllValidatorClasses();
            }
        };

        final AjaxDropDownChoicePanel<String> validatorClass = new AjaxDropDownChoicePanel<String>("validatorClass",
                getString("validatorClass"), new PropertyModel(schema, "validatorClass"));

        ((DropDownChoice) validatorClass.getField()).setNullValid(true);
        validatorClass.setChoices(validatorsList.getObject());

        final AjaxDropDownChoicePanel<SchemaType> type = new AjaxDropDownChoicePanel<SchemaType>("type",
                getString("type"), new PropertyModel(schema, "type"));
        type.setChoices(Arrays.asList(SchemaType.values()));
        type.addRequiredLabel();

        final MultiValueSelectorPanel<String> enumerationValues =
                new MultiValueSelectorPanel<String>("enumerationValues",
                new Model(),
                new AjaxTextFieldPanel("panel", "enumerationValues", new Model(null)).addRequiredLabel());
        schemaForm.add(enumerationValues);

        enumerationValues.setModelObject((Serializable) getEnumValuesAsList(schema.getEnumerationValues()));

        final MultiValueSelectorPanel<String> enumerationKeys =
                new MultiValueSelectorPanel<String>("enumerationKeys",
                new Model(),
                new AjaxTextFieldPanel("panel", "enumerationKeys", new Model(null)));
        schemaForm.add(enumerationKeys);

        enumerationKeys.setModelObject((Serializable) getEnumValuesAsList(schema.getEnumerationKeys()));

        if (schema != null && SchemaType.Enum.equals(((SchemaTO) schema).getType())) {
            enumerationValues.setEnabled(Boolean.TRUE);
            enumerationKeys.setEnabled(Boolean.TRUE);
        } else {
            enumerationValues.setEnabled(Boolean.FALSE);
            enumerationKeys.setEnabled(Boolean.FALSE);
        }

        type.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (SchemaType.Enum.ordinal() == Integer.parseInt(type.getField().getValue())) {
                    enumerationValues.setEnabled(Boolean.TRUE);
                    enumerationValues.setModelObject((Serializable) getEnumValuesAsList(schema.getEnumerationValues()));

                    enumerationKeys.setEnabled(Boolean.TRUE);
                    enumerationKeys.setModelObject((Serializable) getEnumValuesAsList(schema.getEnumerationKeys()));
                } else {
                    final List<String> values = new ArrayList<String>();
                    values.add("");

                    enumerationValues.setEnabled(Boolean.FALSE);
                    enumerationValues.setModelObject((Serializable) values);

                    final List<String> keys = new ArrayList<String>();
                    keys.add("");

                    enumerationKeys.setEnabled(Boolean.FALSE);
                    enumerationKeys.setModelObject((Serializable) keys);
                }

                target.add(schemaForm);
            }
        });

        final AutoCompleteTextField mandatoryCondition = new AutoCompleteTextField("mandatoryCondition") {

            private static final long serialVersionUID = -2428903969518079100L;

            @Override
            protected Iterator<String> getChoices(String input) {
                List<String> choices = new ArrayList<String>();

                if (Strings.isEmpty(input)) {
                    choices = Collections.emptyList();
                    return choices.iterator();
                }

                if ("true".startsWith(input.toLowerCase())) {
                    choices.add("true");
                } else if ("false".startsWith(input.toLowerCase())) {
                    choices.add("false");
                }

                return choices.iterator();
            }
        };

        mandatoryCondition.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
            }
        });

        final AjaxCheckBoxPanel multivalue = new AjaxCheckBoxPanel("multivalue", getString("multivalue"),
                new PropertyModel<Boolean>(schema, "multivalue"));

        final AjaxCheckBoxPanel readonly = new AjaxCheckBoxPanel("readonly", getString("readonly"),
                new PropertyModel<Boolean>(schema, "readonly"));

        final AjaxCheckBoxPanel uniqueConstraint = new AjaxCheckBoxPanel("uniqueConstraint",
                getString("uniqueConstraint"), new PropertyModel<Boolean>(schema, "uniqueConstraint"));

        final AjaxButton submit = new ClearIndicatingAjaxButton("apply", new ResourceModel("submit"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                final SchemaTO schemaTO = (SchemaTO) form.getDefaultModelObject();

                schemaTO.setEnumerationValues(getEnumValuesAsString(enumerationValues.getView().getModelObject()));
                schemaTO.setEnumerationKeys(getEnumValuesAsString(enumerationKeys.getView().getModelObject()));

                if (schemaTO.isMultivalue() && schemaTO.isUniqueConstraint()) {
                    error(getString("multivalueAndUniqueConstr.validation"));
                    target.add(feedbackPanel);
                    return;
                }

                try {
                    if (createFlag) {
                        restClient.createSchema(kind, schemaTO);
                    } else {
                        restClient.updateSchema(kind, schemaTO);
                    }
                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema", "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema", "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        schemaForm.add(name);
        schemaForm.add(conversionPattern);
        schemaForm.add(validatorClass);
        schemaForm.add(type);
        schemaForm.add(mandatoryCondition);
        schemaForm.add(multivalue);
        schemaForm.add(readonly);
        schemaForm.add(uniqueConstraint);

        schemaForm.add(submit);

        add(schemaForm);
    }

    private String getEnumValuesAsString(final List<String> enumerationValues) {
        final StringBuilder builder = new StringBuilder();

        for (String str : enumerationValues) {
            if (StringUtils.isNotBlank(str)) {
                if (builder.length() > 0) {
                    builder.append(Schema.enumValuesSeparator);
                }

                builder.append(str.trim());
            }
        }

        return builder.toString();
    }

    private List<String> getEnumValuesAsList(final String enumerationValues) {
        final List<String> values = new ArrayList<String>();

        if (StringUtils.isNotBlank(enumerationValues)) {
            for (String value : enumerationValues.split(Schema.enumValuesSeparator)) {
                values.add(value.trim());
            }
        } else {
            values.add("");
        }

        return values;
    }
}
