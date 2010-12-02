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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata
        .MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.commons.SchemaWrapper;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.rest.RolesRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UsersRestClient;

import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateFieldPanel;
import org.syncope.console.wicket.markup.html.tree.SyncopeRoleTree;
import org.syncope.console.wicket.markup.html.tree.TreeModelBean;

/**
 * Modal window with User form.
 */
public class UserModalPage extends SyncopeModalPage {

@SpringBean(name = "usersRestClient")
UsersRestClient usersRestClient;

@SpringBean(name = "rolesRestClient")
RolesRestClient rolesRestClient;

WebMarkupContainer container;
WebMarkupContainer membershipsContainer;

AjaxButton submit;

List<SchemaWrapper> schemaWrappers;
List<MembershipTO> membershipTOs;

final ModalWindow createUserWin;

UserTO oldUser;
UserMod userMod;

Map rolesMap;

/**
 *
 * @param basePage base
 * @param modalWindow modal window
 * @param connectorTO
 * @param create : set to true only if a CREATE operation is required
 */
public UserModalPage(final BasePage basePage, final ModalWindow window,
    final UserTO userTO, final boolean createFlag) {

if (!createFlag) {
    cloneOldUserTO(userTO);
}

setupRolesMap();

add(createUserWin = new ModalWindow("membershipWin"));

createUserWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
createUserWin.setPageMapName("create-membership-modal");
createUserWin.setCookieName("create-membership-modal");

final Form userForm = new Form("UserForm");

userForm.setModel(new CompoundPropertyModel(userTO));

setupSchemaWrappers(createFlag, userTO);
setupMemberships(createFlag, userTO);

final ListView userAttributesView = new ListView("userSchemas",
        schemaWrappers) {

@Override
protected void populateItem(ListItem item) {
final SchemaWrapper schemaWrapper =
        (SchemaWrapper) item.getDefaultModelObject();

final SchemaTO schemaTO = schemaWrapper.getSchemaTO();

item.add(new Label("name", schemaWrapper.getSchemaTO()
        .getName()));

item.add(new ListView("fields", schemaWrapper.getValues()) {

Panel panel;

    @Override
    protected void populateItem(final ListItem item) {

        String mandatoryCondition = schemaTO.getMandatoryCondition();
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
            }, required,schemaTO.isReadonly());
        }
        else if (schemaTO.getType().getClassName()
                .equals("java.lang.Boolean")) {
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

        } else if (schemaTO.getType().getClassName().equals("java.util.Date")) {
            panel = new DateFieldPanel("panel", schemaTO.getName(),
                    new Model() {

                        @Override
                        public Serializable getObject() {
                            DateFormat formatter = new SimpleDateFormat(
                                    schemaTO.getConversionPattern());
                            Date date = new Date();
                            try {
                                String dateValue = (String) item
                                        .getModelObject();
                                //Default value:yyyy-MM-dd
                                if(!dateValue.equals(""))
                                    date = formatter.parse(dateValue);
                                else
                                    date = null;
                            } catch (ParseException ex) {
                                Logger.getLogger(
                                        RoleModalPage.class.getName())
                                        .log(Level.SEVERE, null, ex);
                            }
                            return date;
                        }

                        @Override
                        public void setObject(Serializable object) {
                            Date date = (Date) object;
                            Format formatter = new SimpleDateFormat(schemaTO
                                    .getConversionPattern());
                            String val = formatter.format(date);
                            item.setModelObject(val);
                        }
                    }, schemaTO.getConversionPattern(),required,
                            schemaTO.isReadonly(),userForm);
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
            }, required,schemaTO.isReadonly());
        }

        item.add(panel);
    }
    });

        AjaxButton addButton = new AjaxButton("add",
                new Model(getString("add"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    Form form) {
                schemaWrapper.getValues().add("");

                target.addComponent(container);
            }
        };

        AjaxButton dropButton = new AjaxButton("drop",
                new Model(getString("drop"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target,
                    Form form) {
                //Drop the last component added
                schemaWrapper.getValues().remove(schemaWrapper
                        .getValues().size() - 1);

                target.addComponent(container);
            }
        };

        if (schemaTO.getType().getClassName()
                .equals("java.lang.Boolean")) {
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

        if(schemaTO.isReadonly()) {
            addButton.setEnabled(false);
            dropButton.setEnabled(false);
        }

        item.add(addButton);
        item.add(dropButton);
    }
};

userForm.add(userAttributesView);

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
password.setResetPassword(false);
container.add(password);

container.setOutputMarkupId(true);

userForm.add(container);

submit = new IndicatingAjaxButton ("submit", new Model(getString("submit"))){

    @Override
    protected void onSubmit(AjaxRequestTarget target, Form form) {
        UserTO userTO = (UserTO) form.getDefaultModelObject();

        boolean res = false;

        try {
            userTO.setResources(getResourcesSet(resourcesPalette
                    .getModelCollection()));
            userTO.setAttributes(getUserAttributesList());
            userTO.setMemberships(getMembershipsSet());

            if (createFlag) {
                usersRestClient.createUser(userTO);
            } else {
                setupUserMod(userTO);

                //Update user just if it is changed
                if(userMod != null){

                res = usersRestClient.updateUser(userMod);

                if (!res)
                    error(getString("error_updating"));

                Users callerPage = (Users) basePage;
                callerPage.setOperationResult(true);
                }

            }

            window.close(target);

        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
            error(getString("error") + ":" + e.getMessage());
        }
    }

    @Override
    protected void onError(AjaxRequestTarget target, Form form) {
        target.addComponent(form.get("feedback"));
    }

//    @Override
//    protected IAjaxCallDecorator getAjaxCallDecorator() {
//        return new AjaxPostprocessingCallDecorator(super.getAjaxCallDecorator()) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public CharSequence postDecorateScript(CharSequence script) {
//                return script + "for(i = 0;i<document.forms[0].length;i++) " +
//                        "{document.forms[0].elements[i].disabled ='disabled'}";
//            }
//        };
//    }
};

String allowedRoles = null;

if(createFlag)
    allowedRoles = xmlRolesReader.getAllAllowedRoles("Users","create");
else
    allowedRoles = xmlRolesReader.getAllAllowedRoles("Users","update");

MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, allowedRoles);

