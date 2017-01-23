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

import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDateTimePicker;
import java.util.Date;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxDateTimeFieldPanel extends DateFieldPanel {

    private static final long serialVersionUID = -428975732068281726L;

    public AjaxDateTimeFieldPanel(
            final String id, final String name, final IModel<Date> model, final String datePattern) {
        super(id, name, model, datePattern);

        field = new AjaxDateTimePicker("field", model, SyncopeConsoleSession.get().getDateFormat().getLocale());
        add(field.setLabel(new Model<>(name)).setOutputMarkupId(true));
    }

    @Override
    public FieldPanel<Date> clone() {
        final FieldPanel<Date> panel =
                new AjaxDateTimeFieldPanel(getId(), name, new Model<Date>(null), fmt.getPattern());

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
