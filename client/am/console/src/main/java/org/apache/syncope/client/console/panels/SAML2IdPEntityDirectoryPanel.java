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
import org.apache.syncope.client.console.commons.AMConstants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.SAML2IdPEntityDirectoryPanel.SAML2IdPEntityProvider;
import org.apache.syncope.client.console.rest.SAML2IdPEntityRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.SAML2IdPEntityWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

public class SAML2IdPEntityDirectoryPanel extends DirectoryPanel<
        SAML2IdPEntityTO, SAML2IdPEntityTO, SAML2IdPEntityProvider, SAML2IdPEntityRestClient> {

    private static final long serialVersionUID = -6535332920023200166L;

    private final String metadataURL;

    public SAML2IdPEntityDirectoryPanel(
            final String id,
            final SAML2IdPEntityRestClient restClient,
            final String waPrefix,
            final PageReference pageRef) {

        super(id, restClient, pageRef);
        this.metadataURL = waPrefix + "/idp/metadata";

        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addNewItemPanelBuilder(new SAML2IdPEntityWizardBuilder(new SAML2IdPEntityTO(), restClient, pageRef), false);

        initResultTable();
    }

    @Override
    protected List<IColumn<SAML2IdPEntityTO, String>> getColumns() {
        List<IColumn<SAML2IdPEntityTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new AbstractColumn<>(Model.of("URL")) {

            private static final long serialVersionUID = -7226955670801277153L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<SAML2IdPEntityTO>> cellItem,
                    final String componentId,
                    final IModel<SAML2IdPEntityTO> rowModel) {

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
    protected ActionsPanel<SAML2IdPEntityTO> getActions(final IModel<SAML2IdPEntityTO> model) {
        ActionsPanel<SAML2IdPEntityTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2IdPEntityTO ignore) {
                send(SAML2IdPEntityDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                restClient.get(model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.SAML2_IDP_ENTITY_SET);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected SAML2IdPEntityProvider dataProvider() {
        return new SAML2IdPEntityProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return AMConstants.PREF_SAML2_IDP_ENTITY_PAGINATOR_ROWS;
    }

    protected final class SAML2IdPEntityProvider extends DirectoryDataProvider<SAML2IdPEntityTO> {

        private static final long serialVersionUID = 5282134321828253058L;

        private final SortableDataProviderComparator<SAML2IdPEntityTO> comparator;

        public SAML2IdPEntityProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends SAML2IdPEntityTO> iterator(final long first, final long count) {
            List<SAML2IdPEntityTO> idps = restClient.list();
            idps.sort(comparator);
            return idps.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SAML2IdPEntityTO> model(final SAML2IdPEntityTO metadata) {
            return new CompoundPropertyModel<>(metadata);
        }
    }
}
