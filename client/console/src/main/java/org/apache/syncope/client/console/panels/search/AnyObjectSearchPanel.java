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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class AnyObjectSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    public static class Builder extends AbstractSearchPanel.Builder<AnyObjectSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        private final String type;

        public Builder(final String type, final IModel<List<SearchClause>> model) {
            super(model);
            this.type = type;
        }

        @Override
        public AnyObjectSearchPanel build(final String id) {
            return new AnyObjectSearchPanel(id, AnyTypeKind.ANY_OBJECT, type, this);
        }
    }

    protected AnyObjectSearchPanel(final String id, final AnyTypeKind kind, final Builder builder) {
        super(id, kind, builder);
    }

    protected AnyObjectSearchPanel(final String id, final AnyTypeKind kind, final String type, final Builder builder) {
        super(id, kind, type, builder);
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<List<SearchClause.Type>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchClause.Type> load() {
                return getAvailableTypes();
            }
        };

        this.groupNames = new LoadableDetachableModel<Map<String, String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, String> load() {
                List<GroupTO> groupTOs = groupRestClient.search("/", null, -1, -1, new SortParam<>("name", true), null);

                final Map<String, String> result = new HashMap<>(groupTOs.size());
                for (GroupTO group : groupTOs) {
                    result.put(group.getKey(), group.getName());
                }

                return result;
            }
        };
    }

    protected List<SearchClause.Type> getAvailableTypes() {
        List<SearchClause.Type> result = new ArrayList<>();
        result.add(SearchClause.Type.ATTRIBUTE);
        result.add(SearchClause.Type.GROUP_MEMBERSHIP);
        result.add(SearchClause.Type.RESOURCE);
        result.add(SearchClause.Type.RELATIONSHIP);
        return result;
    }
}
