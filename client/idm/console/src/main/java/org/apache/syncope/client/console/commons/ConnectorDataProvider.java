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
package org.apache.syncope.client.console.commons;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectorDataProvider extends DirectoryDataProvider<Serializable> {

    private static final long serialVersionUID = 3122389673525690470L;

    protected static final Logger LOG = LoggerFactory.getLogger(ConnectorDataProvider.class);

    protected final ConnectorRestClient connectorRestClient;

    protected final PageReference pageRef;

    protected int currentPage;

    protected final String keyword;

    public ConnectorDataProvider(
            final ConnectorRestClient connectorRestClient,
            final int paginatorRows,
            final PageReference pageRef,
            final String keyword) {

        super(paginatorRows);

        setSort("displayNameSortParam", SortOrder.ASCENDING);
        this.connectorRestClient = connectorRestClient;
        this.pageRef = pageRef;
        this.keyword = keyword;
    }

    @Override
    public Iterator<ConnInstanceTO> iterator(final long first, final long count) {
        List<ConnInstanceTO> result = Collections.emptyList();

        try {
            currentPage = ((int) first / paginatorRows);
            if (currentPage < 0) {
                currentPage = 0;
            }
            if (StringUtils.isBlank(keyword)) {
                result = connectorRestClient.getAllConnectors();
            } else {
                result = connectorRestClient.getAllConnectors().stream().
                        filter(conn -> conn.getDisplayName().toLowerCase().contains(keyword)).
                        collect(Collectors.toList());
            }
        } catch (Exception e) {
            LOG.error("While searching", e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(t -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(t));
        }

        SortParam<String> sortParam = getSort();
        if (sortParam != null) {
            result.sort(getComparator(sortParam));
        }

        return result.subList((int) first, (int) first + (int) count).iterator();
    }

    private Comparator<ConnInstanceTO> getComparator(final SortParam<String> sortParam) {
        Comparator<ConnInstanceTO> comparator;

        switch (sortParam.getProperty()) {
            case "displayNameSortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getDisplayName().toLowerCase()));
                break;
            case "connectorNameSortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getConnectorName().toLowerCase()));
                break;
            default:
                throw new IllegalStateException("The sort param " + sortParam.getProperty() + " is not correct");
        }

        if (!sortParam.isAscending()) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    @Override
    public long size() {
        long result = 0;

        try {
            if (StringUtils.isBlank(keyword)) {
                result = connectorRestClient.getAllConnectors().size();
            } else {
                result = connectorRestClient.getAllConnectors().stream().filter(conn
                        -> conn.getDisplayName().toLowerCase().contains(keyword)).count();
            }
        } catch (Exception e) {
            LOG.error("While requesting for size()", e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(target -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target));
        }

        return result;
    }

    @Override
    public IModel<Serializable> model(final Serializable object) {
        return new CompoundPropertyModel<>(object);
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
