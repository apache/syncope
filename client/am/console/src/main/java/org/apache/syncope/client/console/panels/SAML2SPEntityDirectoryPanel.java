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
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.SAML2SPEntityDirectoryPanel.SAML2SPEntityProvider;
import org.apache.syncope.client.console.rest.SAML2SPEntityRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.SAML2SPEntityWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class SAML2SPEntityDirectoryPanel extends DirectoryPanel<
        SAML2SPEntityTO, SAML2SPEntityTO, SAML2SPEntityProvider, SAML2SPEntityRestClient> {

    private static final long serialVersionUID = -3890622383545171017L;

    private final String waPrefix;

    public SAML2SPEntityDirectoryPanel(
            final String id,
            final SAML2SPEntityRestClient restClient,
            final String waPrefix,
            final PageReference pageRef) {

        super(id, restClient, pageRef);
        this.waPrefix = waPrefix;

        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addNewItemPanelBuilder(new SAML2SPEntityWizardBuilder(new SAML2SPEntityTO(), restClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, AMEntitlement.SAML2_SP_ENTITY_SET);

        initResultTable();
    }

    @Override
    protected List<IColumn<SAML2SPEntityTO, String>> getColumns() {
        List<IColumn<SAML2SPEntityTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));

        columns.add(new AbstractColumn<>(Model.of("URL")) {

            private static final long serialVersionUID = -7226955670801277153L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<SAML2SPEntityTO>> cellItem,
                    final String componentId,
                    final IModel<SAML2SPEntityTO> rowModel) {

                String metadataURL = waPrefix + "/sp/" + rowModel.getObject().getKey() + "/metadata";
                cellItem.add(new ExternalLink(
                        componentId,
                        Model.of(metadataURL),
                        Model.of(metadataURL)) {

                    private static final long serialVersionUID = -1919646533527005367L;

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);

                        tag.setName("a");
                        if (metadataURL.startsWith("http")) {
                            tag.put("href", getDefaultModelObject().toString());
                            tag.put("target", "_blank");
                        }
                    }
                });
            }
        });

        return columns;
    }

    @Override
    protected ActionsPanel<SAML2SPEntityTO> getActions(final IModel<SAML2SPEntityTO> model) {
        ActionsPanel<SAML2SPEntityTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SPEntityTO ignore) {
                send(SAML2SPEntityDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                restClient.get(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.SAML2_SP_ENTITY_SET);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SPEntityTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

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

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected SAML2SPEntityProvider dataProvider() {
        return new SAML2SPEntityProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_SAML2_SP_ENTITY_PAGINATOR_ROWS;
    }

    protected final class SAML2SPEntityProvider extends DirectoryDataProvider<SAML2SPEntityTO> {

        private static final long serialVersionUID = 5282134321828253058L;

        private final SortableDataProviderComparator<SAML2SPEntityTO> comparator;

        public SAML2SPEntityProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends SAML2SPEntityTO> iterator(final long first, final long count) {
            List<SAML2SPEntityTO> sps = restClient.list();
            sps.sort(comparator);
            return sps.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SAML2SPEntityTO> model(final SAML2SPEntityTO metadata) {
            return new CompoundPropertyModel<>(metadata);
        }
    }
}
