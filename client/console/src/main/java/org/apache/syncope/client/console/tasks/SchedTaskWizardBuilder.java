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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DateFormatROModel;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.AbstractProvisioningTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
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

public class SchedTaskWizardBuilder<T extends SchedTaskTO> extends AjaxWizardBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final TaskRestClient taskRestClient = new TaskRestClient();

    private final LoadableDetachableModel<List<String>> realms = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            List<String> result = CollectionUtils.collect(
                    new RealmRestClient().list(), new Transformer<RealmTO, String>() {

                @Override
                public String transform(final RealmTO realm) {
                    return realm.getFullPath();
                }
            }, new ArrayList<String>());

            Collections.sort(result);

            return result;
        }
    };

    public SchedTaskWizardBuilder(final T taskTO, final PageReference pageRef) {
        super("wizard", taskTO, pageRef);
    }

    @Override
    protected Serializable onApplyInternal(final SchedTaskTO modelObject) {
        if (modelObject.getKey() == null || modelObject.getKey() == 0L) {
            taskRestClient.create(modelObject);
        } else {
            taskRestClient.update(modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SchedTaskTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final IModel<List<String>> classNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ArrayList<>(new TaskRestClient().getJobClasses());
            }
        };

        public Profile(final SchedTaskTO taskTO) {
            final AjaxTextFieldPanel name = new AjaxTextFieldPanel("name", "name", new PropertyModel<String>(taskTO,
                    "name"), false);
            name.setEnabled(true);
            add(name);

            final AjaxTextFieldPanel description = new AjaxTextFieldPanel("description", "description",
                    new PropertyModel<String>(taskTO, "description"), false);
            description.setEnabled(true);
            add(description);

            final AjaxCheckBoxPanel active = new AjaxCheckBoxPanel("active", "active",
                    new PropertyModel<Boolean>(taskTO, "active"), false);
            add(active);

            final WebMarkupContainer pullTaskSpecifics = new WebMarkupContainer("pullTaskSpecifics");
            add(pullTaskSpecifics.setRenderBodyOnly(true));

            boolean isFiltered = false;

            if (taskTO instanceof PullTaskTO) {
                isFiltered = PullTaskTO.class.cast(taskTO).getPullMode() == PullMode.FILTERED_RECONCILIATION;
            } else {
                pullTaskSpecifics.setEnabled(false).setVisible(false);
            }

            final AjaxDropDownChoicePanel<PullMode> pullMode = new AjaxDropDownChoicePanel<>("pullMode", "pullMode",
                    new PropertyModel<PullMode>(taskTO, "pullMode"), false);
            pullMode.setChoices(Arrays.asList(PullMode.values()));
            pullMode.setRequired(taskTO instanceof PullTaskTO);
            pullMode.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(pullMode);

            final AjaxTextFieldPanel filter = new AjaxTextFieldPanel(
                    "reconciliationFilterBuilderClassName", "reconciliationFilterBuilderClassName",
                    new PropertyModel<String>(taskTO, "reconciliationFilterBuilderClassName"), false);
            pullTaskSpecifics.add(filter);
            filter.setEnabled(isFiltered);

            pullMode.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    filter.setEnabled(pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    target.add(filter);
                }
            });

            final AjaxDropDownChoicePanel<String> destinationRealm = new AjaxDropDownChoicePanel<>(
                    "destinationRealm", "destinationRealm",
                    new PropertyModel<String>(taskTO, "destinationRealm"), false).
                    setChoices(realms);
            destinationRealm.setRequired(taskTO instanceof PullTaskTO);
            destinationRealm.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(destinationRealm);

            final AjaxDropDownChoicePanel<String> className = new AjaxDropDownChoicePanel<>(
                    "jobDelegateClassName",
                    getString("jobDelegateClassName"),
                    new PropertyModel<String>(taskTO, "jobDelegateClassName"), false);

            className.setChoices(classNames.getObject());
            className.addRequiredLabel();
            className.setEnabled(taskTO.getKey() == null || taskTO.getKey() == 0L);
            className.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");
            add(className);

            final WebMarkupContainer provisioningTaskSpecifics = new WebMarkupContainer("provisioningTaskSpecifics");
            add(provisioningTaskSpecifics.setRenderBodyOnly(true));

            final AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<MatchingRule>(taskTO, "matchingRule"), false);
            provisioningTaskSpecifics.add(matchingRule);
            matchingRule.setChoices(Arrays.asList(MatchingRule.values()));

            final AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<UnmatchingRule>(taskTO, "unmatchingRule"),
                    false);
            provisioningTaskSpecifics.add(unmatchingRule);
            unmatchingRule.setChoices(Arrays.asList(UnmatchingRule.values()));

            final AjaxCheckBoxPanel performCreate = new AjaxCheckBoxPanel(
                    "performCreate", "performCreate", new PropertyModel<Boolean>(taskTO, "performCreate"), false);
            provisioningTaskSpecifics.add(performCreate);

            final AjaxCheckBoxPanel performUpdate = new AjaxCheckBoxPanel(
                    "performUpdate", "performUpdate", new PropertyModel<Boolean>(taskTO, "performUpdate"), false);
            provisioningTaskSpecifics.add(performUpdate);

            final AjaxCheckBoxPanel performDelete = new AjaxCheckBoxPanel(
                    "performDelete", "performDelete", new PropertyModel<Boolean>(taskTO, "performDelete"), false);
            provisioningTaskSpecifics.add(performDelete);

            final AjaxCheckBoxPanel pullStatus = new AjaxCheckBoxPanel(
                    "pullStatus", "pullStatus", new PropertyModel<Boolean>(taskTO, "pullStatus"), false);
            provisioningTaskSpecifics.add(pullStatus);

            if (taskTO instanceof AbstractProvisioningTaskTO) {
                className.setEnabled(false).setVisible(false);
            } else {
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
            }

            final AjaxTextFieldPanel lastExec = new AjaxTextFieldPanel("lastExec", "lastExec",
                    new DateFormatROModel(new PropertyModel<String>(taskTO, "lastExec")));
            lastExec.setEnabled(false);
            add(lastExec);

            final AjaxTextFieldPanel nextExec = new AjaxTextFieldPanel("nextExec", "nextExec",
                    new DateFormatROModel(new PropertyModel<String>(taskTO, "nextExec")));
            nextExec.setEnabled(false);
            add(nextExec);
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final SchedTaskTO taskTO) {
            add(new CrontabPanel(
                    "schedule", new PropertyModel<String>(taskTO, "cronExpression"), taskTO.getCronExpression()));
        }

    }
}
