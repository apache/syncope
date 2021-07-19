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
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.AuthModuleDirectoryPanel.AuthModuleProvider;
import org.apache.syncope.client.console.rest.AuthModuleRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AuthModuleWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class AuthModuleDirectoryPanel
        extends DirectoryPanel<AuthModuleTO, AuthModuleTO, AuthModuleProvider, AuthModuleRestClient> {

    private static final long serialVersionUID = 1L;

    public AuthModuleDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, pageRef);

        disableCheckBoxes();

        this.addNewItemPanelBuilder(new AuthModuleWizardBuilder(new AuthModuleTO(), pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.AUTH_MODULE_CREATE);

        modal.size(Modal.Size.Extra_large);
        initResultTable();
    }

    @Override
    protected AuthModuleProvider dataProvider() {
        return new AuthModuleProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_AUTHMODULE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<AuthModuleTO, String>> getColumns() {
        List<IColumn<AuthModuleTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel("type"), "conf") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                final Item<ICellPopulator<AuthModuleTO>> item,
                final String componentId,
                final IModel<AuthModuleTO> rowModel) {

                item.add(new Label(componentId, rowModel.getObject().getConf() == null
                    ? StringUtils.EMPTY
                    : StringUtils.substringBefore(
                    rowModel.getObject().getConf().getClass().getSimpleName(), "AuthModuleConf")));
            }
        });
        return columns;
    }

    @Override
    public ActionsPanel<AuthModuleTO> getActions(final IModel<AuthModuleTO> model) {
        ActionsPanel<AuthModuleTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthModuleTO ignore) {
                send(AuthModuleDirectoryPanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(
                    AuthModuleRestClient.read(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.AUTH_MODULE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthModuleTO ignore) {
                try {
                    AuthModuleRestClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.AUTH_MODULE_DELETE, true);

        return panel;
    }

    protected static final class AuthModuleProvider extends DirectoryDataProvider<AuthModuleTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AuthModuleTO> comparator;

        private AuthModuleProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort(Constants.KEY_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AuthModuleTO> iterator(final long first, final long count) {
            List<AuthModuleTO> result = AuthModuleRestClient.list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return AuthModuleRestClient.list().size();
        }

        @Override
        public IModel<AuthModuleTO> model(final AuthModuleTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
