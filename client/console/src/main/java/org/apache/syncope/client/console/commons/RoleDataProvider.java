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
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public class RoleDataProvider extends SearchableDataProvider<RoleTO> {

    private static final long serialVersionUID = 6267494272884913376L;

    private final SortableDataProviderComparator<RoleTO> comparator;

    private final RoleRestClient restClient = new RoleRestClient();

    public RoleDataProvider(final int paginatorRows) {
        super(paginatorRows);
        this.comparator = new SortableDataProviderComparator<>(this);
    }

    @Override
    public Iterator<RoleTO> iterator(final long first, final long count) {
        final List<RoleTO> result = restClient.list();
        Collections.sort(result, comparator);
        return result.iterator();
    }

    @Override
    public long size() {
        return restClient.count();
    }

    @Override
    public IModel<RoleTO> model(final RoleTO object) {
        return new CompoundPropertyModel<>(object);
    }
}
