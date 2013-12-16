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
package org.apache.syncope.console.pages.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.search.SyncopeFiqlSearchConditionBuilder;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    @SpringBean
    private RoleRestClient roleRestClient;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 6308997285778809578L;

        private String id;

        private String fiql = null;

        private boolean required = true;

        public Builder(final String id) {
            this.id = id;
        }

        public UserSearchPanel.Builder fiql(final String fiql) {
            this.fiql = fiql;
            return this;
        }

        public UserSearchPanel.Builder required(final boolean required) {
            this.required = required;
            return this;
        }

        public UserSearchPanel build() {
            return new UserSearchPanel(this);
        }
    }

    private UserSearchPanel(final Builder builder) {
        super(builder.id, AttributableType.USER, builder.fiql, builder.required);
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<List<SearchClause.Type>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchClause.Type> load() {
                List<SearchClause.Type> result = new ArrayList<SearchClause.Type>();
                result.add(SearchClause.Type.ATTRIBUTE);
                result.add(SearchClause.Type.MEMBERSHIP);
                result.add(SearchClause.Type.RESOURCE);
                return result;
            }
        };

        this.roleNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                List<RoleTO> roleTOs = roleRestClient.list();

                List<String> result = new ArrayList<String>(roleTOs.size());
                for (RoleTO role : roleTOs) {
                    result.add(role.getDisplayName());
                }

                return result;
            }
        };
    }

    @Override
    protected SyncopeFiqlSearchConditionBuilder getSearchConditionBuilder() {
        return SyncopeClient.getUserSearchConditionBuilder();
    }

}
