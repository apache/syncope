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
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserSearchPanel extends AnyObjectSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    public static class Builder extends AnyObjectSearchPanel.Builder {

        private static final long serialVersionUID = 6308997285778809578L;

        public Builder(final IModel<List<SearchClause>> model, final PageReference pageRef) {
            super(AnyTypeKind.USER.name(), model, pageRef);
        }

        @Override
        public UserSearchPanel build(final String id) {
            return new UserSearchPanel(id, this);
        }
    }

    @SpringBean
    protected RoleRestClient roleRestClient;

    protected UserSearchPanel(final String id, final Builder builder) {
        super(id, AnyTypeKind.USER, builder);
    }

    @Override
    protected AbstractFiqlSearchConditionBuilder<?, ?, ?> getSearchConditionBuilder() {
        return SyncopeClient.getUserSearchConditionBuilder();
    }

    @Override
    protected String getFIQLQueryTarget() {
        return AnyTypeKind.USER.name();
    }

    @Override
    protected void populate() {
        super.populate();

        this.roleNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return roleRestClient.list().stream().map(RoleTO::getKey).collect(Collectors.toList());
            }
        };
    }

    @Override
    protected List<SearchClause.Type> getAvailableTypes() {
        List<SearchClause.Type> result = new ArrayList<>();
        result.add(SearchClause.Type.ATTRIBUTE);
        result.add(SearchClause.Type.ROLE_MEMBERSHIP);
        result.add(SearchClause.Type.GROUP_MEMBERSHIP);
        result.add(SearchClause.Type.AUX_CLASS);
        result.add(SearchClause.Type.RESOURCE);
        result.add(SearchClause.Type.RELATIONSHIP);
        return result;
    }
}
