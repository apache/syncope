/*
 *  Copyright 2010 sara.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.wicket.markup.html.tree;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.RoleTO;
import org.syncope.console.pages.BasePage;
import org.syncope.console.pages.RoleModalPage;
import org.syncope.console.pages.Roles;
import org.syncope.console.rest.RolesRestClient;

/**
 * Panel for a node element form.
 */
public class NodeEditablePanel extends Panel {
    @SpringBean(name = "rolesRestClient")
    RolesRestClient restClient;

    Fragment fragment;
    /**
     * Panel constructor.
     *
     * @param id
     *            Markup id
     * @param parentId
     *            Role id
     * @param inputModel
     *            Model of the text field
     * @param window
     *            Modal window to open
     */
    public NodeEditablePanel(String id, final Long idRole,IModel inputModel,
                               final ModalWindow window,final BasePage basePage) {
        super(id);

        if (idRole == -1)
            fragment = new Fragment("menuPanel", "frag2",this);
        else {
            fragment = new Fragment("menuPanel", "frag1",this);

        fragment.add(new AjaxLink("createRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        RoleTO roleTO = new RoleTO();
                        roleTO.setParent(idRole);
                        RoleModalPage form = new RoleModalPage
                                (basePage,window, roleTO, true);
                        return form;
                    }
                });

                window.show(target);
            }
        });

        fragment.add(new AjaxLink("updateRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                window.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        RoleTO roleTO = restClient.readRole(idRole);
                        RoleModalPage form =
                                new RoleModalPage(basePage, window, roleTO, false);
                        return form;
                    }
                });

                window.show(target);
            }
        });

        fragment.add(new AjaxLink("dropRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                restClient.deleteRole(idRole);

                getSession().info(getString("operation_succeded"));

                setResponsePage(new Roles(null));
            }
        });
    }

    add(fragment);
    }
}
