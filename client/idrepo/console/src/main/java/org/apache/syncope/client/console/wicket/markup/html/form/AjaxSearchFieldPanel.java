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

import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAutoCompleteBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.TextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class AjaxSearchFieldPanel extends TextFieldPanel implements Cloneable {

    private static final long serialVersionUID = 6890905510177974519L;

    private List<String> choices = List.of();

    private final IAutoCompleteRenderer<String> renderer;

    private final AutoCompleteSettings settings;

    public AjaxSearchFieldPanel(final String id, final String name, final IModel<String> model) {
        this(id, name, model, null, null);
    }

    public AjaxSearchFieldPanel(
            final String id, final String name,
            final IModel<String> model,
            final AutoCompleteSettings settings) {
        this(id, name, model, null, settings);
    }

    public AjaxSearchFieldPanel(
            final String id, final String name,
            final IModel<String> model,
            final IAutoCompleteRenderer<String> renderer,
            final AutoCompleteSettings settings) {
        super(id, name, model);

        this.settings = settings;
        this.renderer = renderer;

        field = new AutoCompleteTextField<>("textField", model, settings) {

            private static final long serialVersionUID = -6648767303091874219L;

            @Override
            protected Iterator<String> getChoices(final String input) {
                return AjaxSearchFieldPanel.this.getChoices(input);
            }

            @Override
            protected AutoCompleteBehavior<String> newAutoCompleteBehavior(
                    final IAutoCompleteRenderer<String> renderer,
                    final AutoCompleteSettings settings) {

                return new IndicatorAutoCompleteBehavior<>(
                        AjaxSearchFieldPanel.this.renderer != null ? AjaxSearchFieldPanel.this.renderer : renderer,
                        AjaxSearchFieldPanel.this.settings != null ? AjaxSearchFieldPanel.this.settings : settings) {

                    private static final long serialVersionUID = 1070808433195962931L;

                    @Override
                    protected Iterator<String> getChoices(final String input) {
                        return AjaxSearchFieldPanel.this.getChoices(input);
                    }
                };
            }
        };
        setHTMLInputNotAllowed();
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        if (!isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    AjaxSearchFieldPanel.this.onUpdateBehavior();
                }
            });
        }
    }

    public List<String> getChoices() {
        return choices;
    }

    public void onUpdateBehavior() {
    }

    protected Iterator<String> getChoices(final String input) {
        return choices.iterator();
    }

    @Override
    public FieldPanel<String> clone() {
        final AjaxSearchFieldPanel panel = (AjaxSearchFieldPanel) super.clone();
        return panel;
    }
}