userForm.add(submit);

userForm.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

//Roles Tab
SyncopeRoleTree roleTree = new SyncopeRoleTree(rolesRestClient);

BaseTree tree;

tree = new LinkTree("treeTable", roleTree.createTreeModel()) {

    @Override
    protected IModel<Object> getNodeTextModel(IModel<Object> model) {
        return new PropertyModel(model, "userObject.treeNode.displayName");
    }

    @Override
    protected void onNodeLinkClicked(final Object node,
            final BaseTree tree, final AjaxRequestTarget target) {

        DefaultMutableTreeNode syncopeTreeNode =
                (DefaultMutableTreeNode) node;
        final TreeModelBean treeModel = (TreeModelBean) syncopeTreeNode
                .getUserObject();

        if (treeModel.getTreeNode() != null) {

            createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                MembershipTO membershipTO;

                @Override
                public Page createPage() {

                    membershipTO = new MembershipTO();
                    membershipTO.setRoleId(treeModel.getTreeNode()
                            .getId());
                    String title = treeModel.getTreeNode().getName();

                    MembershipModalPage form =
                            new MembershipModalPage(getPage(),
                            createUserWin, membershipTO, true);

                    return form;
                }
            });
            createUserWin.show(target);
        }
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

        item.add(new Label("roleId", new Model(membershipTO.getRoleId())));
        item.add(new Label("roleName", new Model((String) rolesMap
                .get(membershipTO.getRoleId()))));

        AjaxLink editLink = new AjaxLink("editLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                createUserWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {

                        MembershipModalPage window = new MembershipModalPage(
                                getPage(), createUserWin, membershipTO, false);

                        return window;

                    }
                });
                createUserWin.show(target);
            }
        };
        item.add(editLink);

        AjaxLink deleteLink = new AjaxLink("deleteLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
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

setWindowClosedCallback(createUserWin, membershipsContainer);

userForm.add(membershipsContainer);
add(userForm);
}

