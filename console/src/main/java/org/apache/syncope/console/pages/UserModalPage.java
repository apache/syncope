/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages;

import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.AttributesPanel;
import org.apache.syncope.console.pages.panels.DerivedAttributesPanel;
import org.apache.syncope.console.pages.panels.MembershipsPanel;
import org.apache.syncope.console.pages.panels.ResourcesPanel;
import org.apache.syncope.console.pages.panels.UserDetailsPanel;
import org.apache.syncope.console.pages.panels.VirtualAttributesPanel;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with User form.
 */
public abstract class UserModalPage extends BaseModalPage {

    public enum Mode {

        ADMIN,
        SELF,
        TEMPLATE;

    }

    private static final long serialVersionUID = 5002005009737457667L;

    protected final PageReference pageRef;

    protected final ModalWindow window;

    protected UserTO userTO;

    protected final Mode mode;

    private Fragment fragment = null;

    private final boolean resetPassword;

    public UserModalPage(final PageReference callerPageRef, final ModalWindow window, final UserTO userTO,
            final Mode mode, final boolean resetPassword) {

        super();

        this.pageRef = callerPageRef;
        this.window = window;
        this.userTO = userTO;
        this.mode = mode;
        this.resetPassword = resetPassword;

        fragment = new Fragment("userModalFrag", "userModalEditFrag", this);
        fragment.setOutputMarkupId(true);
        add(fragment);
    }

    public UserTO getUserTO() {
        return userTO;
    }

    public void setUserTO(final UserTO userTO) {
        this.userTO = userTO;
    }

    protected Form setupEditPanel() {
        fragment.add(new Label("id", userTO.getId() == 0
                ? ""
                : userTO.getUsername()));

        fragment.add(new Label("new", userTO.getId() == 0
                ? new ResourceModel("new")
                : new Model("")));

        final Form form = new Form("UserForm");
        form.setModel(new CompoundPropertyModel(userTO));

        //--------------------------------
        // User details
        //--------------------------------
        form.add(new UserDetailsPanel("details", userTO, form, resetPassword, mode == Mode.TEMPLATE));

        form.add(new Label("statuspanel", ""));

        form.add(new Label("pwdChangeInfo", ""));

        form.add(new Label("accountinformation", ""));
        //--------------------------------

        //--------------------------------
        // Attributes panel
        //--------------------------------
        form.add(new AttributesPanel("attributes", userTO, form, mode == Mode.TEMPLATE));
        //--------------------------------

        //--------------------------------
        // Derived attributes panel
        //--------------------------------
        form.add(new DerivedAttributesPanel("derivedAttributes", userTO));
        //--------------------------------

        //--------------------------------
        // Virtual attributes panel
        //--------------------------------
        form.add(new VirtualAttributesPanel("virtualAttributes", userTO, mode == Mode.TEMPLATE));
        //--------------------------------

        //--------------------------------
        // Resources panel
        //--------------------------------
        form.add(new ResourcesPanel.Builder("resources")
                .attributableTO(userTO).statusPanel(null).build());
        //--------------------------------

        //--------------------------------
        // Roles panel
        //--------------------------------
        form.add(new MembershipsPanel("memberships", userTO, mode == Mode.TEMPLATE, null, getPageReference()));
        //--------------------------------

        final AjaxButton submit = getOnSubmit();

        if (mode == Mode.ADMIN) {
            String allowedRoles = userTO.getId() == 0
                    ? xmlRolesReader.getAllAllowedRoles("Users", "create")
                    : xmlRolesReader.getAllAllowedRoles("Users", "update");
            MetaDataRoleAuthorizationStrategy.authorize(submit, RENDER, allowedRoles);
        }

        fragment.add(form);
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new AjaxButton("cancel", new ResourceModel("cancel")) {

            private static final long serialVersionUID = 530608535790823587L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        return form;
    }

    protected AjaxButton getOnSubmit() {
        return new IndicatingAjaxButton("apply", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    closeAction(target, form);
                } catch (Exception e) {
                    LOG.error("While creating or updating user", e);
                    error(getString(Constants.ERROR) + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };
    }

    protected abstract void submitAction(final AjaxRequestTarget target, final Form form);

    protected abstract void closeAction(final AjaxRequestTarget target, final Form form);
}
