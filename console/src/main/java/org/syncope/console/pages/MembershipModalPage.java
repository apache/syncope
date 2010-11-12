/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.commons.SchemaWrapper;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;

/**
 * MembershipModalPage.
 */
public class MembershipModalPage extends SyncopeModalPage {
    List<SchemaWrapper> schemaWrappers = new ArrayList<SchemaWrapper>();
    WebMarkupContainer container;

    AjaxButton submit;

    public MembershipModalPage(final Page basePage, final ModalWindow window,
            final MembershipTO membershipTO, final boolean createFlag) {

        final Form form = new Form("MembershipForm");

        form.setModel(new CompoundPropertyModel(membershipTO));

        setupSchemaWrappers(createFlag,membershipTO);

        final ListView userAttributesView = new ListView("membershipSchemas",
                schemaWrappers) {

            @Override
            protected void populateItem(ListItem item) {
            final SchemaWrapper schemaWrapper = (SchemaWrapper) item
                    .getDefaultModelObject();

            final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

            item.add(new Label("name", schemaWrapper.getSchemaTO().getName()));

            item.add(new ListView("fields", schemaWrapper.getValues()) {

                Panel panel;

                @Override
                protected void populateItem(final ListItem item) {
                    String mandatoryCondition =
                            schemaTO.getMandatoryCondition();

                    boolean required = false;

                    if (mandatoryCondition.equalsIgnoreCase("true"))
                        required = true;

                    if (schemaTO.getType().getClassName()
                            .equals("java.lang.String")) {
                        panel = new AjaxTextFieldPanel("panel",
                                schemaTO.getName(), new Model() {

                            @Override
                            public Serializable getObject() {
                                return (String) item.getModelObject();
                            }

                            @Override
                            public void setObject(Serializable object) {
                                item.setModelObject((String) object);
                            }
                        }, required);
                    } else if (schemaTO.getType().getClassName()
                            .equals("java.lang.Boolean")) {
                        panel = new AjaxCheckBoxPanel("panel", schemaTO
                                .getName(), new Model() {

                            @Override
                            public Serializable getObject() {
                                //return (String) item.getModelObject();
                                return "false";
                            }

                            @Override
                            public void setObject(Serializable object) {
                                Boolean val = (Boolean) object;
                                item.setModelObject(val.toString());
                            }
                        }, required);

                    } else if (schemaTO.getType().getClassName()
                            .equals("java.util.Date")) {
                        panel = new DateFieldPanel("panel", schemaTO
                                .getName(),
                                new Model() {

                                    @Override
                                    public Serializable getObject() {
                                        DateFormat formatter =
                                                new SimpleDateFormat(schemaTO
                                                .getConversionPattern());
                                        Date date = new Date();
                                        try {
                                            String dateValue = (String) item
                                                    .getModelObject();
                                            formatter = new SimpleDateFormat(
                                                    schemaTO
                                                    .getConversionPattern());

                                            if(!dateValue.equals(""))
                                                date = formatter
                                                        .parse(dateValue);
                                            else
                                                date = null;
                                        } catch (ParseException ex) {
                                            Logger.getLogger(UserModalPage
                                                    .class.getName())
                                                    .log(Level.SEVERE, null, ex);
                                        }
                                        return date;
                                    }

                                    @Override
                                    public void setObject(Serializable object) {
                                        Date date = (Date) object;
                                        Format formatter = new SimpleDateFormat(
                                                schemaTO.getConversionPattern());
                                        String val = formatter.format(date);
                                        item.setModelObject(val);
                                    }
                                }, schemaTO.getConversionPattern(),required,
                                        schemaTO.isReadonly(), form);
                    }

                    item.add(panel);
                }
            });

            AjaxButton addButton = new AjaxButton("add",
                    new Model(getString("add"))) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    schemaWrapper.getValues().add("");

                    target.addComponent(container);
                }
            };

            AjaxButton dropButton = new AjaxButton("drop",
                    new Model(getString("drop"))) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    //Drop the last component added
                    schemaWrapper.getValues().remove(schemaWrapper.getValues()
                            .size() - 1);

                    target.addComponent(container);
                }
            };

            if (schemaTO.getType().getClassName().equals("java.lang.Boolean")) {
                addButton.setVisible(false);
                dropButton.setVisible(false);
            }

            addButton.setDefaultFormProcessing(false);
            addButton.setVisible(schemaTO.isMultivalue());

            dropButton.setDefaultFormProcessing(false);
            dropButton.setVisible(schemaTO.isMultivalue());

            if (schemaWrapper.getValues().size() == 1) {
                dropButton.setVisible(false);
            }

            item.add(addButton);
            item.add(dropButton);
        }
        };

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                MembershipTO membershipTO = (MembershipTO) form
                        .getDefaultModelObject();
                membershipTO.setAttributes(getMembershipAttributes());
                
                UserModalPage userModalPage = (UserModalPage) basePage;

                if(createFlag)
                    userModalPage.getMembershipTOs().add(membershipTO);
                else {
                    userModalPage.getMembershipTOs().remove(membershipTO);
                    userModalPage.getMembershipTOs().add(membershipTO);
                }
                window.close(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        };

        form.add(submit);


        container = new WebMarkupContainer("container");
        container.add(userAttributesView);
        container.setOutputMarkupId(true);
        
        form.add(new FeedbackPanel("feedback").setOutputMarkupId(true));
        form.add(container);

        add(form);
    }

        public List<AttributeTO> getMembershipAttributes() {

        List<AttributeTO> attributes = new ArrayList<AttributeTO>();

        AttributeTO attribute;

        for (SchemaWrapper schemaWrapper : schemaWrappers) {

            attribute = new AttributeTO();
            attribute.setSchema(schemaWrapper.getSchemaTO().getName());
            attribute.setValues(new ArrayList<String>());

            for (String value : schemaWrapper.getValues()) {
                attribute.getValues().add(value);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    public void setupSchemaWrappers(boolean create, MembershipTO membershipTO) {

        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        SchemaRestClient schemaRestClient = (SchemaRestClient) 
                ((SyncopeApplication) Application.get()).getApplicationContext()
                .getBean("schemaRestClient");

        List<SchemaTO> schemas = schemaRestClient.getAllMemberhipSchemas();

        boolean found = false;

        if(create) {
            for (SchemaTO schema : schemas) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            }
        }
        else {
            for (SchemaTO schema : schemas) {
                for (AttributeTO attribute : membershipTO.getAttributes()) {
                    if (schema.getName().equals(attribute.getSchema())) {
                        schemaWrapper = new SchemaWrapper(schema);
                        schemaWrapper.setValues(attribute.getValues());
                        schemaWrappers.add(schemaWrapper);
                        found = true;
                    }
                }
           if (!found) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            } else {
                found = false;
            }
            }
        }
    }
}
