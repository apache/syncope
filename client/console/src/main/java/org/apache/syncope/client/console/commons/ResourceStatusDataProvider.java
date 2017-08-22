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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public class ResourceStatusDataProvider extends DirectoryDataProvider<StatusBean> {

    private static final long serialVersionUID = 6267494272884913376L;

    private final StatusUtils statusUtils;

    private final String resource;

    private final SortableDataProviderComparator<StatusBean> comparator;

    private final AbstractAnyRestClient<? extends AnyTO, ?> restClient;

    protected String fiql;

    private final String realm;

    private final String type;

    public ResourceStatusDataProvider(
            final String type,
            final String resource,
            final int paginatorRows,
            final String realm) {

        super(paginatorRows);
        statusUtils = new StatusUtils();
        this.resource = resource;

        AbstractFiqlSearchConditionBuilder bld;

        if (StringUtils.isEmpty(type)) {
            this.fiql = null;
            restClient = null;
        } else {
            switch (type) {
                case "USER":
                    bld = SyncopeClient.getUserSearchConditionBuilder();
                    restClient = new UserRestClient();
                    break;
                case "GROUP":
                    bld = SyncopeClient.getGroupSearchConditionBuilder();
                    restClient = new GroupRestClient();
                    break;
                default:
                    bld = SyncopeClient.getAnyObjectSearchConditionBuilder(type);
                    restClient = new AnyObjectRestClient();
            }

            this.fiql = bld.hasResources(resource).query();
        }

        setSort("connObjectLink", SortOrder.ASCENDING);

        this.comparator = new SortableDataProviderComparator<>(this);

        this.realm = realm;
        this.type = type;
    }

    @Override
    public Iterator<StatusBean> iterator(final long first, final long count) {
        if (fiql == null) {
            return Collections.<StatusBean>emptyList().iterator();
        }

        final int page = ((int) first / paginatorRows);
        List<? extends AnyTO> result =
                restClient.search(realm, fiql, (page < 0 ? 0 : page) + 1, paginatorRows, getSort(), type);

        List<StatusBean> statuses = result.stream().map(any -> {
            List<ConnObjectWrapper> connObjects =
                    statusUtils.getConnectorObjects(any, Collections.singletonList(resource));

            return statusUtils.getStatusBean(
                    any,
                    resource,
                    connObjects.isEmpty() ? null : connObjects.iterator().next().getConnObjectTO(),
                    any instanceof GroupTO);
        }).collect(Collectors.toList());

        Collections.sort(statuses, comparator);
        return statuses.iterator();
    }

    @Override
    public long size() {
        if (fiql == null) {
            return 0;
        }
        return restClient.searchCount(realm, fiql, type);
    }

    @Override
    public IModel<StatusBean> model(final StatusBean object) {
        return new CompoundPropertyModel<>(object);
    }
}
