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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public class AnyDataProvider<A extends AnyTO> extends DirectoryDataProvider<A> {

    private static final long serialVersionUID = 6267494272884913376L;

    private final SortableAnyProviderComparator<A> comparator;

    private final AbstractAnyRestClient<A, ?> restClient;

    protected String fiql;

    protected final boolean filtered;

    private final String realm;

    private final String type;

    public AnyDataProvider(
            final AbstractAnyRestClient<A, ?> restClient,
            final int paginatorRows,
            final boolean filtered,
            final String realm,
            final String type) {

        super(paginatorRows);

        this.restClient = restClient;

        this.filtered = filtered;

        // default sorting
        switch (type) {
            case "USER":
                setSort("username", SortOrder.ASCENDING);
                break;

            case "GROUP":
                setSort("name", SortOrder.ASCENDING);
                break;

            default:
                setSort("key", SortOrder.ASCENDING);
        }

        this.comparator = new SortableAnyProviderComparator<>(this);

        this.realm = realm;
        this.type = type;
    }

    @Override
    public Iterator<A> iterator(final long first, final long count) {
        List<A> result;

        final int page = ((int) first / paginatorRows);

        if (filtered) {
            result = fiql == null
                    ? Collections.<A>emptyList()
                    : restClient.search(realm, fiql, (page < 0 ? 0 : page) + 1, paginatorRows, getSort(), type);
        } else {
            result = restClient.search(realm, null, (page < 0 ? 0 : page) + 1, paginatorRows, getSort(), type);
        }

        Collections.sort(result, comparator);
        return result.iterator();
    }

    @Override
    public long size() {
        long result;

        if (filtered) {
            result = fiql == null ? 0 : restClient.searchCount(realm, fiql, type);
        } else {
            result = restClient.searchCount(realm, null, type);
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
}
