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
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.PolicyTO;
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
import org.apache.wicket.model.StringResourceModel;

/**
 * Policies page.
 *
 * @param <T> policy type.
 */
public abstract class PolicyDirectoryPanel<T extends PolicyTO>
        extends DirectoryPanel<T, T, DirectoryDataProvider<T>, PolicyRestClient> {

    private static final long serialVersionUID = 4984337552918213290L;

    protected final BaseModal<T> ruleCompositionModal = new BaseModal<T>(Constants.OUTER) {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(false);
        }

    };

    protected final BaseModal<T> policySpecModal = new BaseModal<>(Constants.OUTER);

    private final PolicyType type;

    public PolicyDirectoryPanel(final String id, final PolicyType policyType, final PageReference pageRef) {
        super(id, pageRef, true);
        this.type = policyType;
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

        disableCheckBoxes();
    }

    @Override
    protected List<IColumn<T, String>> getColumns() {
        final List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("description", this), "description", "description"));
        columns.add(new CollectionPropertyColumn<>(
                new StringResourceModel("usedByResources", this), "usedByResources"));
        if (type != PolicyType.PULL && type != PolicyType.PUSH) {
            columns.add(new CollectionPropertyColumn<>(
                    new StringResourceModel("usedByRealms", this), "usedByRealms"));
        }

        addCustomColumnFields(columns);

        return columns;
    }

    @Override
    public ActionsPanel<T> getActions(final IModel<T> model) {
        final ActionsPanel<T> panel = super.getActions(model);

        panel.add(new ActionLink<T>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyTO ignore) {
                send(PolicyDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                PolicyRestClient.getPolicy(type, model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.POLICY_UPDATE);

        panel.add(new ActionLink<T>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyTO ignore) {
                final PolicyTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(PolicyDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(clone, target));
            }
        }, ActionLink.ActionType.CLONE, IdRepoEntitlement.POLICY_CREATE);

        addCustomActions(panel, model);

        panel.add(new ActionLink<T>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PolicyTO ignore) {
                final T policyTO = model.getObject();
                try {
                    PolicyRestClient.delete(type, policyTO.getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", policyTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.POLICY_DELETE, true);

        return panel;
    }

    protected void addCustomColumnFields(final List<IColumn<T, String>> columns) {
    }

    protected void addCustomActions(final ActionsPanel<T> panel, final IModel<T> model) {
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected PolicyDataProvider dataProvider() {
        return new PolicyDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_POLICY_PAGINATOR_ROWS;
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
            List<T> list = PolicyRestClient.getPolicies(type);
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return PolicyRestClient.getPolicies(type).size();
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
