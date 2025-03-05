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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.ComposablePolicy;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Policy rules page.
 *
 * @param <T> policy type.
 */
public class PolicyRuleDirectoryPanel<T extends PolicyTO> extends DirectoryPanel<
        PolicyRuleWrapper, PolicyRuleWrapper, DirectoryDataProvider<PolicyRuleWrapper>, PolicyRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    private final BaseModal<T> baseModal;

    private final PolicyType type;

    private final String implementationType;

    private final String policy;

    protected PolicyRuleDirectoryPanel(
            final BaseModal<T> baseModal,
            final String policy,
            final PolicyType type,
            final PolicyRestClient restClient,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID, restClient, pageRef, false);

        disableCheckBoxes();

        this.baseModal = baseModal;
        this.type = type;
        this.implementationType = type == PolicyType.ACCOUNT
                ? IdRepoImplementationType.ACCOUNT_RULE
                : IdRepoImplementationType.PASSWORD_RULE;
        this.policy = policy;

        enableUtilityButton();

        this.addNewItemPanelBuilder(new PolicyRuleWizardBuilder(
                policy, type, new PolicyRuleWrapper(true), restClient, implementationRestClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<PolicyRuleWrapper, String>> getColumns() {
        List<IColumn<PolicyRuleWrapper, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("rule", this), "implementationKey", "implementationKey"));

        columns.add(new AbstractColumn<>(
                new StringResourceModel("configuration", this)) {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PolicyRuleWrapper>> cellItem,
                    final String componentId,
                    final IModel<PolicyRuleWrapper> rowModel) {

                if (rowModel.getObject().getConf() == null) {
                    cellItem.add(new Label(componentId, ""));
                } else {
                    cellItem.add(new Label(componentId, rowModel.getObject().getConf().getClass().getName()));
                }
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<PolicyRuleWrapper> getActions(final IModel<PolicyRuleWrapper> model) {
        final ActionsPanel<PolicyRuleWrapper> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                PolicyRuleDirectoryPanel.this.getTogglePanel().close(target);
                if (model.getObject().getConf() == null) {
                    SyncopeConsoleSession.get().info(getString("noConf"));
                    ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                } else {
                    send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                            new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                }
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.POLICY_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                RuleConf rule = model.getObject().getConf();
                try {
                    T actual = restClient.read(type, policy);
                    if (actual instanceof final ComposablePolicy composablePolicy) {
                        composablePolicy.getRules().remove(model.getObject().getImplementationKey());
                        restClient.update(type, actual);

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        customActionOnFinishCallback(target);
                    }
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", rule.getName(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.POLICY_DELETE, true);

        return panel;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        ActionsPanel<Serializable> panel = new ActionsPanel<>(componentId, null);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (target != null) {
                    customActionOnFinishCallback(target);
                }
            }
        }, ActionLink.ActionType.RELOAD, IdRepoEntitlement.POLICY_LIST).hideLabel();

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected PolicyRuleDataProvider dataProvider() {
        return new PolicyRuleDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_POLICY_RULE_PAGINATOR_ROWS;
    }

    protected class PolicyRuleDataProvider extends DirectoryDataProvider<PolicyRuleWrapper> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<PolicyRuleWrapper> comparator;

        public PolicyRuleDataProvider(final int paginatorRows) {
            super(paginatorRows);

            // Default sorting
            setSort("implementationKey", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @SuppressWarnings("unchecked")
        private List<PolicyRuleWrapper> getPolicyRuleWrappers(final ComposablePolicy policy) {
            return policy.getRules().stream().map(rule -> {
                ImplementationTO implementation = implementationRestClient.read(implementationType, rule);

                PolicyRuleWrapper wrapper = new PolicyRuleWrapper(false).
                        setImplementationKey(implementation.getKey()).
                        setImplementationEngine(implementation.getEngine());
                if (implementation.getEngine() == ImplementationEngine.JAVA) {
                    try {
                        RuleConf ruleConf = MAPPER.readValue(implementation.getBody(), RuleConf.class);
                        wrapper.setConf(ruleConf);
                    } catch (Exception e) {
                        LOG.error("During deserialization", e);
                    }
                }

                return wrapper;
            }).collect(Collectors.toList());
        }

        @Override
        public Iterator<PolicyRuleWrapper> iterator(final long first, final long count) {
            T actual = restClient.read(type, policy);

            List<PolicyRuleWrapper> rules = actual instanceof final ComposablePolicy composablePolicy
                    ? getPolicyRuleWrappers(composablePolicy)
                    : new ArrayList<>();

            rules.sort(comparator);
            return rules.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            T actual = restClient.read(type, policy);
            return actual instanceof final ComposablePolicy composablePolicy
                    ? getPolicyRuleWrappers(composablePolicy).size()
                    : 0;
        }

        @Override
        public IModel<PolicyRuleWrapper> model(final PolicyRuleWrapper object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
