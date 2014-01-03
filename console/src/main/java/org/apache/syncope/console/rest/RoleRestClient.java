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
package org.apache.syncope.console.rest;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractAttributableRestClient {

    private static final long serialVersionUID = -8549081557283519638L;

    @Override
    public int count() {
        return getService(RoleService.class).list(1, 1).getTotalCount();
    }

    public List<RoleTO> list() {
        return getService(RoleService.class).list().getResult();
    }

    @Override
    public List<RoleTO> list(final int page, final int size, final SortParam<String> sort) {
        return getService(RoleService.class).list(page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public int searchCount(final String fiql) {
        return getService(RoleService.class).search(fiql, 1, 1).getTotalCount();
    }

    @Override
    public List<RoleTO> search(final String fiql, final int page, final int size, final SortParam<String> sort) {
        return getService(RoleService.class).search(fiql, page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public ConnObjectTO getConnectorObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).getConnectorObject(resourceName, AttributableType.ROLE, id);
    }

    public RoleTO create(final RoleTO roleTO) {
        Response response = getService(RoleService.class).create(roleTO);
        return response.readEntity(RoleTO.class);
    }

    public RoleTO read(final Long id) {
        return getAnonymousService(RoleService.class).read(id);
    }

    public RoleTO update(final RoleMod roleMod) {
        return getService(RoleService.class).update(roleMod.getId(), roleMod).readEntity(RoleTO.class);
    }

    @Override
    public RoleTO delete(final Long id) {
        return getService(RoleService.class).delete(id).readEntity(RoleTO.class);
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(RoleRestClient.class).bulkAction(action);
    }

    public BulkActionResult unlink(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkDeassociation(roleId, ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }

    public BulkActionResult link(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkAssociation(roleId, ResourceAssociationActionType.LINK,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }

    public BulkActionResult deprovision(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkDeassociation(roleId, ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }

    public BulkActionResult provision(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkAssociation(roleId, ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }

    public BulkActionResult unassign(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkDeassociation(roleId, ResourceDeassociationActionType.UNASSIGN,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }

    public BulkActionResult assign(final long roleId, final List<StatusBean> statuses) {
        return getService(RoleService.class).bulkAssociation(roleId, ResourceAssociationActionType.ASSIGN,
                CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(), ResourceName.class)).
                readEntity(BulkActionResult.class);
    }
}
