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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.ComposablePolicy;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
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

/**
 * Policy rules page.
 *
 * @param <T> policy type.
 */
public class PolicyRuleDirectoryPanel<T extends PolicyTO> extends DirectoryPanel<
        PolicyRuleWrapper, PolicyRuleWrapper, DirectoryDataProvider<PolicyRuleWrapper>, PolicyRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final BaseModal<T> baseModal;

    private final PolicyType type;

    private final ImplementationType implementationType;

    private final String policy;

    protected PolicyRuleDirectoryPanel(
            final BaseModal<T> baseModal, final String policy, final PolicyType type, final PageReference pageRef) {
        super(BaseModal.CONTENT_ID, pageRef, false);

        disableCheckBoxes();

        this.baseModal = baseModal;
        this.type = type;
        this.implementationType = type == PolicyType.ACCOUNT
                ? ImplementationType.ACCOUNT_RULE
                : ImplementationType.PASSWORD_RULE;
        this.policy = policy;
        this.restClient = new PolicyRestClient();

        enableExitButton();

        this.addNewItemPanelBuilder(
                new PolicyRuleWizardBuilder(policy, type, new PolicyRuleWrapper(true), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.POLICY_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<PolicyRuleWrapper, String>> getColumns() {
        final List<IColumn<PolicyRuleWrapper, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("rule", this), "implementationKey", "implementationKey"));

        columns.add(new AbstractColumn<PolicyRuleWrapper, String>(
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

        panel.add(new ActionLink<PolicyRuleWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                PolicyRuleDirectoryPanel.this.getTogglePanel().close(target);
                if (model.getObject().getConf() == null) {
                    SyncopeConsoleSession.get().info(getString("noConf"));
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                } else {
                    send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                            new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                }
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.POLICY_UPDATE);
        panel.add(new ActionLink<PolicyRuleWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                RuleConf rule = model.getObject().getConf();
                try {
                    T actual = restClient.getPolicy(type, policy);
                    if (actual instanceof ComposablePolicy) {
                        ((ComposablePolicy) actual).getRules().remove(model.getObject().getImplementationKey());
                        restClient.updatePolicy(type, actual);

                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        customActionOnFinishCallback(target);
                    }
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", rule.getName(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.POLICY_DELETE, true);

        return panel;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = new ActionsPanel<>(componentId, null);

        panel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (target != null) {
                    customActionOnFinishCallback(target);
                }
            }
        }, ActionLink.ActionType.RELOAD, StandardEntitlement.POLICY_LIST).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return Collections.emptyList();
    }

    @Override
    protected PolicyRuleDataProvider dataProvider() {
        return new PolicyRuleDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_POLICY_RULE_PAGINATOR_ROWS;
    }

    protected class PolicyRuleDataProvider extends DirectoryDataProvider<PolicyRuleWrapper> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final ImplementationRestClient implementationClient = new ImplementationRestClient();

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
                ImplementationTO implementation = implementationClient.read(implementationType, rule);

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
            final T actual = restClient.getPolicy(type, policy);

            List<PolicyRuleWrapper> rules = actual instanceof ComposablePolicy
                    ? getPolicyRuleWrappers((ComposablePolicy) actual)
                    : Collections.emptyList();

            Collections.sort(rules, comparator);
            return rules.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            final T actual = restClient.getPolicy(type, policy);
            return actual instanceof ComposablePolicy
                    ? getPolicyRuleWrappers((ComposablePolicy) actual).size()
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
        if (event.getPayload() instanceof ExitEvent && modal != null) {
            final AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
