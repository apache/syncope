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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconTaskPanel extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = 5870444905957760434L;

    protected static final Logger LOG = LoggerFactory.getLogger(ReconTaskPanel.class);

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected RealmRestClient realmRestClient;

    @SpringBean
    protected ReconciliationRestClient reconciliationRestClient;

    protected final IModel<List<String>> inboundActions = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdMImplementationType.INBOUND_ACTIONS).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    protected final IModel<List<String>> pushActions = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return implementationRestClient.list(IdMImplementationType.PUSH_ACTIONS).stream().
                    map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    public ReconTaskPanel(
            final String resource,
            final ProvisioningTaskTO taskTO,
            final String anyType,
            final String anyKey,
            final boolean isOnSyncope,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef) {

        this(resource, taskTO, anyType, anyKey, null, isOnSyncope, multiLevelPanelRef, pageRef);
    }

    public ReconTaskPanel(
            final String resource,
            final ProvisioningTaskTO taskTO,
            final String anyType,
            final String anyKey,
            final String fiql,
            final boolean isOnSyncope,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef) {

        Form<ProvisioningTaskTO> form = new Form<>("form", new CompoundPropertyModel<>(taskTO));
        add(form);

        if (taskTO instanceof PushTaskTO) {
            form.add(new Label("realm", ""));
            form.add(new Label("remediation", ""));
        } else {
            boolean fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);

            AjaxSearchFieldPanel realm = new AjaxSearchFieldPanel(
                    "realm", "destinationRealm", new PropertyModel<>(taskTO, "destinationRealm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? (realmRestClient.search(fullRealmsTree
                                    ? RealmsUtils.buildBaseQuery()
                                    : RealmsUtils.buildKeywordQuery(input)).getResult())
                            : List.<RealmTO>of()).stream().
                            map(RealmTO::getFullPath).iterator();
                }
            };

            realm.addRequiredLabel();
            realm.setOutputMarkupId(true);
            // add a default destination realm if missing in the task
            if (StringUtils.isBlank(PullTaskTO.class.cast(taskTO).getDestinationRealm())) {
                realm.getField().setModelObject(SyncopeConstants.ROOT_REALM);
            }
            if (isOnSyncope) {
                realm.setEnabled(false);
            }
            form.add(realm);

            form.add(new AjaxCheckBoxPanel(
                    "remediation", "remediation", new PropertyModel<>(taskTO, "remediation"), false));
        }

        AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                setAllowMoveAll(true).setAllowOrder(true).
                build("actions",
                        new PropertyModel<>(taskTO, "actions"),
                        new ListModel<>(taskTO instanceof PushTaskTO
                                ? pushActions.getObject() : inboundActions.getObject()));
        actions.setOutputMarkupId(true);
        form.add(actions);

        AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                "matchingRule", "matchingRule", new PropertyModel<>(taskTO, "matchingRule"), false);
        matchingRule.setChoices(List.of(MatchingRule.values()));
        form.add(matchingRule);

        AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                "unmatchingRule", "unmatchingRule", new PropertyModel<>(taskTO, "unmatchingRule"),
                false);
        unmatchingRule.setChoices(List.of(UnmatchingRule.values()));
        form.add(unmatchingRule);

        taskTO.setPerformCreate(true);
        AjaxCheckBoxPanel performCreate = new AjaxCheckBoxPanel(
                "performCreate", "performCreate", new PropertyModel<>(taskTO, "performCreate"), false);
        form.add(performCreate);

        taskTO.setPerformUpdate(true);
        AjaxCheckBoxPanel performUpdate = new AjaxCheckBoxPanel(
                "performUpdate", "performUpdate", new PropertyModel<>(taskTO, "performUpdate"), false);
        form.add(performUpdate);

        taskTO.setPerformDelete(true);
        AjaxCheckBoxPanel performDelete = new AjaxCheckBoxPanel(
                "performDelete", "performDelete", new PropertyModel<>(taskTO, "performDelete"), false);
        form.add(performDelete);

        taskTO.setSyncStatus(true);
        AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel(
                "syncStatus", "syncStatus", new PropertyModel<>(taskTO, "syncStatus"), false);
        form.add(syncStatus);

        form.add(new AjaxSubmitLink("reconcile") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                ReconQuery reconQuery = new ReconQuery.Builder(anyType, resource).anyKey(anyKey).fiql(fiql).build();
                try {
                    if (taskTO instanceof PushTaskTO) {
                        reconciliationRestClient.push(reconQuery, (PushTaskTO) form.getModelObject());
                    } else {
                        reconciliationRestClient.pull(reconQuery, (PullTaskTO) form.getModelObject());
                    }

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                } catch (Exception e) {
                    LOG.error("While attempting reconciliation using query {} on {}",
                            reconQuery, form.getModelObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                multiLevelPanelRef.prev(target);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }
}
