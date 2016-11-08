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
import java.util.ArrayList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.tasks.CrontabPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.PropertyModel;

public class ReportWizardBuilder extends AjaxWizardBuilder<ReportTO> {

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
            restClient.create(modelObject);
        } else {
            restClient.update(modelObject);
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
                    "name", "name", new PropertyModel<String>(reportTO, "name"), false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            final AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<>(
                    "template", getString("template"),
                    new PropertyModel<String>(reportTO, "template"));
            template.setChoices(CollectionUtils.collect(
                    restClient.listTemplates(), new Transformer<ReportTemplateTO, String>() {

                @Override
                public String transform(final ReportTemplateTO input) {
                    return input.getKey();
                }
            }, new ArrayList<String>()));

            template.addRequiredLabel();
            add(template);

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel(
                    "active", "active", new PropertyModel<Boolean>(reportTO, "active"), false);
            add(active);
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final ReportTO reportTO) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<String>(reportTO, "cronExpression"), reportTO.getCronExpression());
            add(crontabPanel);
        }

    }
}
