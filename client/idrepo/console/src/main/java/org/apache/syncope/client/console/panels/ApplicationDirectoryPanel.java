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
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.ApplicationDirectoryPanel.ApplicationDataProvider;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class ApplicationDirectoryPanel extends
        DirectoryPanel<ApplicationTO, ApplicationTO, ApplicationDataProvider, ApplicationRestClient> {

    private static final long serialVersionUID = -5491515010207202168L;

    protected final BaseModal<PrivilegeTO> privilegeModal = new BaseModal<>(Constants.OUTER) {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(false);
        }
    };

    protected ApplicationDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.APPLICATION_CREATE);
        setReadOnly(!SyncopeConsoleSession.get().owns(IdRepoEntitlement.APPLICATION_UPDATE));

        disableCheckBoxes();
        setShowResultPage(true);

        modal.size(Modal.Size.Default);
        modal.addSubmitButton();
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        privilegeModal.size(Modal.Size.Large);
        privilegeModal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        addOuterObject(privilegeModal);

        AjaxLink<Void> newApplLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                modal.header(new StringResourceModel("any.new"));
                modal.setContent(new ApplicationModalPanel(new ApplicationTO(), true, modal, pageRef));
                modal.show(true);
                target.add(modal);
            }
        };
        ((MarkupContainer) get("container:content")).addOrReplace(newApplLink);
        MetaDataRoleAuthorizationStrategy.authorize(newApplLink, RENDER, IdRepoEntitlement.APPLICATION_CREATE);

        initResultTable();
    }

    @Override
    protected ApplicationDataProvider dataProvider() {
        return new ApplicationDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_APPLICATION_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<ApplicationTO, String>> getColumns() {
        final List<IColumn<ApplicationTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new AbstractColumn<>(new ResourceModel("privileges")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                final Item<ICellPopulator<ApplicationTO>> item,
                final String componentId,
                final IModel<ApplicationTO> rowModel) {

                item.add(new Label(componentId, '[' + rowModel.getObject().getPrivileges().stream().
                    map(EntityTO::getKey).collect(Collectors.joining(", ")) + ']'));
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<ApplicationTO> getActions(final IModel<ApplicationTO> model) {
        final ActionsPanel<ApplicationTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ApplicationTO ignore) {
                modal.header(new StringResourceModel("any.edit", model));
                modal.setContent(new ApplicationModalPanel(model.getObject(), false, modal, pageRef));
                modal.show(true);
                target.add(modal);
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.APPLICATION_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ApplicationTO ignore) {
                target.add(privilegeModal.setContent(new PrivilegeDirectoryPanel(
                    privilegeModal, model.getObject(), pageRef)));

                privilegeModal.header(new StringResourceModel(
                    "application.privileges", ApplicationDirectoryPanel.this, Model.of(model.getObject())));

                MetaDataRoleAuthorizationStrategy.authorize(
                    privilegeModal.getForm(), ENABLE, IdRepoEntitlement.APPLICATION_UPDATE);

                privilegeModal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, IdRepoEntitlement.APPLICATION_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 3766262567901552032L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ApplicationTO ignore) {
                try {
                    ApplicationRestClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting application {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.APPLICATION_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<ApplicationTO, ApplicationTO, ApplicationRestClient> {

        private static final long serialVersionUID = 5530948153889495221L;

        public Builder(final PageReference pageRef) {
            super(new ApplicationRestClient(), pageRef);
        }

        @Override
        protected WizardMgtPanel<ApplicationTO> newInstance(final String id, final boolean wizardInModal) {
            return new ApplicationDirectoryPanel(id, this);
        }
    }

    protected static class ApplicationDataProvider extends DirectoryDataProvider<ApplicationTO> {

        private static final long serialVersionUID = 3124431855954382273L;

        private final SortableDataProviderComparator<ApplicationTO> comparator;

        public ApplicationDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ApplicationTO> iterator(final long first, final long count) {
            List<ApplicationTO> result = ApplicationRestClient.list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return ApplicationRestClient.list().size();
        }

        @Override
        public IModel<ApplicationTO> model(final ApplicationTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
