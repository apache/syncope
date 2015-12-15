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

import java.util.List;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

public final class UserSearchPanel extends AnyObjectSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    private final UserRestClient userRestClient = new UserRestClient();

    public static class Builder extends AnyObjectSearchPanel.Builder {

        private static final long serialVersionUID = 6308997285778809578L;

        public Builder(final IModel<List<SearchClause>> model) {
            super(model);
        }

        @Override
        public UserSearchPanel build(final String id) {
            return new UserSearchPanel(id, this);
        }
    }

    private UserSearchPanel(final String id, final Builder builder) {
        super(id, AnyTypeKind.USER, builder);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            final String fiql = SearchUtils.buildFIQL(
                    UserSearchPanel.this.getModel().getObject(), SyncopeClient.getUserSearchConditionBuilder());

            final List<UserTO> res = userRestClient.search(
                    "/", fiql, 1, 50, new SortParam<>("username", true), AnyTypeKind.USER.name());

            // let's provide result panel to be populated ....
            
        } else {
            super.onEvent(event);
        }
    }

}
