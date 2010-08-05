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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/**
 * Panel that contains an text field that submits automatically after it loses focus.
 *
 */
public class LeafEditablePanel extends Panel {

    /**
     * Panel constructor.
     *
     * @param id
     *            Markup id
     *
     * @param inputModel
     *            Model of the text field
     */
    public LeafEditablePanel(String id, IModel inputModel) {
        super(id);

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
