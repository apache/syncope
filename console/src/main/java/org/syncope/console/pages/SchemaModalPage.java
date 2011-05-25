/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.syncope.client.to.SchemaTO;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.form.CheckBox;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.SchemaType;

/**
 * Modal window with Schema form.
 */
public class SchemaModalPage extends AbstractSchemaModalPage {

    public SchemaModalPage(String kind) {
        super(kind);
    }

    @Override
    public void setSchemaModalPage(
            final BasePage basePage,
            final ModalWindow window,
            AbstractBaseBean schemaTO,
            final boolean createFlag) {

        final SchemaTO schema =
                schemaTO == null ? new SchemaTO() : (SchemaTO) schemaTO;

        final Form schemaForm = new Form("SchemaForm");

        schemaForm.setModel(new CompoundPropertyModel(schema));
        schemaForm.setOutputMarkupId(Boolean.TRUE);


        final TextField name = new TextField("name");
        addEmptyBehaviour(name, "onblur");
        name.setRequired(true);

        name.setEnabled(createFlag);

        final TextField conversionPattern = new TextField("conversionPattern");
        addEmptyBehaviour(conversionPattern, "onblur");

        final ArrayList<String> validatorsList = new ArrayList<String>();
        validatorsList.add("org.syncope.core.persistence.validation"
                + ".AlwaysTrueValidator");
        validatorsList.add("org.syncope.core.persistence.validation"
                + ".EmailAddressValidator");

        final DropDownChoice validatorClass = new DropDownChoice(
                "validatorClass",
                new PropertyModel(schema, "validatorClass"),
                validatorsList);

        addEmptyBehaviour(validatorClass, "onblur");

        final DropDownChoice type = new DropDownChoice(
                "type",
                Arrays.asList(SchemaType.Enum.values()));
        type.setRequired(true);

        final TextField enumerationValues = new TextField("enumerationValues");

        if (schema != null
                && SchemaType.Enum.equals(((SchemaTO) schema).getType())) {
            enumerationValues.setRequired(Boolean.TRUE);
            enumerationValues.setEnabled(Boolean.TRUE);
        } else {
            enumerationValues.setRequired(Boolean.FALSE);
            enumerationValues.setEnabled(Boolean.FALSE);
        }

        type.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (SchemaType.Enum.ordinal()
                        == Integer.parseInt(type.getValue())) {
                    enumerationValues.setRequired(Boolean.TRUE);
                    enumerationValues.setEnabled(Boolean.TRUE);
                    enumerationValues.getModel().setObject(
                            ((SchemaTO) schema).getEnumerationValues());
                } else {
                    enumerationValues.setRequired(Boolean.FALSE);
                    enumerationValues.setEnabled(Boolean.FALSE);
                    enumerationValues.getModel().setObject(null);
                }

                target.addComponent(schemaForm);
            }
        });

        final AutoCompleteTextField mandatoryCondition =
                new AutoCompleteTextField("mandatoryCondition") {

                    @Override
                    protected Iterator getChoices(String input) {
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

        addEmptyBehaviour(mandatoryCondition, "onblur");

        final CheckBox multivalue = new CheckBox("multivalue");
        addEmptyBehaviour(multivalue, "onchange");

        final CheckBox readonly = new CheckBox("readonly");
        addEmptyBehaviour(readonly, "onchange");

        final CheckBox uniqueConstraint = new CheckBox("uniqueConstraint");
        addEmptyBehaviour(uniqueConstraint, "onchange");

        final AjaxButton submit = new IndicatingAjaxButton(
                "submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                SchemaTO schemaTO = (SchemaTO) form.getDefaultModelObject();

                if (schemaTO.isMultivalue() && schemaTO.isUniqueConstraint()) {
                    error(getString("multivalueAndUniqueConstr.validation"));
                    return;
                }

                LOG.error("aaaaaaaaaaa {}", schemaTO);

                if (createFlag) {
                    restClient.createSchema(kind, schemaTO);
                } else {
                    restClient.updateSchema(kind, schemaTO);
                }

                Schema callerPage = (Schema) basePage;
                callerPage.setOperationResult(true);

                window.close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, allowedRoles);

        schemaForm.add(name);
        schemaForm.add(conversionPattern);
        schemaForm.add(validatorClass);
        schemaForm.add(type);
        schemaForm.add(enumerationValues);
        schemaForm.add(mandatoryCondition);
        schemaForm.add(multivalue);
        schemaForm.add(readonly);
        schemaForm.add(uniqueConstraint);

        schemaForm.add(submit);

        add(schemaForm);
    }

    private void addEmptyBehaviour(Component component, String event) {
        component.add(new AjaxFormComponentUpdatingBehavior(event) {

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
            }
        });
    }
}
