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

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.apache.syncope.core.provisioning.api.AuditEntryImpl;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
@Repository
public class JPAAuditDAO extends AbstractDAO<AbstractEntity> implements AuditDAO {

    @Autowired
    private DomainHolder domainHolder;

    private static String buildWhereClauseForEntityKey(final String key) {
        return " WHERE MESSAGE LIKE '%" + key + "%' ";
    }

    @Override
    public List<AuditEntry> findByEntityKey(
            final String key,
            final int page,
            final int itemsPerPage,
            final List<AuditElements.Result> results,
            final List<String> events,
            final List<OrderByClause> orderByClauses) {

        try {
            String query = new MessageCriteriaBuilder().
                    results(results).
                    events(events).
                    key(key).
                    build();
            String queryString = "SELECT * FROM " + TABLE_NAME + " WHERE " + query;
            if (!orderByClauses.isEmpty()) {
                queryString += " ORDER BY " + orderByClauses.stream().
                        map(orderBy -> orderBy.getField() + ' ' + orderBy.getDirection().name()).
                        collect(Collectors.joining(","));
            }
            JdbcTemplate template = getJdbcTemplate();
            template.setMaxRows(itemsPerPage);
            template.setFetchSize(itemsPerPage * (page <= 0 ? 0 : page - 1));
            return template.query(queryString, (resultSet, i) -> {
                AuditEntryImpl entry = POJOHelper.deserialize(resultSet.getString("MESSAGE"), AuditEntryImpl.class);
                String throwable = resultSet.getString("THROWABLE");
                entry.setThrowable(throwable);
                Timestamp date = resultSet.getTimestamp("EVENT_DATE");
                entry.setDate(new Date(date.getTime()));
                entry.setKey(key);
                return entry;
            });
        } catch (Exception e) {
            LOG.error("Unable to execute search query to find entity " + key, e);
        }
        return Collections.emptyList();
    }

    @Override
    public Integer count(final String key) {
        try {
            String queryString = "SELECT COUNT(0) FROM " + AuditDAO.TABLE_NAME + buildWhereClauseForEntityKey(key);
            return Objects.requireNonNull(getJdbcTemplate().queryForObject(queryString, Integer.class));
        } catch (Exception e) {
            LOG.error("Unable to execute count query for entity " + key, e);
        }
        return 0;
    }

    private JdbcTemplate getJdbcTemplate() {
        String domain = AuthContextUtils.getDomain();
        DataSource datasource = domainHolder.getDomains().get(domain);
        if (datasource == null) {
            throw new IllegalArgumentException("Could not get to DataSource for domain " + domain);
        }
        return new JdbcTemplate(datasource);
    }

    private static class MessageCriteriaBuilder {

        private final StringBuilder query = new StringBuilder(" 1=1 ");

        public MessageCriteriaBuilder key(final String key) {
            query.append(" AND MESSAGE LIKE '%\"key\":\"").append(key).append("\"%' ");
            return this;
        }

        public MessageCriteriaBuilder results(final List<AuditElements.Result> results) {
            buildCriteriaFor(results.stream().map(Enum::name).collect(Collectors.toList()), "result");
            return this;
        }

        private void buildCriteriaFor(final List<String> items, final String field) {
            if (!items.isEmpty()) {
                query.append(" AND ( ");
                query.append(items.stream().map(res -> "MESSAGE LIKE '%\"" + field + "\":\"" + res + "\"%'").
                        collect(Collectors.joining(" OR ")));
                query.append(" )");
            }
        }

        public MessageCriteriaBuilder events(final List<String> events) {
            buildCriteriaFor(events, "event");
            return this;
        }

        public String build() {
            query.trimToSize();
            return query.toString();
        }
    }
}
