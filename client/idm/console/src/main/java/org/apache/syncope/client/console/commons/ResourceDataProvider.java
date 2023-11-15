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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceDataProvider extends DirectoryDataProvider<Serializable> {

    private static final long serialVersionUID = 3189980210236051840L;

    protected static final Logger LOG = LoggerFactory.getLogger(ResourceDataProvider.class);

    protected final ResourceRestClient resourceRestClient;

    protected final PageReference pageRef;

    protected int currentPage;

    protected final String keyword;

    public ResourceDataProvider(
            final ResourceRestClient resourceRestClient,
            final int paginatorRows,
            final PageReference pageRef,
            final String keyword) {

        super(paginatorRows);

        setSort("keySortParam", SortOrder.ASCENDING);
        this.resourceRestClient = resourceRestClient;
        this.pageRef = pageRef;
        this.keyword = keyword;
    }

    @Override
    public Iterator<ResourceTO> iterator(final long first, final long count) {
        List<ResourceTO> result = List.of();

        try {
            currentPage = ((int) first / paginatorRows);
            if (currentPage < 0) {
                currentPage = 0;
            }
            if (StringUtils.isBlank(keyword)) {
                result = resourceRestClient.list();
            } else {
                result = resourceRestClient.list().stream().
                        filter(resource -> resource.getKey().toLowerCase().contains(keyword)).
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

    private Comparator<ResourceTO> getComparator(final SortParam<String> sortParam) {
        Comparator<ResourceTO> comparator;

        switch (sortParam.getProperty()) {
            case "keySortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getKey().toLowerCase()));
                break;
            case "connectorDisplayNameSortParam":
                comparator = Comparator.nullsFirst(Comparator.comparing(
                        item -> item.getConnectorDisplayName().toLowerCase()));
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
                result = resourceRestClient.list().size();
            } else {
                result = resourceRestClient.list().stream().filter(resource
                        -> resource.getKey().toLowerCase().contains(keyword)).count();
            }
        } catch (Exception e) {
            LOG.error("While requesting for size()", e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(t -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(t));
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
