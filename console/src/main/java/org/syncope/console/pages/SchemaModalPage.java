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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.Strings;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.types.SchemaValueType;

import org.apache.wicket.authorization.strategies.role.metadata
        .MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
/**
 * Modal window with Schema form.
 */
public class SchemaModalPage extends SyncopeModalPage
{
    public TextField name;
    public TextField conversionPattern;
    public DropDownChoice validatorClass;
    public DropDownChoice type;
    public DropDownChoice action;
    public AutoCompleteTextField mandatoryCondition;
    public RadioChoice virtual;
    public RadioChoice multivalue;
    public RadioChoice readonly;
    public AjaxButton submit;

    public enum Entity {USER,ROLE,MEMBERSHIP};

    public Entity entity;

    @SpringBean(name = "schemaRestClient")
    SchemaRestClient restClient;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param schemaTO
     * @param create : set to true only if a CREATE operation is required
     */
    public SchemaModalPage(final BasePage basePage, final ModalWindow window,
            SchemaTO schema, final boolean createFlag)
    {

        if (schema == null)
            schema=new SchemaTO();

        Form schemaForm = new Form("SchemaForm");

        schemaForm.setModel(new CompoundPropertyModel(schema));

        name = new TextField("name");
        name.setRequired(true);

        name.setEnabled(createFlag);

        conversionPattern = new TextField("conversionPattern");

        ArrayList<String> validatorsList = new ArrayList<String>();
        validatorsList.add("org.syncope.core.persistence.validation" +
                ".AlwaysTrueValidator");
        validatorsList.add("org.syncope.core.persistence.validation" +
                ".EmailAddressValidator");

        validatorClass = new DropDownChoice("validatorClass",
                new PropertyModel(schema, "validatorClass")
                ,validatorsList);

        type = new DropDownChoice("type",Arrays.asList(SchemaValueType.values()));
        type.setRequired(true);

        mandatoryCondition = new AutoCompleteTextField("mandatoryCondition") {

            @Override
            protected Iterator getChoices(String input) {
                List<String> choices = new ArrayList<String>();

                if (Strings.isEmpty(input))
                {
                    choices = Collections.emptyList();
                    return choices.iterator();
                }

                if("true".startsWith(input.toLowerCase()))
                    choices.add("true");
                else if ("false".startsWith(input.toLowerCase()))
                       choices.add("false");


                return choices.iterator();
            }
        };

        virtual = new RadioChoice("virtual",Arrays.asList(new Boolean[]{true,false}));

        multivalue = new RadioChoice("multivalue",Arrays.asList(new Boolean[]{true,false}));

        readonly = new RadioChoice("readonly",Arrays.asList(new Boolean[]{true,false}));

        submit = new IndicatingAjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                if (getEntity() == Entity.USER)
                {

                    if (createFlag)
                        restClient.createUserSchema((SchemaTO)form.getDefaultModelObject());

                    else
                        restClient.updateUserSchema((SchemaTO)form.getDefaultModelObject());

                }

                else if (getEntity() == Entity.ROLE)
                {

                    if (createFlag)
                        restClient.createRoleSchema((SchemaTO)form.getDefaultModelObject());

                    else
                        restClient.updateRoleSchema((SchemaTO)form.getDefaultModelObject());

                }

                else if (getEntity() == Entity.MEMBERSHIP)
                {

                    if (createFlag)
                        restClient.createMemberhipSchema((SchemaTO)form.getDefaultModelObject());

                    else
                        restClient.updateMemberhipSchema((SchemaTO)form.getDefaultModelObject());

                }
                Schema callerPage = (Schema)basePage;
                callerPage.setOperationResult(true);
                
                window.close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get( "feedback" ));
            }
            
        };

        String allowedRoles;

        if(createFlag)
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "create");
        else
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Schema",
                    "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                        allowedRoles);

        schemaForm.add(new FeedbackPanel("feedback").setOutputMarkupId( true ));
        
        schemaForm.add(name);
        schemaForm.add(conversionPattern);
        schemaForm.add(validatorClass);
        schemaForm.add(type);
        schemaForm.add(mandatoryCondition);
        schemaForm.add(virtual);
        schemaForm.add(multivalue);
        schemaForm.add(readonly);

        schemaForm.add(submit);

        add(schemaForm);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

}