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

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.RolesRestClient;
import org.syncope.console.rest.SchemaRestClient;

import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends SyncopeModalPage {

    @SpringBean(name = "rolesRestClient")
    RolesRestClient restClient;

    AjaxButton submit;

    WebMarkupContainer container;
    List<SchemaWrapper> schemaWrappers = new ArrayList<SchemaWrapper>();

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public RoleModalPage(final BasePage basePage, final ModalWindow window,
            final RoleTO roleTO, final boolean createFlag) {

        Form form = new Form("RoleForm");

        form.setModel(new CompoundPropertyModel(roleTO));

        setupSchemaWrappers(createFlag, roleTO);

        final ListView roleAttributesView = new ListView("roleSchemas", schemaWrappers) {

            @Override
            protected void populateItem(ListItem item) {
                final SchemaWrapper schemaWrapper = (SchemaWrapper) item.getDefaultModelObject();

                final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

                item.add(new Label("name", schemaWrapper.getSchemaTO().getName()));

                item.add(new ListView("fields", schemaWrapper.getValues()) {

                    Panel panel;

                    @Override
                    protected void populateItem(final ListItem item) {

                        if (schemaTO.getType().getClassName().equals("java.lang.String")) {
                            panel = new AjaxTextFieldPanel("panel", schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    item.setModelObject((String) object);
                                }
                            }, schemaTO.isMandatory());
                        } else if (schemaTO.getType().getClassName().equals("java.lang.Boolean")) {
                            panel = new AjaxCheckBoxPanel("panel", schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    Boolean val = (Boolean) object;
                                    item.setModelObject(val.toString());
                                }
                            }, schemaTO.isMandatory());

                        } else if (schemaTO.getType().getClassName().equals("java.util.Date")) {
                            panel = new DateFieldPanel("panel", schemaTO.getName(),
                                    new Model() {

                                        @Override
                                        public Serializable getObject() {
                                            DateFormat formatter = new SimpleDateFormat(schemaTO.getConversionPattern());
                                            Date date = new Date();
                                            String val;
                                            try {
                                                date = formatter.parse((String) item.getModelObject());
                                                formatter = new SimpleDateFormat("yyyy-MM-dd");
                                                val = formatter.format(date);
                                            } catch (ParseException ex) {
                                                Logger.getLogger(RoleModalPage.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                            return date;
                                        }

                                        @Override
                                        public void setObject(Serializable object) {
                                            Date date = (Date) object;
                                            Format formatter = new SimpleDateFormat(schemaTO.getConversionPattern());
                                            String val = formatter.format(date);
                                            item.setModelObject(val);
                                        }
                                    }, schemaTO.isMandatory());
                        }

                        item.add(panel);
                    }
                });

                AjaxButton addButton = new AjaxButton("add", new Model(getString("add"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        schemaWrapper.getValues().add("");

                        target.addComponent(container);
                    }
                };

                AjaxButton dropButton = new AjaxButton("drop", new Model(getString("drop"))) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        //Drop the last component added
                        schemaWrapper.getValues().remove(schemaWrapper.getValues().size() - 1);

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

        form.add(roleAttributesView);

//        final ListMultipleChoiceTransfer resourcesTransfer =
//                new ListMultipleChoiceTransfer("resourcesChoiceTransfer",
//                getString("firstResourcesList"), getString("secondResourcesList")) {
//
//                    @Override
//                    public List<String> setupOriginals() {
//                        //Originals:user's resources
//                        List<String> resources = new ArrayList<String>();
//
//                        for (String resourceName : roleTO.getResources()) {
//                            resources.add(resourceName);
//                        }
//
//                        return resources;
//
//                    }
//
//                    @Override
//                    public List<String> setupDestinations() {
//                        //Destinations:available resources
//                        List<String> resources = new ArrayList<String>();
//
//                        ResourcesRestClient resourcesRestClient = (ResourcesRestClient)
//                                ((SyncopeApplication) Application.get()).getApplicationContext().getBean("resourcesRestClient");
//
//                        ResourceTOs resourcesTos = resourcesRestClient.getAllResources();
//
//                        if (roleTO.getResources().size() == 0) {
//                            for (ResourceTO resourceTO : resourcesTos) {
//                                resources.add(resourceTO.getName());
//                            }
//
//                        } else {
//
//                            for (String resource : roleTO.getResources()) {
//                                for (ResourceTO resourceTO : resourcesTos) {
//                                    if (!resource.equals(resourceTO.getName())) {
//                                        resources.add(resourceTO.getName());
//                                    }
//                                }
//                            }
//                        }
//                        return resources;
//                    }
//                };
//
//        form.add(resourcesTransfer);

        container = new WebMarkupContainer("container");
        container.add(roleAttributesView);

        container.setOutputMarkupId(true);

        form.add(container);

        TextField name = new TextField("name");
        name.setRequired(true);
        container.add(name);

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                RoleTO roleTO = (RoleTO) form.getDefaultModelObject();

                boolean res = false;

                try {

                    roleTO.setAttributes(getRoleAttributes());

                    if (createFlag) {
                        restClient.createRole(roleTO);
                    } else {
                        res = restClient.updateRole(roleTO);
                        
                        if (!res) 
                              error(getString("error_updating"));

                    }
                    
                    window.close(target);

                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                }


            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        };

        form.add(submit);

        form.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        add(form);
    }

    public void setupSchemaWrappers(boolean create, RoleTO roleTO) {

        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        SchemaRestClient schemaRestClient = (SchemaRestClient) ((SyncopeApplication)
                Application.get()).getApplicationContext().getBean("schemaRestClient");

        SchemaTOs schemas = schemaRestClient.getAllRoleSchemas();

        if (create) {
            for (SchemaTO schema : schemas) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            }
        } else {
            for (SchemaTO schema : schemas) {
                for (AttributeTO attribute : roleTO.getAttributes()) {
                    if (schema.getName().equals(attribute.getSchema())) {
                        schemaWrapper = new SchemaWrapper(schema);
                        schemaWrapper.setValues(attribute.getValues());
                        schemaWrappers.add(schemaWrapper);
                    }
                }
            }
        }
    }

    public Set<AttributeTO> getRoleAttributes() {

        Set<AttributeTO> attributes = new HashSet<AttributeTO>();

        AttributeTO attribute;

        for (SchemaWrapper schemaWrapper : schemaWrappers) {

            attribute = new AttributeTO();
            attribute.setSchema(schemaWrapper.getSchemaTO().getName());
            attribute.setValues(new HashSet<String>());

            for (String value : schemaWrapper.getValues()) {
                attribute.getValues().add(value);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    /**
     * Set in the UserTO the resources set.
     * @return UserTO with resources set
     */
    public Set<String> getResourcesSelected(List<String> resourcesList) {
        Set<String> resourcesSet = new HashSet<String>();

        for (String resource : resourcesList) {
            resourcesSet.add(resource);
        }

        return resourcesSet;
    }

    /**
     * Wrapper for User's Schema - Attribute.
     */
    public class SchemaWrapper {

        SchemaTO schemaTO;
        List<String> values;

        public SchemaWrapper(SchemaTO schemaTO) {
            this.schemaTO = schemaTO;
            values = new ArrayList<String>();
            values.add("");
        }

        public SchemaTO getSchemaTO() {
            return schemaTO;
        }

        public void setSchemaTO(SchemaTO schemaTO) {
            this.schemaTO = schemaTO;
        }

        public List<String> getValues() {
            return values;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        public void setValues(Set<String> values) {
            for (String value : values) {
                this.values = new ArrayList<String>();
                this.values.add(value);
            }
        }
    }
}
