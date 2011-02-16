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

import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxDropDownChoicePanel extends Panel {

    public AjaxDropDownChoicePanel(final String id, final String name,
            final IModel model, final List choices,
            final IChoiceRenderer renderer, final boolean required) {

        super(id, model);

        if (required) {
            add(new Label("required", "*"));
        } else {
            add(new Label("required", ""));
        }

        add(new DropDownChoice("dropDownChoiceField", model, choices,
                renderer).setRequired(required).setLabel(new Model(name)));
    }

    public AjaxDropDownChoicePanel(final String id, final String name,
            final IModel model,
            final boolean required, final boolean readonly) {

        super(id, model);

        if (required) {
            add(new Label("required", "*"));
        } else {
            add(new Label("required", ""));
        }

        add(new TextField("dropDownChoiceField", model).setRequired(
                required).setLabel(new Model(name)).setEnabled(!readonly));
    }
}
