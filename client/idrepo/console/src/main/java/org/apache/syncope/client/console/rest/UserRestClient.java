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

import jakarta.ws.rs.core.GenericType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.Status;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking rest users services.
 */
public class UserRestClient extends AbstractAnyRestClient<UserTO> {

    private static final long serialVersionUID = -1575748964398293968L;

    @Override
    protected Class<? extends AnyService<UserTO>> getAnyServiceClass() {
        return UserService.class;
    }

    public ProvisioningResult<UserTO> create(final UserCR createReq) {
        return getService(UserService.class).create(createReq).readEntity(new GenericType<>() {
        });
    }

    public ProvisioningResult<UserTO> update(final String etag, final UserUR updateReq) {
        ProvisioningResult<UserTO> result;
        synchronized (this) {
            result = getService(etag, UserService.class).update(updateReq).readEntity(new GenericType<>() {
            });
            resetClient(getAnyServiceClass());
        }
        return result;
    }

    @Override
    public long count(final String realm, final String fiql, final String type) {
        return getService(UserService.class).
                search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(1).size(0).details(false).build()).
                getTotalCount();
    }

    @Override
    public List<UserTO> search(
            final String realm, final String fiql, final int page, final int size, final SortParam<String> sort,
            final String type) {

        return getService(UserService.class).
                search(new AnyQuery.Builder().realm(realm).fiql(fiql).page(page).size(size).details(false).
                        orderBy(toOrderBy(sort)).details(false).build()).getResult();
    }

    public ProvisioningResult<UserTO> mustChangePassword(final String etag, final boolean value, final String key) {
        UserUR userUR = new UserUR.Builder(key).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(value).build()).
                build();
        return update(etag, userUR);
    }

    private Map<String, String> status(
            final StatusRType type, final String etag, final String userKey, final List<StatusBean> statuses) {

        StatusR statusR = StatusUtils.statusR(userKey, type, statuses);

        Map<String, String> results;
        synchronized (this) {
            ProvisioningResult<UserTO> provisioningResult = getService(etag, UserService.class).status(statusR).
                    readEntity(new GenericType<>() {
                    });

            statuses.forEach(statusBean -> statusBean.setStatus(Status.UNDEFINED));

            results = new HashMap<>();
            provisioningResult.getPropagationStatuses().forEach(propagationStatus -> {
                results.put(propagationStatus.getResource(), propagationStatus.getStatus().name());

                if (propagationStatus.getAfterObj() != null) {
                    Boolean enabled = StatusUtils.isEnabled(propagationStatus.getAfterObj());
                    if (enabled != null) {
                        statuses.stream().
                                filter(statusBean -> propagationStatus.getResource().equals(statusBean.getResource())).
                                findFirst().
                                ifPresent(statusBean -> statusBean.setStatus(
                                enabled ? Status.ACTIVE : Status.SUSPENDED));
                    }
                }
            });
            statuses.stream().
                    filter(statusBean -> Constants.SYNCOPE.equals(statusBean.getResource())).
                    findFirst().
                    ifPresent(statusBean -> statusBean.setStatus(
                    "suspended".equalsIgnoreCase(provisioningResult.getEntity().getStatus())
                    ? Status.SUSPENDED : Status.ACTIVE));
            if (statusR.isOnSyncope()) {
                results.put(Constants.SYNCOPE,
                        ("suspended".equalsIgnoreCase(provisioningResult.getEntity().getStatus())
                        && type == StatusRType.SUSPEND)
                        || ("active".equalsIgnoreCase(provisioningResult.getEntity().getStatus())
                        && type == StatusRType.REACTIVATE)
                                ? ExecStatus.SUCCESS.name()
                                : ExecStatus.FAILURE.name());
            }

            resetClient(UserService.class);
        }
        return results;
    }

    public Map<String, String> suspend(
            final String etag, final String userKey, final List<StatusBean> statuses) {

        return status(StatusRType.SUSPEND, etag, userKey, statuses);
    }

    public Map<String, String> reactivate(
            final String etag, final String userKey, final List<StatusBean> statuses) {

        return status(StatusRType.REACTIVATE, etag, userKey, statuses);
    }
}
