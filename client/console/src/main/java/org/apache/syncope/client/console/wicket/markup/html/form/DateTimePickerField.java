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
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.string.Strings;

public class DateTimePickerField extends DateTimeField {

    private static final long serialVersionUID = 3733881705516982654L;

    public DateTimePickerField(final String id) {
        this(id, null);
    }

    public DateTimePickerField(final String id, final IModel<Date> model) {
        super(id, model);
    }

    @Override
    protected DatePicker newDatePicker() {
        return new DatePicker() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void configure(final Map<String, Object> widgetProperties,
                    final IHeaderResponse response, final Map<String, Object> initVariables) {
                super.configure(widgetProperties, response, initVariables);
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
                response.write("Dp\"></span><i class=\"glyphicon glyphicon-calendar\" style=\"margin-left: 5px;\"");
                response.write(" id=\"");
                response.write(getIconId());
                response.write("\" ");
                response.write(" alt=\"");
                CharSequence alt = getIconAltText();
                response.write(Strings.escapeMarkup((alt != null) ? alt.toString() : ""));
                response.write("\" title=\"");
                CharSequence title = getIconTitle();
                response.write(Strings.escapeMarkup((title != null) ? title.toString() : ""));
                response.write("\"/>");

                if (renderOnLoad()) {
                    response.write("<br style=\"clear:left;\"/>");
                }
                response.write("</span>");
            }

        };
    }
}
