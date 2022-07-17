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

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class AjaxDropDownChoicePanel<T extends Serializable> extends FieldPanel<T> implements Cloneable {

    private static final long serialVersionUID = -4716376580659196095L;

    public AjaxDropDownChoicePanel(final String id, final String name, final IModel<T> model) {
        this(id, name, model, true);
    }

    public AjaxDropDownChoicePanel(
            final String id, final String name, final IModel<T> model, final boolean enableOnBlur) {

        super(id, name, model);

        field = new DropDownChoice<>("dropDownChoiceField", model, List.of(), new ChoiceRenderer<>());
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        if (enableOnBlur) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            }).add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        setNullValid(true);
    }

    @Override
    public FieldPanel<T> setRequired(final boolean required) {
        setNullValid(!required);
        return super.setRequired(required);
    }

    @SuppressWarnings("unchecked")
    public AjaxDropDownChoicePanel<T> setChoiceRenderer(final IChoiceRenderer<T> renderer) {
        DropDownChoice.class.cast(field).setChoiceRenderer(renderer);
        return this;
    }

    @SuppressWarnings("unchecked")
    public AjaxDropDownChoicePanel<T> setChoices(final List<T> choices) {
        DropDownChoice.class.cast(field).setChoices(choices);
        return this;
    }

    @SuppressWarnings("unchecked")
    public AjaxDropDownChoicePanel<T> setChoices(final IModel<? extends List<? extends T>> choices) {
        DropDownChoice.class.cast(field).setChoices(choices);
        return this;
    }

    public final AjaxDropDownChoicePanel<T> setNullValid(final boolean validity) {
        DropDownChoice.class.cast(field).setNullValid(validity);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FieldPanel<T> clone() {
        final AjaxDropDownChoicePanel<T> panel = (AjaxDropDownChoicePanel<T>) super.clone();
        panel.setChoiceRenderer(DropDownChoice.class.cast(field).getChoiceRenderer());
        panel.setChoices(DropDownChoice.class.cast(field).getChoices());
        return panel;
    }
}
