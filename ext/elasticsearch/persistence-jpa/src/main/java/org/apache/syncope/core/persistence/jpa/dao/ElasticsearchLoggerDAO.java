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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.log.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("deprecation")
public class ElasticsearchLoggerDAO extends JPALoggerDAO {

    @Autowired
    protected org.elasticsearch.client.RestHighLevelClient client;

    @Autowired
    protected ElasticsearchUtils elasticsearchUtils;

    protected QueryBuilder getQueryBuilder(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final Date before,
            final Date after) {

        List<QueryBuilder> queryBuilders = new ArrayList<>();

        if (entityKey != null) {
            queryBuilders.add(QueryBuilders.multiMatchQuery(
                    entityKey, "message.before", "message.inputs", "message.output", "message.throwable").
                    type(MultiMatchQueryBuilder.Type.PHRASE));
        }

        if (type != null) {
            queryBuilders.add(QueryBuilders.termQuery("message.logger.type", type.name()));
        }

        if (StringUtils.isNotBlank(category)) {
            queryBuilders.add(QueryBuilders.termQuery("message.logger.category", category));
        }

        if (StringUtils.isNotBlank(subcategory)) {
            queryBuilders.add(QueryBuilders.termQuery("message.logger.subcategory", subcategory));
        }

        List<QueryBuilder> eventQueryBuilders = events.stream().
                map(event -> QueryBuilders.termQuery("message.logger.event", event)).
                collect(Collectors.toList());
        if (!eventQueryBuilders.isEmpty()) {
            if (eventQueryBuilders.size() == 1) {
                queryBuilders.add(eventQueryBuilders.get(0));
            } else {
                DisMaxQueryBuilder disMax = QueryBuilders.disMaxQuery();
                eventQueryBuilders.forEach(disMax::add);
                queryBuilders.add(disMax);
            }
        }

        if (result != null) {
            queryBuilders.add(QueryBuilders.termQuery("message.logger.result", result.name()));
        }

        if (before != null) {
            queryBuilders.add(QueryBuilders.rangeQuery("instant").lte(before.getTime()));
        }

        if (after != null) {
            queryBuilders.add(QueryBuilders.rangeQuery("instant").gte(after.getTime()));
        }

        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders.forEach(bool::must);
        return bool;
    }

    @Override
    public int countAuditEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final Date before,
            final Date after) {

        CountRequest request = new CountRequest(
                ElasticsearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                query(getQueryBuilder(entityKey, type, category, subcategory, events, result, before, after));
        try {
            return (int) client.count(request, RequestOptions.DEFAULT).getCount();
        } catch (IOException e) {
            LOG.error("Search error", e);
            return 0;
        }
    }

    protected List<SortBuilder<?>> sortBuilders(final List<OrderByClause> orderBy) {
        return orderBy.stream().map(clause -> {
            String sortField = clause.getField();
            if ("EVENT_DATE".equalsIgnoreCase(sortField)) {
                sortField = "message.date";
            }

            return new FieldSortBuilder(sortField).order(SortOrder.valueOf(clause.getDirection().name()));
        }).collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> findAuditEntries(
            final String entityKey,
            final int page,
            final int itemsPerPage,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final Date before,
            final Date after,
            final List<OrderByClause> orderBy) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().
                query(getQueryBuilder(entityKey, type, category, subcategory, events, result, before, after)).
                from(itemsPerPage * (page <= 0 ? 0 : page - 1)).
                size(itemsPerPage < 0 ? elasticsearchUtils.getIndexMaxResultWindow() : itemsPerPage);
        sortBuilders(orderBy).forEach(sourceBuilder::sort);

        SearchRequest request = new SearchRequest(
                ElasticsearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QUERY_THEN_FETCH).
                source(sourceBuilder);

        SearchHit[] esResult = null;
        try {
            esResult = client.search(request, RequestOptions.DEFAULT).getHits().getHits();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }

        return ArrayUtils.isEmpty(esResult)
                ? Collections.emptyList()
                : Arrays.stream(esResult).
                        map(hit -> POJOHelper.convertValue(hit.getSourceAsMap().get("message"), AuditEntry.class)).
                        filter(Objects::nonNull).collect(Collectors.toList());
    }
}
