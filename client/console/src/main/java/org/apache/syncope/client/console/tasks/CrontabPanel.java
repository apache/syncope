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
package org.apache.syncope.client.console.tasks;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SelectChoiceRenderer;
import org.apache.syncope.client.console.wicket.markup.html.form.SelectOption;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class CrontabPanel extends Panel {

    private static final long serialVersionUID = 7879593326085337650L;

    private final AjaxTextFieldPanel seconds;

    private final AjaxTextFieldPanel minutes;

    private final AjaxTextFieldPanel hours;

    private final AjaxTextFieldPanel daysOfMonth;

    private final AjaxTextFieldPanel months;

    private final AjaxTextFieldPanel daysOfWeek;

    public CrontabPanel(final String id, final PropertyModel<String> cronExpressionModel, final String cronExpression) {
        super(id);
        setOutputMarkupId(true);

        final SelectOption[] cronTemplates = {
            new SelectOption(getString("selOpt1"), "UNSCHEDULE"),
            new SelectOption(getString("selOpt2"), "0 0/5 * * * ?"),
            new SelectOption(getString("selOpt3"), "0 0 12 * * ?"),
            new SelectOption(getString("selOpt4"), "0 0 0 1 * ?"),
            new SelectOption(getString("selOpt5"), "0 0 0 L * ?"),
            new SelectOption(getString("selOpt6"), "0 0 0 ? * 2")
        };

        final AjaxDropDownChoicePanel<SelectOption> cronTemplateChooser = new AjaxDropDownChoicePanel<SelectOption>(
                "cronTemplateChooser", "cronTemplateChooser", new Model<SelectOption>());

        cronTemplateChooser.setNullValid(false);
        cronTemplateChooser.setPlaceholder("chooseForTemplate");

        cronTemplateChooser.getField().setModel(new IModel<SelectOption>() {

            private static final long serialVersionUID = 6762568283146531315L;

            @Override
            public SelectOption getObject() {
                SelectOption result = null;
                for (SelectOption so : cronTemplates) {
                    if (so.getKeyValue().equals(cronExpressionModel.getObject())) {
                        result = so;
                    }
                }

                return result;
            }

            @Override
            public void setObject(final SelectOption object) {
                cronExpressionModel.setObject(object == null || object.equals(cronTemplates[0])
                        ? null
                        : object.toString());
            }

            @Override
            public void detach() {
                // no detach
            }
        });
        cronTemplateChooser.setChoices(Arrays.asList(cronTemplates));
        cronTemplateChooser.setChoiceRenderer(new SelectChoiceRenderer<SelectOption>());
        add(cronTemplateChooser);

        seconds = new AjaxTextFieldPanel("seconds", "seconds", new Model<>(getCronField(cronExpression, 0)));
        add(seconds.hideLabel());

        minutes = new AjaxTextFieldPanel("minutes", "minutes", new Model<>(getCronField(cronExpression, 1)));
        add(minutes.hideLabel());

        hours = new AjaxTextFieldPanel("hours", "hours", new Model<>(getCronField(cronExpression, 2)));
        add(hours.hideLabel());

        daysOfMonth = new AjaxTextFieldPanel(
                "daysOfMonth", "daysOfMonth", new Model<>(getCronField(cronExpression, 3)));
        add(daysOfMonth.hideLabel());

        months = new AjaxTextFieldPanel("months", "months", new Model<>(getCronField(cronExpression, 4)));
        add(months.hideLabel());

        daysOfWeek = new AjaxTextFieldPanel("daysOfWeek", "daysOfWeek", new Model<>(getCronField(cronExpression, 5)));
        add(daysOfWeek.hideLabel());

        final FormComponent<SelectOption> component = cronTemplateChooser.getField();

        cronTemplateChooser.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                seconds.setModelObject(getCronField(component, 0));
                minutes.setModelObject(getCronField(component, 1));
                hours.setModelObject(getCronField(component, 2));
                daysOfMonth.setModelObject(getCronField(component, 3));
                months.setModelObject(getCronField(component, 4));
                daysOfWeek.setModelObject(getCronField(component, 5));
                target.add(CrontabPanel.this);
            }
        });
    }

    private String getCronField(final FormComponent<?> formComponent, final int field) {
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

        if (seconds != null && seconds.getField().getInput() != null
                && minutes != null && minutes.getField().getInput() != null
                && hours != null && hours.getField().getInput() != null
                && daysOfMonth != null && daysOfMonth.getField().getInput() != null
                && months != null && months.getField().getInput() != null
                && daysOfWeek != null && daysOfWeek.getField().getInput() != null) {

            cronExpression = new StringBuilder().
                    append(seconds.getField().getInput().trim()).append(" ").
                    append(minutes.getField().getInput().trim()).append(" ").
                    append(hours.getField().getInput().trim()).append(" ").
                    append(daysOfMonth.getField().getInput().trim()).append(" ").
                    append(months.getField().getInput().trim()).append(" ").
                    append(daysOfWeek.getField().getInput().trim()).toString();
        }

        return StringUtils.isNotBlank(cronExpression) ? cronExpression : null;
    }
}
