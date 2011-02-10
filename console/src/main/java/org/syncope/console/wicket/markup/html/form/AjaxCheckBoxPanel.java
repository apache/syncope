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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxCheckBoxPanel extends Panel {

    public AjaxCheckBoxPanel(final String id, final String name,
            final IModel<Boolean> model, boolean required) {

        super(id, model);

        add(new UpdatingCheckBox("checkboxField", model).setLabel(
                new Model(name)));
    }

    public AjaxCheckBoxPanel(final String id, final String name,
            final IModel<Boolean> model,
            final boolean required, final boolean readonly) {

        super(id, model);

        add(new UpdatingCheckBox("checkboxField", model).setLabel(
                new Model(name)).setEnabled(!readonly));
    }
}
