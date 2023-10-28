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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DynRealmDirectoryPanel.DynRealmDataProvider;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.DynRealmWrapper;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class DynRealmDirectoryPanel extends
        DirectoryPanel<DynRealmTO, DynRealmWrapper, DynRealmDataProvider, DynRealmRestClient> {

    private static final long serialVersionUID = -5491515010207202168L;

    protected DynRealmDirectoryPanel(final String id, final Builder builder) {
        super(id, builder);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.DYNREALM_CREATE);
        setReadOnly(!SyncopeConsoleSession.get().owns(IdRepoEntitlement.DYNREALM_UPDATE));

        disableCheckBoxes();
        setShowResultPanel(true);

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        AjaxLink<Void> newDynRealmlLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                modal.header(new StringResourceModel("any.new"));
                modal.setContent(new DynRealmModalPanel(new DynRealmWrapper(new DynRealmTO()), true, modal, pageRef));
                modal.show(true);
                target.add(modal);
            }
        };
        ((WebMarkupContainer) get("container:content")).addOrReplace(newDynRealmlLink);
        MetaDataRoleAuthorizationStrategy.authorize(newDynRealmlLink, RENDER, IdRepoEntitlement.DYNREALM_CREATE);

        initResultTable();
    }

    @Override
    protected DynRealmDataProvider dataProvider() {
        return new DynRealmDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_DYNREALM_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<DynRealmTO, String>> getColumns() {
        final List<IColumn<DynRealmTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));

        return columns;
    }

    @Override
    public ActionsPanel<DynRealmTO> getActions(final IModel<DynRealmTO> model) {
        final ActionsPanel<DynRealmTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final DynRealmTO ignore) {
                modal.header(new StringResourceModel("any.edit", model));
                modal.setContent(new DynRealmModalPanel(new DynRealmWrapper(model.getObject()), false, modal, pageRef));
                modal.show(true);
                target.add(modal);
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.DYNREALM_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 3766262567901552032L;

            @Override
            public void onClick(final AjaxRequestTarget target, final DynRealmTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting dynamic realm {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.DYNREALM_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class DynRealmDataProvider extends DirectoryDataProvider<DynRealmTO> {

        private static final long serialVersionUID = 3124431855954382273L;

        private final SortableDataProviderComparator<DynRealmTO> comparator;

        public DynRealmDataProvider(final int paginatorRows) {
            super(paginatorRows);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<DynRealmTO> iterator(final long first, final long count) {
            List<DynRealmTO> result = restClient.list();
            result.sort(comparator);
            return result.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<DynRealmTO> model(final DynRealmTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    public abstract static class Builder
            extends DirectoryPanel.Builder<DynRealmTO, DynRealmWrapper, DynRealmRestClient> {

        private static final long serialVersionUID = 5530948153889495221L;

        public Builder(final DynRealmRestClient restClient, final PageReference pageRef) {
            super(restClient, pageRef);
        }

        @Override
        protected WizardMgtPanel<DynRealmWrapper> newInstance(final String id, final boolean wizardInModal) {
            return new DynRealmDirectoryPanel(id, this);
        }
    }
}
