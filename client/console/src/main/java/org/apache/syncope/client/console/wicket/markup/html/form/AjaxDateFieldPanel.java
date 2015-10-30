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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextFieldConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.datetime.DatetimePicker;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.datetime.DatetimePickerConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.datetime.DatetimePickerIconConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconType;
import java.util.Date;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class AjaxDateFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = -428975732068281726L;

    private final String datePattern;

    public AjaxDateFieldPanel(final String id, final String name, final IModel<Date> model, final String datePattern) {
        super(id, name, model);

        this.datePattern = datePattern == null ? SyncopeConstants.DEFAULT_DATE_PATTERN : datePattern;

        if (this.datePattern.contains("H")) {
            field = new DatetimePicker("textField", new DatetimePickerConfig()
                    .withFormat(this.datePattern)
                    .setShowToday(true)
                    .with(new DatetimePickerIconConfig()
                            .useDateIcon(FontAwesomeIconType.calendar)
                            .useTimeIcon(FontAwesomeIconType.clock_o)
                            .useUpIcon(FontAwesomeIconType.arrow_up)
                            .useDownIcon(FontAwesomeIconType.arrow_down)
                    ));
        } else {
            field = new DateTextField("textField", new DateTextFieldConfig()
                    .withFormat(this.datePattern)
                    .highlightToday(true)
                    .autoClose(true)
                    .showTodayButton(DateTextFieldConfig.TodayButton.TRUE));
        }

        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));
        add(field);
    }

    @Override
    public FieldPanel<Date> clone() {
        final FieldPanel<Date> panel = new AjaxDateFieldPanel(getId(), name, new Model<Date>(null), datePattern);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
