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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyDataProvider<A extends AnyTO> extends DirectoryDataProvider<A> {

    private static final long serialVersionUID = 6267494272884913376L;

    protected static final Logger LOG = LoggerFactory.getLogger(AnyDataProvider.class);

    protected final SortableAnyProviderComparator<A> comparator;

    protected final AbstractAnyRestClient<A> restClient;

    protected String fiql;

    protected final boolean filtered;

    protected final String realm;

    protected final String type;

    protected final PageReference pageRef;

    protected int currentPage;

    public AnyDataProvider(
            final AbstractAnyRestClient<A> restClient,
            final int paginatorRows,
            final boolean filtered,
            final String realm,
            final String type,
            final PageReference pageRef) {

        super(paginatorRows);

        this.restClient = restClient;

        this.filtered = filtered;

        // default sorting
        switch (type) {
            case "USER":
                setSort(Constants.USERNAME_FIELD_NAME, SortOrder.ASCENDING);
                break;

            case "GROUP":
                setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
                break;

            default:
        }

        this.comparator = new SortableAnyProviderComparator<>(this);

        this.realm = realm;
        this.type = type;
        this.pageRef = pageRef;
    }

    @Override
    public Iterator<A> iterator(final long first, final long count) {
        List<A> result = new ArrayList<>();

        try {
            currentPage = ((int) first / paginatorRows);
            if (currentPage < 0) {
                currentPage = 0;
            }

            if (filtered) {
                result = Optional.ofNullable(fiql).
                        map(s -> restClient.search(realm, s, currentPage + 1, paginatorRows, getSort(), type)).
                        orElseGet(List::of);
            } else {
                result = restClient.search(realm, null, currentPage + 1, paginatorRows, getSort(), type);
            }
        } catch (Exception e) {
            LOG.error("While searching with FIQL {}", fiql, e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(target -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target));
        }

        return result.iterator();
    }

    @Override
    public long size() {
        long result = 0;

        try {
            if (filtered) {
                result = Optional.ofNullable(fiql).map(s -> restClient.count(realm, s, type)).orElse(0L);
            } else {
                result = restClient.count(realm, null, type);
            }
        } catch (Exception e) {
            LOG.error("While requesting for size() with FIQL {}", fiql, e);
            SyncopeConsoleSession.get().onException(e);

            RequestCycle.get().find(AjaxRequestTarget.class).
                    ifPresent(target -> ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target));
        }

        return result;
    }

    public AnyDataProvider<A> setFIQL(final String fiql) {
        this.fiql = fiql;
        return this;
    }

    @Override
    public IModel<A> model(final A object) {
        return new CompoundPropertyModel<>(object);
    }

    public int getCurrentPage() {
        return currentPage;
    }
}
