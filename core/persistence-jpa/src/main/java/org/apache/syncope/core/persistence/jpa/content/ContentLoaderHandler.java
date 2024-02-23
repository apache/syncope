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
package org.apache.syncope.core.persistence.jpa.content;

import jakarta.xml.bind.DatatypeConverter;
import java.sql.Types;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.common.content.AbstractContentLoaderHandler;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.Attributes;

/**
 * SAX handler for generating SQL INSERT statements out of given XML file.
 */
public class ContentLoaderHandler extends AbstractContentLoaderHandler {

    private final JdbcTemplate jdbcTemplate;

    public ContentLoaderHandler(
            final DataSource dataSource,
            final String rootElement,
            final boolean continueOnError,
            final Environment env) {

        super(rootElement, continueOnError, env);
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    protected void fetch(final Attributes atts) {
        String value = jdbcTemplate.queryForObject(atts.getValue("query"), String.class);
        String key = atts.getValue("key");
        fetches.put(key, value);
    }

    private Object[] getParameters(final String tableName, final Attributes attrs) {
        Map<String, Integer> colTypes = jdbcTemplate.query(
                "SELECT * FROM " + tableName + " WHERE 0=1", rs -> {
                    Map<String, Integer> types = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        types.put(rs.getMetaData().getColumnName(i).toUpperCase(), rs.getMetaData().getColumnType(i));
                    }
                    return types;
                });

        Object[] parameters = new Object[attrs.getLength()];
        for (int i = 0; i < attrs.getLength(); i++) {
            Integer colType = Objects.requireNonNull(colTypes).get(attrs.getQName(i).toUpperCase());
            if (colType == null) {
                LOG.warn("No column type found for {}", attrs.getQName(i).toUpperCase());
                colType = Types.VARCHAR;
            }

            String value = paramSubstitutor.replace(attrs.getValue(i));
            if (value == null) {
                LOG.warn("Variable ${} could not be resolved", attrs.getValue(i));
                value = attrs.getValue(i);
            }
            value = StringEscapeUtils.unescapeXml(value);

            switch (colType) {
                case Types.INTEGER:
                case Types.TINYINT:
                case Types.SMALLINT:
                    try {
                        parameters[i] = Integer.valueOf(value);
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Integer '{}'", value);
                        parameters[i] = value;
                    }
                    break;

                case Types.NUMERIC:
                case Types.DECIMAL:
                case Types.BIGINT:
                    try {
                        parameters[i] = Long.valueOf(value);
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Long '{}'", value);
                        parameters[i] = value;
                    }
                    break;

                case Types.DOUBLE:
                    try {
                        parameters[i] = Double.valueOf(value);
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Double '{}'", value);
                        parameters[i] = value;
                    }
                    break;

                case Types.REAL:
                case Types.FLOAT:
                    try {
                        parameters[i] = Float.valueOf(value);
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Float '{}'", value);
                        parameters[i] = value;
                    }
                    break;

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                case -101:
                    try {
                        parameters[i] = FormatUtils.parseDate(value);
                    } catch (DateTimeParseException e) {
                        LOG.error("Unparsable Date '{}'", value);
                        parameters[i] = value;
                    }
                    break;

                case Types.BIT:
                case Types.BOOLEAN:
                    parameters[i] = "1".equals(value) ? Boolean.TRUE : Boolean.FALSE;
                    break;

                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    try {
                        parameters[i] = DatatypeConverter.parseHexBinary(value);
                    } catch (IllegalArgumentException e) {
                        parameters[i] = value;
                    }
                    break;

                case Types.BLOB:
                    try {
                        parameters[i] = DatatypeConverter.parseHexBinary(value);
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Error decoding hex string to specify a blob parameter", e);
                        parameters[i] = value;
                    } catch (Exception e) {
                        LOG.warn("Error creating a new blob parameter", e);
                    }
                    break;

                default:
                    parameters[i] = value;
            }
        }

        return parameters;
    }

    @Override
    protected void create(final String qName, final Attributes atts) {
        StringBuilder query = new StringBuilder("INSERT INTO ").append(qName).append('(');

        StringBuilder values = new StringBuilder();

        for (int i = 0; i < atts.getLength(); i++) {
            query.append(atts.getQName(i));
            values.append('?');
            if (i < atts.getLength() - 1) {
                query.append(',');
                values.append(',');
            }
        }
        query.append(") VALUES (").append(values).append(')');

        try {
            jdbcTemplate.update(query.toString(), getParameters(qName, atts));
        } catch (DataAccessException e) {
            LOG.error("While trying to perform {} with params {}", query, getParameters(qName, atts), e);
            if (!continueOnError) {
                throw e;
            }
        }
    }
}