/**
 * Originals : user's resources
 * @param userTO
 * @return
 */
public List<ResourceTO> getSelectedResources(UserTO userTO) {
    List<ResourceTO> resources = new ArrayList<ResourceTO>();
    ResourceTO clusterableResourceTO;

    for (String resourceName : userTO.getResources()) {
        clusterableResourceTO = new ResourceTO();
        clusterableResourceTO.setName(resourceName);
        resources.add(clusterableResourceTO);
    }
    return resources;
}

/**
 * Destinations : available resources
 * @param userTO
 * @return
 */
public List<ResourceTO> getAvailableResources(UserTO userTO) {

    List<ResourceTO> resources = new ArrayList<ResourceTO>();

    ResourcesRestClient resourcesRestClient = (ResourcesRestClient)
            ((SyncopeApplication) Application.get()).getApplicationContext()
            .getBean("resourcesRestClient");

    List<ResourceTO> resourcesTos = resourcesRestClient.getAllResources();

    for (ResourceTO resourceTO : resourcesTos)
            resources.add(resourceTO);

    return resources;
}

/**
 * Create a copy of old userTO object.
 * @param userTO
 */
public void cloneOldUserTO(UserTO userTO) {

    oldUser = new UserTO();

    oldUser.setId(userTO.getId());
    oldUser.setPassword(userTO.getPassword());

    AttributeTO tempAttr;
    for(AttributeTO attribute : userTO.getAttributes()) {
        tempAttr = new AttributeTO();
        tempAttr.setReadonly(attribute.isReadonly());

        tempAttr.setSchema(attribute.getSchema());

        for (String tempVal : attribute.getValues() )
            tempAttr.getValues().add(tempVal);

        oldUser.getAttributes().add(tempAttr);
    }

    oldUser.setResources(userTO.getResources());
    oldUser.setMemberships(new ArrayList<MembershipTO>());

    MembershipTO membership;

    for (MembershipTO membershipTO : userTO.getMemberships()) {
        membership = new MembershipTO();
        membership.setId(membershipTO.getId());
        membership.setRoleId(membershipTO.getRoleId());
        membership.setAttributes(membershipTO.getAttributes());
        oldUser.getMemberships().add(membership);
    }
}

/**
 * Populate a roles hashmap of type (roleId,roleName)
 */
public void setupRolesMap() {

    rolesMap = new HashMap();

    List<RoleTO> roleTOs = rolesRestClient.getAllRoles();

    for (RoleTO roleTO : roleTOs) {
        rolesMap.put(roleTO.getId(), roleTO.getName());
    }
}

/**
 * Set a WindowClosedCallback for a ModalWindow instance.
 * @param window
 * @param container
 */
public void setWindowClosedCallback(ModalWindow window,
        final WebMarkupContainer container) {

    window.setWindowClosedCallback(
            new ModalWindow.WindowClosedCallback() {

                public void onClose(AjaxRequestTarget target) {
                    target.addComponent(container);
                }
            });
}

/**
 * Initialize the SchemaWrapper collection
 * @param create
 * @param userTO
 */
