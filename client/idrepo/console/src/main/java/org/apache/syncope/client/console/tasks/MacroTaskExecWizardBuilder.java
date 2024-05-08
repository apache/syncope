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

import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.panels.SyncopeFormPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class MacroTaskExecWizardBuilder extends BaseAjaxWizardBuilder<MacroTaskTO> {

    private static final long serialVersionUID = 3318576575286024205L;

    protected final TaskRestClient taskRestClient;

    protected final IModel<SyncopeForm> formModel = Model.of();

    protected final Model<Date> startAtDateModel = new Model<>();

    protected final Model<Boolean> dryRunModel = new Model<>(false);

    public MacroTaskExecWizardBuilder(
            final MacroTaskTO defaultItem,
            final TaskRestClient taskRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.taskRestClient = taskRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final MacroTaskTO modelObject) {
        if (formModel.getObject() == null) {
            taskRestClient.startExecution(modelObject.getKey(),
                    startAtDateModel.getObject(),
                    dryRunModel.getObject());
        } else {
            taskRestClient.startExecution(modelObject.getKey(),
                    startAtDateModel.getObject(),
                    dryRunModel.getObject(),
                    formModel.getObject());
        }

        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final MacroTaskTO modelObject, final WizardModel wizardModel) {
        if (!modelObject.getFormPropertyDefs().isEmpty()) {
            formModel.setObject(taskRestClient.getMacroTaskForm(modelObject.getKey()));
            wizardModel.add(new Form());
        }
        wizardModel.add(new StartAt());
        return wizardModel;
    }

    protected class Form extends WizardStep {

        private static final long serialVersionUID = 7352192594863229013L;

        protected Form() {
            add(new SyncopeFormPanel<>("form", formModel.getObject()));
        }
    }

    protected class StartAt extends WizardStep {

        private static final long serialVersionUID = -961082324376783538L;

        protected StartAt() {
            AjaxDateTimeFieldPanel startAtDate = new AjaxDateTimeFieldPanel(
                    "startAtDate", "startAtDate", startAtDateModel,
                    FastDateFormat.getInstance(SyncopeConstants.DATE_PATTERNS[3]));
            add(startAtDate.setReadOnly(true).hideLabel());

            AjaxCheckBoxPanel startAtCheck = new AjaxCheckBoxPanel(
                    "startAtCheck", "startAtCheck", new Model<>(false), false);
            startAtCheck.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    target.add(startAtDate.setModelObject(null).setReadOnly(!startAtCheck.getModelObject()));
                }
            });
            add(startAtCheck);

            add(new AjaxCheckBoxPanel("dryRun", "dryRun", dryRunModel, false));
        }
    }
}
