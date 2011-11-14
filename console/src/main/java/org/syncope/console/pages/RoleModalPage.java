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

import java.util.ArrayList;
import java.util.List;
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
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.console.rest.RoleRestClient;
import org.syncope.console.pages.panels.RoleAttributesPanel;

/**
 * Modal window with Role form.
 */
public class RoleModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1732493223434085205L;

    @SpringBean
    private RoleRestClient roleRestClient;

    private RoleTO originalRoleTO;

    public RoleModalPage(final PageReference callerPageRef,
            final ModalWindow window, final RoleTO roleTO) {

        super();

        final boolean createFlag = roleTO.getId() == 0;
        if (!createFlag) {
            originalRoleTO = AttributableOperations.clone(roleTO);
        }

        final Form form = new Form("RoleForm");

        add(new Label("displayName",
                roleTO.getId() != 0 ? roleTO.getDisplayName() : ""));

        form.setModel(new CompoundPropertyModel(roleTO));

        final RoleAttributesPanel attributesPanel =
                new RoleAttributesPanel("attributesPanel", form, roleTO);

        form.add(attributesPanel);

        final AjaxButton submit = new IndicatingAjaxButton(
                "submit", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                final RoleTO roleTO = (RoleTO) form.getDefaultModelObject();
                try {
                    final List<String> entitlementList =
                            new ArrayList<String>(
                            attributesPanel.getEntitlementsPalette().
                            getModelCollection());
                    roleTO.setEntitlements(entitlementList);

                    if (createFlag) {
                        roleRestClient.createRole(roleTO);
                    } else {
                        RoleMod roleMod = AttributableOperations.diff(
                                roleTO, originalRoleTO);

                        // update role just if it is changed
                        if (!roleMod.isEmpty()) {
                            roleRestClient.updateRole(roleMod);
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
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Roles", "create")
                : xmlRolesReader.getAllAllowedRoles("Roles", "update");
        MetaDataRoleAuthorizationStrategy.authorize(
                submit, ENABLE, allowedRoles);

        form.add(submit);

        add(form);
    }
}
