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

import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.console.pages.RoleModalPage;
import org.apache.syncope.console.pages.Roles;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;

/**
 * Panel for a node element form.
 */
public class TreeActionLinkPanel extends Panel {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TreeActionLinkPanel.class);

    private static final long serialVersionUID = -7292448006463567909L;

    @SpringBean
    private RoleRestClient restClient;

    @SpringBean
    private XMLRolesReader xmlRolesReader;

    private Fragment fragment;

    public TreeActionLinkPanel(final String id, final long idRole, final IModel inputModel, final ModalWindow window,
            final PageReference callerPageRef) {

        super(id);

        fragment = new Fragment("menuPanel", idRole == 0
                ? "fakerootFrag"
                : "roleFrag", this);

        AjaxLink createRoleLink = new IndicatingAjaxLink("createRoleLink") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        RoleTO roleTO = new RoleTO();
                        roleTO.setParent(idRole);
                        RoleModalPage form = new RoleModalPage(callerPageRef, window, roleTO);
                        return form;
                    }
                });

                window.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles("Roles",
                "create"));

        fragment.add(createRoleLink);

        if (idRole != 0) {
            AjaxLink updateRoleLink = new IndicatingAjaxLink("updateRoleLink") {

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    window.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            RoleTO roleTO = restClient.read(idRole);
                            RoleModalPage form = new RoleModalPage(callerPageRef, window, roleTO);
                            return form;
                        }
                    });

                    window.show(target);
                }
            };

            MetaDataRoleAuthorizationStrategy.authorize(updateRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Roles", "read"));

            fragment.add(updateRoleLink);

            AjaxLink dropRoleLink = new IndicatingDeleteOnConfirmAjaxLink("dropRoleLink") {

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        restClient.delete(idRole);
                        getSession().info(getString("operation_succeded"));
                    } catch (SyncopeClientCompositeErrorException e) {
                        LOG.error("While deleting role " + idRole, e);
                        getSession().error(getString("operation_error"));
                    }

                    setResponsePage(new Roles(null));
                }
            };

            MetaDataRoleAuthorizationStrategy.authorize(dropRoleLink, ENABLE, xmlRolesReader.getAllAllowedRoles(
                    "Roles", "delete"));

            fragment.add(dropRoleLink);
        }

        add(fragment);
    }
}
