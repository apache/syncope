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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ConnInstanceHistoryConfDirectoryPanel.CHConfProvider;
import org.apache.syncope.client.console.rest.ConnectorHistoryRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * List all connector configuration history instances for the selected connector.
 */
public abstract class ConnInstanceHistoryConfDirectoryPanel extends DirectoryPanel<
        ConnInstanceHistoryConfTO, ConnInstanceHistoryConfTO, CHConfProvider, ConnectorHistoryRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    protected final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private final String entityKey;

    public ConnInstanceHistoryConfDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String entityKey,
            final PageReference pageRef) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, false, false);

        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        restClient = new ConnectorHistoryRestClient();
        setShowResultPage(false);
        disableCheckBoxes();

        this.entityKey = entityKey;
        initResultTable();
    }

    @Override
    protected List<IColumn<ConnInstanceHistoryConfTO, String>> getColumns() {
        final List<IColumn<ConnInstanceHistoryConfTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<ConnInstanceHistoryConfTO>(
                new StringResourceModel("key", this), "key"));

        columns.add(new PropertyColumn<ConnInstanceHistoryConfTO, String>(new StringResourceModel(
                "creator", this), "creator", "creator"));

        columns.add(new DatePropertyColumn<ConnInstanceHistoryConfTO>(
                new StringResourceModel("creation", this), "creation", "creation"));

        return columns;
    }

    @Override
    public ActionsPanel<ConnInstanceHistoryConfTO> getActions(final IModel<ConnInstanceHistoryConfTO> model) {
        final ActionsPanel<ConnInstanceHistoryConfTO> panel = super.getActions(model);
        final ConnInstanceHistoryConfTO connHistoryConfTO = model.getObject();

        // -- view
        panel.add(new ActionLink<ConnInstanceHistoryConfTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConnInstanceHistoryConfTO modelObject) {

                ConnInstanceHistoryConfDirectoryPanel.this.getTogglePanel().close(target);
                viewConfiguration(modelObject, target);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.CONNECTOR_HISTORY_LIST);

        // -- restore
        panel.add(new ActionLink<ConnInstanceHistoryConfTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConnInstanceHistoryConfTO modelObject) {
                try {
                    restClient.restore(modelObject.getKey());
                    ConnInstanceHistoryConfDirectoryPanel.this.getTogglePanel().close(target);
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While restoring {}", connHistoryConfTO.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.RESTORE, StandardEntitlement.CONNECTOR_HISTORY_RESTORE);

        // -- delete
        panel.add(new ActionLink<ConnInstanceHistoryConfTO>() {

            private static final long serialVersionUID = -6745431735457245600L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConnInstanceHistoryConfTO modelObject) {
                try {
                    restClient.delete(modelObject.getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                    ConnInstanceHistoryConfDirectoryPanel.this.getTogglePanel().close(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", connHistoryConfTO.getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.CONNECTOR_HISTORY_DELETE, true);

        return panel;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_CONNECTOR_HISTORY_CONF_PAGINATOR_ROWS;
    }

    protected abstract void viewConfiguration(ConnInstanceHistoryConfTO connHistoryTO,
            final AjaxRequestTarget target);

    @Override
    protected void resultTableCustomChanges(
            final AjaxDataTablePanel.Builder<ConnInstanceHistoryConfTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected CHConfProvider dataProvider() {
        return new CHConfProvider(rows);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent && modal != null) {
            final AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }

    protected class CHConfProvider extends DirectoryDataProvider<ConnInstanceHistoryConfTO> {

        private static final long serialVersionUID = -4402560904215049574L;

        private final SortableDataProviderComparator<ConnInstanceHistoryConfTO> comparator;

        public CHConfProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("creation", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ConnInstanceHistoryConfTO> iterator(final long first, final long count) {
            final List<ConnInstanceHistoryConfTO> configurations = restClient.list(entityKey);

            Collections.sort(configurations, comparator);
            return configurations.iterator();
        }

        @Override
        public long size() {
            return restClient.list(entityKey).size();
        }

        @Override
        public IModel<ConnInstanceHistoryConfTO> model(final ConnInstanceHistoryConfTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
