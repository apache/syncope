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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.sql.DataSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.jpa.entity.JPAReportExec;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Export internal storage content as XML.
 */
@Component
public class XMLContentExporter extends AbstractContentDealer implements ContentExporter {

    protected static final Set<String> TABLE_PREFIXES_TO_BE_EXCLUDED = new HashSet<>(Arrays.asList(new String[] {
        "QRTZ_", "LOGGING", JPAReportExec.TABLE, JPATaskExec.TABLE,
        JPAUser.TABLE, JPAUPlainAttr.TABLE, JPAUPlainAttrValue.TABLE, JPAUPlainAttrUniqueValue.TABLE,
        JPAURelationship.TABLE, JPAUMembership.TABLE,
        JPAAnyObject.TABLE, JPAAPlainAttr.TABLE, JPAAPlainAttrValue.TABLE, JPAAPlainAttrUniqueValue.TABLE,
        JPAARelationship.TABLE, JPAAMembership.TABLE
    }));

    protected static final Map<String, String> TABLES_TO_BE_FILTERED =
            Collections.singletonMap("TASK", "DTYPE <> 'PropagationTask'");

    protected static final Map<String, Set<String>> COLUMNS_TO_BE_NULLIFIED =
            Collections.singletonMap("SYNCOPEGROUP", Collections.singleton("USEROWNER_ID"));

    private boolean isTableAllowed(final String tableName) {
        return IterableUtils.matchesAll(TABLE_PREFIXES_TO_BE_EXCLUDED, new Predicate<String>() {

            @Override
            public boolean evaluate(final String prefix) {
                return !tableName.toUpperCase().startsWith(prefix.toUpperCase());
            }
        });
    }

    private List<String> sortByForeignKeys(final String dbSchema, final Connection conn, final Set<String> tableNames)
            throws SQLException {

        Set<MultiParentNode<String>> roots = new HashSet<>();

        final DatabaseMetaData meta = conn.getMetaData();

        final Map<String, MultiParentNode<String>> exploited = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Set<String> pkTableNames = new HashSet<>();

        for (String tableName : tableNames) {
            MultiParentNode<String> node = exploited.get(tableName);
            if (node == null) {
                node = new MultiParentNode<>(tableName);
                roots.add(node);
                exploited.put(tableName, node);
            }

            pkTableNames.clear();

            ResultSet rs = null;
            try {
                rs = meta.getImportedKeys(conn.getCatalog(), dbSchema, tableName);

                // this is to avoid repetition
                while (rs.next()) {
                    pkTableNames.add(rs.getString("PKTABLE_NAME"));
                }
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        LOG.error("While closing tables result set", e);
                    }
                }
            }

