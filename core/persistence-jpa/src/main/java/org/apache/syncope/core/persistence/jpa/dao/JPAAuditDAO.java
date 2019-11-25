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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.persistence.api.dao.AuditDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AuditEntry;
import org.apache.syncope.core.provisioning.api.AuditEntryImpl;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Throwable.class)
@Repository
public class JPAAuditDAO extends AbstractDAO implements AuditDAO<AuditEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(JPAAuditDAO.class);

    @Autowired
    private DomainsHolder domainsHolder;

    @Override
    public List<AuditEntry> findByEntityKey(final String key, final int page,
                                            final int itemsPerPage,
                                            final List<OrderByClause> orderByClauses) {
        try {
            String queryString = "SELECT * FROM SYNCOPEAUDIT WHERE MESSAGE LIKE '%" + key + "%' ";
            if (!orderByClauses.isEmpty()) {
                 queryString += " ORDER BY " + orderByClauses.
                    stream().
                    map(orderBy -> orderBy.getField() + " " + orderBy.getDirection().name()).
                    collect(Collectors.joining(","));
            }
            JdbcTemplate template = getJdbcTemplate();
            template.setMaxRows(itemsPerPage);
            template.setFetchSize(itemsPerPage * (page <= 0 ? 0 : page - 1));
            return template.query(queryString, new AuditEntryRowMapper(key));
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private JdbcTemplate getJdbcTemplate() {
        DataSource datasource = domainsHolder.getDomains().get(AuthContextUtils.getDomain());
        if (datasource == null) {
            throw new IllegalArgumentException("Could not get to DataSource");
        }
        return new JdbcTemplate(datasource);
    }

    private static class AuditEntryRowMapper implements RowMapper<AuditEntry> {
        private final String key;
        
        AuditEntryRowMapper(final String key) {
            this.key = key;
        }

        @Override
        public AuditEntry mapRow(final ResultSet resultSet, final int i) throws SQLException {
            AuditEntryImpl entry = POJOHelper.deserialize(resultSet.getString("MESSAGE"), AuditEntryImpl.class);
            String throwable = resultSet.getString("THROWABLE");
            entry.setThrowable(throwable);
            Timestamp date = resultSet.getTimestamp("EVENT_DATE");
            entry.setDate(new Date(date.getTime()));
            entry.setKey(this.key);
            return entry;
        }
    }
}
