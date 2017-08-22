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
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.policies.PolicyRuleDirectoryPanel.PolicyRuleWrapper;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.core.util.lang.PropertyResolver;
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
public class PolicyRuleDirectoryPanel<T extends AbstractPolicyTO> extends DirectoryPanel<
        PolicyRuleWrapper, PolicyRuleWrapper, DirectoryDataProvider<PolicyRuleWrapper>, PolicyRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    private final BaseModal<T> baseModal;

    private final String policy;

    protected PolicyRuleDirectoryPanel(
            final BaseModal<T> baseModal, final String policy, final PolicyType type, final PageReference pageRef) {
        super(BaseModal.CONTENT_ID, pageRef, false);

        disableCheckBoxes();

        this.baseModal = baseModal;
        this.policy = policy;
        this.restClient = new PolicyRestClient();

        enableExitButton();

        this.addNewItemPanelBuilder(new PolicyRuleWizardBuilder(policy, type, new PolicyRuleWrapper(), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.POLICY_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<PolicyRuleWrapper, String>> getColumns() {
        final List<IColumn<PolicyRuleWrapper, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("ruleConf", this), "name", "name"));

        columns.add(new AbstractColumn<PolicyRuleWrapper, String>(
                new StringResourceModel("configuration", this)) {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<PolicyRuleWrapper>> cellItem,
                    final String componentId,
                    final IModel<PolicyRuleWrapper> rowModel) {
                cellItem.add(new Label(componentId, rowModel.getObject().getConf().getClass().getName()));
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
                RuleConf clone = SerializationUtils.clone(model.getObject().getConf());

                PolicyRuleDirectoryPanel.this.getTogglePanel().close(target);
                send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new PolicyRuleWrapper().setConf(clone),
                                target));
            }
        }, ActionLink.ActionType.CLONE, StandardEntitlement.POLICY_CREATE);
        panel.add(new ActionLink<PolicyRuleWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                PolicyRuleDirectoryPanel.this.getTogglePanel().close(target);
                send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, StandardEntitlement.POLICY_UPDATE);
        panel.add(new ActionLink<PolicyRuleWrapper>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                final RuleConf rule = model.getObject().getConf();
                try {
                    final T actual = restClient.getPolicy(policy);
                    List<RuleConf> conf = getRuleConf(actual);
                    conf.removeAll(conf.stream().
                            filter(object -> object.getName().equals(rule.getName())).collect(Collectors.toList()));
                    restClient.updatePolicy(actual);
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    customActionOnFinishCallback(target);
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
        }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.DELETE);
        return bulkActions;
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

        private final SortableDataProviderComparator<PolicyRuleWrapper> comparator;

        public PolicyRuleDataProvider(final int paginatorRows) {
            super(paginatorRows);

            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<PolicyRuleWrapper> iterator(final long first, final long count) {
            final T actual = restClient.getPolicy(policy);

            final List<PolicyRuleWrapper> rules = getRuleConf(actual).stream().map(input
                    -> new PolicyRuleWrapper(input.getName()).setName(input.getName()).setConf(input)).
                    collect(Collectors.toList());

            Collections.sort(rules, comparator);
            return rules.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            final T actual = restClient.getPolicy(policy);
            return getRuleConf(actual).size();
        }

        @Override
        public IModel<PolicyRuleWrapper> model(final PolicyRuleWrapper object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @SuppressWarnings("unchecked")
    private List<RuleConf> getRuleConf(final T policyTO) {
        Object res = PropertyResolver.getValue("ruleConfs", policyTO);
        if (res instanceof List) {
            return (List<RuleConf>) res;
        } else {
            return null;
        }
    }

    public static class PolicyRuleWrapper implements Serializable {

        private static final long serialVersionUID = 2472755929742424558L;

        private String oldname;

        private String name;

        private RuleConf conf;

        public PolicyRuleWrapper() {
            this(null);
        }

        public PolicyRuleWrapper(final String name) {
            this.oldname = name;
        }

        public boolean isNew() {
            return oldname == null;
        }

        public String getOldName() {
            return this.oldname;
        }

        public String getName() {
            return this.name;
        }

        public PolicyRuleWrapper setName(final String name) {
            this.name = name;
            return this;
        }

        public RuleConf getConf() {
            return conf;
        }

        public PolicyRuleWrapper setConf(final RuleConf conf) {
            this.conf = conf;
            return this;
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
