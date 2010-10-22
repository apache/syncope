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
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.Model;
import org.apache.wicket.markup.html.panel.Fragment;

/**
 * DateFieldPanel
 */
public class DateFieldPanel extends Panel {
    /**
     * Build a DateFieldPanel.
     * @param component id
     * @param label name
     * @param IModel<Date> date object
     * @param schema attribute's date pattern
     * @param required flag
     */
    public DateFieldPanel(String id, String name ,IModel<Date> model,
                          final String datePattern, boolean required) {
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

            datePanel.add(field);
        }

        else {
            datePanel = new Fragment("datePanel","dateTimeField",this);

            DateTimeField field = new DateTimeField("field", model);

            field.setRequired(required);
            field.setLabel(new Model(name));
            
            datePanel.add(field);
        }

        add(datePanel);
    }

    /**
     * Build a DateFieldPanel.
     * @param component id
     * @param label name
     * @param IModel<Date> date object
     * @param schema attribute's date pattern
     * @param required flag
     * @param readonly flag
     */
    public DateFieldPanel(String id, String name ,IModel<Date> model,
            final String datePattern, boolean required,boolean readonly) {
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

            field.setRequired(required);
            field.setEnabled(!readonly);
            field.setLabel(new Model(name));

            datePanel.add(field);
        }

        else {
            datePanel = new Fragment("datePanel","dateTimeField",this);

            DateTimeField field = new DateTimeField("field", model);

            field.setRequired(required);
            field.setEnabled(!readonly);
            field.setLabel(new Model(name));

            datePanel.add(field);
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
}
