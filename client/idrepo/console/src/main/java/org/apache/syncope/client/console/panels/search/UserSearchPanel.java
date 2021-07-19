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
package org.apache.syncope.client.console.panels.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class UserSearchPanel extends AnyObjectSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    public static class Builder extends AnyObjectSearchPanel.Builder {

        private static final long serialVersionUID = 6308997285778809578L;

        public Builder(final IModel<List<SearchClause>> model) {
            super(AnyTypeKind.USER.name(), model);
        }

        @Override
        public UserSearchPanel build(final String id) {
            return new UserSearchPanel(id, this);
        }
    }

    protected UserSearchPanel(final String id, final Builder builder) {
        super(id, AnyTypeKind.USER, builder);
    }

    @Override
    protected void populate() {
        super.populate();

        this.roleNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return RoleRestClient.list().stream().map(EntityTO::getKey).collect(Collectors.toList());
            }
        };

        this.privilegeNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return ApplicationRestClient.list().stream().
                    flatMap(application -> application.getPrivileges().stream()).
                    map(EntityTO::getKey).collect(Collectors.toList());
            }
        };
    }

    @Override
    protected List<SearchClause.Type> getAvailableTypes() {
        List<SearchClause.Type> result = new ArrayList<>();
        result.add(SearchClause.Type.ATTRIBUTE);
        result.add(SearchClause.Type.ROLE_MEMBERSHIP);
        result.add(SearchClause.Type.PRIVILEGE);
        result.add(SearchClause.Type.GROUP_MEMBERSHIP);
        result.add(SearchClause.Type.RESOURCE);
        result.add(SearchClause.Type.RELATIONSHIP);
        return result;
    }
}
