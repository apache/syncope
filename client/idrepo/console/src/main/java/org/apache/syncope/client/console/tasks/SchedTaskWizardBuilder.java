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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.InboundTaskTO;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.springframework.beans.PropertyAccessorFactory;

public class SchedTaskWizardBuilder<T extends SchedTaskTO> extends BaseAjaxWizardBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected final TaskType type;

    protected final RealmRestClient realmRestClient;

    protected final TaskRestClient taskRestClient;

    protected PushTaskWrapper wrapper;

    protected CrontabPanel crontabPanel;

    protected final boolean fullRealmsTree;

    public SchedTaskWizardBuilder(
            final TaskType type,
            final T taskTO,
            final RealmRestClient realmRestClient,
            final TaskRestClient taskRestClient,
            final PageReference pageRef) {

        super(taskTO, pageRef);
        this.type = type;
        this.realmRestClient = realmRestClient;
        this.taskRestClient = taskRestClient;
        this.fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
    }

    @Override
    protected Serializable onApplyInternal(final SchedTaskTO modelObject) {
        if (modelObject instanceof PushTaskTO && wrapper != null) {
            wrapper.fillFilterConditions();
        }

        Optional.ofNullable(crontabPanel).ifPresent(cp -> modelObject.setCronExpression(cp.getCronExpression()));
        if (modelObject.getKey() == null) {
            taskRestClient.create(type, modelObject);
        } else {
            taskRestClient.update(type, modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SchedTaskTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        if (modelObject instanceof PushTaskTO pushTask) {
            wrapper = new PushTaskWrapper(pushTask);
            wizardModel.add(new PushTaskFilters(wrapper, pageRef));
        }
        if (!(modelObject instanceof LiveSyncTaskTO)) {
            wizardModel.add(new Schedule(modelObject));
        }
        return wizardModel;
    }

    protected List<String> searchRealms(final String realmQuery) {
        return realmRestClient.search(fullRealmsTree
                ? RealmsUtils.buildBaseQuery()
                : RealmsUtils.buildKeywordQuery(realmQuery)).
                getResult().stream().map(RealmTO::getFullPath).collect(Collectors.toList());
    }

    protected class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        protected final IModel<List<String>> taskJobDelegates = SyncopeWebApplication.get().
                getImplementationInfoProvider().getTaskJobDelegates();

        protected final IModel<List<String>> reconFilterBuilders = SyncopeWebApplication.get().
                getImplementationInfoProvider().getReconFilterBuilders();

        protected final IModel<List<String>> liveSyncDeltaMappers = SyncopeWebApplication.get().
                getImplementationInfoProvider().getLiveSyncDeltaMappers();

        protected final IModel<List<String>> macroActions = SyncopeWebApplication.get().
                getImplementationInfoProvider().getMacroActions();

        protected final IModel<List<String>> inboundActions = SyncopeWebApplication.get().
                getImplementationInfoProvider().getInboundActions();

        protected final IModel<List<String>> pushActions = SyncopeWebApplication.get().
                getImplementationInfoProvider().getPushActions();

        protected Profile(final SchedTaskTO taskTO) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(taskTO, Constants.NAME_FIELD_NAME),
                    false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME,
                    new PropertyModel<>(taskTO, Constants.DESCRIPTION_FIELD_NAME), false);
            description.setEnabled(true);
            add(description);

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel(
                    "active", "active", new PropertyModel<>(taskTO, "active"), false);
            add(active);

            AjaxDropDownChoicePanel<String> jobDelegate = new AjaxDropDownChoicePanel<>(
                    "jobDelegate", "jobDelegate", new PropertyModel<>(taskTO, "jobDelegate"), false);
            jobDelegate.setChoices(taskJobDelegates.getObject());
            jobDelegate.addRequiredLabel();
            jobDelegate.setEnabled(taskTO.getKey() == null);
            add(jobDelegate);

            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);

            // ------------------------------
            // Only for macro tasks
            // ------------------------------            
            WebMarkupContainer macroTaskSpecifics = new WebMarkupContainer("macroTaskSpecifics");
            add(macroTaskSpecifics.setRenderBodyOnly(true));

            AjaxSearchFieldPanel realm = new AjaxSearchFieldPanel(
                    "realm", "realm", new PropertyModel<>(taskTO, "realm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };
            if (taskTO instanceof MacroTaskTO macroTask) {
                realm.addRequiredLabel();
                if (StringUtils.isBlank(macroTask.getRealm())) {
                    // add a default destination realm if missing in the task
                    realm.setModelObject(SyncopeConstants.ROOT_REALM);
                }
            }
            macroTaskSpecifics.add(realm);

            macroTaskSpecifics.add(new AjaxDropDownChoicePanel<>(
                    "macroActions", "macroActions", new PropertyModel<>(taskTO, "macroActions"), false).
                    setChoices(macroActions));

            AjaxCheckBoxPanel continueOnError = new AjaxCheckBoxPanel(
                    "continueOnError", "continueOnError", new PropertyModel<>(taskTO, "continueOnError"), false);
            macroTaskSpecifics.add(continueOnError);

            AjaxCheckBoxPanel saveExecs = new AjaxCheckBoxPanel(
                    "saveExecs", "saveExecs", new PropertyModel<>(taskTO, "saveExecs"), false);
            macroTaskSpecifics.add(saveExecs);

            // ------------------------------
            // Only for live sync tasks
            // ------------------------------            
            WebMarkupContainer liveSyncTaskSpecifics = new WebMarkupContainer("liveSyncTaskSpecifics");
            add(liveSyncTaskSpecifics.setRenderBodyOnly(true));

            boolean isMapped = false;
            if (taskTO instanceof LiveSyncTaskTO) {
                isMapped = true;
            } else {
                liveSyncTaskSpecifics.setEnabled(false).setVisible(false);
            }

            liveSyncTaskSpecifics.add(destinationRealm("liveSyncDestinationRealm", taskTO, settings));

            liveSyncTaskSpecifics.add(remediation("liveSyncRemediation", taskTO));

            liveSyncTaskSpecifics.add(new AjaxNumberFieldPanel.Builder<Integer>().
                    min(1).build(
                    "liveSyncDelaySecondsAcrossInvocations",
                    "liveSyncDelaySecondsAcrossInvocations",
                    Integer.class,
                    new PropertyModel<>(taskTO, "delaySecondsAcrossInvocations")));

            AjaxDropDownChoicePanel<String> liveSyncDeltaMapper = new AjaxDropDownChoicePanel<>(
                    "liveSyncDeltaMapper", "liveSyncDeltaMapper",
                    new PropertyModel<>(taskTO, "liveSyncDeltaMapper"), false);
            liveSyncDeltaMapper.setChoices(liveSyncDeltaMappers.getObject());
            liveSyncDeltaMapper.setEnabled(isMapped);
            liveSyncDeltaMapper.setRequired(isMapped);
            liveSyncTaskSpecifics.add(liveSyncDeltaMapper);

            // ------------------------------
            // Only for pull tasks
            // ------------------------------
            WebMarkupContainer pullTaskSpecifics = new WebMarkupContainer("pullTaskSpecifics");
            add(pullTaskSpecifics.setRenderBodyOnly(true));

            boolean isFiltered = false;
            if (taskTO instanceof PullTaskTO pullTask) {
                isFiltered = pullTask.getPullMode() == PullMode.FILTERED_RECONCILIATION;
            } else {
                pullTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxDropDownChoicePanel<PullMode> pullMode = new AjaxDropDownChoicePanel<>(
                    "pullMode", "pullMode", new PropertyModel<>(taskTO, "pullMode"), false);
            pullMode.setChoices(List.of(PullMode.values()));
            if (taskTO instanceof PullTaskTO) {
                pullMode.addRequiredLabel();
            }
            pullMode.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(pullMode);

            AjaxDropDownChoicePanel<String> reconFilterBuilder = new AjaxDropDownChoicePanel<>(
                    "reconFilterBuilder", "reconFilterBuilder",
                    new PropertyModel<>(taskTO, "reconFilterBuilder"), false);
            reconFilterBuilder.setChoices(reconFilterBuilders.getObject());
            reconFilterBuilder.setEnabled(isFiltered);
            reconFilterBuilder.setRequired(isFiltered);
            pullTaskSpecifics.add(reconFilterBuilder);

            pullMode.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    reconFilterBuilder.setEnabled(pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    reconFilterBuilder.setRequired(pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    target.add(reconFilterBuilder);
                }
            });

            pullTaskSpecifics.add(destinationRealm("pullDestinationRealm", taskTO, settings));

            pullTaskSpecifics.add(remediation("pullRemediation", taskTO));

            // ------------------------------
            // Only for push tasks
            // ------------------------------
            WebMarkupContainer pushTaskSpecifics = new WebMarkupContainer("pushTaskSpecifics");
            add(pushTaskSpecifics.setRenderBodyOnly(true));

            if (!(taskTO instanceof PushTaskTO)) {
                pushTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxSearchFieldPanel sourceRealm = new AjaxSearchFieldPanel(
                    "sourceRealm", "sourceRealm", new PropertyModel<>(taskTO, "sourceRealm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };

            if (taskTO instanceof PushTaskTO) {
                sourceRealm.addRequiredLabel();
            }
            pushTaskSpecifics.add(sourceRealm);

            // ------------------------------
            // For push and pull tasks
            // ------------------------------
            WebMarkupContainer provisioningTaskSpecifics = new WebMarkupContainer("provisioningTaskSpecifics");
            add(provisioningTaskSpecifics.setOutputMarkupId(true));

            if (taskTO instanceof ProvisioningTaskTO) {
                jobDelegate.setEnabled(false).setVisible(false);
                macroTaskSpecifics.setEnabled(false).setVisible(false);
            } else if (taskTO instanceof MacroTaskTO) {
                jobDelegate.setEnabled(false).setVisible(false);
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
            } else {
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
                macroTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                    setAllowMoveAll(true).setAllowOrder(true).
                    build("actions",
                            new PropertyModel<>(taskTO, "actions"),
                            new ListModel<>(taskTO instanceof PushTaskTO
                                    ? pushActions.getObject() : inboundActions.getObject()));
            provisioningTaskSpecifics.add(actions.setOutputMarkupId(true));

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(taskTO, "matchingRule"), false);
            matchingRule.setChoices(List.of(MatchingRule.values()));
            provisioningTaskSpecifics.add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(taskTO, "unmatchingRule"), false);
            unmatchingRule.setChoices(List.of(UnmatchingRule.values()));
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

            // Concurrent settings
            PropertyModel<ThreadPoolSettings> concurrentSettingsModel =
                    new PropertyModel<>(taskTO, "concurrentSettings");

            AjaxCheckBoxPanel enableConcurrentSettings = new AjaxCheckBoxPanel(
                    "enableConcurrentSettings", "enableConcurrentSettings", new IModel<Boolean>() {

                private static final long serialVersionUID = -7126718045816207110L;

                @Override
                public Boolean getObject() {
                    return concurrentSettingsModel.getObject() != null;
                }

                @Override
                public void setObject(final Boolean object) {
                    // nothing to do
                }
            });
            provisioningTaskSpecifics.add(enableConcurrentSettings.
                    setVisible(taskTO instanceof ProvisioningTaskTO).setOutputMarkupId(true));

            FieldPanel<Integer> poolSize = new AjaxNumberFieldPanel.Builder<Integer>().min(1).build(
                    "poolSize",
                    "poolSize",
                    Integer.class,
                    new ConcurrentSettingsValueModel(concurrentSettingsModel, "poolSize")).setRequired(true);
            poolSize.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
            poolSize.setVisible(taskTO instanceof ProvisioningTaskTO
                    ? concurrentSettingsModel.getObject() != null
                    : false);
            provisioningTaskSpecifics.add(poolSize);

            enableConcurrentSettings.getField().add(
                    new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (concurrentSettingsModel.getObject() == null) {
                        concurrentSettingsModel.setObject(new ThreadPoolSettings());
                    } else {
                        concurrentSettingsModel.setObject(null);
                    }

                    poolSize.setVisible(concurrentSettingsModel.getObject() != null);

                    target.add(provisioningTaskSpecifics);
                }
            });
        }

        private AjaxSearchFieldPanel destinationRealm(
                final String id,
                final SchedTaskTO taskTO,
                final AutoCompleteSettings settings) {

            AjaxSearchFieldPanel destinationRealm = new AjaxSearchFieldPanel(
                    id,
                    "destinationRealm",
                    new PropertyModel<>(taskTO, "destinationRealm"),
                    settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };

            if (taskTO instanceof InboundTaskTO inboundTask) {
                destinationRealm.addRequiredLabel();
                if (StringUtils.isBlank(inboundTask.getDestinationRealm())) {
                    // add a default destination realm if missing in the task
                    destinationRealm.setModelObject(SyncopeConstants.ROOT_REALM);
                }
            }

            return destinationRealm;
        }

        private AjaxCheckBoxPanel remediation(final String id, final SchedTaskTO taskTO) {
            return new AjaxCheckBoxPanel(id, "remediation", new PropertyModel<>(taskTO, "remediation"), false);
        }
    }

    protected static class ConcurrentSettingsValueModel implements IModel<Integer> {

        private static final long serialVersionUID = 8869612332790116116L;

        private final PropertyModel<ThreadPoolSettings> concurrentSettingsModel;

        private final String property;

        public ConcurrentSettingsValueModel(
                final PropertyModel<ThreadPoolSettings> concurrentSettingsModel,
                final String property) {

            this.concurrentSettingsModel = concurrentSettingsModel;
            this.property = property;
        }

        @Override
        public Integer getObject() {
            return Optional.ofNullable(concurrentSettingsModel.getObject()).
                    map(s -> (Integer) PropertyAccessorFactory.forBeanPropertyAccess(s).getPropertyValue(property)).
                    orElse(null);
        }

        @Override
        public void setObject(final Integer object) {
            Optional.ofNullable(concurrentSettingsModel.getObject()).
                    ifPresent(s -> PropertyAccessorFactory.forBeanPropertyAccess(s).setPropertyValue(property, object));
        }
    }

    protected class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        protected Schedule(final SchedTaskTO taskTO) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<>(taskTO, "cronExpression"), taskTO.getCronExpression());
            add(crontabPanel);
        }
    }
}
