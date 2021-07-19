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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchPanel extends Panel {

    private static final long serialVersionUID = 5922413053568696414L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchPanel.class);

    protected IModel<Map<String, PlainSchemaTO>> dnames;

    protected IModel<Map<String, PlainSchemaTO>> anames;

    protected IModel<List<String>> resourceNames;

    protected IModel<List<SearchClause.Type>> types;

    protected IModel<List<String>> groupNames;

    protected IModel<List<String>> roleNames;

    protected IModel<List<String>> privilegeNames;

    protected IModel<List<SearchClause>> model;

    protected WebMarkupContainer searchFormContainer;

    protected final AnyTypeKind typeKind;

    protected final String type;

    protected final boolean required;

    protected final boolean enableSearch;

    protected final GroupRestClient groupRestClient = new GroupRestClient();

    public abstract static class Builder<T extends AbstractSearchPanel> implements Serializable {

        private static final long serialVersionUID = 6308997285778809578L;

        protected final IModel<List<SearchClause>> model;

        protected boolean required = true;

        protected boolean enableSearch = false;

        protected SearchClausePanel.Customizer customizer = new SearchClausePanel.Customizer() {

            private static final long serialVersionUID = 4449199405807453441L;

        };

        protected IEventSink resultContainer;

        public Builder(final IModel<List<SearchClause>> model) {
            this.model = model;
        }

        public Builder<T> enableSearch(final IEventSink resultContainer) {
            this.resultContainer = resultContainer;
            return enableSearch();
        }

        public Builder<T> enableSearch() {
            this.enableSearch = true;
            return this;
        }

        public Builder<T> customizer(final SearchClausePanel.Customizer customizer) {
            this.customizer = customizer;
            return this;
        }

        public Builder<T> required(final boolean required) {
            this.required = required;
            return this;
        }

        public abstract T build(String id);
    }

    protected AbstractSearchPanel(final String id, final AnyTypeKind kind, final Builder<?> builder) {
        this(id, kind, kind.name(), builder);
    }

    protected AbstractSearchPanel(
            final String id, final AnyTypeKind kind, final String type, final Builder<?> builder) {

        super(id);
        populate();

        this.model = builder.model;
        this.typeKind = kind;
        this.type = type;
        this.required = builder.required;
        this.enableSearch = builder.enableSearch;

        setOutputMarkupId(true);

        searchFormContainer = new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);
        add(searchFormContainer);

        Pair<IModel<List<String>>, IModel<Integer>> groupInfo =
                typeKind != AnyTypeKind.GROUP && SyncopeConsoleSession.get().owns(IdRepoEntitlement.GROUP_SEARCH)
                ? Pair.of(groupNames, new LoadableDetachableModel<>() {

                    private static final long serialVersionUID = 7362833782319137329L;

                    @Override
                    protected Integer load() {
                        return groupRestClient.count(SyncopeConstants.ROOT_REALM, null, null);
                    }
                })
                : Pair.of(groupNames, Model.of(0));
        SearchClausePanel searchClausePanel = new SearchClausePanel("panel", "panel",
                Model.of(new SearchClause()),
                required,
                types,
                builder.customizer,
                anames, dnames, groupInfo, roleNames, privilegeNames, resourceNames);

        if (enableSearch) {
            searchClausePanel.enableSearch(builder.resultContainer);
        }

        MultiFieldPanel.Builder<SearchClause> searchView = new MultiFieldPanel.Builder<>(model) {

            private static final long serialVersionUID = 1343431509987473047L;

            @Override
            protected SearchClause newModelObject() {
                return new SearchClause();
            }
        };

        searchFormContainer.add(searchView.build("search", "search", searchClausePanel).hideLabel());
    }

    protected void populate() {
        dnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                Map<String, PlainSchemaTO> dSchemaNames = new HashMap<>();
                SearchableFields.get(typeKind.getTOClass()).forEach((key, type) -> {
                    PlainSchemaTO plain = new PlainSchemaTO();
                    plain.setType(type);
                    dSchemaNames.put(key, plain);
                });
                return dSchemaNames;
            }
        };

        resourceNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getResourceProvider().get();
            }
        };
    }

    public IModel<List<SearchClause>> getModel() {
        return this.model;
    }

    public String getBackObjectType() {
        return this.type;
    }

    public Map<String, PlainSchemaTO> getAvailableSchemaTypes() {
        return anames.getObject();
    }
}
