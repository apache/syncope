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

import org.syncope.console.pages.panels.AttributesPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.RoleTO;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.rest.EntitlementRestClient;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.pages.panels.DerivedAttributesPanel;
import org.syncope.console.pages.panels.ResourcesPanel;
import org.syncope.console.pages.panels.VirtualAttributesPanel;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1732493223434085205L;

    @SpringBean
    private RoleRestClient roleRestClient;

    @SpringBean
    private EntitlementRestClient entitlementRestClient;

    private AjaxButton submit;

    private RoleTO oldRole;

    private RoleMod roleMod;

    /**
     * Constructor.
     *
     * @param callerPageRef
     * @param window
     * @param roleTO
     * @param createFlag
     */
    public RoleModalPage(final PageReference callerPageRef,
            final ModalWindow window, final RoleTO roleTO,
            final boolean createFlag) {

        super();

        if (!createFlag) {
            cloneOldRoleTO(roleTO);
        }

        final Form form = new Form("RoleForm");

        add(new Label("displayName",
                roleTO.getId() != 0 ? roleTO.getDisplayName() : ""));

        form.setModel(new CompoundPropertyModel(roleTO));

        //--------------------------------
        // Attributes panel
        //--------------------------------
        final AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                "name", getString("name"),
                new PropertyModel<String>(roleTO, "name"), false);
        name.addRequiredLabel();
        form.add(name);

        form.add(new AttributesPanel("attributes", roleTO, form));

        final AjaxCheckBoxPanel inhAttributes = new AjaxCheckBoxPanel(
                "inheritAttributes",
                getString("inheritAttributes"),
                new PropertyModel<Boolean>(roleTO, "inheritAttributes"),
                false);
        form.add(inhAttributes);
        //--------------------------------

        //--------------------------------
        // Derived attributes container
        //--------------------------------
        form.add(new DerivedAttributesPanel("derivedAttributes", roleTO));

        final AjaxCheckBoxPanel inhDerivedAttributes = new AjaxCheckBoxPanel(
                "inheritDerivedAttributes",
                getString("inheritDerivedAttributes"),
                new PropertyModel<Boolean>(roleTO, "inheritDerivedAttributes"),
                false);
        inhDerivedAttributes.setOutputMarkupId(true);
        form.add(inhDerivedAttributes);
        //--------------------------------

        //--------------------------------
        // Virtual attributes container
        //--------------------------------
        form.add(new VirtualAttributesPanel("virtualAttributes", roleTO));

        final AjaxCheckBoxPanel inhVirtualAttributes = new AjaxCheckBoxPanel(
                "inheritVirtualAttributes",
                getString("inheritVirtualAttributes"),
                new PropertyModel<Boolean>(roleTO, "inheritVirtualAttributes"),
                false);
        inhVirtualAttributes.setOutputMarkupId(true);
        form.add(inhVirtualAttributes);
        //--------------------------------

        form.add(new ResourcesPanel("resources", roleTO));

        ListModel<String> selectedEntitlements =
                new ListModel<String>(roleTO.getEntitlements());

        ListModel<String> availableEntitlements =
                new ListModel<String>(
                entitlementRestClient.getAllEntitlements());

        final Palette<String> entitlementsPalette = new Palette(
                "entitlementsPalette", selectedEntitlements,
                availableEntitlements, new SelectChoiceRenderer(), 20, false);

        form.add(entitlementsPalette);

        submit = new IndicatingAjaxButton(
                "submit", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                final RoleTO roleTO = (RoleTO) form.getDefaultModelObject();

                try {

                    final List<String> entitlementList = new ArrayList<String>(
                            entitlementsPalette.getModelCollection().size());
                    for (String entitlement :
                            entitlementsPalette.getModelCollection()) {

                        entitlementList.add(entitlement);
                    }
                    roleTO.setEntitlements(entitlementList);

                    if (createFlag) {
                        roleRestClient.createRole(roleTO);
                    } else {
                        setupRoleMod(roleTO);
                        if (roleMod != null) {
                            roleRestClient.updateRole(roleMod);
                        }
                    }
                    ((Roles) callerPageRef.getPage()).setOperationResult(true);

                    window.close(target);
                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
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
     * Create a copy of old RoleTO
     * @param roleTO
     */
    private void cloneOldRoleTO(RoleTO roleTO) {
        oldRole = new RoleTO();

        oldRole.setId(roleTO.getId());
        oldRole.setName(roleTO.getName());
        oldRole.setParent(roleTO.getParent());

        List<AttributeTO> attributes = new ArrayList<AttributeTO>();

        AttributeTO attributeTO;

        for (AttributeTO attribute : roleTO.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setReadonly(attribute.isReadonly());
            attributeTO.setSchema(attribute.getSchema());

            for (String value : attribute.getValues()) {
                attributeTO.addValue(value);
            }

            attributes.add(attributeTO);
        }

        oldRole.setAttributes(attributes);

        attributes = new ArrayList<AttributeTO>();

        for (AttributeTO attribute : roleTO.getDerivedAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setReadonly(attribute.isReadonly());
            attributeTO.setSchema(attribute.getSchema());
            attributes.add(attributeTO);
        }

        oldRole.setDerivedAttributes(attributes);

        attributes = new ArrayList<AttributeTO>();

        for (AttributeTO attribute : roleTO.getVirtualAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setReadonly(attribute.isReadonly());
            attributeTO.setSchema(attribute.getSchema());
            attributes.add(attributeTO);
        }

        oldRole.setVirtualAttributes(attributes);

        for (String resource : roleTO.getResources()) {
            oldRole.addResource(resource);
        }

        List<String> entList = new ArrayList<String>();

        for (String entitlement : roleTO.getEntitlements()) {
            entList.add(entitlement);
        }

        oldRole.setEntitlements(entList);
    }

    private void setupRoleMod(final RoleTO roleTO) {
        roleMod = new RoleMod();

        roleMod.setInheritAttributes(
                roleTO.isInheritAttributes());

        roleMod.setInheritDerivedAttributes(
                roleTO.isInheritDerivedAttributes());

        roleMod.setInheritVirtualAttributes(
                roleTO.isInheritVirtualAttributes());

        //1.Check if the role's name has been changed
        if (!oldRole.getName().equals(roleTO.getName())) {
            roleMod.setName(roleTO.getName());
        }

        //2.Update roles's schema derived attributes
        final List<AttributeTO> newDerivedAttributes =
                roleTO.getDerivedAttributes();

        final List<AttributeTO> oldDerivedAttributes =
                oldRole.getDerivedAttributes();

        for (AttributeTO oldDerivedAttribute : oldDerivedAttributes) {
            roleMod.addDerivedAttributeToBeRemoved(
                    oldDerivedAttribute.getSchema());
        }

        for (AttributeTO newDerivedAttribute : newDerivedAttributes) {
            roleMod.addDerivedAttributeToBeAdded(
                    newDerivedAttribute.getSchema());
        }

        //4.Update roles's schema virtual attributes
        final List<AttributeTO> newVirtualAttributes =
                roleTO.getVirtualAttributes();

        final List<AttributeTO> oldVirtualAttributes =
                oldRole.getVirtualAttributes();

        for (AttributeTO oldVirtualAttribute : oldVirtualAttributes) {
            roleMod.addVirtualAttributeToBeRemoved(
                    oldVirtualAttribute.getSchema());
        }

        for (AttributeTO newVirtualAttribute : newVirtualAttributes) {
            roleMod.addVirtualAttributeToBeAdded(
                    newVirtualAttribute.getSchema());
        }

        //4.Search and update role's attributes
        for (AttributeTO attributeTO : roleTO.getAttributes()) {
            searchAndUpdateAttribute(attributeTO);
        }

        //5.Search and update role's resources
        for (String resource : roleTO.getResources()) {
            searchAndAddResource(resource);
        }

        for (String resource : oldRole.getResources()) {
            searchAndDropResource(resource, roleTO);
        }

        //6.Check if entitlements' list has been changed
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
    private void searchAndAddResource(String resource) {
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
    private void searchAndDropResource(final String resource,
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

    private void searchAndUpdateAttribute(AttributeTO attributeTO) {
        boolean found = false;
        boolean changed = false;

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema(attributeTO.getSchema());

        for (AttributeTO oldAttribute : oldRole.getAttributes()) {
            if (attributeTO.getSchema().equals(oldAttribute.getSchema())) {

                if (attributeTO.getSchema().equals(oldAttribute.getSchema())
                        && !attributeTO.equals(oldAttribute)
                        && !oldAttribute.isReadonly()) {

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
