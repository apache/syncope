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
package org.apache.syncope.core.persistence.jpa.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ScriptLanguage;
import co.elastic.clients.elasticsearch._types.ScriptSortType;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

public class ElasticsearchRealmDAO extends JPARealmDAO {

    protected static final List<SortOptions> ES_SORT_OPTIONS_REALM = List.of(
            new SortOptions.Builder().
                    script(s -> s.type(ScriptSortType.Number).
                    script(t -> t.lang(ScriptLanguage.Painless).
                    source("doc['fullPath'].value.chars().filter(ch -> ch == '/').count()")).
                    order(SortOrder.Asc)).
                    build());

    protected final ElasticsearchClient client;

    protected final int indexMaxResultWindow;

    public ElasticsearchRealmDAO(
            final RoleDAO roleDAO,
            final ApplicationEventPublisher publisher,
            final ElasticsearchClient client,
            final int indexMaxResultWindow) {

        super(roleDAO, publisher);
        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    @Transactional(readOnly = true)
    @Override
    public Realm findByFullPath(final String fullPath) {
        if (SyncopeConstants.ROOT_REALM.equals(fullPath)) {
            return getRoot();
        }

        if (StringUtils.isBlank(fullPath) || !PATH_PATTERN.matcher(fullPath).matches()) {
            throw new MalformedPathException(fullPath);
        }

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(fullPath).build()).build()).
                size(1).
                build();

        try {
            String result = client.search(request, Void.class).hits().hits().stream().findFirst().
                    map(Hit::id).
                    orElse(null);
            return find(result);
        } catch (Exception e) {
            LOG.error("While searching ES for one match", e);
        }

        return null;
    }

    protected List<String> search(final Query query) {
        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(query).
                sort(ES_SORT_OPTIONS_REALM).
                build();

        try {
            return client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
            return List.of();
        }
    }

    @Override
    public List<Realm> findByName(final String name) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("name").value(name).build()).build());
        return result.stream().map(this::find).collect(Collectors.toList());
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("parent_id").value(realm.getKey()).build()).build());
        return result.stream().map(this::find).collect(Collectors.toList());
    }

    protected static Query buildDescendantsQuery(final Set<String> bases, final String keyword) {
        List<Query> basesQueries = new ArrayList<>();
        bases.forEach(base -> {
            basesQueries.add(new Query.Builder().term(QueryBuilders.term().
                    field("fullPath").value(base).build()).build());
            basesQueries.add(new Query.Builder().regexp(QueryBuilders.regexp().
                    field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*").
                    build()).build());
        });
        Query prefix = new Query.Builder().disMax(QueryBuilders.disMax().queries(basesQueries).build()).build();

        if (keyword == null) {
            return prefix;
        }

        StringBuilder output = new StringBuilder();
        for (char c : keyword.toLowerCase().toCharArray()) {
            if (c == '%') {
                output.append(".*");
            } else if (Character.isLetter(c)) {
                output.append('[').
                        append(c).
                        append(Character.toUpperCase(c)).
                        append(']');
            } else {
                output.append(ElasticsearchUtils.escapeForLikeRegex(c));
            }
        }

        return new Query.Builder().bool(QueryBuilders.bool().filter(
                prefix,
                new Query.Builder().regexp(QueryBuilders.regexp().
                        field("name").value(output.toString()).build()).
                        build()).build()).
                build();
    }

    @Override
    public int countDescendants(final String base, final String keyword) {
        return countDescendants(Set.of(base), keyword);
    }

    @Override
    public int countDescendants(final Set<String> bases, final String keyword) {
        CountRequest request = new CountRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                query(buildDescendantsQuery(bases, keyword)).
                build();

        try {
            return (int) client.count(request).count();
        } catch (Exception e) {
            LOG.error("While counting in Elasticsearch", e);
            return 0;
        }
    }

    @Override
    public List<Realm> findDescendants(
            final String base,
            final String keyword,
            final int page,
            final int itemsPerPage) {

        return findDescendants(Set.of(base), keyword, page, itemsPerPage);
    }

    @Override
    public List<Realm> findDescendants(
            final Set<String> bases,
            final String keyword,
            final int page,
            final int itemsPerPage) {

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(buildDescendantsQuery(bases, keyword)).
                from(itemsPerPage * (page <= 0 ? 0 : page - 1)).
                size(itemsPerPage < 0 ? indexMaxResultWindow : itemsPerPage).
                sort(ES_SORT_OPTIONS_REALM).
                build();

        List<String> result = List.of();
        try {
            result = client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }

        return result.stream().map(this::find).collect(Collectors.toList());
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        Query prefixQuery = new Query.Builder().disMax(QueryBuilders.disMax().queries(
                new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(prefix).build()).build(),
                new Query.Builder().prefix(QueryBuilders.prefix().
                        field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(prefix) ? "/" : prefix + "/").
                        build()).build()).build()).build();

        Query query = new Query.Builder().bool(QueryBuilders.bool().filter(
                buildDescendantsQuery(Set.of(base), null), prefixQuery).build()).
                build();

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(query).
                from(0).
                size(indexMaxResultWindow).
                sort(ES_SORT_OPTIONS_REALM).
                build();

        List<String> result = List.of();
        try {
            result = client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }
        return result;
    }
}
