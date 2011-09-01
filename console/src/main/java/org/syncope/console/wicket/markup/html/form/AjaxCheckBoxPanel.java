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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxCheckBoxPanel extends FieldPanel<Boolean> {

    private static final long serialVersionUID = 5664138233103884310L;

    public AjaxCheckBoxPanel(
            final String id,
            final String name,
            final IModel<Boolean> model,
            final boolean active) {

        super(id, name, model, active);

        field = new CheckBox("checkboxField", model);
        add(field.setLabel(new Model(name)).setOutputMarkupId(true));

        if (active) {
            field.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID =
                        -1107858522700306810L;

                @Override
                protected void onUpdate(AjaxRequestTarget art) {
                    // nothing to do
                }
            });
        }
    }

    @Override
    public FieldPanel addRequiredLabel() {

        if (!isRequired()) {
            setRequired(true);
        }

        this.isRequiredLabelAdded = true;

        return this;
    }
}
