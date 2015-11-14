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
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converter.DateConverter;

public class AjaxDateFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = -428975732068281726L;

    private final String datePattern;

    public AjaxDateFieldPanel(final String id, final String name, final IModel<Date> model, final String pattern) {
        super(id, name, model);

        this.datePattern = pattern == null ? SyncopeConstants.DEFAULT_DATE_PATTERN : pattern;

        if (this.datePattern.contains("H")) {
            field = new DatetimePicker("date", model, new DatetimePickerConfig()
                    .withFormat(this.datePattern.replaceAll("'", ""))
                    .setShowToday(true)
                    .useSideBySide(true)
                    .with(new DatetimePickerIconConfig()
                            .useDateIcon(FontAwesomeIconType.calendar)
                            .useTimeIcon(FontAwesomeIconType.clock_o)
                            .useUpIcon(FontAwesomeIconType.arrow_up)
                            .useDownIcon(FontAwesomeIconType.arrow_down)
                    )) {

                        private static final long serialVersionUID = 1L;

                        // T0DO: trying to resolve issue 730.
                        @Override
                        @SuppressWarnings("unchecked")
                        public <C> IConverter<C> getConverter(final Class<C> type) {
                            return (IConverter<C>) new DateConverter() {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public DateFormat getDateFormat(final Locale locale) {
                                    return new SimpleDateFormat(
                                            datePattern,
                                            locale == null ? SyncopeConsoleSession.get().getLocale() : locale) {

                                                private static final long serialVersionUID = 1L;

                                                @Override
                                                public Date parse(final String text, final ParsePosition pos) {
                                                    return super.parse(text, pos);
                                                }

                                            };
                                }
                            };
                        }

                    };
        } else {
            field = new DateTextField("date", model, new DateTextFieldConfig()
                    .withFormat(this.datePattern)
                    .highlightToday(true)
                    .autoClose(true)
                    .showTodayButton(DateTextFieldConfig.TodayButton.TRUE));
        }

        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));
        add(field);
    }

    // todo: Evaluate the actual needs to keep this override.
    @Override
    public FieldPanel<Date> setNewModel(final List<Serializable> list) {
        final SimpleDateFormat formatter = datePattern == null
                ? new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN, Locale.getDefault())
                : new SimpleDateFormat(datePattern, Locale.getDefault());

        IModel<Date> model = new Model<Date>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Date getObject() {
                Date date = null;
                final Object obj = list == null || list.isEmpty() ? null : list.get(0);

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        // Parse string using datePattern
                        try {
                            date = formatter.parse(obj.toString());
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                    } else if (obj instanceof Date) {
                        // Don't parse anything
                        date = (Date) obj;
                    } else {
                        // consider Long
                        date = new Date((Long) obj);
                    }
                }
                return date;
            }

            @Override
            public void setObject(final Date object) {
                super.setObject(object);
            }
        };

        field.setModel(model);
        return this;
    }

    // todo: Evaluate the actual needs to keep this override.
    @SuppressWarnings("rawtypes")
    @Override
    public FieldPanel<Date> setNewModel(final ListItem item) {
        final SimpleDateFormat formatter = datePattern == null
                ? new SimpleDateFormat(SyncopeConstants.DEFAULT_DATE_PATTERN, Locale.getDefault())
                : new SimpleDateFormat(datePattern, Locale.getDefault());

        IModel<Date> model = new Model<Date>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Date getObject() {
                Date date = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        // Parse string using datePattern
                        try {
                            date = formatter.parse(obj.toString());
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                    } else if (obj instanceof Date) {
                        // Don't parse anything
                        date = (Date) obj;
                    } else {
                        // consider Long
                        date = new Date((Long) obj);
                    }
                }

                return date;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final Date object) {
                item.setModelObject(object != null ? formatter.format(object) : null);
            }
        };

        field.setModel(model);
        return this;
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
