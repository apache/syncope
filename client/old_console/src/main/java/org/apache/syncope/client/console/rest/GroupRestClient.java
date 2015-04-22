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
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Group's services.
 */
@Component
public class GroupRestClient extends AbstractSubjectRestClient {

    private static final long serialVersionUID = -8549081557283519638L;

    @Override
    public int count() {
        return getService(GroupService.class).list(1, 1).getTotalCount();
    }

    public List<GroupTO> list() {
        return getService(GroupService.class).list(1, 1000).getResult();
    }

    @Override
    public List<GroupTO> list(final int page, final int size, final SortParam<String> sort) {
        return getService(GroupService.class).list(page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public int searchCount(final String fiql) {
        return getService(GroupService.class).search(fiql, 1, 1).getTotalCount();
    }

    @Override
    public List<GroupTO> search(final String fiql, final int page, final int size, final SortParam<String> sort) {
        return getService(GroupService.class).search(fiql, page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public ConnObjectTO getConnectorObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).getConnectorObject(resourceName, SubjectType.GROUP, id);
    }

    public GroupTO create(final GroupTO groupTO) {
        Response response = getService(GroupService.class).create(groupTO);
        return response.readEntity(GroupTO.class);
    }

    public GroupTO read(final Long id) {
        return getAnonymousService(GroupService.class).read(id);
    }

    public GroupTO update(final String etag, final GroupMod groupMod) {
        GroupTO result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.update(groupMod.getKey(), groupMod).readEntity(GroupTO.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    @Override
    public GroupTO delete(final String etag, final Long id) {
        GroupTO result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.delete(id).readEntity(GroupTO.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(GroupService.class).bulk(action);
    }

    public void unlink(final String etag, final long groupId, final List<StatusBean> statuses) {
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            service.bulkDeassociation(groupId, ResourceDeassociationActionType.UNLINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            resetClient(GroupService.class);
        }
    }

    public void link(final String etag, final long groupId, final List<StatusBean> statuses) {
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            service.bulkAssociation(groupId, ResourceAssociationActionType.LINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            resetClient(GroupService.class);
        }
    }

    public BulkActionResult deprovision(final String etag, final long groupId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkDeassociation(groupId, ResourceDeassociationActionType.DEPROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long groupId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkAssociation(groupId, ResourceAssociationActionType.PROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long groupId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkDeassociation(groupId, ResourceDeassociationActionType.UNASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long groupId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            GroupService service = getService(etag, GroupService.class);
            result = service.bulkAssociation(groupId, ResourceAssociationActionType.ASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(GroupService.class);
        }
        return result;
    }
}
