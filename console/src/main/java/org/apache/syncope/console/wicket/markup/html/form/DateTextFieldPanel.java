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

import java.util.Date;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class DateTextFieldPanel extends DateFieldPanel {

    private static final long serialVersionUID = 1919852712185883648L;

    public DateTextFieldPanel(final String id, final String name, final IModel<Date> model, final String datePattern) {

        super(id, name, model, datePattern);

        field = DateTextField.forDatePattern("field", model, datePattern);

        if (!isReadOnly()) {
            field.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(AjaxRequestTarget art) {
                    // nothing to do
                }
            });
        }

        field.add(getDatePicker());

        add(field.setLabel(new Model(name)).setOutputMarkupId(true));
    }

    /**
     * Setup a DatePicker component.
     */
    private DatePicker getDatePicker() {
        final DatePicker picker = new DatePicker() {

            private static final long serialVersionUID = 4166072895162221956L;

            @Override
            protected boolean enableMonthYearSelection() {
                return true;
            }
        };

        picker.setShowOnFieldClick(true);

        return picker;
    }

    @Override
    public FieldPanel clone() {
        final FieldPanel panel = new DateTextFieldPanel(id, name, new Model(), datePattern);
        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
