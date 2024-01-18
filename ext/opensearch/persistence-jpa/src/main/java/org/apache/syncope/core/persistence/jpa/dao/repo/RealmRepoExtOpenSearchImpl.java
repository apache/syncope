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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.ScriptSortType;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class RealmRepoExtOpenSearchImpl extends RealmRepoExtImpl {

    protected static final List<SortOptions> ES_SORT_OPTIONS_REALM = List.of(
            new SortOptions.Builder().
                    script(s -> s.type(ScriptSortType.Number).
                    script(t -> t.inline(i -> i.lang("painless").
                    source("doc['fullPath'].value.chars().filter(ch -> ch == '/').count()"))).
                    order(SortOrder.Asc)).
                    build());

    protected final OpenSearchClient client;

    protected final int indexMaxResultWindow;

    public RealmRepoExtOpenSearchImpl(
            final RoleDAO roleDAO,
            final ApplicationEventPublisher publisher,
            final EntityManager entityManager,
            final OpenSearchClient client,
            final int indexMaxResultWindow) {

        super(roleDAO, publisher, entityManager);
        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    protected Optional<Realm> findById(final String key) {
        return Optional.ofNullable(entityManager.find(JPARealm.class, key));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (SyncopeConstants.ROOT_REALM.equals(fullPath)) {
            return Optional.of(getRoot());
        }

        if (StringUtils.isBlank(fullPath) || !RealmDAO.PATH_PATTERN.matcher(fullPath).matches()) {
            throw new MalformedPathException(fullPath);
        }

        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(FieldValue.of(fullPath)).build()).build()).
                size(1).
                build();

        try {
            String result = client.search(request, Void.class).hits().hits().stream().findFirst().
                    map(Hit::id).
                    orElse(null);
            return findById(result);
        } catch (Exception e) {
            LOG.error("While searching ES for one match", e);
        }

        return Optional.empty();
    }

    protected List<String> search(final Query query) {
        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(query).
                sort(ES_SORT_OPTIONS_REALM).
                build();

        try {
            return client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in OpenSearch", e);
            return List.of();
        }
    }

    @Override
    public List<Realm> findByName(final String name) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("name").value(FieldValue.of(name)).build()).build());
        return result.stream().map(this::findById).
                filter(Optional::isPresent).map(Optional::get).toList();
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("parent_id").value(FieldValue.of(realm.getKey())).build()).build());
        return result.stream().map(this::findById).
                filter(Optional::isPresent).map(Optional::get).toList();
    }

    protected Query buildDescendantQuery(final String base, final String keyword) {
        Query prefix = new Query.Builder().disMax(QueryBuilders.disMax().queries(
                new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(FieldValue.of(base)).build()).build(),
                new Query.Builder().regexp(QueryBuilders.regexp().
                        field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*").
                        build()).build()).build()).build();

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
                output.append(OpenSearchUtils.escapeForLikeRegex(c));
            }
        }

        return new Query.Builder().bool(QueryBuilders.bool().must(
                prefix,
                new Query.Builder().regexp(QueryBuilders.regexp().
                        field("name").value(output.toString()).build()).
                        build()).build()).
                build();
    }

    @Override
    public long countDescendants(final String base, final String keyword) {
        CountRequest request = new CountRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                query(buildDescendantQuery(base, keyword)).
                build();

        try {
            return client.count(request).count();
        } catch (Exception e) {
            LOG.error("While counting in OpenSearch", e);
            return 0;
        }
    }

    @Override
    public List<Realm> findDescendants(final String base, final String keyword, final Pageable pageable) {
        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(buildDescendantQuery(base, keyword)).
                from(pageable.isUnpaged() ? 0 : pageable.getPageSize() * (pageable.getPageNumber() - 1)).
                size(pageable.isUnpaged() ? indexMaxResultWindow : pageable.getPageSize()).
                sort(ES_SORT_OPTIONS_REALM).
                build();

        List<String> result = List.of();
        try {
            result = client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in OpenSearch", e);
        }

        return result.stream().map(this::findById).
                filter(Optional::isPresent).map(Optional::get).toList();
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        Query prefixQuery = new Query.Builder().disMax(QueryBuilders.disMax().queries(
                new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(FieldValue.of(prefix)).build()).build(),
                new Query.Builder().prefix(QueryBuilders.prefix().
                        field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(prefix) ? "/" : prefix + "/").
                        build()).build()).build()).build();

        Query query = new Query.Builder().bool(QueryBuilders.bool().must(
                buildDescendantQuery(base, (String) null),
                prefixQuery).build()).
                build();

        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
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
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in OpenSearch", e);
        }
        return result;
    }
}
