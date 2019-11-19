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
package org.apache.syncope.core.rest.cxf.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.lib.to.AuditTO;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.core.persistence.api.DomainsHolder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.java.AuditEntry;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditServiceImpl extends AbstractServiceImpl implements AuditService {
    private static final String SQL_TABLE = "SYNCOPEAUDIT";

    private static final String SQL_SELECT = "SELECT * FROM SYNCOPEAUDIT %s ORDER BY EVENT_DATE DESC";

    @Autowired
    private DomainsHolder domainsHolder;

    @Override
    public List<AuditTO> list() {
        return getJdbcTemplate().
            query(String.format(SQL_SELECT, StringUtils.EMPTY), new AuditTORowMapper());
    }

    @Override
    public AuditTO read(final String logger) {
        return getJdbcTemplate().
            queryForObject(String.format(SQL_SELECT, "WHERE LOGGER=:logger"),
                new MapSqlParameterSource("logger", logger),
                new AuditTORowMapper());
    }

    private NamedParameterJdbcTemplate getJdbcTemplate() {
        DataSource datasource = domainsHolder.getDomains().get(AuthContextUtils.getDomain());
        if (datasource == null) {
            throw new IllegalArgumentException("Could not get to DataSource");
        }
        return new NamedParameterJdbcTemplate(datasource);
    }

    private static class AuditTORowMapper implements RowMapper<AuditTO> {
        @Override
        public AuditTO mapRow(final ResultSet resultSet, final int i) throws SQLException {
            AuditEntry auditEntry = POJOHelper.deserialize(resultSet.getString("MESSAGE"), AuditEntry.class);
            AuditTO auditTO = new AuditTO();
            auditTO.setWho(auditEntry.getWho());

            if (StringUtils.isNotBlank(auditEntry.getLogger().getSubcategory())) {
                auditTO.setSubCategory(auditEntry.getLogger().getSubcategory());
            }
            if (StringUtils.isNotBlank(auditEntry.getLogger().getEvent())) {
                auditTO.setEvent(auditEntry.getLogger().getEvent());
            }
            if (auditEntry.getLogger().getResult() != null) {
                auditTO.setResult(auditEntry.getLogger().getResult().name());
            }

            if (auditEntry.getBefore() != null) {
                String before = ToStringBuilder.reflectionToString(
                    auditEntry.getBefore(), ToStringStyle.JSON_STYLE);
                auditTO.setBefore(before);
            }

            if (auditEntry.getInput() != null) {
                auditTO.getInputs().addAll(Arrays.stream(auditEntry.getInput())
                    .map(input -> ToStringBuilder.reflectionToString(input, ToStringStyle.JSON_STYLE))
                    .collect(Collectors.toList()));
            }

            if (auditEntry.getOutput() != null) {
                auditTO.setOutput(ToStringBuilder.reflectionToString(
                    auditEntry.getOutput(), ToStringStyle.JSON_STYLE));
            }

            String throwable = resultSet.getString("THROWABLE");
            auditTO.setThrowable(throwable);

            Timestamp date = resultSet.getTimestamp("EVENT_DATE");
            auditTO.setDate(new Date(date.getTime()));

            return auditTO;
        }
    }
}