public void setupSchemaWrappers(boolean create, UserTO userTO) {

    schemaWrappers = new ArrayList<SchemaWrapper>();
    SchemaWrapper schemaWrapper;

    SchemaRestClient schemaRestClient = (SchemaRestClient)
            ((SyncopeApplication) Application.get()).getApplicationContext()
            .getBean("schemaRestClient");

    List<SchemaTO> schemas = schemaRestClient.getAllUserSchemas();

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

/**
 * Initialize the membershipTOs
 * @param creation flag: true if a new User is being created, false otherwise
 * @param userTO object
 */
public void setupMemberships(boolean create, UserTO userTO) {

    membershipTOs = new ArrayList<MembershipTO>();

    if (!create) {
        List<MembershipTO> memberships = userTO.getMemberships();

        for (MembershipTO membership : memberships) {
            membershipTOs.add(membership);
        }
    }
}

/**
 * Initialize the user's attributes
 * @param creation flag: true if a new User is being created, false otherwise
 * @param userTO object
 */
public List<AttributeTO> getUserAttributesList() {

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
public List<MembershipTO> getMembershipsSet() {

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
public Set<String> getResourcesSet(Collection<ResourceTO> resourcesList) {
    Set<String> resourcesSet = new HashSet<String>();

   for (ResourceTO resourceTO : resourcesList) {
        resourcesSet.add(resourceTO.getName());
    }

    return resourcesSet;
}

public List<MembershipTO> getMembershipTOs() {
    return membershipTOs;
}

public void setMembershipTOs(List<MembershipTO> membershipTOs) {
    this.membershipTOs = membershipTOs;
}

/**
 * Updates the modified user object.
 * @param updated userTO
 * @return
 */
public void setupUserMod(UserTO userTO) {

    //1.Check if the password has been changed and update it
    if (!oldUser.getPassword().equals(userTO.getPassword())) {
        userMod = new UserMod();
        userMod.setPassword(userTO.getPassword());
    }

    //2.Update user's schema attributes
    for (AttributeTO attributeTO : userTO.getAttributes()) {
        searchAndUpdateAttribute(attributeTO);
    }

    //3.Update user's resources
    for (String resource : userTO.getResources()) {
        searchAndAddResource(resource);
    }


    for (String resource : oldUser.getResources()) {
        searchAndDropResource(resource, userTO);
    }

    //4.Update user's memberships
    for (MembershipTO membership : userTO.getMemberships()) {
        searchAndUpdateMembership(membership);
    }

    for (MembershipTO membership : oldUser.getMemberships()) {
        searchAndDropMembership(membership, userTO);
    }

    if(userMod != null)
        userMod.setId(oldUser.getId());
}

public void searchAndUpdateAttribute(AttributeTO attributeTO) {
    boolean found = false;
    boolean changed = false;

    AttributeMod attributeMod = new AttributeMod();
    attributeMod.setSchema(attributeTO.getSchema());

    for (AttributeTO oldAttribute : oldUser.getAttributes()) {
        if (attributeTO.getSchema().equals(oldAttribute.getSchema())) {

            if (!attributeTO.equals(oldAttribute) && !oldAttribute
                    .isReadonly()) {

                if (attributeTO.getValues().size() > 1)
                    attributeMod.setValuesToBeAdded(attributeTO.getValues());
                else
                    attributeMod.addValueToBeAdded(attributeTO.getValues()
                            .iterator().next());

                if(userMod == null)
                    userMod = new UserMod();

                userMod.addAttributeToBeRemoved(oldAttribute.getSchema());
                userMod.addAttributeToBeUpdated(attributeMod);

                changed = true;
                break;
            }
                found = true;
        }
    }

    if (!found & !changed && !attributeTO.isReadonly() &&
            attributeTO.getValues() != null) {

        if(attributeTO.getValues().iterator().next() != null ){
            attributeMod.setValuesToBeAdded(attributeTO.getValues());

        if(userMod == null)
            userMod = new UserMod();

            userMod.addAttributeToBeUpdated(attributeMod);
        }
        else attributeMod = null;
    }
}

/**
 * Search for a resource and add that one to the UserMod object if
 * it doesn't exist.
 * @param resource, new resource added
 */
public void searchAndAddResource(String resource) {
    boolean found = false;

    /* Check if the current resource was existent before the update and in this
     case just ignore it */
    for (String oldResource : oldUser.getResources()) {
        if (resource.equals(oldResource)) {
            found = true;
        }
    }

    if (!found) {
        if(userMod == null)
          userMod = new UserMod();

          userMod.addResourceToBeAdded(resource);
    }
}

/**
 * Search for a resource and drop that one from the UserMod object if
 * it doesn't exist anymore.
 * @param resource
 * @param userTO
 */
public void searchAndDropResource(String resource, UserTO userTO) {
    boolean found = false;

    /*Check if the current resource was existent before the update and in this 
     case just ignore it */
    for (String newResource : userTO.getResources()) {
        if (resource.equals(newResource)) {
            found = true;
        }
    }

    if (!found) {
       if(userMod == null)
           userMod = new UserMod();
        userMod.addResourceToBeRemoved(resource);
    }
}

/**
 * Update the Membership.
 * @param new membershipTO
 */
public void searchAndUpdateMembership(MembershipTO newMembership) {
    boolean found = false;
    boolean attrFound = false;

    MembershipMod membershipMod = new MembershipMod();
    membershipMod.setRole(newMembership.getRoleId());

    AttributeMod attributeMod;

    //1. If the membership exists update it
    for (MembershipTO oldMembership : oldUser.getMemberships()) {
        if (newMembership.getRoleId() == oldMembership.getRoleId()) {

            for (AttributeTO newAttribute : newMembership.getAttributes()) {
                for (AttributeTO oldAttribute : oldMembership.getAttributes()) {

                    if(oldAttribute.getSchema().equals(newAttribute.getSchema())) {

                        attributeMod = new AttributeMod();
                        attributeMod.setSchema(newAttribute.getSchema());

                        attributeMod.setValuesToBeAdded(newAttribute.getValues());

                        membershipMod.addAttributeToBeUpdated(attributeMod);
                        //membershipMod.addAttributeToBeRemoved(oldAttribute.getSchema());
                        attrFound = true;
                        break;
                    }
                }
                if(!attrFound) {
                    attributeMod = new AttributeMod();
                    attributeMod.setSchema(newAttribute.getSchema());
                    attributeMod.setValuesToBeAdded(newAttribute.getValues());
                    membershipMod.addAttributeToBeUpdated(attributeMod);
                }
                attrFound = false;
            }

            if(userMod == null)
                userMod = new UserMod();

            userMod.addMembershipToBeRemoved(oldMembership.getId());
            userMod.addMembershipToBeAdded(membershipMod);

            found = true;
            break;
        }
    }

    //2.Otherwise, if it doesn't exist, create it from scratch
    if (!found) {
        Set<AttributeMod> attributes = new HashSet<AttributeMod>();

        for (AttributeTO newAttribute : newMembership.getAttributes()) {
            attributeMod = new AttributeMod();
            attributeMod.setSchema(newAttribute.getSchema());
            attributeMod.setValuesToBeAdded(newAttribute.getValues());

            attributes.add(attributeMod);
        }

        membershipMod.setAttributesToBeUpdated(attributes);

        if(userMod == null)
           userMod = new UserMod();

        userMod.addMembershipToBeAdded(membershipMod);
    }
}

/**
 * Drop membership not present anymore.
 * @param membershipTO
 * @param userTO
 */
public void searchAndDropMembership(MembershipTO oldMembership,
        UserTO userTO) {
    boolean found = false;

    /*Check if the current resource was existent before the update and
     in this case just ignore it*/
    for (MembershipTO newMembership : userTO.getMemberships()) {
        if (newMembership.getId() == oldMembership.getId()) {
            found = true;
        }
    }

    if (!found) {
        if(userMod == null)
           userMod = new UserMod();

        userMod.addMembershipToBeRemoved(oldMembership.getId());
    }
}
}