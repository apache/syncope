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
package org.apache.syncope.client.cli.commands.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.UserService;

public class UserSyncopeOperations {

    private static final int PAGE_SIZE = 100;

    private final UserService userService = SyncopeServices.get(UserService.class);

    public boolean auth(final String username, final String password) {
        try {
            SyncopeServices.testUsernameAndPassword(username, password);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public List<UserTO> searchByRole(final String realm, final String role) {
        return search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles(role).query()).build());
    }

    public List<UserTO> searchByResource(final String realm, final String resource) {
        return search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().hasResources(resource).query()).build());
    }

    public List<UserTO> searchByAttribute(final String realm, final String attributeName, final String attributeValue) {
        return search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is(attributeName).equalTo(attributeValue).query()).
                build());
    }

    public List<UserTO> list() {
        return search(new AnyQuery());
    }

    private List<UserTO> search(final AnyQuery query) {
        query.setPage(0);
        query.setSize(0);
        int count = userService.search(query).getTotalCount();

        List<UserTO> result = new ArrayList<>();

        query.setSize(PAGE_SIZE);
        for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
            query.setPage(page);

            result.addAll(userService.search(query).getResult());
        }

        return result;
    }

    public UserTO read(final String userKey) {
        return userService.read(userKey);
    }

    public void delete(final String userKey) {
        userService.delete(userKey);
    }

    public List<BatchResponseItem> deleteByAttribute(
            final String realm, final String attributeName, final String attributeValue) throws IOException {

        return batchDelete(new AnyQuery.Builder().realm(realm).fiql(
                SyncopeClient.getUserSearchConditionBuilder().is(attributeName).equalTo(attributeValue).query()).
                build());
    }

    public List<BatchResponseItem> deleteAll(final String realm) throws IOException {
        return batchDelete(new AnyQuery.Builder().realm(realm).details(false).build());
    }

    private List<BatchResponseItem> batchDelete(final AnyQuery query) throws IOException {
        query.setPage(0);
        query.setSize(0);
        int count = userService.search(query).getTotalCount();

        BatchRequest batchRequest = SyncopeServices.batch();
        UserService batchUserService = batchRequest.getService(UserService.class);

        query.setSize(PAGE_SIZE);
        for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
            query.setPage(page);

            userService.search(query).getResult().forEach(user -> batchUserService.delete(user.getKey()));
        }

        Response response = batchRequest.commit().getResponse();
        return BatchPayloadParser.parse(
                (InputStream) response.getEntity(), response.getMediaType(), new BatchResponseItem());
    }
}
