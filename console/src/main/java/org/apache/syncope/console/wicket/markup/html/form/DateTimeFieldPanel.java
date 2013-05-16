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

import java.util.Calendar;
import java.util.Date;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;

public class DateTimeFieldPanel extends DateFieldPanel {

    private static final long serialVersionUID = -428975732068281726L;

    private Form form = null;

    public DateTimeFieldPanel(final String id, final String name, final IModel<Date> model, final String datePattern) {

        super(id, name, model, datePattern);

        field = new DateTimeField("field", model);

        final Calendar cal = Calendar.getInstance();

        field.get("hours").add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (((DateTimeField) field).getHours() > 12) {
                    cal.set(Calendar.HOUR_OF_DAY, ((DateTimeField) field).getHours());
                } else {
                    cal.set(Calendar.HOUR, ((DateTimeField) field).getHours());
                }
                field.setModelObject(cal.getTime());
            }
        });

        field.get("minutes").add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                cal.set(Calendar.MINUTE, ((DateTimeField) field).getMinutes());
                field.setModelObject(cal.getTime());
            }
        });

        field.get("date").add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                cal.setTime(((DateTimeField) field).getDate());

                if ("PM".equals("" + ((DateTimeField) field).getAmOrPm())) {
                    cal.set(Calendar.AM_PM, Calendar.PM);
                } else {
                    cal.set(Calendar.AM_PM, Calendar.AM);
                }

                field.setModelObject(cal.getTime());
            }
        });

        field.get("amOrPmChoice").add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
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
     * Custom form validator for registering and handling DateTimeField components that are in it.
     */
    private class DateTimeFormValidator extends AbstractFormValidator {

        private static final long serialVersionUID = 6842264694946633582L;

        private FormComponent[] dateTimeComponents;

        public DateTimeFormValidator(final DateTimeField dateTimeComponent) {
            if (dateTimeComponent == null) {
                throw new IllegalArgumentException("argument dateTimeComponent cannot be null");
            }

            dateTimeComponents = new FormComponent[]{dateTimeComponent};
        }

        @Override
        public FormComponent[] getDependentFormComponents() {
            return dateTimeComponents;
        }

        /**
         * Validation rule : all 3 fields (date,hours,minutes) must be not-null.
         *
         * @param form
         */
        @Override
        public void validate(final Form form) {
            final DateTimeField dateTimeField = (DateTimeField) dateTimeComponents[0];

            if (!(dateTimeField.getDate() != null && dateTimeField.getHours() != null
                    && dateTimeField.getMinutes() != null)) {

                ValidationError ve = new ValidationError();
                ve.setVariables(DateTimeFormValidator.this.variablesMap());
                ve.addKey(resourceKey());
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
    public FieldPanel setStyleSheet(final String classes) {
        field.get("date").add(AttributeModifier.replace("class", (classes == null ? "" : classes) + " date_size"));

        field.get("hours").add(AttributeModifier.replace("class", classes == null ? "" : classes));

        field.get("minutes").add(AttributeModifier.replace("class", classes == null ? "" : classes));

        field.get("amOrPmChoice").add(AttributeModifier.replace("class", classes == null ? "" : classes));

        return this;
    }

    @Override
    public FieldPanel clone() {
        final FieldPanel panel = new DateTimeFieldPanel(id, name, new Model<Date>(null), datePattern);

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
