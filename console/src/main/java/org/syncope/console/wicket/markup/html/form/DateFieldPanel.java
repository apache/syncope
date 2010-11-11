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

import java.util.Date;
import java.util.StringTokenizer;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;

/**
 * DateFieldPanel
 */
public class DateFieldPanel extends Panel {
    /**
     *
     * @param id wicket id
     * @param name component name
     * @param model model object
     * @param datePattern schema attribute date pattern defined for it
     * @param required TRUE if it is required, FALSE otherwise
     * @param readonly TRUE if it can't be valued, FALSE otherwise
     * @param form form where it will be included
     */
    public DateFieldPanel(String id, String name ,final IModel<Date> model,
            final String datePattern, boolean required,boolean readonly,
            Form form) {
        super(id, model);

        if (required) {
            add(new Label("required", "*"));
        } else {
            add(new Label("required", ""));
        }

        Fragment datePanel = null;

        if(!datePattern.contains("H")) {
            datePanel = new Fragment("datePanel","dateField",this);

            DateTextField field = new DateTextField("field", model,datePattern);
            field.add(getDatePicker());

            field.setEnabled(!readonly);
            field.setLabel(new Model(name));

            datePanel.add(field);
        }

        else {
            datePanel = new Fragment("datePanel","dateTimeField",this);

            DateTimeField field = new DateTimeField("field", model);

            field.setEnabled(!readonly);
            field.setLabel(new Model(name));

            datePanel.add(field);

            if(required)
                form.add(new DateTimeFormValidator(field));
        }

        add(datePanel);
    }

    /**
     * Setup a DatePicker component.
     */
    public DatePicker getDatePicker(){

        DatePicker picker = new DatePicker(){

            @Override
            protected boolean enableMonthYearSelection() {
                return true;
            }
        };

        picker.setShowOnFieldClick(true);

        return picker;
    }

    /**
     * Custom form validator for registering and handling DateTimeField
     * components that are in it.
     */
    public class DateTimeFormValidator extends AbstractFormValidator {
        private FormComponent[] dateTimeComponents;

        public DateTimeFormValidator(DateTimeField dateTimeComponent) {
            if(dateTimeComponent == null)
                throw new IllegalArgumentException(
					"argument dateTimeComponent cannot " +
                                        "be null");

            dateTimeComponents = new FormComponent[] {dateTimeComponent};
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
            DateTimeField dateTimeField = (DateTimeField) dateTimeComponents[0];

            StringTokenizer inputDateTokenizer = new StringTokenizer(
                    dateTimeField.getInput(), ",");

            int tokens = inputDateTokenizer.countTokens();

            boolean isValid = true;

            if (tokens < 2) {
                isValid = false;
            } else {
                //First token = date
                String date = inputDateTokenizer.nextToken();
                //Second token = time
                StringTokenizer timeTokenizer = new StringTokenizer(
                        inputDateTokenizer.nextToken(), ":");

                if (timeTokenizer.countTokens() < 2) {
                    isValid = false;
                }

            }

            if (!isValid) {
                ValidationError ve = new ValidationError();
                ve.setVariables(DateTimeFormValidator.this.variablesMap());
                ve.addMessageKey(resourceKey());
                dateTimeComponents[0].error((IValidationError) ve);
            }
        }
    }
}