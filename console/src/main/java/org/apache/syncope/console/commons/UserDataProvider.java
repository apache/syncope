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
package org.apache.syncope.console.commons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.exceptions.InvalidSearchConditionException;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.UserTO;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDataProvider extends SortableDataProvider<UserTO> {

    private static final long serialVersionUID = 6267494272884913376L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(UserDataProvider.class);

    private final SortableUserProviderComparator comparator;

    private NodeCond filter = null;

    private final int paginatorRows;

    private boolean filtered = false;

    private final UserRestClient restClient;

    public UserDataProvider(final UserRestClient restClient, final int paginatorRows, final boolean filtered) {

        super();

        this.restClient = restClient;
        this.filtered = filtered;
        this.paginatorRows = paginatorRows;

        // Default sorting
        setSort("id", SortOrder.ASCENDING);

        comparator = new SortableUserProviderComparator(this);
    }

    public void setSearchCond(final NodeCond searchCond) {
        this.filter = searchCond;
    }

    @Override
    public Iterator<UserTO> iterator(final int first, final int count) {
        List<UserTO> users;

        if (filtered) {
            users = new ArrayList<UserTO>();
            if (filter != null) {
                try {
                    users = restClient.search(filter, (first / paginatorRows) + 1, paginatorRows);
                } catch (Exception e) {
                    LOG.error("Could not search for user. \n {}", e.getMessage());
                }
            }
        } else {
            users = restClient.list((first / paginatorRows) + 1, paginatorRows);
        }

        Collections.sort(users, comparator);
        return users.iterator();
    }

    @Override
    public int size() {
        if (filtered) {
            try {
                return filter == null
                        ? 0
                        : restClient.searchCount(filter);
            } catch (InvalidSearchConditionException e) {
                LOG.error("Could not search for user. \n {}", e.getMessage());
                return 0;
            }
        } else {
            return restClient.count();
        }
    }

    @Override
    public IModel<UserTO> model(final UserTO object) {
        return new CompoundPropertyModel<UserTO>(object);
    }
}
