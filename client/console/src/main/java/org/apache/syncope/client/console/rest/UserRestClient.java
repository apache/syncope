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
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.common.lib.patch.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking rest users services.
 */
public class UserRestClient extends AbstractAnyRestClient<UserTO, UserPatch> {

    private static final long serialVersionUID = -1575748964398293968L;

    @Override
    protected Class<? extends AnyService<UserTO, UserPatch>> getAnyServiceClass() {
        return UserService.class;
    }

    public ProvisioningResult<UserTO> create(final UserTO userTO, final boolean storePassword) {
        Response response = getService(UserService.class).create(userTO, storePassword);
        return response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        });
    }

    @Override
    public int searchCount(final String realm, final String fiql, final String type) {
        return getService(UserService.class).
                search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(1).size(1).build()).
                getTotalCount();
    }

    @Override
    public List<UserTO> search(
            final String realm, final String fiql, final int page, final int size, final SortParam<String> sort,
            final String type) {

        return getService(UserService.class).
                search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(page).size(size).
                        orderBy(toOrderBy(sort)).details(false).build()).getResult();
    }

    public ProvisioningResult<UserTO> mustChangePassword(final String etag, final boolean value, final String key) {
        final UserPatch userPatch = new UserPatch();
        userPatch.setKey(key);
        userPatch.setMustChangePassword(new BooleanReplacePatchItem.Builder().value(value).build());
        return update(etag, userPatch);
    }

    public BulkActionResult suspend(final String etag, final String userKey, final List<StatusBean> statuses) {
        StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses, false);
        statusPatch.setKey(userKey);
        statusPatch.setType(StatusPatchType.SUSPEND);

        BulkActionResult result;
        synchronized (this) {
            result = new BulkActionResult();
            Map<String, BulkActionResult.Status> res = result.getResults();
            UserService service = getService(etag, UserService.class);

            @SuppressWarnings("unchecked")
            ProvisioningResult<UserTO> provisions = (ProvisioningResult<UserTO>) service.status(statusPatch).
                    readEntity(ProvisioningResult.class);

            if (statusPatch.isOnSyncope()) {
                res.put(StringUtils.capitalize(Constants.SYNCOPE),
                        "suspended".equalsIgnoreCase(provisions.getEntity().getStatus())
                        ? BulkActionResult.Status.SUCCESS
                        : BulkActionResult.Status.FAILURE);
            }

            for (PropagationStatus status : provisions.getPropagationStatuses()) {
                res.put(status.getResource(), BulkActionResult.Status.valueOf(status.getStatus().name()));
            }
            resetClient(UserService.class);
        }
        return result;
    }

    public BulkActionResult reactivate(final String etag, final String userKey, final List<StatusBean> statuses) {
        StatusPatch statusPatch = StatusUtils.buildStatusPatch(statuses, true);
        statusPatch.setKey(userKey);
        statusPatch.setType(StatusPatchType.REACTIVATE);

        BulkActionResult result;
        synchronized (this) {
            result = new BulkActionResult();
            Map<String, BulkActionResult.Status> res = result.getResults();
            UserService service = getService(etag, UserService.class);

            @SuppressWarnings("unchecked")
            ProvisioningResult<UserTO> provisions = (ProvisioningResult<UserTO>) service.status(statusPatch).
                    readEntity(ProvisioningResult.class);

            if (statusPatch.isOnSyncope()) {
                res.put(StringUtils.capitalize(Constants.SYNCOPE),
                        "active".equalsIgnoreCase(provisions.getEntity().getStatus())
                        ? BulkActionResult.Status.SUCCESS
                        : BulkActionResult.Status.FAILURE);
            }

            for (PropagationStatus status : provisions.getPropagationStatuses()) {
                res.put(status.getResource(), BulkActionResult.Status.valueOf(status.getStatus().name()));
            }
            resetClient(UserService.class);
        }
        return result;
    }
}
