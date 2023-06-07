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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class GroupSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 5757183539269316263L;

    public static class Builder extends AbstractSearchPanel.Builder<GroupSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        public Builder(final IModel<List<SearchClause>> model, final PageReference pageRef) {
            super(model, pageRef);
        }

        @Override
        public GroupSearchPanel build(final String id) {
            return new GroupSearchPanel(id, this);
        }
    }

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    protected GroupSearchPanel(final String id, final GroupSearchPanel.Builder builder) {
        super(id, AnyTypeKind.GROUP, builder);
    }

    @Override
    protected AbstractFiqlSearchConditionBuilder<?, ?, ?> getSearchConditionBuilder() {
        return SyncopeClient.getGroupSearchConditionBuilder();
    }

    @Override
    protected String getFIQLQueryTarget() {
        return AnyTypeKind.GROUP.name();
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchClause.Type> load() {
                List<SearchClause.Type> result = new ArrayList<>();
                result.add(SearchClause.Type.ATTRIBUTE);
                result.add(SearchClause.Type.AUX_CLASS);
                result.add(SearchClause.Type.RESOURCE);
                result.add(SearchClause.Type.GROUP_MEMBER);
                return result;
            }
        };

        this.groupNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return List.of();
            }
        };

        this.anames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                return schemaRestClient.<PlainSchemaTO>getSchemas(
                        SchemaType.PLAIN, null, anyTypeRestClient.read(type).getClasses().toArray(String[]::new)).
                        stream().collect(Collectors.toMap(SchemaTO::getKey, Function.identity()));
            }
        };
    }
}
