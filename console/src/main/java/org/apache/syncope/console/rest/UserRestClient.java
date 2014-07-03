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
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractSubjectRestClient {

    private static final long serialVersionUID = -1575748964398293968L;

    @Override
    public int count() {
        return getService(UserService.class).list(1, 1).getTotalCount();
    }

    @Override
    public List<UserTO> list(final int page, final int size, final SortParam<String> sort) {
        return getService(UserService.class).list(page, size, toOrderBy(sort)).getResult();
    }

    public UserTO create(final UserTO userTO) {
        Response response = getService(UserService.class).create(userTO);
        return response.readEntity(UserTO.class);
    }

    public UserTO update(final String etag, final UserMod userMod) {
        UserTO result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.update(userMod.getId(), userMod).readEntity(UserTO.class);
            resetClient(UserService.class);
        }
        return result;
    }

    @Override
    public UserTO delete(final String etag, final Long id) {
        UserTO result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.delete(id).readEntity(UserTO.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public UserTO read(final Long id) {
        UserTO userTO = null;
        try {
            userTO = getService(UserService.class).read(id);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    @Override
    public int searchCount(final String fiql) {
        return getService(UserService.class).search(fiql, 1, 1).getTotalCount();
    }

    @Override
    public List<UserTO> search(final String fiql, final int page, final int size, final SortParam<String> sort) {
        return getService(UserService.class).search(fiql, page, size, toOrderBy(sort)).getResult();
    }

    @Override
    public ConnObjectTO getConnectorObject(final String resourceName, final Long id) {
        return getService(ResourceService.class).getConnectorObject(resourceName, SubjectType.USER, id);
    }

    public void suspend(final String etag, final long userId, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, false);
        statusMod.setType(StatusMod.ModType.SUSPEND);
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.status(userId, statusMod);
            resetClient(UserService.class);
        }
    }

    public void reactivate(final String etag, final long userId, final List<StatusBean> statuses) {
        StatusMod statusMod = StatusUtils.buildStatusMod(statuses, true);
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.status(userId, statusMod);
            resetClient(UserService.class);
        }
    }

    @Override
    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(UserService.class).bulk(action);
    }

    public void unlink(final String etag, final long userId, final List<StatusBean> statuses) {
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            service.bulkDeassociation(userId, ResourceDeassociationActionType.UNLINK,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            resetClient(UserService.class);
        }
    }

    public void link(final String etag, final long userId, final List<StatusBean> statuses) {
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            final ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            service.bulkAssociation(userId, ResourceAssociationActionType.LINK, associationMod);

            resetClient(UserService.class);
        }
    }

    public BulkActionResult deprovision(final String etag, final long userId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.bulkDeassociation(userId, ResourceDeassociationActionType.DEPROVISION,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult provision(final String etag, final long userId,
            final List<StatusBean> statuses, final boolean changepwd, final String password) {

        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            final ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            associationMod.setChangePwd(changepwd);
            associationMod.setPassword(password);

            result = service.bulkAssociation(userId, ResourceAssociationActionType.PROVISION, associationMod).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult unassign(final String etag, final long userId, final List<StatusBean> statuses) {
        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);
            result = service.bulkDeassociation(userId, ResourceDeassociationActionType.UNASSIGN,
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class)).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult assign(final String etag, final long userId,
            final List<StatusBean> statuses, final boolean changepwd, final String password) {

        BulkActionResult result;
        synchronized (this) {
            UserService service = getService(etag, UserService.class);

            final ResourceAssociationMod associationMod = new ResourceAssociationMod();
            associationMod.getTargetResources().addAll(
                    CollectionWrapper.wrap(StatusUtils.buildStatusMod(statuses).getResourceNames(),
                            ResourceName.class));
            associationMod.setChangePwd(changepwd);
            associationMod.setPassword(password);

            result = service.bulkAssociation(userId, ResourceAssociationActionType.ASSIGN, associationMod).
                    readEntity(BulkActionResult.class);
            resetClient(UserService.class);
        }
        return result;
    }
}
