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

import java.sql.ResultSet;
import java.sql.Types;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import javax.xml.bind.DatatypeConverter;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler for generating SQL INSERT statements out of given XML file.
 */
public class ContentLoaderHandler extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ContentLoaderHandler.class);

    private final DataSource dataSource;

    private final String rootElement;

    private final boolean continueOnError;

    public ContentLoaderHandler(final DataSource dataSource, final String rootElement, final boolean continueOnError) {
        this.dataSource = dataSource;
        this.rootElement = rootElement;
        this.continueOnError = continueOnError;
    }

    private Object[] getParameters(final String tableName, final Attributes attrs) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Map<String, Integer> colTypes = jdbcTemplate.query(
                "SELECT * FROM " + tableName + " WHERE 0=1", (final ResultSet rs) -> {
                    Map<String, Integer> colTypes1 = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount();
                    i++) {
                        colTypes1.
                                put(rs.getMetaData().getColumnName(i).toUpperCase(), rs.getMetaData().getColumnType(i));
                    }
                    return colTypes1;
                });

        Object[] parameters = new Object[attrs.getLength()];
        for (int i = 0; i < attrs.getLength(); i++) {
            Integer colType = colTypes.get(attrs.getQName(i).toUpperCase());
            if (colType == null) {
                LOG.warn("No column type found for {}", attrs.getQName(i).toUpperCase());
                colType = Types.VARCHAR;
            }

            switch (colType) {
                case Types.INTEGER:
                case Types.TINYINT:
                case Types.SMALLINT:
                    try {
                        parameters[i] = Integer.valueOf(attrs.getValue(i));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Integer '{}'", attrs.getValue(i));
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.NUMERIC:
                case Types.DECIMAL:
                case Types.BIGINT:
                    try {
                        parameters[i] = Long.valueOf(attrs.getValue(i));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Long '{}'", attrs.getValue(i));
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.DOUBLE:
                    try {
                        parameters[i] = Double.valueOf(attrs.getValue(i));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Double '{}'", attrs.getValue(i));
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.REAL:
                case Types.FLOAT:
                    try {
                        parameters[i] = Float.valueOf(attrs.getValue(i));
                    } catch (NumberFormatException e) {
                        LOG.error("Unparsable Float '{}'", attrs.getValue(i));
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    try {
                        parameters[i] = FormatUtils.parseDate(attrs.getValue(i));
                    } catch (ParseException e) {
                        LOG.error("Unparsable Date '{}'", attrs.getValue(i));
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.BIT:
                case Types.BOOLEAN:
                    parameters[i] = "1".equals(attrs.getValue(i)) ? Boolean.TRUE : Boolean.FALSE;
                    break;

                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    try {
                        parameters[i] = DatatypeConverter.parseHexBinary(attrs.getValue(i));
                    } catch (IllegalArgumentException e) {
                        parameters[i] = attrs.getValue(i);
                    }
                    break;

                case Types.BLOB:
                    try {
                        parameters[i] = DatatypeConverter.parseHexBinary(attrs.getValue(i));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Error decoding hex string to specify a blob parameter", e);
                        parameters[i] = attrs.getValue(i);
                    } catch (Exception e) {
                        LOG.warn("Error creating a new blob parameter", e);
                    }
                    break;

                default:
                    parameters[i] = attrs.getValue(i);
            }
        }

        return parameters;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {

        // skip root element
        if (rootElement.equals(qName)) {
            return;
        }

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

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
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
