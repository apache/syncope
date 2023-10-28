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
package org.apache.syncope.client.console.panels.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.FIQLQueryRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class FIQLQueryDirectoryPanel extends DirectoryPanel<
        FIQLQueryTO, FIQLQueryTO, DirectoryDataProvider<FIQLQueryTO>, FIQLQueryRestClient> {

    private static final long serialVersionUID = -913956855318099854L;

    private final AbstractSearchPanel searchPanel;

    private final String target;

    private final FIQLQueries parent;

    public FIQLQueryDirectoryPanel(
            final String id,
            final FIQLQueryRestClient restClient,
            final AbstractSearchPanel searchPanel,
            final String target,
            final FIQLQueries parent,
            final PageReference pageRef) {

        super(id, restClient, pageRef, false);
        this.target = target;
        this.searchPanel = searchPanel;
        this.parent = parent;

        disableCheckBoxes();

        initResultTable();
    }

    @Override
    protected List<IColumn<FIQLQueryTO, String>> getColumns() {
        List<IColumn<FIQLQueryTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(new StringResourceModel(
                Constants.NAME_FIELD_NAME, this), Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

        columns.add(new PropertyColumn<>(Model.of("FIQL"), "fiql"));

        return columns;
    }

    @Override
    protected ActionsPanel<FIQLQueryTO> getActions(final IModel<FIQLQueryTO> model) {
        ActionsPanel<FIQLQueryTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final FIQLQueryTO ignore) {
                searchPanel.updateFIQL(target, model.getObject().getFiql());
                parent.close(target);
            }
        }, ActionLink.ActionType.SELECT, StringUtils.EMPTY);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final FIQLQueryTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    customActionOnFinishCallback(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", model.getObject().getName(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StringUtils.EMPTY);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Set.of();
    }

    @Override
    protected DirectoryDataProvider<FIQLQueryTO> dataProvider() {
        return new FIQLQueryDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_FIQL_QUERIES_PAGINATOR_ROWS;
    }

    protected class FIQLQueryDataProvider extends DirectoryDataProvider<FIQLQueryTO> {

        private static final long serialVersionUID = 4725679400450513556L;

        protected final SortableDataProviderComparator<FIQLQueryTO> comparator;

        public FIQLQueryDataProvider(final int paginatorRows) {
            super(paginatorRows);

            //Default sorting
            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<FIQLQueryTO> iterator(final long first, final long count) {
            List<FIQLQueryTO> list = restClient.list(target);
            list.sort(comparator);
            return list.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            return restClient.list(target).size();
        }

        @Override
        public IModel<FIQLQueryTO> model(final FIQLQueryTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
