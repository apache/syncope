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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.Model;
import org.springframework.util.StringUtils;
import org.syncope.console.SyncopeSession;

public class DateTimeFieldPanel extends FieldPanel<Date> {

    private static final long serialVersionUID = -428975732068281726L;

    private Form form = null;

    public DateTimeFieldPanel(
            final String id,
            final String name,
            final IModel<Date> model,
            final boolean active) {

        super(id, name, model, active);

        field = new DateTimeField("field", model);

        final Calendar cal = Calendar.getInstance();

        field.get("hours").
                add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID =
                    -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
                if (((DateTimeField) field).getHours() > 12) {
                    cal.set(Calendar.HOUR_OF_DAY,
                            ((DateTimeField) field).getHours());
                } else {
                    cal.set(Calendar.HOUR,
                            ((DateTimeField) field).getHours());
                }
                field.setModelObject(cal.getTime());
            }
        });

        field.get("minutes").
                add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID =
                    -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
                cal.set(Calendar.MINUTE, ((DateTimeField) field).getMinutes());
                field.setModelObject(cal.getTime());
            }
        });

        field.get("date").
                add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID =
                    -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
                cal.setTime(((DateTimeField) field).getDate());

                if ("PM".equals("" + ((DateTimeField) field).getAmOrPm())) {
                    cal.set(Calendar.AM_PM, Calendar.PM);
                } else {
                    cal.set(Calendar.AM_PM, Calendar.AM);
                }

                field.setModelObject(cal.getTime());
            }
        });

        field.get("amOrPmChoice").
                add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID =
                    -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget art) {
                if ("PM".equals("" + ((DateTimeField) field).getAmOrPm())) {
                    cal.set(Calendar.AM_PM, Calendar.PM);
                } else {
                    cal.set(Calendar.AM_PM, Calendar.AM);
                }

                field.setModelObject(cal.getTime());
            }
        });

        add(field.setLabel(new Model(name)).setOutputMarkupId(true));
    }

    /**
     * Custom form validator for registering and handling DateTimeField
     * components that are in it.
     */
    private class DateTimeFormValidator extends AbstractFormValidator {

        private static final long serialVersionUID = 6842264694946633582L;

        private FormComponent[] dateTimeComponents;

        public DateTimeFormValidator(DateTimeField dateTimeComponent) {
            if (dateTimeComponent == null) {
                throw new IllegalArgumentException(
                        "argument dateTimeComponent cannot be null");
            }

            dateTimeComponents = new FormComponent[]{dateTimeComponent};
        }

        @Override
        public FormComponent[] getDependentFormComponents() {
            return dateTimeComponents;
        }

        /**
         * Validation rule : all 3 fields (date,hours,minutes) must be not-null.
         * @param form
         */
        @Override
        public void validate(Form form) {
            final DateTimeField dateTimeField =
                    (DateTimeField) dateTimeComponents[0];

            if (!(dateTimeField.getDate() != null
                    && dateTimeField.getHours() != null
                    && dateTimeField.getMinutes() != null)) {

                ValidationError ve = new ValidationError();
                ve.setVariables(DateTimeFormValidator.this.variablesMap());
                ve.addMessageKey(resourceKey());
                dateTimeComponents[0].error((IValidationError) ve);
            }
        }
    }

    public FieldPanel setFormValidator(final Form form) {

        if (field == null) {
            LOG.error("Error setting form validator");
        } else {
            form.add(new DateTimeFormValidator(((DateTimeField) field)));
            this.form = form;
        }

        return this;
    }

    @Override
    public FieldPanel setNewModel(final ListItem item, final Class reference) {
        final DateFormat formatter = SyncopeSession.get().getDateFormat();

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
            public void setObject(Serializable object) {
                if (object != null) {
                    if (reference.equals(String.class)) {
                        // Parse string using datePattern
                        item.setModelObject(
                                (String) formatter.format((Date) object));
                    } else if (reference.equals(Date.class)) {
                        // Don't parse anything
                        item.setModelObject(
                                (Date) object);
                    } else {
                        // consider Long
                        item.setModelObject(
                                new Long(((Date) object).getTime()));
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
    public FieldPanel setNewModel(final List<String> list) {
        setNewModel(new Model() {

            private static final long serialVersionUID = 527651414610325237L;

            final DateFormat formatter = SyncopeSession.get().getDateFormat();

            @Override
            public Serializable getObject() {
                Date date = null;

                if (list != null && !list.isEmpty()
                        && StringUtils.hasText(list.get(0))) {
                    try {
                        // Parse string using datePattern
                        date = formatter.parse(list.get(0));
                    } catch (ParseException e) {
                        LOG.error("invalid parse exception", e);
                    }
                }

                return date;
            }

            @Override
            public void setObject(Serializable object) {
                if (object != null) {
                    list.clear();
                    list.add((String) formatter.format((Date) object));
                }
            }
        });

        return this;
    }

    @Override
    public FieldPanel setStyleShet(String classes) {
        field.get("date").add(AttributeModifier.replace(
                "class", (classes != null ? classes : "") + " date_size"));

        field.get("hours").add(AttributeModifier.replace(
                "class", classes != null ? classes : ""));

        field.get("minutes").add(AttributeModifier.replace(
                "class", classes != null ? classes : ""));

        field.get("amOrPmChoice").add(AttributeModifier.replace(
                "class", classes != null ? classes : ""));

        return this;
    }

    @Override
    public FieldPanel clone() {
        final FieldPanel panel = new DateTimeFieldPanel(
                id, name, new Model(null), active);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        if (form != null && isRequired()) {
            ((DateTimeFieldPanel) panel).setFormValidator(form);
        }

        return panel;
    }
}
