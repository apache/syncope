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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.spinner.Spinner;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.spinner.SpinnerConfig;
import org.apache.wicket.model.IModel;

public class SpinnerFieldPanel<T extends Number> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private Class<T> reference;

    private IModel<T> model;

    private SpinnerConfig conf;

    public SpinnerFieldPanel(
            final String id,
            final String name,
            final Class<T> reference,
            final IModel<T> model,
            final T min, final T max) {
        super(id, name, model);

        final SpinnerConfig config = new SpinnerConfig();
        config.withMax(max);
        config.withMin(min);

        init(name, reference, model, config);
    }

    public SpinnerFieldPanel(
            final String id,
            final String name,
            final Class<T> reference,
            final IModel<T> model) {
        this(id, name, reference, model, new SpinnerConfig());
    }

    public SpinnerFieldPanel(
            final String id,
            final String name,
            final Class<T> reference,
            final IModel<T> model,
            final SpinnerConfig conf) {

        super(id, name, model);
        init(name, reference, model, conf);
    }

    private void init(final String name, final Class<T> reference, final IModel<T> model, final SpinnerConfig conf) {
        field = new Spinner<>("spinner", model, conf);
        add(field);

        this.name = name;
        this.model = model;
        this.conf = conf;
        this.reference = reference;

        this.conf.withMouseWheel(true);
        this.conf.withVerticalbuttons(true);
    }

    @Override
    public SpinnerFieldPanel<T> clone() {
        SpinnerFieldPanel<T> panel = new SpinnerFieldPanel<T>(getId(), name, reference, model, conf);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

}
