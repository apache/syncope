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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.RoleTreeBuilder;
import org.syncope.console.commons.SchemaWrapper;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.types.SchemaType;

/**
 * Modal window with User form.
 */
public class UserModalPage extends BaseModalPage {

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private WebMarkupContainer container;

    private WebMarkupContainer membershipsContainer;

    private AjaxButton submit;

    private List<SchemaWrapper> schemaWrappers;

    private List<MembershipTO> membershipTOs;

    private final ModalWindow membershipWin;

    private UserTO oldUser;

    private UserMod userMod;

    public UserModalPage(final Users basePage, final ModalWindow window,
            final UserTO userTO, final boolean createFlag) {

        super();

        if (!createFlag) {
            cloneOldUserTO(userTO);
        }

        membershipWin = new ModalWindow("membershipWin");
        membershipWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        membershipWin.setPageMapName("create-membership-modal");
        membershipWin.setCookieName("create-membership-modal");
        add(membershipWin);

        add(new Label("id", String.valueOf(userTO.getId())));

        final Form userForm = new Form("UserForm");

        userForm.setModel(new CompoundPropertyModel(userTO));

        setupSchemaWrappers(createFlag, userTO);
        setupMemberships(createFlag, userTO);

        final IModel<List<String>> derivedSchemaNames =
                new LoadableDetachableModel<List<String>>() {

                    @Override
                    protected List<String> load() {
                        return schemaRestClient.getDerivedSchemaNames("user");
                    }
                };


        final IModel<List<String>> virtualSchemaNames =
                new LoadableDetachableModel<List<String>>() {

                    @Override
                    protected List<String> load() {
                        return schemaRestClient.getDerivedSchemaNames("user");
                    }
                };

        final ListView userAttributesView =
                new ListView("userSchemas", schemaWrappers) {

                    @Override
                    protected void populateItem(ListItem item) {
                        final SchemaWrapper schemaWrapper =
                                (SchemaWrapper) item.getDefaultModelObject();

                        final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

                        item.add(new Label("name",
                                schemaWrapper.getSchemaTO().getName()));

                        item.add(new ListView("fields", schemaWrapper.getValues()) {

                            Panel panel;

                            @Override
                            protected void populateItem(final ListItem item) {

                                String mandatoryCondition = schemaTO.getMandatoryCondition();
                                boolean required = false;

                                if (mandatoryCondition.equalsIgnoreCase("true")) {
                                    required = true;
                                }

                                if (schemaTO.getType() == SchemaType.String) {
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
                                    }, required, schemaTO.isReadonly());
                                } else if (schemaTO.getType() == SchemaType.Boolean) {
                                    panel = new AjaxCheckBoxPanel("panel",
                                            schemaTO.getName(), new Model() {

                                        @Override
                                        public Serializable getObject() {
                                            return (String) item.getModelObject();
                                        }

                                        @Override
                                        public void setObject(Serializable object) {
                                            Boolean val = (Boolean) object;
                                            item.setModelObject(val.toString());
                                        }
                                    }, required, schemaTO.isReadonly());

                                } else if (schemaTO.getType() == SchemaType.Date) {
                                    panel = new DateFieldPanel("panel",
                                            schemaTO.getName(), new Model() {

                                        @Override
                                        public Serializable getObject() {
                                            DateFormat formatter =
                                                    new SimpleDateFormat(
                                                    schemaTO.getConversionPattern());
                                            Date date = new Date();
                                            try {
                                                String dateValue = (String) item.getModelObject();
                                                //Default value:yyyy-MM-dd
                                                if (!dateValue.equals("")) {
                                                    date = formatter.parse(
                                                            dateValue);
                                                } else {
                                                    date = null;
                                                }
                                            } catch (ParseException e) {
                                                LOG.error("While parsing date", e);
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
                                            schemaTO.isReadonly(), userForm);
                                } else {
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
                                    }, required, schemaTO.isReadonly());
                                }

                                item.add(panel);
                            }
                        });

                        AjaxButton addButton = new IndicatingAjaxButton("add",
                                new Model(getString("add"))) {

                            @Override
                            protected void onSubmit(AjaxRequestTarget target,
                                    Form form) {
                                schemaWrapper.getValues().add("");

                                target.addComponent(container);
                            }
                        };

                        AjaxButton dropButton = new IndicatingAjaxButton("drop",
                                new Model(getString("drop"))) {

                            @Override
                            protected void onSubmit(AjaxRequestTarget target,
                                    Form form) {
                                //Drop the last component added
                                schemaWrapper.getValues().remove(
                                        schemaWrapper.getValues().size() - 1);

                                target.addComponent(container);
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

                        if (schemaTO.isReadonly()) {
                            addButton.setEnabled(false);
                            dropButton.setEnabled(false);
                        }

                        item.add(addButton);
                        item.add(dropButton);
                    }
                };

        userForm.add(userAttributesView);

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        setDerivedAttributeContainer(userForm, userTO, derivedSchemaNames);
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        setVirtualAttributeContainer(userForm, userTO, derivedSchemaNames);
        //--------------------------------

        ListModel<ResourceTO> selectedResources = new ListModel<ResourceTO>();
        selectedResources.setObject(getSelectedResources(userTO));

        ListModel<ResourceTO> availableResources = new ListModel<ResourceTO>();
        availableResources.setObject(getAvailableResources(userTO));

        ChoiceRenderer paletteRenderer = new ChoiceRenderer("name", "name");
        final Palette resourcesPalette = new Palette("resourcesPalette",
                selectedResources, availableResources, paletteRenderer,
                8, false);
        userForm.add(resourcesPalette);

        container = new WebMarkupContainer("container");
        container.add(userAttributesView);

        PasswordTextField password = new PasswordTextField("password");
        password.setRequired(createFlag);
        password.setResetPassword(true);
        container.add(password);

        WebMarkupContainerWithAssociatedMarkup mandatoryPassword =
                new WebMarkupContainerWithAssociatedMarkup("mandatory_pwd");
        mandatoryPassword.add(new AbstractBehavior() {

            @Override
            public void onComponentTag(final Component component,
                    final ComponentTag tag) {

                if (!createFlag) {
                    tag.put("style", "display:none;");
                }
            }
        });

        container.add(mandatoryPassword);

        container.setOutputMarkupId(true);

        userForm.add(container);

        submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                UserTO userTO = (UserTO) form.getModelObject();

                try {
                    userTO.setResources(getResourcesSet(
                            resourcesPalette.getModelCollection()));
                    userTO.setAttributes(getUserAttributesList());
                    userTO.setMemberships(getMembershipsSet());

                    if (createFlag) {
                        userRestClient.create(userTO);
                    } else {
                        setupUserMod(userTO);

                        //Update user just if it is changed
                        if (userMod != null) {
                            userRestClient.update(userMod);
                        }
                    }

                    basePage.setModalResult(true);
                    basePage.getPageParameters().put(
                            Constants.PAGEPARAM_CREATE, createFlag);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    LOG.error("While creating or updating user", e);
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles = null;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Users", "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Users", "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(
                submit, RENDER, allowedRoles);

        userForm.add(submit);

        // Roles Tab
        final List<RoleTO> roles = roleRestClient.getAllRoles();
        BaseTree tree = new LinkTree("treeTable",
                roleTreeBuilder.build(roles)) {

            @Override
            protected IModel getNodeTextModel(final IModel model) {
                return new PropertyModel(model, "userObject.displayName");
            }

            @Override
            protected void onNodeLinkClicked(final Object node,
                    final BaseTree tree, final AjaxRequestTarget target) {

                final RoleTO roleTO = (RoleTO) ((DefaultMutableTreeNode) node).getUserObject();

                membershipWin.setPageCreator(new ModalWindow.PageCreator() {

                    private MembershipTO membershipTO;

                    @Override
                    public Page createPage() {
                        membershipTO = new MembershipTO();
                        membershipTO.setRoleId(roleTO.getId());

                        return new MembershipModalPage(getPage(),
                                membershipWin, membershipTO, true);
                    }
                });
                membershipWin.show(target);
            }
        };

        tree.getTreeState().expandAll();
        tree.updateTree();

        userForm.add(tree);


        ListView membershipsView = new ListView("memberships", membershipTOs) {

            @Override
            protected void populateItem(final ListItem item) {
                final MembershipTO membershipTO =
                        (MembershipTO) item.getDefaultModelObject();

                item.add(new Label("roleId", new Model(
                        membershipTO.getRoleId())));
                item.add(new Label("roleName", new Model(
                        getRoleName(membershipTO.getRoleId(), roles))));

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        membershipWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    @Override
                                    public Page createPage() {
                                        MembershipModalPage window =
                                                new MembershipModalPage(
                                                getPage(), membershipWin,
                                                membershipTO,
                                                false);

                                        return window;

                                    }
                                });
                        membershipWin.show(target);
                    }
                };
                item.add(editLink);

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        int componentId = new Integer(getParent().getId());
                        membershipTOs.remove(componentId);

                        target.addComponent(membershipsContainer);
                    }
                };
                item.add(deleteLink);
            }
        };

        membershipsContainer = new WebMarkupContainer("membershipsContainer");
        membershipsContainer.add(membershipsView);
        membershipsContainer.setOutputMarkupId(true);

        setWindowClosedCallback(membershipWin, membershipsContainer);

        userForm.add(membershipsContainer);
        add(userForm);
    }

    private String getRoleName(long roleId, List<RoleTO> roles) {
        boolean found = false;
        RoleTO roleTO;
        String result = null;
        for (Iterator<RoleTO> itor = roles.iterator();
                itor.hasNext() && !found;) {

            roleTO = itor.next();
            if (roleTO.getId() == roleId) {
                result = roleTO.getName();
            }
        }

        return result;
    }

    private List<ResourceTO> getSelectedResources(final UserTO userTO) {
        List<ResourceTO> resources = new ArrayList<ResourceTO>();
        ResourceTO clusterableResourceTO;

        for (String resourceName : userTO.getResources()) {
            clusterableResourceTO = new ResourceTO();
            clusterableResourceTO.setName(resourceName);
            resources.add(clusterableResourceTO);
        }
        return resources;
    }

    private List<DerivedSchemaTO> getSelectedDerivedSchemas(final UserTO userTO) {
        List<AttributeTO> attributes = userTO.getDerivedAttributes();

        List<DerivedSchemaTO> schemas = new ArrayList<DerivedSchemaTO>();

        DerivedSchemaTO schema;

        for (AttributeTO attribute : attributes) {
            schema = new DerivedSchemaTO();
            schema.setName(attribute.getSchema());
            schemas.add(schema);
        }

        return schemas;
    }

    private List<ResourceTO> getAvailableResources(final UserTO userTO) {
        List<ResourceTO> resources = new ArrayList<ResourceTO>();

        List<ResourceTO> resourcesTos = resourceRestClient.getAllResources();

        for (ResourceTO resourceTO : resourcesTos) {
            resources.add(resourceTO);
        }

        return resources;
    }

    private List<DerivedSchemaTO> getAvailableDerivedSchemas(final UserTO userTO) {
        return schemaRestClient.getDerivedSchemas("user");
    }

    /**
     * Create a copy of old userTO object.
     * @param userTO the original TO
     */
    private void cloneOldUserTO(final UserTO userTO) {
        oldUser = new UserTO();

        oldUser.setId(userTO.getId());
        oldUser.setPassword(userTO.getPassword());

        AttributeTO attributeTO;
        for (AttributeTO attribute : userTO.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setReadonly(attribute.isReadonly());

            attributeTO.setSchema(attribute.getSchema());

            for (String value : attribute.getValues()) {
                attributeTO.addValue(value);
            }

            oldUser.addAttribute(attributeTO);
        }

        for (AttributeTO attribute : userTO.getDerivedAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setReadonly(attribute.isReadonly());

            attributeTO.setSchema(attribute.getSchema());

            for (String value : attribute.getValues()) {
                attributeTO.addValue(value);
            }

            oldUser.addDerivedAttribute(attributeTO);
        }

        for (String resource : userTO.getResources()) {
            oldUser.addResource(resource);
        }

        MembershipTO membership;

        for (MembershipTO membershipTO : userTO.getMemberships()) {
            membership = new MembershipTO();
            membership.setId(membershipTO.getId());
            membership.setRoleId(membershipTO.getRoleId());
            membership.setAttributes(membershipTO.getAttributes());
            oldUser.addMembership(membership);
        }
    }

    private void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);
                    }
                });
    }

    private void setupSchemaWrappers(boolean create, final UserTO userTO) {
        schemaWrappers = new ArrayList<SchemaWrapper>();
        SchemaWrapper schemaWrapper;

        List<SchemaTO> schemas = schemaRestClient.getSchemas("user");

        boolean found = false;

        if (create) {
            for (SchemaTO schema : schemas) {
                schemaWrapper = new SchemaWrapper(schema);
                schemaWrappers.add(schemaWrapper);
            }
        } else {
            for (SchemaTO schema : schemas) {
                for (AttributeTO attribute : userTO.getAttributes()) {
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

    private void setupMemberships(final boolean create, final UserTO userTO) {
        membershipTOs = new ArrayList<MembershipTO>();

        if (!create) {
            List<MembershipTO> memberships = userTO.getMemberships();

            for (MembershipTO membership : memberships) {
                membershipTOs.add(membership);
            }
        }
    }

    private List<AttributeTO> getUserAttributesList() {
        List<AttributeTO> attributes = new ArrayList<AttributeTO>();

        AttributeTO attribute;

        for (SchemaWrapper schemaWrapper : schemaWrappers) {
            attribute = new AttributeTO();
            attribute.setSchema(schemaWrapper.getSchemaTO().getName());
            attribute.setValues(new ArrayList<String>());
            attribute.setReadonly(schemaWrapper.getSchemaTO().isReadonly());

            for (String value : schemaWrapper.getValues()) {
                attribute.getValues().add(value);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    /**
     * Convert a memberships ArrayList in a memberships HashSet list.
     * @return Set<MembershipTO> selected for a new user.
     */
    private List<MembershipTO> getMembershipsSet() {
        List<MembershipTO> memberships = new ArrayList<MembershipTO>();

        for (MembershipTO membership : membershipTOs) {
            memberships.add(membership);
        }

        return memberships;
    }

    /**
     * Covert a resources List<String> to Set<String>.
     * @return Set<String>
     */
    private Set<String> getResourcesSet(Collection<ResourceTO> resourcesList) {
        Set<String> resourcesSet = new HashSet<String>();

        for (ResourceTO resourceTO : resourcesList) {
            resourcesSet.add(resourceTO.getName());
        }

        return resourcesSet;
    }

    public List<MembershipTO> getMembershipTOs() {
        return membershipTOs;
    }

    /**
     * Updates the modified user object.
     * @param userTO
     */
    private void setupUserMod(final UserTO userTO) {
        userMod = new UserMod();

        //1.Check if the password has been changed and update it
        if (oldUser.getPassword() != null
                && !oldUser.getPassword().equals(userTO.getPassword())) {
            userMod.setPassword(userTO.getPassword());
        }

        //2.Update user's schema derived attributes
        final List<AttributeTO> newDerivedAttributes =
                userTO.getDerivedAttributes();

        final List<AttributeTO> oldDerivedAttributes =
                oldUser.getDerivedAttributes();

        for (AttributeTO oldDerivedAttribute : oldDerivedAttributes) {
            userMod.addDerivedAttributeToBeRemoved(
                    oldDerivedAttribute.getSchema());
        }

        for (AttributeTO newDerivedAttribute : newDerivedAttributes) {
            userMod.addDerivedAttributeToBeAdded(
                    newDerivedAttribute.getSchema());
        }

        //3.Update user's schema attributes
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            searchAndUpdateAttribute(attributeTO);
        }

        //4.Update user's resources
        for (String resource : userTO.getResources()) {
            searchAndAddResource(resource);
        }


        for (String resource : oldUser.getResources()) {
            searchAndDropResource(resource, userTO);
        }

        //5.Update user's memberships
        for (MembershipTO membership : userTO.getMemberships()) {
            searchAndUpdateMembership(membership);
        }

        for (MembershipTO membership : oldUser.getMemberships()) {
            searchAndDropMembership(membership, userTO);
        }

        if (userMod != null) {
            userMod.setId(oldUser.getId());
        }
    }

    private void searchAndUpdateAttribute(AttributeTO attributeTO) {
        boolean found = false;
        boolean changed = false;

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema(attributeTO.getSchema());

        for (AttributeTO oldAttribute : oldUser.getAttributes()) {
            if (attributeTO.getSchema().equals(oldAttribute.getSchema())) {

                if (!attributeTO.equals(oldAttribute) && !oldAttribute.isReadonly()) {

                    if (attributeTO.getValues().size() > 1) {
                        attributeMod.setValuesToBeAdded(
                                attributeTO.getValues());
                    } else {
                        attributeMod.addValueToBeAdded(
                                attributeTO.getValues().iterator().next());
                    }

                    if (userMod == null) {
                        userMod = new UserMod();
                    }

                    userMod.addAttributeToBeRemoved(oldAttribute.getSchema());
                    userMod.addAttributeToBeUpdated(attributeMod);

                    changed = true;
                    break;
                }
                found = true;
            }
        }

        if (!found & !changed && !attributeTO.isReadonly()
                && attributeTO.getValues() != null) {

            if (attributeTO.getValues().iterator().next() != null) {
                attributeMod.setValuesToBeAdded(attributeTO.getValues());

                if (userMod == null) {
                    userMod = new UserMod();
                }

                userMod.addAttributeToBeUpdated(attributeMod);
            } else {
                attributeMod = null;
            }
        }
    }

    /**
     * Search for a resource and add that one to the UserMod object if
     * it doesn't exist.
     * @param resource new resource added
     */
    private void searchAndAddResource(final String resource) {
        boolean found = false;

        /* Check if the current resource was existent before the update
        and in this        case just ignore it */
        for (String oldResource : oldUser.getResources()) {
            if (resource.equals(oldResource)) {
                found = true;
            }
        }

        if (!found) {
            if (userMod == null) {
                userMod = new UserMod();
            }

            userMod.addResourceToBeAdded(resource);
        }
    }

    /**
     * Search for a resource and drop that one from the UserMod object if
     * it doesn't exist anymore.
     * @param resource
     * @param userTO
     */
    private void searchAndDropResource(String resource, UserTO userTO) {
        boolean found = false;

        /*Check if the current resource was existing before the update
        and in this case just ignore it */
        for (String newResource : userTO.getResources()) {
            if (resource.equals(newResource)) {
                found = true;
            }
        }

        if (!found) {
            if (userMod == null) {
                userMod = new UserMod();
            }
            userMod.addResourceToBeRemoved(resource);
        }
    }

    /**
     * Update the Membership.
     * @param new membershipTO
     */
    private void searchAndUpdateMembership(MembershipTO membership) {
        boolean found = false;
        boolean attrFound = false;

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(membership.getRoleId());

        AttributeMod attributeMod;

        //1. If the membership exists update it
        for (MembershipTO oldMembership : oldUser.getMemberships()) {
            if (membership.getRoleId() == oldMembership.getRoleId()) {

                for (AttributeTO newDerivedAttribute :
                        membership.getDerivedAttributes()) {
                    membershipMod.addDerivedAttributeToBeAdded(
                            newDerivedAttribute.getSchema());
                }

                for (AttributeTO newAttribute : membership.getAttributes()) {
                    for (AttributeTO oldAttribute :
                            oldMembership.getAttributes()) {

                        if (oldAttribute.getSchema().equals(
                                newAttribute.getSchema())) {

                            attributeMod = new AttributeMod();
                            attributeMod.setSchema(newAttribute.getSchema());

                            attributeMod.setValuesToBeAdded(
                                    newAttribute.getValues());

                            membershipMod.addAttributeToBeUpdated(attributeMod);
                            attrFound = true;
                            break;
                        }
                    }
                    if (!attrFound) {
                        attributeMod = new AttributeMod();
                        attributeMod.setSchema(newAttribute.getSchema());
                        attributeMod.setValuesToBeAdded(
                                newAttribute.getValues());
                        membershipMod.addAttributeToBeUpdated(attributeMod);
                    }
                    attrFound = false;
                }

                if (userMod == null) {
                    userMod = new UserMod();
                }

                userMod.addMembershipToBeRemoved(oldMembership.getId());
                userMod.addMembershipToBeAdded(membershipMod);

                found = true;
                break;
            }
        }

        //2.Otherwise, if it doesn't exist, create it from scratch
        if (!found) {
            Set<AttributeMod> attributes = new HashSet<AttributeMod>();

            for (AttributeTO newAttribute : membership.getAttributes()) {
                attributeMod = new AttributeMod();
                attributeMod.setSchema(newAttribute.getSchema());
                attributeMod.setValuesToBeAdded(newAttribute.getValues());

                attributes.add(attributeMod);
            }

            membershipMod.setAttributesToBeUpdated(attributes);

            if (userMod == null) {
                userMod = new UserMod();
            }

            userMod.addMembershipToBeAdded(membershipMod);
        }
    }

    /**
     * Drop membership not present anymore.
     * @param membershipTO
     * @param userTO
     */
    private void searchAndDropMembership(final MembershipTO oldMembership,
            final UserTO userTO) {

        boolean found = false;

        /*Check if the current resource was existent before the update and
        in this case just ignore it*/
        for (MembershipTO newMembership : userTO.getMemberships()) {
            if (newMembership.getId() == oldMembership.getId()) {
                found = true;
            }
        }

        if (!found) {
            if (userMod == null) {
                userMod = new UserMod();
            }

            userMod.addMembershipToBeRemoved(oldMembership.getId());
        }
    }

    private void setDerivedAttributeContainer(
            final Form userForm,
            final UserTO userTO,
            final IModel<List<String>> derivedSchemaNames) {
        final WebMarkupContainer derivedAttributesContainer =
                new WebMarkupContainer("derivedAttributesContainer");
        derivedAttributesContainer.setOutputMarkupId(true);
        userForm.add(derivedAttributesContainer);

        AjaxButton addDerivedAttributeBtn = new IndicatingAjaxButton(
                "addDerivedAttributeBtn",
                new Model(getString("addDerivedAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                userTO.getDerivedAttributes().add(new AttributeTO());
                target.addComponent(derivedAttributesContainer);
            }
        };
        addDerivedAttributeBtn.setDefaultFormProcessing(false);
        userForm.add(addDerivedAttributeBtn);

        ListView<AttributeTO> derivedAttributes = new ListView<AttributeTO>(
                "derivedAttributes", userTO.getDerivedAttributes()) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO derivedAttributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        userTO.getDerivedAttributes().remove(derivedAttributeTO);
                        item.getParent().removeAll();
                        target.addComponent(derivedAttributesContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> derivedSchemaChoice =
                        new DropDownChoice<String>(
                        "schema",
                        new PropertyModel<String>(derivedAttributeTO, "schema"),
                        derivedSchemaNames);

                derivedSchemaChoice.setOutputMarkupId(true);

                if (derivedAttributeTO.getSchema() != null) {
                    item.add(derivedSchemaChoice.setEnabled(Boolean.FALSE));
                } else {
                    item.add(derivedSchemaChoice.setRequired(true));
                }

                final List<String> values = derivedAttributeTO.getValues();

                if (values == null || values.isEmpty()) {
                    item.add(new TextField(
                            "derivedAttributeValue",
                            new Model(null)).setVisible(Boolean.FALSE));
                } else {
                    item.add(new TextField(
                            "derivedAttributeValue",
                            new Model(values.get(0))).setEnabled(
                            Boolean.FALSE));
                }
            }
        };
        derivedAttributes.setReuseItems(true);
        derivedAttributesContainer.add(derivedAttributes);
    }

    private void setVirtualAttributeContainer(
            final Form userForm,
            final UserTO userTO,
            final IModel<List<String>> virtualSchemaNames) {
        final WebMarkupContainer virtualAttributesContainer =
                new WebMarkupContainer("virtualAttributesContainer");
        virtualAttributesContainer.setOutputMarkupId(true);
        userForm.add(virtualAttributesContainer);

        AjaxButton addVirtualAttributeBtn = new IndicatingAjaxButton(
                "addVirtualAttributeBtn",
                new Model(getString("addVirtualAttributeBtn"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                userTO.getVirtualAttributes().add(new AttributeTO());
                target.addComponent(virtualAttributesContainer);
            }
        };
        addVirtualAttributeBtn.setDefaultFormProcessing(false);
        userForm.add(addVirtualAttributeBtn);

        ListView<AttributeTO> virtulaAttributes = new ListView<AttributeTO>(
                "virtualAttributes", userTO.getVirtualAttributes()) {

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO virtualAttributeTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        userTO.getVirtualAttributes().remove(virtualAttributeTO);
                        item.getParent().removeAll();
                        target.addComponent(virtualAttributesContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> virtualSchemaChoice =
                        new DropDownChoice<String>(
                        "schema",
                        new PropertyModel<String>(virtualAttributeTO, "schema"),
                        virtualSchemaNames);

                virtualSchemaChoice.setOutputMarkupId(true);

                if (virtualAttributeTO.getSchema() != null) {
                    item.add(virtualSchemaChoice.setEnabled(Boolean.FALSE));
                } else {
                    item.add(virtualSchemaChoice.setRequired(true));
                }

                final List<String> values = virtualAttributeTO.getValues();

                if (values == null || values.isEmpty()) {
                    item.add(new TextField(
                            "virtualAttributeValue",
                            new Model(null)).setVisible(Boolean.FALSE));
                } else {
                    item.add(new TextField(
                            "virtualAttributeValue",
                            new Model(values.get(0))).setEnabled(
                            Boolean.FALSE));
                }
            }
        };
        virtulaAttributes.setReuseItems(true);
        virtualAttributesContainer.add(virtulaAttributes);
    }
}
