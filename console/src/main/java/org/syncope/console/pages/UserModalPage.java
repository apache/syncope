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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.UserRequestTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.pages.panels.AttributesPanel;
import org.syncope.console.pages.panels.DerivedAttributesPanel;
import org.syncope.console.pages.panels.ResourcesPanel;
import org.syncope.console.pages.panels.RolesPanel;
import org.syncope.console.pages.panels.UserDetailsPanel;
import org.syncope.console.pages.panels.UserSummaryPanel;
import org.syncope.console.pages.panels.VirtualAttributesPanel;
import org.syncope.console.rest.TaskRestClient;
import org.syncope.console.rest.UserRequestRestClient;
import org.syncope.console.rest.UserRestClient;

/**
 * Modal window with User form.
 */
public class UserModalPage extends BaseModalPage {

    public enum Mode {

        ADMIN,
        SELF,
        TEMPLATE;

    }

    private static final long serialVersionUID = 5002005009737457667L;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private UserRequestRestClient requestRestClient;

    @SpringBean
    private TaskRestClient taskRestClient;

    private final PageReference callerPageRef;

    private final ModalWindow window;

    private UserRequestTO userRequestTO;

    private UserTO userTO;

    private SyncTaskTO syncTaskTO;

    private Mode mode = Mode.ADMIN;

    private UserTO initialUserTO;

    private UserTO summaryUserTO;

    private Fragment fragment;

    private boolean submitted = false;

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window,
            final UserRequestTO userRequestTO) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.userRequestTO = userRequestTO;
        this.mode = Mode.SELF;

        setupModalPage();
    }

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window,
            final SyncTaskTO syncTaskTO) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.syncTaskTO = syncTaskTO;
        this.mode = Mode.TEMPLATE;

        setupModalPage();
    }

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window, final UserTO userTO,
            final Mode mode) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.userTO = userTO;
        this.mode = mode == null ? Mode.ADMIN : mode;

        setupModalPage();
    }

    public UserModalPage(final PageReference callerPageRef,
            final ModalWindow window,
            final UserTO summaryUserTO, final boolean submitted) {

        super();

        this.callerPageRef = callerPageRef;
        this.window = window;
        this.summaryUserTO = summaryUserTO;
        this.submitted = submitted;

        setupModalPage();
    }

    public UserTO getUserTO() {
        return userTO;
    }

    public void setUserTO(final UserTO userTO) {
        this.userTO = userTO;
    }

    private void setupModalPage() {

        fragment = new Fragment("userPanel",
                !submitted ? "editPanel" : "summaryPanel", this);
        fragment.setOutputMarkupId(true);

        if (!submitted) {

            if (userRequestTO != null) {
                switch (userRequestTO.getType()) {
                    case CREATE:
                        userTO = userRequestTO.getUserTO();
                        break;

                    case UPDATE:
                        initialUserTO = userRestClient.read(
                                userRequestTO.getUserMod().getId());
                        userTO = AttributableOperations.apply(
                                initialUserTO, userRequestTO.getUserMod());
                        break;

                    case DELETE:
                    default:
                }
            }
            if (syncTaskTO != null) {
                userTO = syncTaskTO.getUserTemplate();
            }

            if (initialUserTO == null && userTO.getId() > 0) {
                initialUserTO = AttributableOperations.clone(userTO);
            }

            fragment.add(new Label("id", userTO.getId() == 0
                    ? "" : userTO.getUsername()));
            fragment.add(new Label("new", userTO.getId() == 0
                    ? getString("new") : ""));

            final Form form = new Form("UserForm");
            form.setModel(new CompoundPropertyModel(userTO));

            //--------------------------------
            // User details
            //--------------------------------
            form.add(new UserDetailsPanel("details", userTO, form,
                    userRequestTO == null, mode == Mode.TEMPLATE));
            //--------------------------------

            //--------------------------------
            // Attributes panel
            //--------------------------------
            form.add(new AttributesPanel("attributes", userTO, form,
                    mode == Mode.TEMPLATE));
            //--------------------------------

            //--------------------------------
            // Derived attributes panel
            //--------------------------------
            form.add(new DerivedAttributesPanel("derivedAttributes", userTO));
            //--------------------------------

            //--------------------------------
            // Virtual attributes panel
            //--------------------------------
            form.add(new VirtualAttributesPanel("virtualAttributes", userTO,
                    mode == Mode.TEMPLATE));
            //--------------------------------

            //--------------------------------
            // Resources panel
            //--------------------------------
            form.add(new ResourcesPanel("resources", userTO));
            //--------------------------------

            //--------------------------------
            // Roles panel
            //--------------------------------
            form.add(new RolesPanel("roles", userTO, mode == Mode.TEMPLATE));
            //--------------------------------

            final AjaxButton submit = new IndicatingAjaxButton(
                    "apply", new ResourceModel("submit")) {

                private static final long serialVersionUID =
                        -958724007591692537L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target,
                        final Form form) {

                    final UserTO updatedUserTO = (UserTO) form.getModelObject();
                    try {
                        if (updatedUserTO.getId() == 0) {
                            switch (mode) {
                                case SELF:
                                    requestRestClient.requestCreate(
                                            updatedUserTO);
                                    break;

                                case ADMIN:
                                default:
                                    summaryUserTO = userRestClient.create(
                                            updatedUserTO);
                                    if (userRequestTO != null) {
                                        requestRestClient.delete(
                                                userRequestTO.getId());
                                    }
                                    break;

                                case TEMPLATE:
                                    syncTaskTO.setUserTemplate(updatedUserTO);
                                    taskRestClient.updateSyncTask(syncTaskTO);
                                    break;

                            }
                        } else {
                            UserMod userMod = AttributableOperations.diff(
                                    updatedUserTO, initialUserTO);

                            // update user just if it is changed
                            if (!userMod.isEmpty()) {
                                if (mode == Mode.SELF) {
                                    requestRestClient.requestUpdate(userMod);
                                } else {
                                    summaryUserTO =
                                            userRestClient.update(userMod);
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

                        setResponsePage(new UserModalPage(callerPageRef,
                                window, summaryUserTO, form.isSubmitted()));
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

            if (mode == Mode.ADMIN) {
                String allowedRoles = userTO.getId() == 0
                        ? xmlRolesReader.getAllAllowedRoles("Users", "create")
                        : xmlRolesReader.getAllAllowedRoles("Users", "update");
                MetaDataRoleAuthorizationStrategy.authorize(
                        submit, RENDER, allowedRoles);
            }

            fragment.add(form);
            form.add(submit);

        } else {
            final UserSummaryPanel userSummaryPanel =
                    new UserSummaryPanel("userSummaryPanel",
                    window, summaryUserTO);

            userSummaryPanel.setOutputMarkupId(true);
            fragment.add(userSummaryPanel);
        }

        add(fragment);
    }
}
