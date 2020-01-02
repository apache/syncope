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
import java.util.Arrays;
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
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.CSVPushSpec;
import org.apache.wicket.PageReference;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class CSVPushWizardBuilder extends BaseAjaxWizardBuilder<CSVPushSpec> {

    private static final long serialVersionUID = -8890710681835661962L;

    private final AnyQuery query;

    private final AjaxDownloadBehavior downloadBehavior;

    public CSVPushWizardBuilder(
            final CSVPushSpec defaultItem,
            final AnyQuery query,
            final AjaxDownloadBehavior downloadBehavior,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.query = query;
        this.downloadBehavior = downloadBehavior;
    }

    @Override
    public CSVPushWizardBuilder setEventSink(final IEventSink eventSink) {
        super.setEventSink(eventSink);
        return this;
    }

    @Override
    protected Serializable onApplyInternal(final CSVPushSpec modelObject) {
        downloadBehavior.setResponse(() -> ReconciliationRestClient.push(query, modelObject));
        return Constants.OPERATION_SUCCEEDED;
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

        private final ImplementationRestClient implRestClient = new ImplementationRestClient();

        private final IModel<List<String>> pushActions = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 4659376149825914247L;

            @Override
            protected List<String> load() {
                return implRestClient.list(IdMImplementationType.PUSH_ACTIONS).stream().
                        map(EntityTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        public PushTask(final CSVPushSpec spec) {
            AjaxCheckBoxPanel ignorePaging = new AjaxCheckBoxPanel(
                    "ignorePaging", "ignorePaging", new PropertyModel<>(spec, "ignorePaging"), true);
            add(ignorePaging);

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(spec, "matchingRule"), false);
            matchingRule.setChoices(Arrays.asList(MatchingRule.values()));
            add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(spec, "unmatchingRule"),
                    false);
            unmatchingRule.setChoices(Arrays.asList(UnmatchingRule.values()));
            add(unmatchingRule);

            AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                    build("actions", new PropertyModel<>(spec, "actions"), new ListModel<>(pushActions.getObject()));
            add(actions);
        }
    }
}
