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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAutoCompleteBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidator;

public class AjaxCharacterFieldPanel extends FieldPanel<Character> implements Cloneable {

    private static final long serialVersionUID = 238940918106696068L;

    private Component questionMarkJexlHelp;

    private List<Character> choices = List.of();

    public AjaxCharacterFieldPanel(final String id, final String name, final IModel<Character> model) {
        this(id, name, model, true);
    }

    public AjaxCharacterFieldPanel(
            final String id, final String name, final IModel<Character> model, final boolean enableOnChange) {
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
            protected Iterator<Character> getChoices(final String input) {
                return AjaxCharacterFieldPanel.this.getChoices(input);
            }

            @Override
            protected AutoCompleteBehavior<Character> newAutoCompleteBehavior(
                final IAutoCompleteRenderer<Character> renderer, final AutoCompleteSettings settings) {
                return new IndicatorAutoCompleteBehavior<>(renderer, settings) {

                    private static final long serialVersionUID = 1070808433195962931L;

                    @Override
                    protected Iterator<Character> getChoices(final String input) {
                        return AjaxCharacterFieldPanel.this.getChoices(input);
                    }
                };
            }
        };
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

    public void addValidator(final IValidator<? super Character> validator) {
        this.field.add(validator);
    }

    public void setChoices(final List<Character> choices) {
        if (choices != null) {
            this.choices = choices;
        }
    }

    public FieldPanel<Character> enableJexlHelp() {
        questionMarkJexlHelp.setVisible(true);
        return this;
    }

    public FieldPanel<Character> enableJexlHelp(final String... jexlExamples) {
        questionMarkJexlHelp = Constants.getJEXLPopover(this, TooltipConfig.Placement.bottom, jexlExamples);
        addOrReplace(questionMarkJexlHelp.setVisible(true));
        return this;
    }

    protected Iterator<Character> getChoices(final String input) {
        return choices.stream().
                filter(choice -> input != null && input.length() > 0 && input.toCharArray()[0] == choice).
                iterator();
    }

    @Override
    public FieldPanel<Character> clone() {
        final AjaxCharacterFieldPanel panel = (AjaxCharacterFieldPanel) super.clone();
        panel.setChoices(choices);
        return panel;
    }
}
