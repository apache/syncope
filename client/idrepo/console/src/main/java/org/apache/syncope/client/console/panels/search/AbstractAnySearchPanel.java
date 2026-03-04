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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

abstract class AbstractAnySearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 5922413053568696414L;

    @SpringBean
    protected GroupRestClient groupRestClient;

    protected final AnyTypeKind typeKind;

    protected final String anyType;

    protected AbstractAnySearchPanel(final String id, final AnyTypeKind kind, final Builder<?> builder) {
        this(id, kind, kind.name(), builder);
    }

    protected AbstractAnySearchPanel(
            final String id, final AnyTypeKind kind, final String type, final Builder<?> builder) {

        super(id);
        this.typeKind = kind;
        this.anyType = type;
        init(builder);
    }

    @Override
    protected void populate() {
        super.populate();

        dnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                Map<String, PlainSchemaTO> dSchemaNames = new HashMap<>();
                SearchableFields.get(typeKind.getTOClass()).forEach((key, type) -> {
                    PlainSchemaTO plainSchema = new PlainSchemaTO();
                    plainSchema.setType(type);
                    plainSchema.setKey(key);
                    dSchemaNames.put(key, plainSchema);
                });
                return dSchemaNames;
            }
        };

        resourceNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getResourceProvider().get(anyType);
            }
        };
    }

    @Override
    protected Pair<IModel<List<String>>, IModel<Long>> getGroupInfo() {
        return typeKind != AnyTypeKind.GROUP && SyncopeConsoleSession.get().owns(IdRepoEntitlement.GROUP_SEARCH)
                ? Pair.of(groupNames, new LoadableDetachableModel<>() {

                    private static final long serialVersionUID = 7362833782319137329L;

                    @Override
                    protected Long load() {
                        return groupRestClient.count(SyncopeConstants.ROOT_REALM, null);
                    }
                })
                : Pair.of(groupNames, Model.of(0L));
    }

    @Override
    protected String sanitizeFIQL(final String fiql) {
        return fiql.replaceAll(SearchUtils.getTypeConditionPattern(anyType).pattern(), "");
    }

    public String getAnyType() {
        return anyType;
    }
}
