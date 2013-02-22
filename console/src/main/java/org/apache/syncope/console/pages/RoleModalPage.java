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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.console.pages.panels.RolePanel;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    public enum Mode {

        ADMIN,
        TEMPLATE;

    }

    private static final long serialVersionUID = -1732493223434085205L;

    @SpringBean
    private RoleRestClient roleRestClient;

    protected final PageReference pageRef;

    protected final ModalWindow window;

    protected final Mode mode;

    protected final boolean createFlag;

    protected final RolePanel rolePanel;

    protected RoleTO originalRoleTO;

    public RoleModalPage(final ModalWindow window, final RoleTO roleTO) {
        this(null, window, roleTO, Mode.ADMIN);
    }

    public RoleModalPage(final PageReference pageRef, final ModalWindow window, final RoleTO roleTO) {
        this(pageRef, window, roleTO, Mode.ADMIN);
    }

    public RoleModalPage(final PageReference pageRef, final ModalWindow window, final RoleTO roleTO, final Mode mode) {
        super();

        this.pageRef = pageRef;
        this.window = window;
        this.mode = mode;

        this.createFlag = roleTO.getId() == 0;
        if (!createFlag) {
            originalRoleTO = AttributableOperations.clone(roleTO);
        }

        final Form form = new Form("RoleForm");

        add(new Label("displayName", roleTO.getId() == 0 ? "" : roleTO.getDisplayName()));

        form.setModel(new CompoundPropertyModel(roleTO));

        this.rolePanel = new RolePanel("rolePanel", form, roleTO, mode);
        form.add(rolePanel);

        final AjaxButton submit = new ClearIndicatingAjaxButton("submit", new ResourceModel("submit"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    closeAction(target, form);
                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                target.add(feedbackPanel);
            }
        };

        final AjaxButton cancel = new ClearIndicatingAjaxButton("cancel", new ResourceModel("cancel"), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                closeAction(target, form);
            }
        };

        cancel.setDefaultFormProcessing(false);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, xmlRolesReader.getAllAllowedRoles("Roles",
                createFlag
                ? "create"
                : "update"));

        form.add(submit);
        form.add(cancel);

        add(form);
    }

    protected void submitAction(final AjaxRequestTarget target, final Form form) {
        final RoleTO roleTO = (RoleTO) form.getDefaultModelObject();
        final List<String> entitlementList = new ArrayList<String>(
                rolePanel.getEntitlementsPalette().getModelCollection());
        roleTO.setEntitlements(entitlementList);

        final RoleTO result;
        if (createFlag) {
            result = roleRestClient.create(roleTO);
        } else {
            RoleMod roleMod = AttributableOperations.diff(roleTO, originalRoleTO);

            // update role just if it is changed
            if (roleMod.isEmpty()) {
                result = roleTO;
            } else {
                result = roleRestClient.update(roleMod);
            }
        }

        setResponsePage(new ResultStatusModalPage(window, result));
    }

    protected void closeAction(final AjaxRequestTarget target, final Form form) {
        window.close(target);
    }
}