            for (String pkTableName : pkTableNames) {
                if (!tableName.equalsIgnoreCase(pkTableName)) {
                    MultiParentNode<String> pkNode = exploited.get(pkTableName);
                    if (pkNode == null) {
                        pkNode = new MultiParentNode<>(pkTableName);
                        roots.add(pkNode);
                        exploited.put(pkTableName, pkNode);
                    }

                    pkNode.addChild(node);

                    if (roots.contains(node)) {
                        roots.remove(node);
                    }
                }
            }
        }

        final List<String> sortedTableNames = new ArrayList<>(tableNames.size());
        MultiParentNodeOp.traverseTree(roots, sortedTableNames);

        // remove from sortedTableNames any table possibly added during lookup 
        // but matching some item in this.tablePrefixesToBeExcluded
        sortedTableNames.retainAll(tableNames);

        LOG.debug("Tables after retainAll {}", sortedTableNames);

        Collections.reverse(sortedTableNames);

        return sortedTableNames;
    }

    private String getValues(final ResultSet rs, final String columnName, final Integer columnType)
            throws SQLException {

        String res = null;

        try {
            switch (columnType) {
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    final InputStream is = rs.getBinaryStream(columnName);
                    if (is != null) {
                        res = new String(Hex.encode(IOUtils.toByteArray(is)));
                    }
                    break;

                case Types.BLOB:
                    final Blob blob = rs.getBlob(columnName);
                    if (blob != null) {
                        res = new String(Hex.encode(IOUtils.toByteArray(blob.getBinaryStream())));
                    }
                    break;

                case Types.BIT:
                case Types.BOOLEAN:
                    if (rs.getBoolean(columnName)) {
                        res = "1";
                    } else {
                        res = "0";
                    }
                    break;

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    final Timestamp timestamp = rs.getTimestamp(columnName);
                    if (timestamp != null) {
                        res = FormatUtils.format(new Date(timestamp.getTime()));
                    }
                    break;

                default:
                    res = rs.getString(columnName);
            }
        } catch (IOException e) {
            LOG.error("Error retrieving hexadecimal string", e);
        }

        return res;
    }

    private void doExportTable(final TransformerHandler handler, final Connection conn, final String tableName,
            final String whereClause) throws SQLException, SAXException {

        LOG.debug("Export table {}", tableName);

        AttributesImpl attrs = new AttributesImpl();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResultSet pkeyRS = null;
        try {
            // ------------------------------------
            // retrieve primary keys to perform an ordered select

            final DatabaseMetaData meta = conn.getMetaData();
            pkeyRS = meta.getPrimaryKeys(null, null, tableName);

            final StringBuilder orderBy = new StringBuilder();

            while (pkeyRS.next()) {
                final String columnName = pkeyRS.getString("COLUMN_NAME");
                if (columnName != null) {
                    if (orderBy.length() > 0) {
                        orderBy.append(",");
                    }

                    orderBy.append(columnName);
                }
            }

            // ------------------------------------
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ").append(tableName).append(" a");
            if (StringUtils.isNotBlank(whereClause)) {
                query.append(" WHERE ").append(whereClause);
            }
            if (orderBy.length() > 0) {
                query.append(" ORDER BY ").append(orderBy);
            }
            stmt = conn.prepareStatement(query.toString());

            rs = stmt.executeQuery();
            while (rs.next()) {
                attrs.clear();

                final ResultSetMetaData rsMeta = rs.getMetaData();
                for (int i = 0; i < rsMeta.getColumnCount(); i++) {
                    final String columnName = rsMeta.getColumnName(i + 1);
                    final Integer columnType = rsMeta.getColumnType(i + 1);

                    // Retrieve value taking care of binary values.
                    String value = getValues(rs, columnName, columnType);
                    if (value != null && (!COLUMNS_TO_BE_NULLIFIED.containsKey(tableName)
                            || !COLUMNS_TO_BE_NULLIFIED.get(tableName).contains(columnName))) {

                        attrs.addAttribute("", "", columnName, "CDATA", value);
                    }
                }

                handler.startElement("", "", tableName, attrs);
                handler.endElement("", "", tableName);

                LOG.debug("Add record {}", attrs);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("While closing result set", e);
                }
            }
            if (pkeyRS != null) {
                try {
                    pkeyRS.close();
                } catch (SQLException e) {
                    LOG.error("While closing result set", e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.error("While closing result set", e);
                }
            }
        }
    }

    @Override
    public void export(
            final String domain,
            final OutputStream os,
            final String uwfPrefix,
            final String gwfPrefix,
            final String awfPrefix)
            throws SAXException, TransformerConfigurationException {

        if (StringUtils.isNotBlank(uwfPrefix)) {
            TABLE_PREFIXES_TO_BE_EXCLUDED.add(uwfPrefix);
        }
        if (StringUtils.isNotBlank(gwfPrefix)) {
            TABLE_PREFIXES_TO_BE_EXCLUDED.add(gwfPrefix);
        }
        if (StringUtils.isNotBlank(awfPrefix)) {
            TABLE_PREFIXES_TO_BE_EXCLUDED.add(awfPrefix);
        }

        StreamResult streamResult = new StreamResult(os);
        final SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);

        TransformerHandler handler = transformerFactory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(streamResult);
        handler.startDocument();
        handler.startElement("", "", ROOT_ELEMENT, new AttributesImpl());

        DataSource dataSource = domainsHolder.getDomains().get(domain);
        if (dataSource == null) {
            throw new IllegalArgumentException("Could not find DataSource for domain " + domain);
        }

        String dbSchema = ApplicationContextProvider.getBeanFactory().getBean(domain + "DatabaseSchema", String.class);

        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            final DatabaseMetaData meta = conn.getMetaData();

            rs = meta.getTables(null, StringUtils.isBlank(dbSchema) ? null : dbSchema, null, new String[] { "TABLE" });

            final Set<String> tableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                LOG.debug("Found table {}", tableName);
                if (isTableAllowed(tableName)) {
                    tableNames.add(tableName);
                }
            }

            LOG.debug("Tables to be exported {}", tableNames);

            // then sort tables based on foreign keys and dump
            for (String tableName : sortByForeignKeys(dbSchema, conn, tableNames)) {
                try {
                    doExportTable(handler, conn, tableName, TABLES_TO_BE_FILTERED.get(tableName.toUpperCase()));
                } catch (Exception e) {
                    LOG.error("Failure exporting table {}", tableName, e);
                }
            }
        } catch (SQLException e) {
            LOG.error("While exporting database content", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("While closing tables result set", e);
                }
            }

            DataSourceUtils.releaseConnection(conn, dataSource);
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    LOG.error("While releasing connection", e);
                }
            }
        }

        handler.endElement("", "", ROOT_ELEMENT);
        handler.endDocument();
    }
}
