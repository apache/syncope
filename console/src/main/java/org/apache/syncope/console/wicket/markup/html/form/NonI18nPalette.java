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
package org.apache.syncope.console.wicket.markup.html.form;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Choices;
import org.apache.wicket.extensions.markup.html.form.palette.component.Selection;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;

/**
 * Workaround for WICKET-5029 to be removed when upgrading Wicket to 1.5.10 / 6.6.0.
 */
public class NonI18nPalette<T> extends Palette<T> {

    private static final long serialVersionUID = 2659070187837941889L;

    public NonI18nPalette(final String id,
            final IModel<? extends Collection<? extends T>> choicesModel,
            final IChoiceRenderer<T> choiceRenderer, final int rows, final boolean allowOrder) {

        super(id, choicesModel, choiceRenderer, rows, allowOrder);
    }

    public NonI18nPalette(final String id,
            final IModel<? extends List<? extends T>> model,
            final IModel<? extends Collection<? extends T>> choicesModel,
            final IChoiceRenderer<T> choiceRenderer, final int rows, final boolean allowOrder) {

        super(id, model, choicesModel, choiceRenderer, rows, allowOrder);
    }

    @Override
    protected Component newChoicesComponent() {
        return new Choices<T>("choices", this) {

            private static final long serialVersionUID = 5631133033579060143L;

            @Override
            protected Map<String, String> getAdditionalAttributes(final Object choice) {
                return NonI18nPalette.this.getAdditionalAttributesForChoices(choice);
            }

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                NonI18nPalette.this.nonI18nOnComponentTagBody(markupStream, openTag, getOptionsIterator());
            }
        };
    }

    @Override
    protected Component newSelectionComponent() {
        return new Selection<T>("selection", this) {

            private static final long serialVersionUID = 409955426639123592L;

            @Override
            protected Map<String, String> getAdditionalAttributes(final Object choice) {
                return NonI18nPalette.this.getAdditionalAttributesForSelection(choice);
            }

            @Override
            public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                NonI18nPalette.this.nonI18nOnComponentTagBody(markupStream, openTag, getOptionsIterator());
            }
        };
    }

    protected void nonI18nOnComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag,
            final Iterator<T> options) {

        StringBuilder buffer = new StringBuilder(128);
        IChoiceRenderer<T> renderer = getChoiceRenderer();

        while (options.hasNext()) {
            final T choice = options.next();

            final CharSequence id;
            {
                String value = renderer.getIdValue(choice, 0);

                if (getEscapeModelStrings()) {
                    id = Strings.escapeMarkup(value);
                } else {
                    id = value;
                }
            }

            final CharSequence value;
            {
                Object displayValue = renderer.getDisplayValue(choice);
                Class<?> displayClass = displayValue == null ? null : displayValue.getClass();

                @SuppressWarnings("unchecked")
                IConverter<Object> converter = (IConverter<Object>) getConverter(displayClass);
                String displayString = converter.convertToString(displayValue, getLocale());

                if (getEscapeModelStrings()) {
                    value = Strings.escapeMarkup(displayString);
                } else {
                    value = displayString;
                }
            }

            buffer.append("\n<option value=\"").append(id).append("\"");
            buffer.append(">").append(value).append("</option>");
        }

        buffer.append("\n");

        replaceComponentTagBody(markupStream, openTag, buffer);
    }
}
