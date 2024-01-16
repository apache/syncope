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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

public class AuditConfRepoExtElasticsearchImpl implements AuditConfRepoExt {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditConfRepoExt.class);

    protected final ElasticsearchClient client;

    protected final int indexMaxResultWindow;

    public AuditConfRepoExtElasticsearchImpl(
            final ElasticsearchClient client,
            final int indexMaxResultWindow) {

        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    protected Query getQuery(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        List<Query> queries = new ArrayList<>();

        if (entityKey != null) {
            queries.add(new Query.Builder().
                    multiMatch(QueryBuilders.multiMatch().
                            fields("message.before", "message.inputs", "message.output", "message.throwable").
                            type(TextQueryType.Phrase).
                            query(entityKey).build()).build());
        }

        if (type != null) {
            queries.add(new Query.Builder().
                    term(QueryBuilders.term().field("message.logger.type").value(type.name()).build()).
                    build());
        }

        if (StringUtils.isNotBlank(category)) {
            queries.add(new Query.Builder().
                    term(QueryBuilders.term().field("message.logger.category").value(category).build()).
                    build());
        }

        if (StringUtils.isNotBlank(subcategory)) {
            queries.add(new Query.Builder().
                    term(QueryBuilders.term().field("message.logger.subcategory").value(subcategory).build()).
                    build());
        }

        List<Query> eventQueries = events.stream().map(event -> new Query.Builder().
                term(QueryBuilders.term().field("message.logger.event").value(event).build()).
                build()).
                collect(Collectors.toList());
        if (!eventQueries.isEmpty()) {
            queries.add(new Query.Builder().disMax(QueryBuilders.disMax().queries(eventQueries).build()).build());
        }

        if (result != null) {
            queries.add(new Query.Builder().
                    term(QueryBuilders.term().field("message.logger.result").value(result.name()).build()).
                    build());
        }

        if (before != null) {
            queries.add(new Query.Builder().
                    range(QueryBuilders.range().
                            field("instant").lte(JsonData.of(before.toInstant().toEpochMilli())).build()).
                    build());
        }

        if (after != null) {
            queries.add(new Query.Builder().
                    range(QueryBuilders.range().
                            field("instant").gte(JsonData.of(after.toInstant().toEpochMilli())).build()).
                    build());
        }

        return new Query.Builder().bool(QueryBuilders.bool().must(queries).build()).build();
    }

    @Override
    public long countEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        CountRequest request = new CountRequest.Builder().
                index(ElasticsearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                query(getQuery(entityKey, type, category, subcategory, events, result, before, after)).
                build();
        try {
            return client.count(request).count();
        } catch (IOException e) {
            LOG.error("Search error", e);
            return 0L;
        }
    }

    protected List<SortOptions> sortBuilders(final Stream<Sort.Order> orderBy) {
        return orderBy.map(clause -> {
            String sortField = clause.getProperty();
            if ("EVENT_DATE".equalsIgnoreCase(sortField)) {
                sortField = "message.date";
            }

            return new SortOptions.Builder().field(
                    new FieldSort.Builder().
                            field(sortField).
                            order(clause.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc).
                            build()).
                    build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> searchEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(getQuery(entityKey, type, category, subcategory, events, result, before, after)).
                fields(f -> f.field("message")).
                from(pageable.isUnpaged() ? 0 : pageable.getPageSize() * (pageable.getPageNumber() - 1)).
                size(pageable.isUnpaged() ? indexMaxResultWindow : pageable.getPageSize()).
                sort(sortBuilders(pageable.getSort().get())).
                build();

        List<Hit<ObjectNode>> esResult = null;
        try {
            esResult = client.search(request, ObjectNode.class).hits().hits();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }

        return CollectionUtils.isEmpty(esResult)
                ? List.of()
                : esResult.stream().
                        map(hit -> POJOHelper.convertValue(hit.source().get("message"), AuditEntry.class)).
                        filter(Objects::nonNull).collect(Collectors.toList());
    }
}
