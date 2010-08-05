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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.syncope.client.to.RoleTO;
import org.syncope.console.pages.RoleModalPage;

/**
 *
 */
public class ParentEditablePanel extends Panel {

    final ModalWindow createRoleWin;
    final int WIN_USER_HEIGHT = 680;
    final int WIN_USER_WIDTH = 900;

    /**
     * Panel constructor.
     *
     * @param id
     *            Markup id
     *
     * @param inputModel
     *            Model of the text field
     */
    public ParentEditablePanel(String id, IModel inputModel) {
        super(id);

        add(createRoleWin = new ModalWindow("createRoleWin"));

        createRoleWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleWin.setInitialHeight(WIN_USER_HEIGHT);
        createRoleWin.setInitialWidth(WIN_USER_WIDTH);
        createRoleWin.setPageMapName("create-role-modal");
        createRoleWin.setCookieName("create-role-modal");

        add(new AjaxLink("createRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                createRoleWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        RoleModalPage form = new RoleModalPage(null,createRoleWin, new RoleTO(), true);
                        return form;
                    }
                });

                createRoleWin.show(target);
            }
        });

        add(new AjaxLink("updateRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        });

        add(new AjaxLink("dropRoleLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        });
    }
}
