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
package org.apache.syncope.client.console.reports;

import java.io.Serializable;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.tasks.CrontabPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;

public class ReportWizardBuilder extends BaseAjaxWizardBuilder<ReportTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final ReportRestClient restClient = new ReportRestClient();

    private CrontabPanel crontabPanel;

    public ReportWizardBuilder(final ReportTO reportTO, final PageReference pageRef) {
        super(reportTO, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final ReportTO modelObject) {
        modelObject.setCronExpression(crontabPanel.getCronExpression());
        if (modelObject.getKey() == null) {
            ReportRestClient.create(modelObject);
        } else {
            ReportRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final ReportTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        public Profile(final ReportTO reportTO) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(reportTO, Constants.NAME_FIELD_NAME), false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<>(
                    "template", getString("template"),
                    new PropertyModel<>(reportTO, "template"));
            template.setChoices(restClient.listTemplates().stream().
                    map(EntityTO::getKey).collect(Collectors.toList()));

            template.addRequiredLabel();
            add(template);

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel(
                    "active", "active", new PropertyModel<>(reportTO, "active"), false);
            add(active);
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final ReportTO reportTO) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<>(reportTO, "cronExpression"), reportTO.getCronExpression());
            add(crontabPanel);
        }

    }
}
