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
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DelegationDirectoryPanel.DelegationDataProvider;
import org.apache.syncope.client.console.rest.DelegationRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DelegationDirectoryPanel extends
        DirectoryPanel<DelegationTO, DelegationTO, DelegationDataProvider, DelegationRestClient> {

    private static final long serialVersionUID = 28300423726398L;

    @SpringBean
    protected UserRestClient userRestClient;

    protected DelegationDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);

        disableCheckBoxes();
        setShowResultPanel(true);

        modal.size(Modal.Size.Large);
        initResultTable();
    }

    @Override
    protected DelegationDataProvider dataProvider() {
        return new DelegationDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_DELEGATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<DelegationTO, String>> getColumns() {
        List<IColumn<DelegationTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));

        columns.add(new AbstractColumn<>(new ResourceModel("delegating"), "delegating") {

            private static final long serialVersionUID = -7835464045129401360L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<DelegationTO>> cellItem,
                    final String componentId,
                    final IModel<DelegationTO> rowModel) {

                String delegating = rowModel.getObject().getDelegating();
                if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_READ)) {
                    delegating = userRestClient.read(delegating).getUsername();
                } else if (SyncopeConsoleSession.get().getSelfTO().getKey().equals(delegating)) {
                    delegating = SyncopeConsoleSession.get().getSelfTO().getUsername();
                }

                cellItem.add(new Label(componentId, delegating));
            }
        });

        columns.add(new AbstractColumn<>(new ResourceModel("delegated"), "delegated") {

            private static final long serialVersionUID = -7835464045129401360L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<DelegationTO>> cellItem,
                    final String componentId,
                    final IModel<DelegationTO> rowModel) {

                String delegated = rowModel.getObject().getDelegated();
                if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.USER_READ)) {
                    delegated = userRestClient.read(delegated).getUsername();
                } else if (SyncopeConsoleSession.get().getSelfTO().getKey().equals(delegated)) {
                    delegated = SyncopeConsoleSession.get().getSelfTO().getUsername();
                }

                cellItem.add(new Label(componentId, delegated));
            }
        });

        columns.add(new DatePropertyColumn<>(new StringResourceModel("start", this), "start", "start"));

        columns.add(new DatePropertyColumn<>(new StringResourceModel("end", this), "end", "end"));

        columns.add(new PropertyColumn<>(new ResourceModel("roles"), null, "roles"));

        return columns;
    }

    @Override
    public ActionsPanel<DelegationTO> getActions(final IModel<DelegationTO> model) {
        ActionsPanel<DelegationTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final DelegationTO ignore) {
                send(DelegationDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, StringUtils.EMPTY);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final DelegationTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true);

        return panel;
    }

    protected class DelegationDataProvider extends DirectoryDataProvider<DelegationTO> {

        private static final long serialVersionUID = 28297380054779L;

        private final SortableDataProviderComparator<DelegationTO> comparator;

        public DelegationDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<DelegationTO> iterator(final long first, final long count) {
            List<DelegationTO> result = restClient.list();
            Collections.sort(result, comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<DelegationTO> model(final DelegationTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<DelegationTO, DelegationTO, DelegationRestClient> {

        private static final long serialVersionUID = 5530948153889495221L;

        public Builder(final DelegationRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
        }

        @Override
        protected WizardMgtPanel<DelegationTO> newInstance(final String id, final boolean wizardInModal) {
            return new DelegationDirectoryPanel(id, this);
        }
    }
}
