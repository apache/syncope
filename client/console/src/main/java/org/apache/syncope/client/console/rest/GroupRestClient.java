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

import java.util.List;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Group's services.
 */
@Component
public class GroupRestClient extends AbstractAnyRestClient {

    private static final long serialVersionUID = -8549081557283519638L;

    @Override
    public int count(final String realm) {
        return getService(GroupService.class).
                list(SyncopeClient.getAnyListQueryBuilder().realm(realm).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<GroupTO> list(final String realm, final int page, final int size, final SortParam<String> sort,
            final String type) {
        return getService(GroupService.class).
                list(SyncopeClient.getAnyListQueryBuilder().realm(realm).page(page).size(size).
                        orderBy(toOrderBy(sort)).details(false).build()).
                getResult();
    }

    @Override
    public int searchCount(final String realm, final String fiql, final String type) {
        return getService(GroupService.class).
                search(SyncopeClient.getAnySearchQueryBuilder().realm(realm).fiql(fiql).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<GroupTO> search(
            final String realm, final String fiql, final int page, final int size, final SortParam<String> sort,
            final String type) {

        return getService(GroupService.class).
                search(SyncopeClient.getAnySearchQueryBuilder().realm(realm).fiql(fiql).page(page).size(size).
                        orderBy(toOrderBy(sort)).details(false).build()).
                getResult();
    }

    @Override
    public ConnObjectTO readConnObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).readConnObject(resourceName, AnyTypeKind.GROUP.name(), id);
    }

    public GroupTO create(final GroupTO groupTO) {
        Response response = getService(GroupService.class).create(groupTO);
        return response.readEntity(GroupTO.class);
    }

    public GroupTO read(final Long key) {
        return getService(GroupService.class).read(key);
    }

    public GroupTO update(final String etag, final GroupMod groupMod) {
        GroupTO result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.update(groupMod).readEntity(GroupTO.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    @Override
    public GroupTO delete(final String etag, final Long key) {
        GroupTO result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.delete(key).readEntity(GroupTO.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(GroupService.class).bulk(action);
    }

    public void unlink(final String etag, final long groupKey, final List<StatusBean> statuses) {
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            service.bulkDeassociation(groupKey, ResourceDeassociationActionType.UNLINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class));
            resetClient(GroupService.class);
        }
    }

    public void link(final String etag, final long groupKey, final List<StatusBean> statuses) {
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));
            service.bulkAssociation(groupKey, ResourceAssociationActionType.LINK, associationMod);

            resetClient(GroupService.class);
        }
    }

    public BulkActionResult deprovision(final String etag, final long groupKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkDeassociation(groupKey, ResourceDeassociationActionType.DEPROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long groupKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));

            result = service.bulkAssociation(groupKey, ResourceAssociationActionType.PROVISION, associationMod).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long groupKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkDeassociation(groupKey, ResourceDeassociationActionType.UNASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceKey.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long groupKey, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);

            ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceKey.class));

            result = service.bulkAssociation(groupKey, ResourceAssociationActionType.ASSIGN, associationMod).
                    readEntity(BulkActionResult.class);

            resetClient(GroupService.class);
        }
        return result;
    }
}
