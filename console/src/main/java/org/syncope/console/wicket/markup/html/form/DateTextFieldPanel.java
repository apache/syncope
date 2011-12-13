/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.wicket.markup.html.form;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.Model;
import org.springframework.util.StringUtils;

public class DateTextFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = 1919852712185883648L;

    private final String datePattern;

    public DateTextFieldPanel(
            final String id,
            final String name,
            final IModel<Date> model,
            final boolean active,
            final String datePattern) {

        super(id, name, model, active);

        this.datePattern = datePattern;

        field = DateTextField.forDatePattern("field", model, datePattern);

        if (active) {
            field.add(
                    new AjaxFormComponentUpdatingBehavior("onchange") {

                        private static final long serialVersionUID =
                                -1107858522700306810L;

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
    public FieldPanel setNewModel(final ListItem item, final Class reference) {
        final DateFormat formatter = new SimpleDateFormat(datePattern);

        IModel<Date> model = new Model() {

            private static final long serialVersionUID =
                    6799404673615637845L;

            @Override
            public Serializable getObject() {
                Date date = null;

                if (StringUtils.hasText((String) item.getModelObject())) {
                    if (reference.equals(String.class)) {
                        // Parse string using datePattern
                        try {
                            date = formatter.parse(
                                    (String) item.getModelObject());
                        } catch (ParseException e) {
                            LOG.error("While parsing date", e);
                        }
                    } else if (reference.equals(Date.class)) {
                        // Don't parse anything
                        date = (Date) item.getModelObject();
                    } else {
                        // consider Long
                        date = new Date((Long) item.getModelObject());
                    }
                }

                return date;
            }

            @Override
            public void setObject(final Serializable object) {
                if (object != null) {
                    if (reference.equals(String.class)) {
                        // Parse string using datePattern
                        item.setModelObject(
                                (String) formatter.format((Date) object));
                    } else if (reference.equals(Date.class)) {
                        // Don't parse anything
                        item.setModelObject((Date) object);
                    } else {
                        // consider Long
                        item.setModelObject(((Date) object).getTime());
                    }
                } else {
                    item.setModelObject(null);
                }
            }
        };

        field.setModel(model);
        return this;
    }

    @Override
    public FieldPanel setNewModel(final List<Serializable> list) {
        setNewModel(new Model() {

            private static final long serialVersionUID = 527651414610325237L;

            private final DateFormat formatter =
                    new SimpleDateFormat(datePattern);

            @Override
            public Serializable getObject() {
                Date date = null;

                if (list != null && !list.isEmpty()
                        && StringUtils.hasText(list.get(0).toString())) {
                    try {
                        // Parse string using datePattern
                        date = formatter.parse(list.get(0).toString());
                    } catch (ParseException e) {
                        LOG.error("invalid parse exception", e);
                    }
                }

                return date;
            }

            @Override
            public void setObject(final Serializable object) {
                if (object != null) {
                    list.clear();
                    list.add((String) formatter.format((Date) object));
                }
            }
        });

        return this;
    }

    @Override
    public FieldPanel clone() {
        final FieldPanel panel = new DateTextFieldPanel(
                id, name, new Model(null), active, datePattern);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }
}
