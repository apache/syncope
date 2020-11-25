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

import com.googlecode.wicket.kendo.ui.form.datetime.AjaxDateTimePicker;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class AjaxDateTimeFieldPanel extends DateFieldPanel {

    private static final long serialVersionUID = -428975732068281726L;

    public AjaxDateTimeFieldPanel(
            final String id,
            final String name,
            final IModel<Date> model,
            final FastDateFormat dateTimePattern) {

        super(id, name, model, dateTimePattern);

        // dateTimePattern should be spit into separate date and time pattern strings in order to be passed to the
        // AjaxDateTimePicker constructor, but there is no safe way to do that - ignoring
        Locale locale = Session.get().getLocale();
        field = new AjaxDateTimePicker(
                "field",
                model,
                locale,
                FastDateFormat.getDateInstance(DateFormat.SHORT, locale).getPattern().replace("yy", "yyyy"),
                FastDateFormat.getTimeInstance(DateFormat.SHORT, locale).getPattern());
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));
    }

    @Override
    public FieldPanel<Date> clone() {
        FieldPanel<Date> panel = new AjaxDateTimeFieldPanel(
                getId(),
                name,
                new Model<>(null),
                fmt);
        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
