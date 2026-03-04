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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmSearchPanel extends AbstractSearchPanel {

    private static final long serialVersionUID = 4313349068728035055L;

    private static final String REALM = "REALM";

    private static final String BASE = "BASE";

    private static final String FULLPATH = "fullPath";

    private static final List<String> ATTRIBUTES_INCLUDED = List.of("name", "key");

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    public static class Builder extends AbstractSearchPanel.Builder<RealmSearchPanel> {

        private static final long serialVersionUID = 6308997285778809578L;

        public Builder(final IModel<List<SearchClause>> model, final PageReference pageRef) {
            super(model, pageRef);
            customizer(new SearchClausePanel.Customizer() {

                private static final long serialVersionUID = -8826680744097357141L;

                @Override
                public IChoiceRenderer<SearchClause.Type> typeRenderer() {
                    return new IChoiceRenderer<>() {

                        private static final long serialVersionUID = 656585115269883745L;

                        @Override
                        public Object getDisplayValue(final SearchClause.Type object) {
                            return object == SearchClause.Type.CUSTOM ? BASE : object;
                        }

                        @Override
                        public String getIdValue(final SearchClause.Type object, final int index) {
                            return object.name();
                        }

                        @Override
                        public SearchClause.Type getObject(
                                final String id,
                                final IModel<? extends List<? extends SearchClause.Type>> choices) {
                            return SearchClause.Type.valueOf(id);
                        }
                    };
                }

                @Override
                public List<SearchClause.Type> types(
                        final List<SearchClause.Type> available,
                        final int index,
                        final SearchClause currentClause) {
                    return currentClause != null && currentClause.getType() == SearchClause.Type.CUSTOM
                            ? available
                            : available.stream().filter(type -> type != SearchClause.Type.CUSTOM).toList();
                }

                @Override
                public boolean typeEnabled(final int index, final SearchClause currentClause) {
                    return !(currentClause != null && currentClause.getType() == SearchClause.Type.CUSTOM);
                }

                @Override
                public List<SearchClause.Comparator> comparators() {
                    return List.of(SearchClause.Comparator.EQUALS);
                }

                @Override
                public List<String> properties() {
                    return List.of(FULLPATH);
                }

                @Override
                public void adjust(
                        final SearchClause.Type type,
                        final AjaxTextFieldPanel property,
                        final FieldPanel<SearchClause.Comparator> comparator,
                        final FieldPanel<?> value,
                        final LoadableDetachableModel<List<Pair<String, String>>> properties) {

                    property.setEnabled(false);
                    property.setReadOnly(true);
                    property.setModelObject(FULLPATH);
                    comparator.setModelObject(SearchClause.Comparator.EQUALS);
                    comparator.setEnabled(false);
                    value.setEnabled(true);
                }

                @Override
                public boolean showOperator(final int index, final SearchClause currentClause) {
                    return isNotBaseClause(currentClause, index);
                }
            });
        }

        @Override
        public RealmSearchPanel build(final String id) {
            return new RealmSearchPanel(id, this);
        }
    }

    protected RealmSearchPanel(final String id, final Builder builder) {
        super(id);
        if (builder.model.getObject().isEmpty()) {
            SearchClause baseClause = createBaseClause(SyncopeConstants.ROOT_REALM);
            builder.model.getObject().add(baseClause);
        }
        init(builder);
    }

    @Override
    protected AbstractFiqlSearchConditionBuilder<?, ?, ?> getSearchConditionBuilder() {
        return SyncopeClient.getRealmFiqlSearchConditionBuilder();
    }

    @Override
    protected String getFIQLQueryTarget() {
        return REALM;
    }

    @Override
    protected boolean isRemovable(final SearchClause clause, final int index) {
        return isNotBaseClause(clause, index);
    }

    @Override
    protected void populate() {
        super.populate();

        this.types = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = -5329135110865860544L;

            @Override
            protected List<SearchClause.Type> load() {
                return List.of(
                        SearchClause.Type.ATTRIBUTE,
                        SearchClause.Type.RESOURCE,
                        SearchClause.Type.AUX_CLASS,
                        SearchClause.Type.CUSTOM);
            }
        };

        this.anames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                return schemaRestClient.<PlainSchemaTO>getSchemas(SchemaType.PLAIN, null, new String[0]).
                        stream().collect(Collectors.toMap(SchemaTO::getKey, Function.identity()));
            }
        };

        this.auxClassNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 4082537432455696478L;

            @Override
            protected List<String> load() {
                return anyTypeClassRestClient.list().stream().map(AnyTypeClassTO::getKey).collect(Collectors.toList());
            }
        };

        this.resourceNames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 2392986851268987236L;

            @Override
            protected List<String> load() {
                return SyncopeWebApplication.get().getResourceProvider().getForRealms();
            }
        };

        this.dnames = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 1727353134876260253L;

            @Override
            protected Map<String, PlainSchemaTO> load() {
                Map<String, PlainSchemaTO> dSchemaNames = new HashMap<>();

                for (String field : ATTRIBUTES_INCLUDED) {
                    PlainSchemaTO plainSchema = new PlainSchemaTO();
                    plainSchema.setType(AttrSchemaType.String);
                    plainSchema.setKey(field);
                    dSchemaNames.put(field, plainSchema);
                }

                return dSchemaNames;
            }
        };
    }

    @Override
    protected ActionLink<Serializable> saveAction(final SaveFIQLQuery saveFIQLQuery) {
        return new ActionLink<>() {

            private static final long serialVersionUID = -7354640433272022003L;

            @Override
            public void onClick(final org.apache.wicket.ajax.AjaxRequestTarget target, final Serializable ignore) {
                Function<SearchClause, CompleteCondition> fullPathHandler = clause -> {
                    if (clause.getType() == SearchClause.Type.CUSTOM && FULLPATH.equals(clause.getProperty())) {
                        return getSearchConditionBuilder().is(FULLPATH).equalTo(clause.getValue());
                    }
                    return null;
                };

                Optional.ofNullable(SearchUtils.buildFIQL(
                        getModel().getObject(),
                        getSearchConditionBuilder(),
                        getAvailableSchemaTypes(),
                        fullPathHandler))
                        .ifPresentOrElse(
                                fiql -> saveFIQLQuery.setFiql(sanitizeFIQL(fiql)),
                                () -> saveFIQLQuery.setFiql(null));

                saveFIQLQuery.toggle(target, true);
            }
        };
    }

    @Override
    protected void updateFIQL(final org.apache.wicket.ajax.AjaxRequestTarget target, final String fiql) {
        List<SearchClause> clauses = SearchUtils.getSearchClauses(sanitizeFIQL(fiql));

        String baseValue = clauses.stream()
                .filter(cl -> FULLPATH.equals(cl.getProperty()))
                .map(SearchClause::getValue)
                .findFirst()
                .orElse(SyncopeConstants.ROOT_REALM);

        SearchClause baseClause = createBaseClause(baseValue);

        clauses.removeIf(cl -> FULLPATH.equals(cl.getProperty()));
        clauses.addFirst(baseClause);

        model.setObject(clauses);
        target.add(searchFormContainer);
    }

    protected SearchClause createBaseClause(final String value) {
        SearchClause baseClause = new SearchClause();
        baseClause.setType(SearchClause.Type.CUSTOM);
        baseClause.setProperty(FULLPATH);
        baseClause.setComparator(SearchClause.Comparator.EQUALS);
        baseClause.setValue(value);
        return baseClause;
    }

    protected static boolean isNotBaseClause(final SearchClause clause, final int index) {
        return clause == null
                || clause.getType() != SearchClause.Type.CUSTOM
                || !FULLPATH.equals(clause.getProperty())
                || index > 1;
    }
}
