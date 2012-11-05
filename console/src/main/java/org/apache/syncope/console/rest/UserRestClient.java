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

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.ConnObjectTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking rest users services.
 */
@Component
public class UserRestClient extends AbstractBaseRestClient {

    public Integer count() {
        return SyncopeSession.get().getRestTemplate().getForObject(baseURL + "user/count.json", Integer.class);
    }

    /**
     * Get all stored users.
     *
     * @param page pagination element to fetch
     * @param size maximum number to fetch
     * @return list of TaskTO objects
     */
    public List<UserTO> list(final int page, final int size) {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "user/list/{page}/{size}.json", UserTO[].class, page, size));
    }

    public UserTO create(final UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        return SyncopeSession.get().getRestTemplate().postForObject(baseURL + "user/create", userTO, UserTO.class);
    }

    public UserTO update(UserMod userModTO)
            throws SyncopeClientCompositeErrorException {

        return SyncopeSession.get().getRestTemplate().postForObject(baseURL + "user/update", userModTO, UserTO.class);
    }

    public UserTO delete(Long id)
            throws SyncopeClientCompositeErrorException {

        return SyncopeSession.get().getRestTemplate().getForObject(baseURL + "user/delete/{userId}", UserTO.class, id);
    }

    public UserTO read(Long id) {
        UserTO userTO = null;
        try {
            userTO = SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "user/read/{userId}.json", UserTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a user", e);
        }
        return userTO;
    }

    public Integer searchCount(final NodeCond searchCond) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/search/count.json", searchCond, Integer.class);
    }

    public List<UserTO> search(final NodeCond searchCond)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/search", searchCond, UserTO[].class));
    }

    public List<UserTO> search(final NodeCond searchCond, final int page, final int size)
            throws SyncopeClientCompositeErrorException {

        return Arrays.asList(SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/search/{page}/{size}", searchCond, UserTO[].class, page, size));
    }

    public ConnObjectTO getRemoteObject(final String resourceName, final String objectId)
            throws SyncopeClientCompositeErrorException {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, resourceName, objectId);
    }

    public UserTO reactivate(long userId, List<StatusBean> statuses)
            throws SyncopeClientCompositeErrorException {

        return enable(userId, statuses, true);
    }

    public UserTO suspend(long userId, List<StatusBean> statuses)
            throws SyncopeClientCompositeErrorException {

        return enable(userId, statuses, false);
    }

    private UserTO enable(final long userId, final List<StatusBean> statuses, final boolean enable)
            throws SyncopeClientCompositeErrorException {

        final StringBuilder query = new StringBuilder();

        query.append(baseURL).append("user/").append(enable
                ? "reactivate/"
                : "suspend/").append(userId).append("?").
                // perform on resource if and only if resources have been speciofied
                append("performRemotely=").append(!statuses.isEmpty()).append("&");

        boolean performLocal = false;

        for (StatusBean status : statuses) {
            if ((enable && !status.getStatus().isActive()) || (!enable && status.getStatus().isActive())) {

                if ("Syncope".equals(status.getResourceName())) {
                    performLocal = true;
                } else {
                    query.append("resourceNames=").append(status.getResourceName()).append("&");
                }
            }
        }

        // perform on syncope if and only if it has been requested
        query.append("performLocally=").append(performLocal);

        return SyncopeSession.get().getRestTemplate().getForObject(query.toString(), UserTO.class);
    }
}
