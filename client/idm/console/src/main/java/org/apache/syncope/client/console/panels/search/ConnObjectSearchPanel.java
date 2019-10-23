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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class ConnObjectSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 21020550706646L;

    protected final String resource;

    private final String typeName;

    public static class Builder extends AbstractSearchPanel.Builder<ConnObjectSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        protected final String resource;

        private final AnyTypeKind anyType;

        private final String typeName;

        public Builder(final String resource, final AnyTypeKind anyType, final String type,
                final IModel<List<SearchClause>> model) {
            super(model);
            this.resource = resource;
            this.anyType = anyType;
            this.typeName = type;
        }

        @Override
        public ConnObjectSearchPanel build(final String id) {
            return new ConnObjectSearchPanel(id, anyType, typeName, this);
        }
    }

    protected ConnObjectSearchPanel(final String id, final AnyTypeKind kind, final Builder builder) {
        super(id, kind, builder);
        this.resource = builder.resource;
        this.typeName = builder.typeName;
    }

    protected ConnObjectSearchPanel(final String id, final AnyTypeKind kind, final String type, final Builder builder) {
        super(id, kind, type, builder);
        this.resource = builder.resource;
        this.typeName = builder.typeName;
    }

    @Override
    protected void populate() {
        this.types = new LoadableDetachableModel<List<SearchClause.Type>>() {

            private static final long serialVersionUID = 22668815812716L;

            @Override
            protected List<SearchClause.Type> load() {
                return getAvailableTypes();
            }
        };

        this.dnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 2989042618372L;

            @Override
            protected List<String> load() {
                return getItems().map(ItemTO::getExtAttrName).collect(Collectors.toList());
            }
        };

        this.anames = new LoadableDetachableModel<Map<String, PlainSchemaTO>>() {

            private static final long serialVersionUID = 3002350300761L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                final PlainSchemaTO plainSchemaTO = new PlainSchemaTO();
                plainSchemaTO.setType(AttrSchemaType.String);
                List<String> schemaNames = SchemaRestClient.getSchemaNames(SchemaType.PLAIN);
                return getItems().collect(Collectors.toMap(ItemTO::getExtAttrName, item
                        -> schemaNames.contains(item.getIntAttrName())
                        ? SchemaRestClient.<PlainSchemaTO>read(SchemaType.PLAIN, item.getIntAttrName())
                        : plainSchemaTO, (item1, item2) -> item1));
            }
        };
    }

    protected List<SearchClause.Type> getAvailableTypes() {
        List<SearchClause.Type> result = new ArrayList<>();
        result.add(SearchClause.Type.ATTRIBUTE);
        return result;
    }

    private Stream<ItemTO> getItems() {
        ResourceTO resourceTO = ResourceRestClient.read(resource);
        if (typeName.equals("REALM")) {
            return resourceTO.getOrgUnit().getItems().stream().
                    filter(ItemTO -> !StringUtils.equalsAny(ItemTO.getExtAttrName(), ConnIdSpecialName.NAME,
                    ConnIdSpecialName.UID));

        } else {
            Optional<ProvisionTO> provision = resourceTO.getProvision(typeName);
            if (provision.isPresent()) {
                return provision.get().getMapping().getItems().stream().
                        filter(ItemTO -> !StringUtils.equalsAny(ItemTO.getExtAttrName(), ConnIdSpecialName.NAME,
                        ConnIdSpecialName.UID));
            } else {
                return Collections.<ItemTO>emptyList().stream();
            }
        }
    }
}
