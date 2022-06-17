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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.SelectChoiceRenderer;
import org.apache.syncope.client.ui.commons.markup.html.form.SelectOption;
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

        final AjaxDropDownChoicePanel<SelectOption> cronTemplateChooser = new AjaxDropDownChoicePanel<>(
                "cronTemplateChooser", "cronTemplateChooser", new Model<>());

        cronTemplateChooser.setNullValid(false);
        cronTemplateChooser.setPlaceholder("chooseForTemplate");

        cronTemplateChooser.getField().setModel(new IModel<>() {

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
        cronTemplateChooser.setChoices(List.of(cronTemplates));
        cronTemplateChooser.setChoiceRenderer(new SelectChoiceRenderer<>());
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

    private static String getCronField(final FormComponent<?> formComponent, final int field) {
        String cronField = null;

        if (formComponent != null) {
            cronField = getCronField(formComponent.getInput(), field);
        }

        return cronField;
    }

    private static String getCronField(final String cron, final int field) {
        String cronField = null;

        if (cron != null && !cron.isEmpty() && !"UNSCHEDULE".equals(cron)) {
            cronField = cron.split(" ")[field].trim();
        }

        return cronField;
    }

    public String getCronExpression() {
        String cronExpression = null;

        if (seconds != null && seconds.getModelObject() != null
                && minutes != null && minutes.getModelObject() != null
                && hours != null && hours.getModelObject() != null
                && daysOfMonth != null && daysOfMonth.getModelObject() != null
                && months != null && months.getModelObject() != null
                && daysOfWeek != null && daysOfWeek.getModelObject() != null) {

            cronExpression = new StringBuilder().
                    append(seconds.getModelObject().trim()).append(' ').
                    append(minutes.getModelObject().trim()).append(' ').
                    append(hours.getModelObject().trim()).append(' ').
                    append(daysOfMonth.getModelObject().trim()).append(' ').
                    append(months.getModelObject().trim()).append(' ').
                    append(daysOfWeek.getModelObject().trim()).toString();
        }

        return StringUtils.isNotBlank(cronExpression) ? cronExpression : null;
    }
}
