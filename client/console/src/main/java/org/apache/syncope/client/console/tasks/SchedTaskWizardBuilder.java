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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AbstractProvisioningTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class SchedTaskWizardBuilder<T extends SchedTaskTO> extends AjaxWizardBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final TaskRestClient taskRestClient = new TaskRestClient();

    private PushTaskWrapper wrapper;

    private CrontabPanel crontabPanel;

    private final LoadableDetachableModel<List<String>> realms = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            List<String> result = new RealmRestClient().list().stream().
                    map(RealmTO::getFullPath).collect(Collectors.toList());
            Collections.sort(result);

            return result;
        }
    };

    public SchedTaskWizardBuilder(final T taskTO, final PageReference pageRef) {
        super(taskTO, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final SchedTaskTO modelObject) {
        if (modelObject instanceof PushTaskTO && wrapper != null) {
            wrapper.fillFilterConditions();
        }

        modelObject.setCronExpression(crontabPanel.getCronExpression());
        if (modelObject.getKey() == null) {
            taskRestClient.create(modelObject);
        } else {
            taskRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SchedTaskTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        if (modelObject instanceof PushTaskTO) {
            wrapper = new PushTaskWrapper(PushTaskTO.class.cast(modelObject));
            wizardModel.add(new PushTaskFilters(wrapper));
        }
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final IModel<List<String>> taskJobClasses = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getTaskJobs());
            }
        };

        private final IModel<List<String>> reconciliationFilterBuilderClasses =
                new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getReconciliationFilterBuilders());
            }
        };

        private final IModel<List<String>> pullActionsClasses = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getPullActions());
            }
        };

        private final IModel<List<String>> pushActionsClasses = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getPushActions());
            }
        };

        public Profile(final SchedTaskTO taskTO) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel("name", "name", new PropertyModel<>(taskTO, "name"),
                    false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    "description", "description", new PropertyModel<>(taskTO, "description"), false);
            description.setEnabled(true);
            add(description);

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel("active", "active", new PropertyModel<>(taskTO, "active"),
                    false);
            add(active);

            AjaxDropDownChoicePanel<String> jobDelegateClassName = new AjaxDropDownChoicePanel<>(
                    "jobDelegateClassName", "jobDelegateClassName",
                    new PropertyModel<>(taskTO, "jobDelegateClassName"), false);
            jobDelegateClassName.setChoices(taskJobClasses.getObject());
            jobDelegateClassName.addRequiredLabel();
            jobDelegateClassName.setEnabled(taskTO.getKey() == null);
            jobDelegateClassName.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
            add(jobDelegateClassName);

            // ------------------------------
            // Only for pull tasks
            // ------------------------------            
            WebMarkupContainer pullTaskSpecifics = new WebMarkupContainer("pullTaskSpecifics");
            add(pullTaskSpecifics.setRenderBodyOnly(true));

            boolean isFiltered = false;
            if (taskTO instanceof PullTaskTO) {
                isFiltered = PullTaskTO.class.cast(taskTO).getPullMode() == PullMode.FILTERED_RECONCILIATION;
            } else {
                pullTaskSpecifics.setEnabled(false).setVisible(false);
            }

            final AjaxDropDownChoicePanel<PullMode> pullMode = new AjaxDropDownChoicePanel<>(
                    "pullMode", "pullMode", new PropertyModel<>(taskTO, "pullMode"), false);
            pullMode.setChoices(Arrays.asList(PullMode.values()));
            if (taskTO instanceof PullTaskTO) {
                pullMode.addRequiredLabel();
            }
            pullMode.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(pullMode);

            final AjaxDropDownChoicePanel<String> reconciliationFilterBuilderClassName = new AjaxDropDownChoicePanel<>(
                    "reconciliationFilterBuilderClassName", "reconciliationFilterBuilderClassName",
                    new PropertyModel<>(taskTO, "reconciliationFilterBuilderClassName"), false);
            reconciliationFilterBuilderClassName.setChoices(reconciliationFilterBuilderClasses.getObject());
            reconciliationFilterBuilderClassName.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
            reconciliationFilterBuilderClassName.setEnabled(isFiltered);
            reconciliationFilterBuilderClassName.setRequired(isFiltered);
            pullTaskSpecifics.add(reconciliationFilterBuilderClassName);

            pullMode.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    reconciliationFilterBuilderClassName.setEnabled(
                            pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    reconciliationFilterBuilderClassName.setRequired(
                            pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    target.add(reconciliationFilterBuilderClassName);
                }
            });

            AjaxDropDownChoicePanel<String> destinationRealm = new AjaxDropDownChoicePanel<>(
                    "destinationRealm", "destinationRealm",
                    new PropertyModel<String>(taskTO, "destinationRealm"), false).
                    setChoices(realms);
            if (taskTO instanceof PullTaskTO) {
                destinationRealm.addRequiredLabel();
            }
            destinationRealm.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(destinationRealm);

            // ------------------------------
            // Only for pull tasks
            // ------------------------------  
            WebMarkupContainer pushTaskSpecifics = new WebMarkupContainer("pushTaskSpecifics");
            add(pushTaskSpecifics.setRenderBodyOnly(true));

            if (!(taskTO instanceof PushTaskTO)) {
                pushTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxDropDownChoicePanel<String> sourceRealm = new AjaxDropDownChoicePanel<>(
                    "sourceRealm", "sourceRealm",
                    new PropertyModel<String>(taskTO, "sourceRealm"), false).
                    setChoices(realms);
            if (taskTO instanceof PushTaskTO) {
                sourceRealm.addRequiredLabel();
            }
            sourceRealm.setNullValid(!(taskTO instanceof PushTaskTO));
            pushTaskSpecifics.add(sourceRealm);

            // ------------------------------
            // For push and pull tasks
            // ------------------------------
            WebMarkupContainer provisioningTaskSpecifics = new WebMarkupContainer("provisioningTaskSpecifics");
            add(provisioningTaskSpecifics.setRenderBodyOnly(true));

            if (taskTO instanceof AbstractProvisioningTaskTO) {
                jobDelegateClassName.setEnabled(false).setVisible(false);
            } else {
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxPalettePanel<String> actionsClassNames = new AjaxPalettePanel.Builder<String>().
                    setAllowMoveAll(true).setAllowOrder(true).
                    build("actionsClassNames",
                            new PropertyModel<List<String>>(taskTO, "actionsClassNames"),
                            new ListModel<>(taskTO instanceof PushTaskTO
                                    ? pushActionsClasses.getObject() : pullActionsClasses.getObject()));
            actionsClassNames.setOutputMarkupId(true);
            provisioningTaskSpecifics.add(actionsClassNames);

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(taskTO, "matchingRule"), false);
            matchingRule.setChoices(Arrays.asList(MatchingRule.values()));
            provisioningTaskSpecifics.add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(taskTO, "unmatchingRule"),
                    false);
            unmatchingRule.setChoices(Arrays.asList(UnmatchingRule.values()));
            provisioningTaskSpecifics.add(unmatchingRule);

            AjaxCheckBoxPanel performCreate = new AjaxCheckBoxPanel(
                    "performCreate", "performCreate", new PropertyModel<>(taskTO, "performCreate"), false);
            provisioningTaskSpecifics.add(performCreate);

            AjaxCheckBoxPanel performUpdate = new AjaxCheckBoxPanel(
                    "performUpdate", "performUpdate", new PropertyModel<>(taskTO, "performUpdate"), false);
            provisioningTaskSpecifics.add(performUpdate);

            AjaxCheckBoxPanel performDelete = new AjaxCheckBoxPanel(
                    "performDelete", "performDelete", new PropertyModel<>(taskTO, "performDelete"), false);
            provisioningTaskSpecifics.add(performDelete);

            AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel(
                    "syncStatus", "syncStatus", new PropertyModel<>(taskTO, "syncStatus"), false);
            provisioningTaskSpecifics.add(syncStatus);
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final SchedTaskTO taskTO) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<>(taskTO, "cronExpression"), taskTO.getCronExpression());
            add(crontabPanel);
        }
    }
}
