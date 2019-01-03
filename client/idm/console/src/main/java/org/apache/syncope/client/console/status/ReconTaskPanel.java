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
package org.apache.syncope.client.console.status;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconTaskPanel extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = 5870444905957760434L;

    protected static final Logger LOG = LoggerFactory.getLogger(ReconTaskPanel.class);

    private final ReconciliationRestClient restClient = new ReconciliationRestClient();

    private final ImplementationRestClient implRestClient = new ImplementationRestClient();

    private final IModel<List<String>> pullActions = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implRestClient.list(ImplementationType.PULL_ACTIONS).stream().
                    map(EntityTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    private final IModel<List<String>> pushActions = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implRestClient.list(ImplementationType.PUSH_ACTIONS).stream().
                    map(EntityTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    public ReconTaskPanel(
            final String resource,
            final ProvisioningTaskTO taskTO,
            final AnyTypeKind anyTypeKind,
            final String anyKey,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef) {

        Form<ProvisioningTaskTO> form = new Form<>("form", new CompoundPropertyModel<>(taskTO));
        add(form);

        AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                setAllowMoveAll(true).setAllowOrder(true).
                build("actions",
                        new PropertyModel<List<String>>(taskTO, "actions"),
                        new ListModel<>(taskTO instanceof PushTaskTO
                                ? pushActions.getObject() : pullActions.getObject()));
        actions.setOutputMarkupId(true);
        form.add(actions);

        AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                "matchingRule", "matchingRule", new PropertyModel<MatchingRule>(taskTO, "matchingRule"), false);
        matchingRule.setChoices(Arrays.asList(MatchingRule.values()));
        form.add(matchingRule);

        AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                "unmatchingRule", "unmatchingRule", new PropertyModel<UnmatchingRule>(taskTO, "unmatchingRule"),
                false);
        unmatchingRule.setChoices(Arrays.asList(UnmatchingRule.values()));
        form.add(unmatchingRule);

        taskTO.setPerformCreate(true);
        AjaxCheckBoxPanel performCreate = new AjaxCheckBoxPanel(
                "performCreate", "performCreate", new PropertyModel<Boolean>(taskTO, "performCreate"), false);
        form.add(performCreate);

        taskTO.setPerformUpdate(true);
        AjaxCheckBoxPanel performUpdate = new AjaxCheckBoxPanel(
                "performUpdate", "performUpdate", new PropertyModel<Boolean>(taskTO, "performUpdate"), false);
        form.add(performUpdate);

        taskTO.setPerformDelete(true);
        AjaxCheckBoxPanel performDelete = new AjaxCheckBoxPanel(
                "performDelete", "performDelete", new PropertyModel<Boolean>(taskTO, "performDelete"), false);
        form.add(performDelete);

        taskTO.setSyncStatus(true);
        AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel(
                "syncStatus", "syncStatus", new PropertyModel<Boolean>(taskTO, "syncStatus"), false);
        form.add(syncStatus);

        form.add(new AjaxSubmitLink("reconcile") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    if (taskTO instanceof PushTaskTO) {
                        restClient.push(anyTypeKind, anyKey, resource, (PushTaskTO) form.getModelObject());
                    } else {
                        restClient.pull(anyTypeKind, anyKey, resource, (PullTaskTO) form.getModelObject());
                    }

                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While attempting reconciliation on {} {} {} {}",
                            anyTypeKind, anyKey, resource, form.getModelObject(), e);
                    SyncopeConsoleSession.get().error(resource + ": "
                            + (StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage()));
                }
                multiLevelPanelRef.prev(target);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }
}
