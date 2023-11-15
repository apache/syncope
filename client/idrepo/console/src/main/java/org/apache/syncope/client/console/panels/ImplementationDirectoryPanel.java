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
import org.apache.syncope.client.console.panels.ImplementationDirectoryPanel.ImplementationProvider;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class ImplementationDirectoryPanel extends DirectoryPanel<
        ImplementationTO, ImplementationTO, ImplementationProvider, ImplementationRestClient> {

    private static final long serialVersionUID = 1868839768348072635L;

    private final String type;

    public ImplementationDirectoryPanel(
            final String id,
            final String type,
            final ImplementationRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        this.type = type;

        ImplementationTO implementation = new ImplementationTO();
        implementation.setType(type);

        disableCheckBoxes();

        modal.size(Modal.Size.Large);
        modal.addSubmitButton();
        modal.setWindowClosedCallback(target -> {
            implementation.setEngine(null);
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        initResultTable();

        ImplementationEngineTogglePanel engineTogglePanel =
                new ImplementationEngineTogglePanel("engineTogglePanel", implementation, pageRef) {

            private static final long serialVersionUID = -112426445257072782L;

            @Override
            protected void onSubmit(final ImplementationEngine engine, final AjaxRequestTarget target) {
                implementation.setKey(null);
                implementation.setBody(null);

                target.add(ImplementationDirectoryPanel.this.modal.setContent(new ImplementationModalPanel(
                        ImplementationDirectoryPanel.this.modal, implementation, pageRef)));
                ImplementationDirectoryPanel.this.modal.header(
                        new StringResourceModel("any.new", Model.of(implementation)));
                ImplementationDirectoryPanel.this.modal.show(true);
            }
        };
        addInnerObject(engineTogglePanel);

        AjaxLink<Void> replaceAddLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(ImplementationDirectoryPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                engineTogglePanel.setHeaderLabel(target);
                engineTogglePanel.toggle(target, true);
            }
        };
        ((WebMarkupContainer) get("container:content")).addOrReplace(replaceAddLink);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.IMPLEMENTATION_CREATE);
    }

    @Override
    protected List<IColumn<ImplementationTO, String>> getColumns() {
        List<IColumn<ImplementationTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(new StringResourceModel("engine", this), "engine", "engine"));

        return columns;
    }

    @Override
    protected ActionsPanel<ImplementationTO> getActions(final IModel<ImplementationTO> model) {
        final ActionsPanel<ImplementationTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ImplementationTO ignore) {
                target.add(modal.setContent(
                        new ImplementationModalPanel(modal, model.getObject(), pageRef)));
                modal.header(new StringResourceModel("any.edit", Model.of(model.getObject())));
                modal.show(true);
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.IMPLEMENTATION_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ImplementationTO ignore) {
                try {
                    restClient.delete(model.getObject().getType(), model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.IMPLEMENTATION_DELETE, true);

        return panel;
    }

    @Override
    protected ImplementationProvider dataProvider() {
        return new ImplementationProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_IMPLEMENTATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class ImplementationProvider extends DirectoryDataProvider<ImplementationTO> {

        private static final long serialVersionUID = 8594921866993979224L;

        private final SortableDataProviderComparator<ImplementationTO> comparator;

        public ImplementationProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ImplementationTO> iterator(final long first, final long count) {
            List<ImplementationTO> implementations = restClient.list(type);
            implementations.sort(comparator);
            return implementations.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list(type).size();
        }

        @Override
        public IModel<ImplementationTO> model(final ImplementationTO implementation) {
            return new IModel<>() {

                private static final long serialVersionUID = 999513782683391483L;

                @Override
                public ImplementationTO getObject() {
                    return implementation;
                }
            };
        }
    }
}
