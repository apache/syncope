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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
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
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
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
import org.apache.wicket.model.ResourceModel;
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

        columns.add(new PropertyColumn<PolicyRuleWrapper, String>(
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

        columns.add(new ActionColumn<PolicyRuleWrapper, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel<PolicyRuleWrapper> getActions(final String componentId,
                    final IModel<PolicyRuleWrapper> model) {

                final ActionLinksPanel<PolicyRuleWrapper> panel = ActionLinksPanel.<PolicyRuleWrapper>builder().
                        add(new ActionLink<PolicyRuleWrapper>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                                RuleConf clone = SerializationUtils.clone(model.getObject().getConf());

                                send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(
                                                new PolicyRuleWrapper().setConf(clone),
                                                target));
                            }
                        }, ActionLink.ActionType.CLONE, StandardEntitlement.POLICY_UPDATE).
                        add(new ActionLink<PolicyRuleWrapper>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                                send(PolicyRuleDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                            }
                        }, ActionLink.ActionType.EDIT, StandardEntitlement.POLICY_UPDATE).
                        add(new ActionLink<PolicyRuleWrapper>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                                final RuleConf rule = model.getObject().getConf();
                                try {
                                    final T actual = restClient.getPolicy(policy);
                                    CollectionUtils.filter(getRuleConf(actual), new Predicate<RuleConf>() {

                                        @Override
                                        public boolean evaluate(final RuleConf object) {
                                            return !object.getName().equals(rule.getName());
                                        }
                                    });
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
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.POLICY_UPDATE).build(componentId);

                return panel;
            }

            @Override
            public ActionLinksPanel<PolicyRuleWrapper> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<PolicyRuleWrapper> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<PolicyRuleWrapper>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final PolicyRuleWrapper ignore) {
                        if (target != null) {
                            customActionOnFinishCallback(target);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).build(componentId);
            }
        });

        return columns;
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

            final ArrayList<PolicyRuleWrapper> rules = CollectionUtils.collect(getRuleConf(actual),
                    new Transformer<RuleConf, PolicyRuleWrapper>() {

                @Override
                public PolicyRuleWrapper transform(final RuleConf input) {
                    return new PolicyRuleWrapper(input.getName()).setName(input.getName()).setConf(input);
                }
            }, new ArrayList<PolicyRuleWrapper>());

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
