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
package org.apache.syncope.core.persistence.opensearch.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchIndexManager;
import org.apache.syncope.ext.opensearch.client.OpenSearchUtils;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

public class OpenSearchAuditEventDAO implements AuditEventDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditEventDAO.class);

    protected final OpenSearchIndexManager indexManager;

    protected final OpenSearchClient client;

    protected final int indexMaxResultWindow;

    public OpenSearchAuditEventDAO(
            final OpenSearchIndexManager indexManager,
            final OpenSearchClient client,
            final int indexMaxResultWindow) {

        this.indexManager = indexManager;
        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    @Override
    public AuditEvent save(final AuditEvent auditEvent) {
        try {
            indexManager.audit(AuthContextUtils.getDomain(), auditEvent);
        } catch (Exception e) {
            throw new IllegalStateException("Could not index audit event", e);
        }
        return auditEvent;
    }

    protected Query getQuery(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        List<Query> queries = new ArrayList<>();

        if (entityKey != null) {
            queries.add(new Query.Builder().
                    multiMatch(QueryBuilders.multiMatch().
                            fields("before", "inputs", "output", "throwable").
                            type(TextQueryType.Phrase).
                            query(entityKey).build()).build());
        }

        queries.add(new Query.Builder().regexp(QueryBuilders.regexp().
                field("opEvent").
                value(OpEvent.toString(type, category, subcategory, op, outcome).
                        replace("[]", "[.*]").
                        replace("[", "\\[").
                        replace("]", "\\]")).build()).
                build());

        if (before != null) {
            queries.add(new Query.Builder().
                    range(QueryBuilders.range().
                            field("when").lte(JsonData.of(before.toInstant().toEpochMilli())).build()).
                    build());
        }

        if (after != null) {
            queries.add(new Query.Builder().
                    range(QueryBuilders.range().
                            field("when").gte(JsonData.of(after.toInstant().toEpochMilli())).build()).
                    build());
        }

        return new Query.Builder().bool(QueryBuilders.bool().filter(queries).build()).build();
    }

    @Override
    public long count(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        CountRequest request = new CountRequest.Builder().
                index(OpenSearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                query(getQuery(entityKey, type, category, subcategory, op, outcome, before, after)).
                build();
        LOG.debug("Count request: {}", request);

        try {
            return client.count(request).count();
        } catch (IOException e) {
            LOG.error("While counting in OpenSearch with request {}", request, e);
            return 0L;
        }
    }

    protected List<SortOptions> sortBuilders(final Stream<Sort.Order> orderBy) {
        return orderBy.map(clause -> new SortOptions.Builder().field(
                new FieldSort.Builder().
                        field("id".equals(clause.getProperty()) ? "key" : clause.getProperty()).
                        order(clause.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc).
                        build()).
                build()).toList();
    }

    @Override
    public List<AuditEventTO> search(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome outcome,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getAuditIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(getQuery(entityKey, type, category, subcategory, op, outcome, before, after)).
                from(pageable.isUnpaged() ? 0 : pageable.getPageSize() * pageable.getPageNumber()).
                size(pageable.isUnpaged() ? indexMaxResultWindow : pageable.getPageSize()).
                sort(sortBuilders(pageable.getSort().get())).
                build();
        LOG.debug("Search request: {}", request);

        List<Hit<ObjectNode>> esResult = null;
        try {
            esResult = client.search(request, ObjectNode.class).hits().hits();
        } catch (Exception e) {
            LOG.error("While searching in OpenSearch with request {}", request, e);
        }

        return CollectionUtils.isEmpty(esResult)
                ? List.of()
                : esResult.stream().
                        map(hit -> POJOHelper.convertValue(hit.source(), AuditEventTO.class)).
                        filter(Objects::nonNull).toList();
    }
}
