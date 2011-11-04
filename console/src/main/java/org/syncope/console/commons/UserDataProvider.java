/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.commons;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.UserRestClient;

public class UserDataProvider extends SortableDataProvider<UserTO> {

    private SortableUserProviderComparator comparator;

    private NodeCond filter = null;

    final private int rows;

    private boolean filtered = false;

    private UserRestClient restClient;

    public UserDataProvider(
            final UserRestClient restClient,
            final int rows,
            final boolean filtered) {

        super();

        this.restClient = restClient;
        this.filtered = filtered;
        this.rows = rows;

        //Default sorting
        setSort("id", SortOrder.ASCENDING);

        comparator = new SortableUserProviderComparator(this);
    }

    public void setSearchCond(final NodeCond searchCond) {
        this.filter = searchCond;
    }

    @Override
    public Iterator<UserTO> iterator(final int first, final int count) {
        final List<UserTO> users;

        if (filtered) {
            users = filter == null
                    ? Collections.EMPTY_LIST
                    : restClient.search(
                    filter, (first / rows) + 1, rows);
        } else {
            users = restClient.list(
                    (first / rows) + 1, rows);
        }

        Collections.sort(users, comparator);
        return users.iterator();
    }

    @Override
    public int size() {
        if (filtered) {
            return filter == null ? 0 : restClient.searchCount(filter);
        } else {
            return restClient.count();
        }
    }

    @Override
    public IModel<UserTO> model(final UserTO object) {
        return new CompoundPropertyModel<UserTO>(object);
    }
}
