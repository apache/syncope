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
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractSubjectRestClient {

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
        return getService(ResourceService.class).getConnectorObject(resourceName, SubjectType.ROLE, id);
    }

    public RoleTO create(final RoleTO roleTO) {
        Response response = getService(RoleService.class).create(roleTO);
        return response.readEntity(RoleTO.class);
    }

    public RoleTO read(final Long id) {
        return getAnonymousService(RoleService.class).read(id);
    }

    public RoleTO update(final String etag, final RoleMod roleMod) {
        RoleTO result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.update(roleMod.getId(), roleMod).readEntity(RoleTO.class);
            resetClient(RoleService.class);
        }
        return result;
    }

    @Override
    public RoleTO delete(final String etag, final Long id) {
        RoleTO result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.delete(id).readEntity(RoleTO.class);
            resetClient(RoleService.class);
        }
        return result;
    }

    @Override
    public void bulkAction(final BulkAction action) {
        getService(RoleRestClient.class).bulkAction(action);
    }

    public void unlink(final String etag, final long roleId, final List<StatusBean> statuses) {
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            service.bulkDeassociation(roleId, ResourceDeassociationActionType.UNLINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            resetClient(RoleService.class);
        }
    }

    public void link(final String etag, final long roleId, final List<StatusBean> statuses) {
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            service.bulkAssociation(roleId, ResourceAssociationActionType.LINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            resetClient(RoleService.class);
        }
    }

    public BulkActionResult deprovision(final String etag, final long roleId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.bulkDeassociation(roleId, ResourceDeassociationActionType.DEPROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(RoleService.class);
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long roleId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.bulkAssociation(roleId, ResourceAssociationActionType.PROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(RoleService.class);
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long roleId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.bulkDeassociation(roleId, ResourceDeassociationActionType.UNASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(RoleService.class);
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long roleId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            RoleService service = getService(etag, RoleService.class);
            result = service.bulkAssociation(roleId, ResourceAssociationActionType.ASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(RoleService.class);
        }
        return result;
    }
}
