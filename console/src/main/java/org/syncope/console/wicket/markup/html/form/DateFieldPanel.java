/*
 *  Copyright 2010 luis.
 * 
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
import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * DateFieldPanel
 */
public class DateFieldPanel extends Panel {
    /**
     * Build a DateFieldPanel.
     * @param component id
     * @param label name
     * @param IModel<?> object
     * @param required flag
     */
    public DateFieldPanel(String id, String name ,IModel<?> model, boolean required) {
        super(id, model);

        if (required) {
            add(new Label("required", "*"));
        } else {
            add(new Label("required", ""));
        }

        UpdatingTextField dateField = new UpdatingTextField("dateField", model, Date.class);
        dateField.setRequired(required);
        dateField.setLabel(new Model(name));
        dateField.add(new DatePicker());

        add(dateField);
    }
}
