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

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.syncope.console.commons.SelectChoiceRenderer;

public class AjaxPalettePanel extends AbstractFieldPanel {

    private static final long serialVersionUID = 7738499668258805567L;

    final Palette<String> palette;

    public AjaxPalettePanel(
            final String id,
            final IModel<List<String>> model,
            final ListModel<String> choices) {

        super(id, model);

        palette = new Palette(
                "paletteField",
                model,
                choices,
                new SelectChoiceRenderer(),
                8,
                false);

        add(palette.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    @Override
    public AbstractFieldPanel setModelObject(Serializable object) {
        palette.setDefaultModelObject(object);
        return this;
    }
}
