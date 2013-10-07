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
package org.apache.syncope.console.wicket.markup.html.tree;

import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.ResultStatusModalPage;
import org.apache.syncope.console.pages.RoleModalPage;
import org.apache.syncope.console.pages.Roles;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.ajax.markup.html.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Panel for a node element form.
 */
public class TreeActionLinkPanel extends Panel {

    private static final long serialVersionUID = -7292448006463567909L;

    @SpringBean
    private RoleRestClient restClient;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    private Fragment fragment;

    public TreeActionLinkPanel(final String id, final long idRole, final ModalWindow window,
            final PageReference pageRef) {

        super(id);

        fragment = new Fragment("menuPanel", idRole == 0
                ? "fakerootFrag"
                : "roleFrag", this);

        fragment.setOutputMarkupId(true);

        final AjaxLink createRoleLink = new ClearIndicatingAjaxLink("createRoleLink", pageRef) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        RoleTO roleTO = new RoleTO();
                        roleTO.setParent(idRole);
                        RoleModalPage form = new RoleModalPage(pageRef, window, roleTO);
                        return form;
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles("Roles",
                "create"));

        createRoleLink.setOutputMarkupId(true);
        fragment.add(createRoleLink);

        if (idRole != 0) {
            final AjaxLink updateRoleLink = new ClearIndicatingAjaxLink("updateRoleLink", pageRef) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void onClickInternal(final AjaxRequestTarget target) {
                    window.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            RoleTO roleTO = restClient.read(idRole);
                            RoleModalPage form = new RoleModalPage(pageRef, window, roleTO);
                            return form;
                        }
                    });

                    window.show(target);
                }
            };

            MetaDataRoleAuthorizationStrategy.authorize(updateRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Roles", "read"));

            updateRoleLink.setOutputMarkupId(true);
            fragment.add(updateRoleLink);

            final AjaxLink dropRoleLink = new IndicatingOnConfirmAjaxLink("dropRoleLink", pageRef) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void onClickInternal(final AjaxRequestTarget target) {
                    try {
                        final RoleTO roleTO = (RoleTO) restClient.delete(idRole);

                        ((Roles) pageRef.getPage()).setModalResult(true);

                        window.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                return new ResultStatusModalPage.Builder(window, roleTO).build();
                            }
                        });

                        window.show(target);
                    } catch (SyncopeClientCompositeException scce) {
                        error(getString(Constants.OPERATION_ERROR) + ": " + scce.getMessage());
                        target.add(((Roles) pageRef.getPage()).getFeedbackPanel());
                    }
                }
            };

            MetaDataRoleAuthorizationStrategy.authorize(dropRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Roles", "delete"));

            dropRoleLink.setOutputMarkupId(true);
            fragment.add(dropRoleLink);
        }

        add(fragment);
    }
}
