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

import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxDropDownChoicePanel<T>
        extends FieldPanel implements Cloneable {

    private static final long serialVersionUID = -4716376580659196095L;

    public AjaxDropDownChoicePanel(
            final String id,
            final String name,
            final IModel<T> model,
            final boolean active) {

        super(id, name, model, active);

        field = new DropDownChoice("dropDownChoiceField", model,
                Collections.EMPTY_LIST, new ChoiceRenderer());

        add(field.setLabel(new Model(name)).setOutputMarkupId(true));

        if (active) {
            field.add(new AjaxFormComponentUpdatingBehavior("onblur") {

                private static final long serialVersionUID =
                        -1107858522700306810L;

                @Override
                protected void onUpdate(AjaxRequestTarget art) {
                    // nothing to do
                }
            });
        }
    }

    public AjaxDropDownChoicePanel<T> setChoiceRenderer(
            final IChoiceRenderer renderer) {

        ((DropDownChoice) field).setChoiceRenderer(renderer);
        return this;
    }

    public AjaxDropDownChoicePanel<T> setChoices(final List<T> choices) {
        ((DropDownChoice) field).setChoices(choices);
        return this;
    }

    public AjaxDropDownChoicePanel<T> setChoices(
            final IModel<? extends List<? extends T>> choices) {

        ((DropDownChoice) field).setChoices(choices);
        return this;
    }

    @Override
    public FieldPanel clone() {

        AjaxDropDownChoicePanel<T> panel =
                (AjaxDropDownChoicePanel<T>) super.clone();

        setChoiceRenderer(((DropDownChoice) field).getChoiceRenderer());
        setChoices(((DropDownChoice) field).getChoices());

        return panel;
    }
}
