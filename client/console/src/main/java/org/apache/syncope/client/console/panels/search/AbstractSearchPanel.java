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
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
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

    protected AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    protected SchemaRestClient schemaRestClient = new SchemaRestClient();

    protected ResourceRestClient resourceRestClient = new ResourceRestClient();

    protected IModel<List<String>> dnames;

    protected IModel<Map<String, PlainSchemaTO>> anames;

    protected IModel<List<String>> resourceNames;

    protected IModel<List<SearchClause.Type>> types;

    protected IModel<Map<String, String>> groupNames;

    protected IModel<List<String>> roleNames;

    protected IModel<List<SearchClause>> model;

    protected WebMarkupContainer searchFormContainer;

    protected final AnyTypeKind typeKind;

    protected final String type;

    protected final boolean required;

    protected final boolean enableSearch;

    public abstract static class Builder<T extends AbstractSearchPanel> implements Serializable {

        private static final long serialVersionUID = 6308997285778809578L;

        protected final IModel<List<SearchClause>> model;

        protected boolean required = true;

        protected boolean enableSearch = false;

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

        public Builder<T> required(final boolean required) {
            this.required = required;
            return this;
        }

        public abstract T build(final String id);
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

        final SearchClausePanel searchClausePanel = new SearchClausePanel("panel", "panel",
                Model.of(new SearchClause()),
                required,
                types, anames, dnames, groupNames, roleNames, resourceNames);

        if (enableSearch) {
            searchClausePanel.enableSearch(builder.resultContainer);
        }

        final MultiFieldPanel.Builder<SearchClause> searchView = new MultiFieldPanel.Builder<SearchClause>(model) {

            private static final long serialVersionUID = 1343431509987473047L;

            @Override
            protected SearchClause newModelObject() {
                return new SearchClause();
            }
        };

        searchFormContainer.add(searchView.build("search", "search", searchClausePanel).hideLabel());
    }

    protected void populate() {
        dnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(typeKind);
            }
        };

        anames = new LoadableDetachableModel<Map<String, PlainSchemaTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                final List<PlainSchemaTO> schemas = schemaRestClient.<PlainSchemaTO>getSchemas(
                        SchemaType.PLAIN,
                        anyTypeRestClient.read(type).getClasses().toArray(new String[] {}));

                final Map<String, PlainSchemaTO> res = new HashMap<>();
                for (PlainSchemaTO schema : schemas) {
                    res.put(schema.getKey(), schema);
                }
                return res;
            }
        };

        resourceNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return resourceRestClient.list().stream().map(EntityTO::getKey).collect(Collectors.toList());
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
