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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.commons.SchemaWrapper;
import org.syncope.console.commons.StringChoiceRenderer;
import org.syncope.console.rest.EntitlementRestClient;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.types.SchemaType;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private EntitlementRestClient entitlementRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private AjaxButton submit;

    private WebMarkupContainer container;

    private List<SchemaWrapper> schemaWrappers = new ArrayList<SchemaWrapper>();

    private RoleTO oldRole;

    private RoleMod roleMod;

    /**
     * Constructor.
     *
     * @param basePage
     * @param window
     * @param roleTO
     * @param createFlag
     */
    public RoleModalPage(final BasePage basePage, final ModalWindow window,
            final RoleTO roleTO, final boolean createFlag) {

        super();

        if (!createFlag) {
            cloneOldRoleTO(roleTO);
        }

        final Form form = new Form("RoleForm");

        add(new Label("displayName",
                roleTO.getId() != 0 ? roleTO.getDisplayName() : ""));

        form.setModel(new CompoundPropertyModel(roleTO));

        setupSchemaWrappers(createFlag, roleTO);

        final ListView roleAttributesView = new ListView("roleSchemas",
                schemaWrappers) {

            @Override
            protected void populateItem(final ListItem item) {
                final SchemaWrapper schemaWrapper = (SchemaWrapper) item.
                        getDefaultModelObject();

                final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

                item.add(new Label("name",
                        schemaWrapper.getSchemaTO().getName()));

                item.add(new ListView("fields", schemaWrapper.getValues()) {

                    Panel panel;

                    @Override
                    protected void populateItem(final ListItem item) {
                        String mandatoryCondition =
                                schemaTO.getMandatoryCondition();

                        boolean required = false;

                        if (mandatoryCondition.equalsIgnoreCase("true")) {
                            required = true;
                        }

                        if (schemaTO.getType() == SchemaType.Boolean) {
                            panel = new AjaxCheckBoxPanel("panel", schemaTO.
                                    getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    Boolean val = (Boolean) object;
                                    item.setModelObject(val.toString());
                                }
                            }, required);

                        } else if (schemaTO.getType() == SchemaType.Date) {
                            panel = new DateFieldPanel("panel",
                                    schemaTO.getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    DateFormat formatter = new SimpleDateFormat(
                                            schemaTO.getConversionPattern());
                                    Date date = new Date();
                                    try {
                                        String dateValue = (String) item.
                                                getModelObject();
                                        //Default value:yyyy-MM-dd
                                        if (!dateValue.equals("")) {
                                            date = formatter.parse(dateValue);
                                        } else {
                                            date = null;
                                        }
                                    } catch (ParseException e) {
                                        LOG.error("While parsing a date", e);
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
                            }, schemaTO.getConversionPattern(),
                                    required,
                                    schemaTO.isReadonly(), form);
                        } else {
                            /*Common other cases :
                            java.lang.String,java.lang.Double, java.lang.Long*/

                            panel = new AjaxTextFieldPanel("panel", schemaTO.
                                    getName(), new Model() {

                                @Override
                                public Serializable getObject() {
                                    return (String) item.getModelObject();
                                }

                                @Override
                                public void setObject(Serializable object) {
                                    item.setModelObject((String) object);
                                }
                            }, required);
                        }

                        item.add(panel);
                    }
                });

                AjaxButton addButton = new IndicatingAjaxButton("add",
                        new Model(getString("add"))) {

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target,
                            final Form form) {

                        schemaWrapper.getValues().add("");

                        target.addComponent(container);
                    }
                };

                AjaxButton dropButton = new AjaxButton("drop",
                        new Model(getString("drop"))) {

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target,
                            final Form form) {

                        //Drop the last component added
                        schemaWrapper.getValues().remove(
                                schemaWrapper.getValues().size() - 1);

                        target.addComponent(container);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };

                if (schemaTO.getType() == SchemaType.Boolean) {
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

        ListModel<ResourceTO> selectedResources = new ListModel<ResourceTO>();
        selectedResources.setObject(getSelectedResources(roleTO));

        ListModel<ResourceTO> availableResources = new ListModel<ResourceTO>();
        availableResources.setObject(getAvailableResources(roleTO));

        final Palette<ResourceTO> resourcesPalette = new Palette(
                "resourcesPalette", selectedResources, availableResources,
                new ChoiceRenderer("name", "name"), 8, false);

        form.add(resourcesPalette);

        ListModel<String> selectedEntitlements =
                new ListModel<String>(roleTO.getEntitlements());

        ListModel<String> availableEntitlements =
                new ListModel<String>(
                entitlementRestClient.getAllEntitlements());

        final Palette<String> entitlementsPalette = new Palette(
                "entitlementsPalette", selectedEntitlements,
                availableEntitlements, new StringChoiceRenderer(), 20, false);

        form.add(entitlementsPalette);

        container = new WebMarkupContainer("container");
        container.add(roleAttributesView);
        container.setOutputMarkupId(true);

        form.add(container);

        TextField name = new TextField("name");
        name.setRequired(true);
        container.add(name);

        submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                RoleTO roleTO = (RoleTO) form.getDefaultModelObject();

                boolean res = false;

                try {
                    Set<String> resourcesSet = new HashSet<String>(
                            resourcesPalette.getModelCollection().size());
                    for (ResourceTO resourceTO :
                            resourcesPalette.getModelCollection()) {

                        resourcesSet.add(resourceTO.getName());
                    }
                    roleTO.setResources(resourcesSet);

                    List<String> entitlementList = new ArrayList<String>(
                            entitlementsPalette.getModelCollection().size());
                    for (String entitlement :
                            entitlementsPalette.getModelCollection()) {

                        entitlementList.add(entitlement);
                    }
                    roleTO.setEntitlements(entitlementList);
                    roleTO.setAttributes(getRoleAttributes());

                    if (createFlag) {
                        roleRestClient.createRole(roleTO);
                    } else {
                        setupRoleMod(roleTO);
                        if (roleMod != null) {
                            roleRestClient.updateRole(roleMod);
                        }
                    }
                    ((Roles) basePage).setOperationResult(true);

                    window.close(target);
                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Roles", "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Roles", "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, allowedRoles);

        form.add(submit);

        add(form);
    }

    /**
     * Originals: resources provided for a stored role.
     * @param roleTO
     * @return List<ResourceTO>
     */
    public List<ResourceTO> getSelectedResources(RoleTO roleTO) {
        List<ResourceTO> resources = new ArrayList<ResourceTO>();
        ResourceTO clusterableResourceTO;

        for (String resourceName : roleTO.getResources()) {
            clusterableResourceTO = new ResourceTO();
            clusterableResourceTO.setName(resourceName);
            resources.add(clusterableResourceTO);
        }
        return resources;
    }

    /**
     * Destinations: all available
     * @param roleTO
     * @return List<ResourceTO>
     */
    public List<ResourceTO> getAvailableResources(RoleTO roleTO) {

        List<ResourceTO> resources = new ArrayList<ResourceTO>();

        List<ResourceTO> resourcesTos = resourceRestClient.getAllResources();

        for (ResourceTO resourceTO : resourcesTos) {
            resources.add(resourceTO);
        }

        return resources;
    }

    public void setupSchemaWrappers(boolean create, RoleTO roleTO) {
        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        List<SchemaTO> schemas = schemaRestClient.getAllRoleSchemas();

        boolean found = false;

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

    public List<AttributeTO> getRoleAttributes() {

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

    /**
     * Create a copy of old RoleTO
     * @param roleTO
     */
    public void cloneOldRoleTO(RoleTO roleTO) {
        oldRole = new RoleTO();

        oldRole.setId(roleTO.getId());
        oldRole.setName(roleTO.getName());
        oldRole.setParent(roleTO.getParent());

        final List<AttributeTO> attributes = new ArrayList<AttributeTO>();

        AttributeTO attributeTO;
        List<String> values;
        for (AttributeTO attribute : roleTO.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(new String(attribute.getSchema()));

            values = new ArrayList<String>();
            for (String val : attribute.getValues()) {
                values.add(val);
            }
            attributeTO.setValues(values);

            attributes.add(attributeTO);
        }

        oldRole.setAttributes(attributes);

        oldRole.setResources(roleTO.getResources());

        List<String> entList = new ArrayList<String>();

        for (String entitlement : roleTO.getEntitlements()) {
            entList.add(entitlement);
        }

        oldRole.setEntitlements(entList);
    }

    public void setupRoleMod(final RoleTO roleTO) {
        //1.Check if the role's name has been changed
        if (!oldRole.getName().equals(roleTO.getName())) {
            roleMod = new RoleMod();
            roleMod.setName(roleTO.getName());
        }

        //2.Search and update role's attributes
        for (AttributeTO attributeTO : roleTO.getAttributes()) {
            searchAndUpdateAttribute(attributeTO);
        }

        //3.Search and update role's resources
        for (String resource : roleTO.getResources()) {
            searchAndAddResource(resource);
        }

        for (String resource : oldRole.getResources()) {
            searchAndDropResource(resource, roleTO);
        }

        //4.Check if entitlements' list has been changed
        if (!oldRole.getEntitlements().equals(roleTO.getEntitlements())) {
            roleMod.setEntitlements(roleTO.getEntitlements());
        }

        if (roleMod != null) {
            roleMod.setId(oldRole.getId());

            if (!oldRole.getEntitlements().equals(roleTO.getEntitlements())) {

                LOG.debug("OLD ROLE ENT LIST: {}", oldRole.getEntitlements());

                LOG.debug("ROLE ENT LIST: {}", roleTO.getEntitlements());

                roleMod.setEntitlements(roleTO.getEntitlements());
            } else {
                roleMod.setEntitlements(oldRole.getEntitlements());
            }
        }
    }

    /**
     * Search for a resource and add that one to the RoleMod object if
     * it doesn't exist.
     * @param resource, new resource added
     */
    public void searchAndAddResource(String resource) {
        boolean found = false;

        /*
        Check if the current resource was existent before the update and
        in this case just ignore it
         */
        for (String oldResource : oldRole.getResources()) {
            if (resource.equals(oldResource)) {
                found = true;
            }

        }

        if (!found) {
            if (roleMod == null) {
                roleMod = new RoleMod();
            }

            roleMod.addResourceToBeAdded(resource);
        }
    }

    /**
     * Search for a resource and drop that one from the RoleMod object if
     * it doesn't exist anymore.
     * @param resource
     * @param roleTO
     */
    public void searchAndDropResource(final String resource,
            final RoleTO roleTO) {

        boolean found = false;

        /* Check if the current resource was existent before the update 
        and in this case just ignore it */
        for (String newResource : roleTO.getResources()) {
            if (resource.equals(newResource)) {
                found = true;
            }
        }

        if (!found) {
            if (roleMod == null) {
                roleMod = new RoleMod();
            }
            roleMod.addResourceToBeRemoved(resource);
        }
    }

    public void searchAndUpdateAttribute(AttributeTO attributeTO) {
        boolean found = false;
        boolean changed = false;

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema(attributeTO.getSchema());

        for (AttributeTO oldAttribute : oldRole.getAttributes()) {
            if (attributeTO.getSchema().equals(oldAttribute.getSchema())) {

                if (attributeTO.getSchema().equals(oldAttribute.getSchema())
                        && !attributeTO.equals(oldAttribute) && !oldAttribute.
                        isReadonly()) {

                    if (attributeTO.getValues().size() > 1) {
                        attributeMod.setValuesToBeAdded(
                                attributeTO.getValues());
                    } else {
                        attributeMod.addValueToBeAdded(
                                attributeTO.getValues().iterator().next());
                    }

                    if (roleMod == null) {
                        roleMod = new RoleMod();
                    }

                    roleMod.addAttributeToBeRemoved(
                            oldAttribute.getSchema());
                    roleMod.addAttributeToBeUpdated(attributeMod);

                    changed = true;
                    break;
                }
                found = true;
            }
        }

        if (!found && !changed && !attributeTO.isReadonly()
                && attributeTO.getValues() != null) {

            if (attributeTO.getValues() != null
                    && !attributeTO.getValues().isEmpty()) {

                attributeMod.setValuesToBeAdded(attributeTO.getValues());

                if (roleMod == null) {
                    roleMod = new RoleMod();
                }
                roleMod.addAttributeToBeUpdated(attributeMod);
            } else {
                attributeMod = null;
            }
        }
    }
}
