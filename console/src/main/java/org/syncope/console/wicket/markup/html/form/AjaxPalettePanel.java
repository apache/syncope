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
package org.syncope.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.syncope.console.commons.SelectChoiceRenderer;

public class AjaxPalettePanel<T> extends AbstractFieldPanel {

    private static final long serialVersionUID = 7738499668258805567L;

    final Palette<T> palette;

    public AjaxPalettePanel(
            final String id,
            final IModel<List<T>> model,
            final ListModel<T> choices) {

        super(id, model);

        this.palette = createPalette(
                id, model, choices, new SelectChoiceRenderer());
        add(palette.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    public AjaxPalettePanel(
            final String id,
            final IModel<List<T>> model,
            final ListModel<T> choices,
            final ChoiceRenderer<T> renderer) {

        super(id, model);

        this.palette = createPalette(id, model, choices, renderer);
        add(palette.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    private Palette<T> createPalette(
            final String id,
            final IModel<List<T>> model,
            final ListModel<T> choices,
            final IChoiceRenderer renderer) {

        final Palette<T> palette = new Palette(
                "paletteField",
                model,
                choices,
                renderer,
                8,
                false);

        return palette;
    }

    @Override
    public AbstractFieldPanel setModelObject(Serializable object) {
        palette.setDefaultModelObject(object);
        return this;
    }
}
