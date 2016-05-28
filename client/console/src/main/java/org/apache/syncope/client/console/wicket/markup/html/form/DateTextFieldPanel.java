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

import java.util.Date;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;

public class DateTextFieldPanel extends DateFieldPanel {

    private static final long serialVersionUID = 1919852712185883648L;

    public DateTextFieldPanel(final String id, final String name, final IModel<Date> model, final String datePattern) {
        super(id, name, model, datePattern);

        field = DateTextField.forDatePattern("field", model, datePattern);

        if (!isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        field.add(getDatePicker());

        add(field.setLabel(new Model<>(name)).setOutputMarkupId(true));
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

            @Override
            public void afterRender(final Component component) {
                Response response = component.getResponse();
                response.write("\n<span class=\"yui-skin-sam\"><span style=\"");

                if (renderOnLoad()) {
                    response.write("display:block;");
                } else {
                    response.write("display:none;");
                    response.write("position:absolute;");
                }

                response.write("z-index: 99999;\" id=\"");
                response.write(getEscapedComponentMarkupId());
                response.write("Dp\"></span>");

                if (renderOnLoad()) {
                    response.write("<br style=\"clear:left;\"/>");
                }
                response.write("</span>");
            }
        };

        picker.setShowOnFieldClick(true);

        return picker;
    }

    @Override
    public FieldPanel<Date> clone() {
        final FieldPanel<Date> panel = new DateTextFieldPanel(getId(), name, new Model<Date>(), fmt.getPattern());
        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
