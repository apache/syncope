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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchIndexManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class OpenSearchAuditEventDAOTest {

    @Mock
    private OpenSearchIndexManager indexManager;

    @Mock
    private OpenSearchClient client;

    private OpenSearchAuditEventDAO auditEventDAO;

    @BeforeEach
    public void setupAuditEventDAO() {
        SecurityContextHolder.clearContext();
        auditEventDAO = new OpenSearchAuditEventDAO(indexManager, client, 500);
    }

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void save() throws Exception {
        AuditEvent auditEvent = mock(AuditEvent.class);

        assertThat(auditEventDAO.save(auditEvent)).isSameAs(auditEvent);

        verify(indexManager).audit(AuthContextUtils.getDomain(), auditEvent);
    }

    @Test
    public void saveFailure() throws Exception {
        AuditEvent auditEvent = mock(AuditEvent.class);
        doThrow(new IOException("boom")).when(indexManager).audit(anyString(), any(AuditEvent.class));

        assertThatThrownBy(() -> auditEventDAO.save(auditEvent)).
                isInstanceOf(IllegalStateException.class).
                hasMessage("Could not index audit event").
                hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void count() throws Exception {
        when(client.count(any(CountRequest.class))).thenReturn(new CountResponse.Builder().
                count(3).
                shards(new ShardStatistics.Builder().failed(0).successful(1).total(1).build()).
                build());

        assertThat(auditEventDAO.count(
                null, null, null,
                OpEvent.CategoryType.LOGIC, "UserLogic", null, "create", OpEvent.Outcome.SUCCESS,
                null, null)).
                isEqualTo(3L);

        ArgumentCaptor<CountRequest> captor = ArgumentCaptor.forClass(CountRequest.class);
        verify(client).count(captor.capture());
        assertThat(captor.getValue().index()).containsExactly("master_audit");
    }

    @Test
    public void countFailure() throws Exception {
        when(client.count(any(CountRequest.class))).thenThrow(new IOException("boom"));

        assertThat(auditEventDAO.count(
                null, null, null, null, null, null, null, null, null, null)).
                isZero();
    }

    @Test
    public void search() throws Exception {
        AuditEventTO auditEventTO = new AuditEventTO();
        Hit<AuditEventTO> hit = Hit.of(b -> b.index("master_audit").id("1").source(auditEventTO));
        when(client.search(any(SearchRequest.class), eq(AuditEventTO.class))).
                thenReturn(SearchResponse.searchResponseOf(b -> b.
                        took(1).
                        timedOut(false).
                        shards(new ShardStatistics.Builder().failed(0).successful(1).total(1).build()).
                        hits(h -> h.hits(List.of(hit)))));

        assertThat(auditEventDAO.search(
                null, null, null, null, null, null, null, null, null, null, Pageable.unpaged())).
                containsExactly(auditEventTO);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(client).search(captor.capture(), eq(AuditEventTO.class));
        assertThat(captor.getValue().index()).containsExactly("master_audit");
        assertThat(captor.getValue().from()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(500);
    }

    @Test
    public void searchFailure() throws Exception {
        when(client.search(any(SearchRequest.class), eq(AuditEventTO.class))).thenThrow(new IOException("boom"));

        assertThat(auditEventDAO.search(
                null, null, null, null, null, null, null, null, null, null, Pageable.unpaged())).
                isEmpty();
    }

    @Test
    public void sortBuilders() {
        List<SortOptions> sortOptions = auditEventDAO.sortBuilders(Stream.of(
                Sort.Order.asc("id"), Sort.Order.desc("when")));

        assertThat(sortOptions).hasSize(2);
        assertThat(sortOptions.get(0).field().field()).isEqualTo("key");
        assertThat(sortOptions.get(0).field().order()).isEqualTo(SortOrder.Asc);
        assertThat(sortOptions.get(1).field().field()).isEqualTo("when");
        assertThat(sortOptions.get(1).field().order()).isEqualTo(SortOrder.Desc);
    }

    @Test
    public void query4invalidEntityKey() {
        assertThatThrownBy(() -> auditEventDAO.getQuery(
                "not-a-uuid", null, null, null, null, null, null, null, null, null)).
                isInstanceOf(IllegalArgumentException.class).
                hasMessage("Invalid entityKey: not-a-uuid");
    }

    @Test
    public void query() {
        Query query = auditEventDAO.getQuery(
                UUID.randomUUID().toString(),
                Set.of("rossi"),
                Set.of("admin"),
                OpEvent.CategoryType.LOGIC, "UserLogic", null, "create", OpEvent.Outcome.SUCCESS,
                null, null);

        assertThat(query._kind()).isEqualTo(Query.Kind.Bool);

        // multi-match on entityKey, multi-match on username, term on who, regexp on opEvent
        assertThat(((BoolQuery) query._get()).filter()).hasSize(4);
    }
}
