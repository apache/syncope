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

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.pages.panels.AttributesPanel;
import org.syncope.console.pages.panels.DerivedAttributesPanel;
import org.syncope.console.pages.panels.ResourcesPanel;
import org.syncope.console.pages.panels.RolesPanel;
import org.syncope.console.pages.panels.UserDetailsPanel;
import org.syncope.console.pages.panels.VirtualAttributesPanel;
import org.syncope.console.rest.UserRequestRestClient;
import org.syncope.console.rest.UserRestClient;

/**
 * Modal window with User form.
 */
public class UserModalPage extends BaseModalPage {

    private static final long serialVersionUID = 5002005009737457667L;

    @SpringBean
    private UserRestClient restClient;

    @SpringBean
    private UserRequestRestClient requestRestClient;

    private final PageReference callerPageRef;

    private final ModalWindow window;

    private UserRequestTO userRequestTO;

    private UserTO userTO;

    private boolean self = false;

    private UserTO initialUserTO;

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window,
            final UserRequestTO userRequestTO) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.userRequestTO = userRequestTO;

        setupModalPage();
    }

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window, final UserTO userTO,
            final boolean self) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.userTO = userTO;
        this.self = self;

        setupModalPage();
    }

    public UserTO getUserTO() {
        return userTO;
    }

    public void setUserTO(final UserTO userTO) {
        this.userTO = userTO;
    }

    private void setupModalPage() {
        if (userRequestTO != null) {
            switch (userRequestTO.getType()) {
                case CREATE:
                    userTO = userRequestTO.getUserTO();
                    break;

                case UPDATE:
                    initialUserTO = restClient.read(
                            userRequestTO.getUserMod().getId());
                    userTO = AttributableOperations.apply(
                            initialUserTO, userRequestTO.getUserMod());
                    break;

                case DELETE:
                default:
            }
        }

        if (initialUserTO == null && userTO.getId() > 0) {
            initialUserTO = AttributableOperations.clone(userTO);
        }

        add(new Label("id", userTO.getId() == 0
                ? "" : userTO.getUsername()));
        add(new Label("new", userTO.getId() == 0
                ? getString("new") : ""));

        final Form form = new Form("UserForm");

        form.setModel(new CompoundPropertyModel(userTO));

        //--------------------------------
        // User details
        //--------------------------------
        form.add(new UserDetailsPanel("details", userTO, form,
                userRequestTO == null));
        //--------------------------------

        //--------------------------------
        // Attributes panel
        //--------------------------------
        form.add(new AttributesPanel("attributes", userTO, form));
        //--------------------------------

        //--------------------------------
        // Derived attributes panel
        //--------------------------------
        form.add(new DerivedAttributesPanel("derivedAttributes", userTO));
        //--------------------------------

        //--------------------------------
        // Virtual attributes panel
        //--------------------------------
        form.add(new VirtualAttributesPanel("virtualAttributes", userTO));
        //--------------------------------

        //--------------------------------
        // Resources panel
        //--------------------------------
        form.add(new ResourcesPanel("resources", userTO));
        //--------------------------------

        //--------------------------------
        // Roles panel
        //--------------------------------
        form.add(new RolesPanel("roles", userTO));
        //--------------------------------

        final AjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                final UserTO updatedUserTO = (UserTO) form.getModelObject();
                try {
                    if (updatedUserTO.getId() == 0) {
                        if (self) {
                            requestRestClient.requestCreate(updatedUserTO);
                        } else {
                            restClient.create(updatedUserTO);
                            if (userRequestTO != null) {
                                requestRestClient.delete(userRequestTO.getId());
                            }
                        }
                    } else {
                        UserMod userMod = AttributableOperations.diff(
                                updatedUserTO, initialUserTO);

                        // update user just if it is changed
                        if (!userMod.isEmpty()) {
                            if (self) {
                                requestRestClient.requestUpdate(userMod);
                            } else {
                                restClient.update(userMod);
                                if (userRequestTO != null) {
                                    requestRestClient.delete(
                                            userRequestTO.getId());
                                }
                            }
                        }
                    }

                    if (callerPageRef.getPage() instanceof BasePage) {
                        ((BasePage) callerPageRef.getPage()).setModalResult(
                                true);
                    }

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    LOG.error("While creating or updating user", e);
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

        if (!self) {
            String allowedRoles = userTO.getId() == 0
                    ? xmlRolesReader.getAllAllowedRoles("Users", "create")
                    : xmlRolesReader.getAllAllowedRoles("Users", "update");
            MetaDataRoleAuthorizationStrategy.authorize(
                    submit, RENDER, allowedRoles);
        }

        form.add(submit);

        add(form);
    }
}
