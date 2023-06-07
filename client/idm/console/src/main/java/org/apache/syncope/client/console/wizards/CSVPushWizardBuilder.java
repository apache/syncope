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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.panels.CSVConfPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.AjaxDownloadBehavior;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;

public class CSVPushWizardBuilder extends BaseAjaxWizardBuilder<CSVPushSpec> {

    private static final long serialVersionUID = -8890710681835661962L;

    protected final AnyQuery query;

    protected final AjaxDownloadBehavior downloadBehavior;

    protected final ReconciliationRestClient reconciliationRestClient;

    protected final ImplementationRestClient implementationRestClient;

    public CSVPushWizardBuilder(
            final CSVPushSpec defaultItem,
            final AnyQuery query,
            final AjaxDownloadBehavior downloadBehavior,
            final ReconciliationRestClient reconciliationRestClient,
            final ImplementationRestClient implementationRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.query = query;
        this.downloadBehavior = downloadBehavior;
        this.reconciliationRestClient = reconciliationRestClient;
        this.implementationRestClient = implementationRestClient;
    }

    @Override
    public CSVPushWizardBuilder setEventSink(final IEventSink eventSink) {
        super.setEventSink(eventSink);
        return this;
    }

    @Override
    protected Serializable onApplyInternal(final CSVPushSpec modelObject) {
        return RequestCycle.get().find(AjaxRequestTarget.class).map(target -> {
            try {
                downloadBehavior.setResponse(new ResponseHolder(reconciliationRestClient.push(query, modelObject)));
                downloadBehavior.initiate(target);

                return Constants.OPERATION_SUCCEEDED;
            } catch (Exception e) {
                LOG.error("While dowloading CSV export", e);
                return e;
            }
        }).orElse(Constants.ERROR);
    }

    @Override
    protected WizardModel buildModelSteps(final CSVPushSpec modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new PushTask(modelObject));
        return wizardModel;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = -2368995286051427297L;

        public Details(final CSVPushSpec spec) {
            add(new CSVConfPanel("csvconf", spec));
        }
    }

    public class PushTask extends WizardStep {

        private static final long serialVersionUID = -2747583614435078452L;

        private final IModel<List<String>> propActions = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 4659376149825914247L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.PROPAGATION_ACTIONS).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        private final IModel<List<String>> pushActions = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 4659376149825914247L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.PUSH_ACTIONS).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        public PushTask(final CSVPushSpec spec) {
            AjaxCheckBoxPanel ignorePaging = new AjaxCheckBoxPanel(
                    "ignorePaging", "ignorePaging", new PropertyModel<>(spec, "ignorePaging"), true);
            add(ignorePaging);

            AjaxPalettePanel<String> propagationActions =
                    new AjaxPalettePanel.Builder<String>().build("propagationActions",
                            new PropertyModel<>(spec, "propagationActions"), new ListModel<>(propActions.getObject()));
            add(propagationActions);

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(spec, "matchingRule"), false);
            matchingRule.setChoices(List.of(MatchingRule.values()));
            add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(spec, "unmatchingRule"),
                    false);
            unmatchingRule.setChoices(List.of(UnmatchingRule.values()));
            add(unmatchingRule);

            AjaxPalettePanel<String> provisioningActions =
                    new AjaxPalettePanel.Builder<String>().build("provisioningActions",
                            new PropertyModel<>(spec, "provisioningActions"), new ListModel<>(pushActions.getObject()));
            add(provisioningActions);
        }
    }
}
