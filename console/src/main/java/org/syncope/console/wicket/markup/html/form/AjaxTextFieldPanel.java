/*
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
package org.syncope.console.wicket.markup.html.form;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;


public class AjaxTextFieldPanel extends Panel {
   /**
     * Build AjaxTextFieldPanel.
     * @param component id
     * @param label name
     * @param IModel<?> object
     * @param required flag
     */
    public AjaxTextFieldPanel(String id,String name,IModel model,boolean required) {
        super(id,model);

        if(required)
            add(new Label("required","*"));
        else
            add(new Label("required",""));
        
        add(new UpdatingTextField("textField", model).setRequired(required)
                                 .setLabel(new Model(name)));

    }

    /**
     * Extension class of TextField. It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
    private static class UpdatingTextField extends TextField
    {
        public UpdatingTextField( String id, IModel model )
        {
            super( id, model );
            add( new AjaxFormComponentUpdatingBehavior( "onblur" )
            {
                protected void onUpdate( AjaxRequestTarget target )
                {
                }
            } );
        }
    }
}