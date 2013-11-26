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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SpinnerFieldPanel<T extends Number> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private final T min;

    private final T max;

    public SpinnerFieldPanel(final String id, final String name, final IModel<T> model,
            final T min, final T max) {

        super(id, name, model);
        this.min = min;
        this.max = max;

        field = new TextField<T>("spinnerField", model);
        field.setMarkupId(id);
        add(field.setLabel(new Model<String>(name)));

        AjaxLink<Void> spinnerFieldDisable = new AjaxLink<Void>("spinnerFieldDisable") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
            }

        };
        spinnerFieldDisable.setMarkupId("spinnerFieldDisable-" + id);
        add(spinnerFieldDisable);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        final StringBuilder statements = new StringBuilder();
        statements.append("jQuery(function() {").
                append("var spinner = $('#").append(id).append("').spinner();");

        if (this.min != null) {
            statements.
                    append("$('#").append(id).append("').spinner(").
                    append("'option', 'min', ").append(this.min).append(");");
        }
        if (this.max != null) {
            statements.
                    append("$('#").append(id).append("').spinner(").
                    append("'option', 'max', ").append(this.max).append(");");
        }

        statements.
                append("$('#spinnerFieldDisable-").append(id).append("').click(function() {").
                append("if (spinner.spinner('option', 'disabled')) {").
                append("spinner.spinner('enable');").
                append("} else {").
                append("spinner.spinner('disable');").
                append("spinner.spinner('value', null)").
                append("}").
                append("});").
                append("});");

        response.render(JavaScriptHeaderItem.forScript(statements, "script-" + id));
    }

}
