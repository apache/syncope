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
package org.apache.syncope.console.markup.html;

import java.util.Arrays;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.commons.SelectOption;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class CrontabContainer extends WebMarkupContainer {

    private static final long serialVersionUID = 7879593326085337650L;

    private final TextField seconds;

    private final TextField minutes;

    private final TextField hours;

    private final TextField daysOfMonth;

    private final TextField months;

    private final TextField daysOfWeek;

    public CrontabContainer(final String id, final PropertyModel<String> cronExpressionModel,
            final String cronExpression) {

        super(id);
        setOutputMarkupId(true);

        final SelectOption[] CRON_TEMPLATES = {
            new SelectOption(getString("selOpt1"), "UNSCHEDULE"),
            new SelectOption(getString("selOpt2"), "0 0/5 * * * ?"),
            new SelectOption(getString("selOpt3"), "0 0 12 * * ?"),
            new SelectOption(getString("selOpt4"), "0 0 0 1 * ?"),
            new SelectOption(getString("selOpt5"), "0 0 0 L * ?"),
            new SelectOption(getString("selOpt6"), "0 0 0 ? * 2")
        };

        final DropDownChoice<SelectOption> cronTemplateChooser =
                new DropDownChoice<SelectOption>("cronTemplateChooser") {

            private static final long serialVersionUID = -5843424545478691442L;

            @Override
            protected CharSequence getDefaultChoice(final String selected) {
                return "<option value=\"\">" + getString("chooseForTemplate") + "</option>";
            }
        };

        cronTemplateChooser.setModel(
                new IModel<SelectOption>() {

            private static final long serialVersionUID = 6762568283146531315L;

            @Override
            public SelectOption getObject() {
                SelectOption result = null;
                for (SelectOption so : CRON_TEMPLATES) {
                    if (so.getKeyValue().equals(cronExpressionModel.getObject())) {

                        result = so;
                    }
                }

                return result;
            }

            @Override
            public void setObject(final SelectOption object) {
                cronExpressionModel.setObject(object == null || object.equals(CRON_TEMPLATES[0])
                        ? null
                        : object.toString());
            }

            @Override
            public void detach() {
            }
        });
        cronTemplateChooser.setChoices(Arrays.asList(CRON_TEMPLATES));
        cronTemplateChooser.setChoiceRenderer(new SelectChoiceRenderer());
        add(cronTemplateChooser);

        seconds = new TextField("seconds", new Model(getCronField(cronExpression, 0)));
        add(seconds);

        minutes = new TextField("minutes", new Model(getCronField(cronExpression, 1)));
        add(minutes);

        hours = new TextField("hours", new Model(getCronField(cronExpression, 2)));
        add(hours);

        daysOfMonth = new TextField("daysOfMonth", new Model(getCronField(cronExpression, 3)));
        add(daysOfMonth);

        months = new TextField("months", new Model(getCronField(cronExpression, 4)));
        add(months);

        daysOfWeek = new TextField("daysOfWeek", new Model(getCronField(cronExpression, 5)));
        add(daysOfWeek);

        cronTemplateChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                seconds.setModelObject(getCronField(cronTemplateChooser, 0));
                minutes.setModelObject(getCronField(cronTemplateChooser, 1));
                hours.setModelObject(getCronField(cronTemplateChooser, 2));
                daysOfMonth.setModelObject(getCronField(cronTemplateChooser, 3));
                months.setModelObject(getCronField(cronTemplateChooser, 4));
                daysOfWeek.setModelObject(getCronField(cronTemplateChooser, 5));
                target.add(CrontabContainer.this);
            }
        });
    }

    private String getCronField(final FormComponent formComponent, final int field) {
        String cronField = null;

        if (formComponent != null) {
            cronField = getCronField(formComponent.getInput(), field);
        }

        return cronField;
    }

    private String getCronField(final String cron, final int field) {
        String cronField = null;

        if (cron != null && !cron.isEmpty() && !"UNSCHEDULE".equals(cron)) {
            cronField = cron.split(" ")[field].trim();
        }

        return cronField;
    }

    public String getCronExpression() {
        String cronExpression = null;

        if (seconds != null && seconds.getInput() != null && minutes != null && minutes.getInput() != null
                && hours != null && hours.getInput() != null && daysOfMonth != null && daysOfMonth.getInput() != null
                && months != null && months.getInput() != null && daysOfWeek != null && daysOfWeek.getInput() != null) {

            cronExpression = new StringBuilder().
                    append(seconds.getInput().trim()).append(" ").
                    append(minutes.getInput().trim()).append(" ").
                    append(hours.getInput().trim()).append(" ").
                    append(daysOfMonth.getInput().trim()).append(" ").
                    append(months.getInput().trim()).append(" ").
                    append(daysOfWeek.getInput().trim()).toString();
        }

        return cronExpression;
    }
}
