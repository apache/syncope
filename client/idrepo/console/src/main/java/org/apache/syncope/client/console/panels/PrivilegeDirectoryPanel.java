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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class PrivilegeDirectoryPanel extends DirectoryPanel<
        PrivilegeTO, PrivilegeTO, DirectoryDataProvider<PrivilegeTO>, ApplicationRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 6524374026036584897L;

    private final ApplicationTO application;

    private final BaseModal<PrivilegeTO> baseModal;

    protected PrivilegeDirectoryPanel(
            final BaseModal<PrivilegeTO> baseModal, final ApplicationTO application, final PageReference pageRef) {

        super(BaseModal.CONTENT_ID, pageRef, false);
        this.baseModal = baseModal;
        this.application = application;

        restClient = new ApplicationRestClient();

        disableCheckBoxes();
        enableUtilityButton();

        this.addNewItemPanelBuilder(new PrivilegeWizardBuilder(application, new PrivilegeTO(), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.APPLICATION_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<PrivilegeTO, String>> getColumns() {
        final List<IColumn<PrivilegeTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));

        return columns;
    }

    @Override
    protected ActionsPanel<PrivilegeTO> getActions(final IModel<PrivilegeTO> model) {
        ActionsPanel<PrivilegeTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PrivilegeTO ignore) {
                PrivilegeDirectoryPanel.this.getTogglePanel().close(target);
                send(PrivilegeDirectoryPanel.this, Broadcast.EXACT,
                    new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.APPLICATION_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PrivilegeTO ignore) {
                try {
                    application.getPrivileges().remove(model.getObject());
                    ApplicationRestClient.update(application);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    customActionOnFinishCallback(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.APPLICATION_UPDATE, true);

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected PrivilegeDataProvider dataProvider() {
        return new PrivilegeDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_PRIVILEGE_PAGINATOR_ROWS;
    }

    protected class PrivilegeDataProvider extends DirectoryDataProvider<PrivilegeTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<PrivilegeTO> comparator;

        public PrivilegeDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort(Constants.DESCRIPTION_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<PrivilegeTO> iterator(final long first, final long count) {
            List<PrivilegeTO> list = application.getPrivileges();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return application.getPrivileges().size();
        }

        @Override
        public IModel<PrivilegeTO> model(final PrivilegeTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent) {
            final AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
