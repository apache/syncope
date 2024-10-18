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

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public final class AjaxNumberFieldPanel<T extends Number & Comparable<T>> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private final IModel<T> model;

    private final Options options;

    private final boolean enableOnChange;

    private AjaxNumberFieldPanel(
            final String id,
            final String name,
            final IModel<T> model,
            final Options options,
            final boolean enableOnChange) {

        super(id, name, model);

        this.model = model;
        this.options = options;
        this.enableOnChange = enableOnChange;

        field = new NumberTextField<>("numberTextField", model, options);

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));
    }

    @Override
    public FieldPanel<T> setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        AjaxNumberFieldPanel.class.cast(field).setEnabled(!readOnly);
        return this;
    }

    @Override
    public AjaxNumberFieldPanel<T> clone() {
        AjaxNumberFieldPanel<T> panel = new AjaxNumberFieldPanel<>(
                getId(), name, model, options, enableOnChange);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

    public static class Builder<T extends Number & Comparable<T>> {

        private final Options options;

        private boolean enableOnChange = false;

        public Builder() {
            this(new Options().set("format", "'#'").set("step", "1"));
        }

        public Builder(final Options options) {
            this.options = options;
            options.set("format", "'#'");
            options.set("step", "1");
        }

        public Builder<T> min(final T min) {
            options.set("min", min);
            return this;
        }

        public Builder<T> max(final T max) {
            options.set("max", max);
            return this;
        }

        public Builder<T> step(final T step) {
            options.set("step", step);
            return this;
        }

        public Builder<T> format(final String format) {
            options.set("format", format);
            return this;
        }

        public Builder<T> enableOnChange() {
            enableOnChange = true;
            return this;
        }

        public AjaxNumberFieldPanel<T> build(
                final String id,
                final String name,
                final IModel<T> model) {

            return new AjaxNumberFieldPanel<>(id, name, model, options, enableOnChange);
        }
    }
}
