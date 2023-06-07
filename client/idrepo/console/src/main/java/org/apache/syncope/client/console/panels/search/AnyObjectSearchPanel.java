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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AnyObjectSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = -1769527800450203738L;

    public static class Builder extends AbstractSearchPanel.Builder<AnyObjectSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        private final String type;

        public Builder(final String type, final IModel<List<SearchClause>> model, final PageReference pageRef) {
            super(model, pageRef);
            this.type = type;
        }

        @Override
        public AnyObjectSearchPanel build(final String id) {
            return new AnyObjectSearchPanel(id, AnyTypeKind.ANY_OBJECT, type, this);
        }
    }

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    protected AnyObjectSearchPanel(final String id, final AnyTypeKind kind, final Builder builder) {
        super(id, kind, builder);
    }

    protected AnyObjectSearchPanel(final String id, final AnyTypeKind kind, final String type, final Builder builder) {
        super(id, kind, type, builder);
    }

    @Override
    protected AbstractFiqlSearchConditionBuilder<?, ?, ?> getSearchConditionBuilder() {
        return SyncopeClient.getAnyObjectSearchConditionBuilder(type);
    }

    @Override
    protected String getFIQLQueryTarget() {
        return type;
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<SearchClause.Type> load() {
                return getAvailableTypes();
            }
        };

        this.groupNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return groupRestClient.search(
                        SyncopeConstants.ROOT_REALM,
                        null,
                        1,
                        Constants.MAX_GROUP_LIST_SIZE,
                        new SortParam<>(Constants.NAME_FIELD_NAME, true),
                        null).stream().map(GroupTO::getName).collect(Collectors.toList());
            }
        };

        this.anames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                return schemaRestClient.<PlainSchemaTO>getSchemas(SchemaType.PLAIN, null, anyTypeRestClient.read(type).
                        getClasses().toArray(String[]::new)).
                        stream().collect(Collectors.toMap(SchemaTO::getKey, Function.identity()));
            }
        };
    }

    protected List<SearchClause.Type> getAvailableTypes() {
        List<SearchClause.Type> result = new ArrayList<>();
        result.add(SearchClause.Type.ATTRIBUTE);
        result.add(SearchClause.Type.GROUP_MEMBERSHIP);
        result.add(SearchClause.Type.AUX_CLASS);
        result.add(SearchClause.Type.RESOURCE);
        result.add(SearchClause.Type.RELATIONSHIP);
        return result;
    }
}
