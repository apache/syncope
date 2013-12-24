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

import java.util.UUID;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SpinnerFieldPanel<T extends Number> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private final String uuid;

    private final T min;

    private final T max;

    public SpinnerFieldPanel(final String id, final String name, final Class<T> reference, final IModel<T> model,
            final T min, final T max, final boolean disableVisible) {

        super(id, name, model);
        this.min = min;
        this.max = max;

        uuid = UUID.randomUUID().toString();
        field = new TextField<T>("spinnerField", model, reference);
        field.setMarkupId(uuid);
        add(field.setLabel(new Model<String>(name)));

        if (!isReadOnly()) {
            field.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        AjaxLink<Void> spinnerFieldDisable = new AjaxLink<Void>("spinnerFieldDisable") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                // nothing to do
            }

        };
        spinnerFieldDisable.setMarkupId("spinnerFieldDisable-" + uuid);
        spinnerFieldDisable.setOutputMarkupPlaceholderTag(true);
        spinnerFieldDisable.setVisible(disableVisible);
        add(spinnerFieldDisable);

        final StringBuilder statements = new StringBuilder();
        statements.append("jQuery(function() {").
                append("var spinner = $('#").append(uuid).append("').spinner();");
        if (this.min != null) {
            statements.
                    append("$('#").append(uuid).append("').spinner(").
                    append("'option', 'min', ").append(this.min).append(");");
        }
        if (this.max != null) {
            statements.
                    append("$('#").append(uuid).append("').spinner(").
                    append("'option', 'max', ").append(this.max).append(");");
        }
        statements.
                append("$('#spinnerFieldDisable-").append(uuid).append("').click(function() {").
                append("if (spinner.spinner('option', 'disabled')) {").
                append("spinner.spinner('enable');").
                append("} else {").
                append("spinner.spinner('disable');").
                append("spinner.spinner('value', null)").
                append("}").
                append("});").
                append("});");
        Label spinnerFieldJS = new Label("spinnerFieldJS", statements.toString());
        spinnerFieldJS.setEscapeModelStrings(false);
        add(spinnerFieldJS);
    }

}
