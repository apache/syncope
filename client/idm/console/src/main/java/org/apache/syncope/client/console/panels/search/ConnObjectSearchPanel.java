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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.ConnIdObjectClass;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ConnObjectSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 21020550706646L;

    protected final ResourceTO resource;

    public static class Builder extends AbstractSearchPanel.Builder<ConnObjectSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        private final ResourceTO resource;

        private final AnyTypeKind anyType;

        private final String typeName;

        public Builder(
                final ResourceTO resource,
                final AnyTypeKind anyType,
                final String type,
                final IModel<List<SearchClause>> model,
                final PageReference pageRef) {

            super(model, pageRef);
            this.resource = resource;
            this.anyType = anyType;
            this.typeName = type;
        }

        @Override
        public ConnObjectSearchPanel build(final String id) {
            return new ConnObjectSearchPanel(id, anyType, typeName, this);
        }
    }

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    protected ConnObjectSearchPanel(final String id, final AnyTypeKind kind, final Builder builder) {
        super(id, kind, builder);
        this.resource = builder.resource;
    }

    protected ConnObjectSearchPanel(final String id, final AnyTypeKind kind, final String type, final Builder builder) {
        super(id, kind, type, builder);
        this.resource = builder.resource;
    }

    @Override
    protected AbstractFiqlSearchConditionBuilder<?, ?, ?> getSearchConditionBuilder() {
        return SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder();
    }

    @Override
    protected String getFIQLQueryTarget() {
        return "CONN_OBJ";
    }

    @Override
    protected void populate() {
        this.types = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 22668815812716L;

            @Override
            protected List<SearchClause.Type> load() {
                return List.of(SearchClause.Type.ATTRIBUTE);
            }
        };

        this.dnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 2989042618372L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                return Map.of();
            }
        };

        this.anames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 3002350300761L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                return connectorRestClient.buildObjectClassInfo(
                        connectorRestClient.read(resource.getConnector()), false).stream().
                        map(ConnIdObjectClass::getAttributes).
                        flatMap(List::stream).
                        collect(Collectors.toMap(
                                PlainSchemaTO::getKey, Function.identity(),
                                (schema1, schema2) -> schema1));
            }
        };
    }
}
