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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.BulkMembersActionType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Group's services.
 */
public class GroupRestClient extends AbstractAnyRestClient<GroupTO, GroupPatch> {

    private static final long serialVersionUID = -8549081557283519638L;

    @Override
    protected Class<? extends AnyService<GroupTO, GroupPatch>> getAnyServiceClass() {
        return GroupService.class;
    }

    @Override
    public int searchCount(final String realm, final String fiql, final String type) {
        return getService(GroupService.class).
                search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<GroupTO> search(
            final String realm,
            final String fiql,
            final int page,
            final int size,
            final SortParam<String> sort,
            final String type) {

        List<GroupTO> result = new ArrayList<>();
        PagedResult<GroupTO> res;
        do {
            res = getService(GroupService.class).
                    search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(page).size(size).
                            orderBy(toOrderBy(sort)).details(false).build());
            result.addAll(res.getResult());
        } while (page == -1 && size == -1 && res.getNext() != null);

        return result;
    }

    public void bulkMembersAction(final String key, final BulkMembersActionType actionType) {
        getService(GroupService.class).bulkMembersAction(key, actionType);
    }
}
