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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
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
        return userService.search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().inRoles(role).query()).build()).getResult();
    }

    public List<UserTO> searchByResource(final String realm, final String resource) {
        return userService.search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().hasResources(resource).query()).build()).getResult();
    }

    public List<UserTO> searchByAttribute(final String realm, final String attributeName, final String attributeValue) {
        return userService.search(new AnyQuery.Builder().realm(realm).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is(attributeName).equalTo(attributeValue).query()).
                build()).getResult();
    }

    public PagedResult<UserTO> list() {
        return userService.search(new AnyQuery());
    }

    public UserTO read(final String userKey) {
        return userService.read(userKey);
    }

    public void delete(final String userKey) {
        userService.delete(userKey);
    }

    public Map<String, BulkActionResult.Status> deleteByAttribute(
            final String realm, final String attributeName, final String attributeValue) {

        return bulkDelete(new AnyQuery.Builder().realm(realm).fiql(
                SyncopeClient.getUserSearchConditionBuilder().is(attributeName).equalTo(attributeValue).query()).
                build());
    }

    public Map<String, BulkActionResult.Status> deleteAll(final String realm) {
        return bulkDelete(new AnyQuery.Builder().realm(realm).details(false).build());
    }

    private Map<String, BulkActionResult.Status> bulkDelete(final AnyQuery query) {
        query.setPage(0);
        query.setSize(0);
        int count = userService.search(query).getTotalCount();

        BulkAction bulkAction = new BulkAction();
        bulkAction.setType(BulkAction.Type.DELETE);

        query.setSize(PAGE_SIZE);
        for (int page = 1; page <= (count / PAGE_SIZE) + 1; page++) {
            query.setPage(page);

            bulkAction.getTargets().addAll(userService.search(query).getResult().stream().
                    map(EntityTO::getKey).collect(Collectors.toList()));
        }

        return userService.bulk(bulkAction).readEntity(BulkActionResult.class).getResults();
    }
}
