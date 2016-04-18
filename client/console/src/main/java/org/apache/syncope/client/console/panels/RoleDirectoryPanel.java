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
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.RoleDataProvider;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.role.RoleHandler;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class RoleDirectoryPanel extends DirectoryPanel<RoleTO, RoleHandler, RoleDataProvider, RoleRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected RoleDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);
        setShowResultPage(true);

        modal.size(Modal.Size.Large);
        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.ROLE_CREATE);
    }

    @Override
    protected RoleDataProvider dataProvider() {
        return new RoleDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_ROLE_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<RoleTO, String>> getColumns() {
        final List<IColumn<RoleTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<RoleTO, String>(
                new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<RoleTO, String>(
                new ResourceModel("entitlements", "Entitlements"), null, "entitlements"));
        columns.add(new PropertyColumn<RoleTO, String>(
                new ResourceModel("realms"), null, "realms"));

        columns.add(new ActionColumn<RoleTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<RoleTO> getActions(final String componentId, final IModel<RoleTO> model) {
                final ActionLinksPanel.Builder<RoleTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<RoleTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                        send(RoleDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(
                                        new RoleHandler(new RoleRestClient().read(model.getObject().getKey())),
                                        target));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.ROLE_READ).add(new ActionLink<RoleTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                        final RoleTO clone = SerializationUtils.clone(model.getObject());
                        clone.setKey(null);
                        send(RoleDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.NewItemActionEvent<>(new RoleHandler(clone), target));
                    }
                }, ActionLink.ActionType.CLONE, StandardEntitlement.ROLE_CREATE).add(new ActionLink<RoleTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                        try {
                            restClient.delete(model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.ROLE_DELETE);

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<RoleTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<RoleTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<RoleTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final RoleTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.ROLE_LIST).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionType.DELETE);
        return bulkActions;
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<RoleTO, RoleHandler, RoleRestClient> {

        private static final long serialVersionUID = 5088962796986706805L;

        public Builder(final PageReference pageRef) {
            super(new RoleRestClient(), pageRef);
        }

        @Override
        protected WizardMgtPanel<RoleHandler> newInstance(final String id) {
            return new RoleDirectoryPanel(id, this);
        }
    }
}
