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

import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.pages.panels.RolePanel;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.util.AttributableOperations;
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

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1732493223434085205L;

    @SpringBean
    private RoleRestClient roleRestClient;

    private RoleTO originalRoleTO;

    public RoleModalPage(final PageReference callerPageRef, final ModalWindow window, final RoleTO roleTO) {

        super();

        final boolean createFlag = roleTO.getId() == 0;
        if (!createFlag) {
            originalRoleTO = AttributableOperations.clone(roleTO);
        }

        final Form form = new Form("RoleForm");

        add(new Label("displayName", roleTO.getId() != 0
                ? roleTO.getDisplayName()
                : ""));

        form.setModel(new CompoundPropertyModel(roleTO));

        final RolePanel rolePanel = new RolePanel("rolePanel", form, roleTO);
        form.add(rolePanel);

        final AjaxButton submit = new IndicatingAjaxButton("submit", new ResourceModel("submit")) {
            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                final RoleTO roleTO = (RoleTO) form.getDefaultModelObject();
                try {
                    final List<String> entitlementList = new ArrayList<String>(rolePanel.getEntitlementsPalette()
                            .getModelCollection());
                    roleTO.setEntitlements(entitlementList);

                    if (createFlag) {
                        roleRestClient.create(roleTO);
                    } else {
                        RoleMod roleMod = AttributableOperations.diff(roleTO, originalRoleTO);

                        // update role just if it is changed
                        if (!roleMod.isEmpty()) {
                            roleRestClient.update(roleMod);
                        }
                    }
                    ((Roles) callerPageRef.getPage()).setModalResult(true);

                    window.close(target);
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

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {
            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
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
        add(new CloseOnESCBehavior(window));
    }
}
