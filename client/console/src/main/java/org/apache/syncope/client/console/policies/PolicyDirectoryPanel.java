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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Policies page.
 *
 * @param <T> policy type.
 */
public abstract class PolicyDirectoryPanel<T extends AbstractPolicyTO>
        extends DirectoryPanel<T, T, DirectoryDataProvider<T>, PolicyRestClient> {

    private static final long serialVersionUID = 4984337552918213290L;

    protected final BaseModal<T> ruleCompositionModal = new BaseModal<T>("outer") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(false);
        }

    };

    protected final BaseModal<T> policySpecModal = new BaseModal<>("outer");

    private final PolicyType policyType;

    public PolicyDirectoryPanel(final String id, final PolicyType policyType, final PageReference pageRef) {
        super(id, pageRef, true);
        this.policyType = policyType;
        this.restClient = new PolicyRestClient();

        ruleCompositionModal.size(Modal.Size.Large);
        setWindowClosedReloadCallback(ruleCompositionModal);
        addOuterObject(ruleCompositionModal);

        policySpecModal.size(Modal.Size.Large);
        policySpecModal.addSubmitButton();
        setWindowClosedReloadCallback(policySpecModal);
        addOuterObject(policySpecModal);

        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487129L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                modal.show(false);
            }
        });

        setFooterVisibility(true);
        modal.addSubmitButton();
        modal.size(Modal.Size.Large);
    }

    @Override
    protected List<IColumn<T, String>> getColumns() {
        final List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<T>(
                new StringResourceModel("key", this), "key", "key"));

        columns.add(new PropertyColumn<T, String>(new StringResourceModel(
                "description", this), "description", "description"));

        columns.add(new CollectionPropertyColumn<T>(
                new StringResourceModel("usedByResources", this), "usedByResources", "usedByResources"));

        columns.add(new CollectionPropertyColumn<T>(
                new StringResourceModel("usedByRealms", this), "usedByRealms", "usedByRealms"));

        addCustomColumnFields(columns);

        columns.add(new ActionColumn<T, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel<T> getActions(final String componentId, final IModel<T> model) {

                final ActionLinksPanel.Builder<T> panel = ActionLinksPanel.<T>builder().
                        add(new ActionLink<T>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AbstractPolicyTO ignore) {
                                final AbstractPolicyTO clone = SerializationUtils.clone(model.getObject());
                                clone.setKey(null);
                                send(PolicyDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(clone, target));
                            }
                        }, ActionLink.ActionType.CLONE, StandardEntitlement.POLICY_CREATE).
                        add(new ActionLink<T>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AbstractPolicyTO ignore) {
                                send(PolicyDirectoryPanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(
                                                restClient.getPolicy(model.getObject().getKey()), target));
                            }
                        }, ActionLink.ActionType.EDIT, StandardEntitlement.POLICY_UPDATE).
                        add(new ActionLink<T>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AbstractPolicyTO ignore) {
                                final T policyTO = model.getObject();
                                try {
                                    restClient.delete(policyTO.getKey());
                                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (SyncopeClientException e) {
                                    LOG.error("While deleting {}", policyTO.getKey(), e);
                                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                }
                                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.POLICY_DELETE);

                addCustomActions(panel, model);
                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<ReportTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<ReportTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<ReportTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.TASK_LIST).build(componentId);
            }
        });

        return columns;
    }

    protected void addCustomColumnFields(final List<IColumn<T, String>> columns) {
    }

    protected void addCustomActions(final ActionLinksPanel.Builder<T> panel, final IModel<T> model) {
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.EXECUTE);
        bulkActions.add(ActionType.DELETE);
        return bulkActions;
    }

    @Override
    protected PolicyDataProvider dataProvider() {
        return new PolicyDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_POLICY_PAGINATOR_ROWS;
    }

    protected class PolicyDataProvider extends DirectoryDataProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<T> comparator;

        public PolicyDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("description", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            List<T> list = restClient.getPolicies(policyType);
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getPolicies(policyType).size();
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
