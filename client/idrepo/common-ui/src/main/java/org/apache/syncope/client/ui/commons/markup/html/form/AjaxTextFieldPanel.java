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
package org.apache.syncope.client.ui.commons.markup.html.form;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAutoCompleteBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.convert.IConverter;

public class AjaxTextFieldPanel extends TextFieldPanel implements Cloneable {

    private static final long serialVersionUID = 238940918106696068L;

    private Component questionMarkJexlHelp;

    private List<String> choices = List.of();

    public AjaxTextFieldPanel(final String id, final String name, final IModel<String> model) {
        this(id, name, model, true);
    }

    public AjaxTextFieldPanel(
            final String id, final String name, final IModel<String> model, final boolean enableOnChange) {
        super(id, name, model);

        questionMarkJexlHelp = Constants.getJEXLPopover(this, TooltipConfig.Placement.right);
        add(questionMarkJexlHelp.setVisible(false));

        final AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(true);
        settings.setShowListOnEmptyInput(true);
        settings.setCssClassName("custom-autocomplete-box");

        field = new AutoCompleteTextField<>("textField", model, settings) {

            private static final long serialVersionUID = -6648767303091874219L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                return AjaxTextFieldPanel.this.getChoices(input);
            }

            @Override
            protected AutoCompleteBehavior<String> newAutoCompleteBehavior(
                    final IAutoCompleteRenderer<String> renderer,
                    final AutoCompleteSettings settings) {

                return new IndicatorAutoCompleteBehavior<>(renderer, settings) {

                    private static final long serialVersionUID = 1070808433195962931L;

                    @Override
                    protected Iterator<String> getChoices(final String input) {
                        return AjaxTextFieldPanel.this.getChoices(input);
                    }
                };
            }

            @SuppressWarnings("unchecked")
            @Override
            public <C> IConverter<C> getConverter(final Class<C> type) {
                return AjaxTextFieldPanel.this.getConverter().
                        map(converter -> (IConverter<C>) converter).
                        orElseGet(() -> super.getConverter(type));
            }
        };
        setHTMLInputNotAllowed();
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }
    }

    public void setChoices(final List<String> choices) {
        if (choices != null) {
            this.choices = choices;
        }
    }

    protected Optional<IConverter<String>> getConverter() {
        return Optional.empty();
    }

    public FieldPanel<String> enableJexlHelp() {
        questionMarkJexlHelp.setVisible(true);
        return this;
    }

    public FieldPanel<String> enableJexlHelp(final String... jexlExamples) {
        questionMarkJexlHelp = Constants.getJEXLPopover(this, TooltipConfig.Placement.bottom, jexlExamples);
        addOrReplace(questionMarkJexlHelp.setVisible(true));
        return this;
    }

    protected Iterator<String> getChoices(final String input) {
        Pattern pattern = Pattern.compile(".*" + Pattern.quote(input) + ".*", Pattern.CASE_INSENSITIVE);
        return choices.stream().filter(pattern.asMatchPredicate()).iterator();
    }

    @Override
    public FieldPanel<String> clone() {
        final AjaxTextFieldPanel panel = (AjaxTextFieldPanel) super.clone();
        panel.setChoices(choices);
        return panel;
    }
}
