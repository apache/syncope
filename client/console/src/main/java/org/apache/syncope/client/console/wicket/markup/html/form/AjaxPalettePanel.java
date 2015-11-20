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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class AjaxPalettePanel<T> extends AbstractFieldPanel<List<T>> {

    private static final long serialVersionUID = 7738499668258805567L;

    protected final Palette<T> palette;

    public AjaxPalettePanel(final String id,
            final IModel<List<T>> model, final ListModel<T> choices,
            final IChoiceRenderer<T> renderer, final boolean allowOrder,
            final boolean allowMoveAll, final String availableLabel, final String selectedLabel) {

        super(id, id, model);

        this.palette = createPalette(model, choices, renderer, allowOrder, allowMoveAll, availableLabel, selectedLabel);
        add(palette.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    protected final Palette<T> createPalette(
            final IModel<List<T>> model, final ListModel<T> choices,
            final IChoiceRenderer<T> renderer,
            final boolean allowOrder, final boolean allowMoveAll,
            final String availableLabel, final String selectedLabel) {

        return new NonI18nPalette<T>("paletteField", model, choices, renderer, 8, allowOrder, allowMoveAll) {

            private static final long serialVersionUID = -3074655279011678437L;

            @Override
            protected Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("palette.available", availableLabel));
            }

            @Override
            protected Component newSelectedHeader(final String componentId) {
                return new Label(componentId, new ResourceModel("palette.selected", selectedLabel));
            }
        };
    }

    @Override
    public AjaxPalettePanel<T> setModelObject(final List<T> object) {
        palette.setDefaultModelObject(object);
        return this;
    }

    public Collection<T> getModelCollection() {
        return palette.getModelCollection();
    }

    public static class Builder<T extends Serializable> {

        private IChoiceRenderer<T> renderer;

        private boolean allowOrder;

        private boolean allowMoveAll;

        private String selectedLabel;

        private String availableLabel;

        public Builder() {
            this.allowMoveAll = false;
            this.allowOrder = false;
            this.renderer = new SelectChoiceRenderer<>();
        }

        public Builder<T> setAllowOrder(final boolean allowOrder) {
            this.allowOrder = allowOrder;
            return this;
        }

        public Builder<T> setAllowMoveAll(final boolean allowMoveAll) {
            this.allowMoveAll = allowMoveAll;
            return this;
        }

        public Builder<T> setSelectedLabel(final String selectedLabel) {
            this.selectedLabel = selectedLabel;
            return this;
        }

        public Builder<T> setAvailableLabel(final String availableLabel) {
            this.availableLabel = availableLabel;
            return this;
        }

        public Builder<T> setRenderer(final IChoiceRenderer<T> renderer) {
            this.renderer = renderer;
            return this;
        }

        public AjaxPalettePanel<T> build(
                final String id, final IModel<List<T>> model, final ListModel<T> choices) {
            return new AjaxPalettePanel<>(id, model,
                    choices, renderer, allowOrder, allowMoveAll, availableLabel, selectedLabel);
        }
    }
}
