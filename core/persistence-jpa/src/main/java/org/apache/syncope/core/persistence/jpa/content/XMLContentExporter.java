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
import java.lang.reflect.Field;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.EntityManagerFactory;
import javax.persistence.JoinTable;
import javax.persistence.Table;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.helpers.IOUtils;
import org.apache.openjpa.lib.util.collections.BidiMap;
import org.apache.openjpa.lib.util.collections.DualHashBidiMap;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPAAccessToken;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
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
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Export internal storage content as XML.
 */
public class XMLContentExporter implements ContentExporter {

    protected static final Logger LOG = LoggerFactory.getLogger(XMLContentExporter.class);

    protected static final Set<String> TABLE_PREFIXES_TO_BE_EXCLUDED = Stream.of(
            "QRTZ_", "LOGGING", "NotificationTask_recipients", AuditConfDAO.AUDIT_ENTRY_TABLE, JPAReportExec.TABLE,
            JPATaskExec.TABLE, JPAUser.TABLE, JPAUPlainAttr.TABLE, JPAUPlainAttrValue.TABLE,
            JPAUPlainAttrUniqueValue.TABLE, JPAURelationship.TABLE, JPAUMembership.TABLE,
            JPAAnyObject.TABLE, JPAAPlainAttr.TABLE, JPAAPlainAttrValue.TABLE, JPAAPlainAttrUniqueValue.TABLE,
            JPAARelationship.TABLE, JPAAMembership.TABLE, JPAAccessToken.TABLE
    ).collect(Collectors.toCollection(HashSet::new));

    protected static final Map<String, String> TABLES_TO_BE_FILTERED =
            Map.of("TASK", "DTYPE <> 'PropagationTask' AND DTYPE <> 'NotificationTask'");

    protected static final Map<String, Set<String>> COLUMNS_TO_BE_NULLIFIED =
            Map.of("SYNCOPEGROUP", Set.of("USEROWNER_ID"));

    protected static boolean isTableAllowed(final String tableName) {
        return TABLE_PREFIXES_TO_BE_EXCLUDED.stream().
                allMatch(prefix -> !tableName.toUpperCase().startsWith(prefix.toUpperCase()));
    }

    protected static List<String> sortByForeignKeys(final String dbSchema, final Connection conn,
            final Set<String> tableNames)
            throws SQLException {

        Set<MultiParentNode<String>> roots = new HashSet<>();

        DatabaseMetaData meta = conn.getMetaData();

        Map<String, MultiParentNode<String>> exploited = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> pkTableNames = new HashSet<>();

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

        List<String> sortedTableNames = new ArrayList<>(tableNames.size());
        MultiParentNodeOp.traverseTree(roots, sortedTableNames);

        // remove from sortedTableNames any table possibly added during lookup 
        // but matching some item in this.tablePrefixesToBeExcluded
        sortedTableNames.retainAll(tableNames);

        LOG.debug("Tables after retainAll {}", sortedTableNames);

        Collections.reverse(sortedTableNames);

        return sortedTableNames;
    }

    protected static String getValues(final ResultSet rs, final String columnName, final Integer columnType)
            throws SQLException {

        String res = null;

        try {
            switch (columnType) {
                case Types.BINARY:
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    InputStream is = rs.getBinaryStream(columnName);
                    if (is != null) {
                        res = DatatypeConverter.printHexBinary(IOUtils.toString(is).getBytes());
                    }
                    break;

                case Types.BLOB:
                    Blob blob = rs.getBlob(columnName);
                    if (blob != null) {
                        res = DatatypeConverter.printHexBinary(IOUtils.toString(blob.getBinaryStream()).getBytes());
                    }
                    break;

                case Types.BIT:
                case Types.BOOLEAN:
                    res = rs.getBoolean(columnName) ? "1" : "0";
                    break;

                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    Timestamp timestamp = rs.getTimestamp(columnName);
                    if (timestamp != null) {
                        res = FormatUtils.format(OffsetDateTime.ofInstant(
                                Instant.ofEpochMilli(timestamp.getTime()), ZoneId.systemDefault()));
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

    protected final DomainHolder domainHolder;

    protected final RealmDAO realmDAO;

    public XMLContentExporter(final DomainHolder domainHolder, final RealmDAO realmDAO) {
        this.domainHolder = domainHolder;
        this.realmDAO = realmDAO;
    }

    protected String columnName(final Supplier<Stream<Attribute<?, ?>>> attrs, final String columnName) {
        String name = attrs.get().map(attr -> {
            if (attr.getName().equalsIgnoreCase(columnName)) {
                return attr.getName();
            }

            Field field = (Field) attr.getJavaMember();
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().equalsIgnoreCase(columnName)) {
                return column.name();
            }

            return null;
        }).filter(Objects::nonNull).findFirst().orElse(columnName);

        if (StringUtils.endsWithIgnoreCase(name, "_ID")) {
            String left = StringUtils.substringBefore(name, "_");
            String prefix = attrs.get().filter(attr -> left.equalsIgnoreCase(attr.getName())).findFirst().
                    map(Attribute::getName).orElse(left);
            name = prefix + "_id";
        }

        return name;
    }

    protected boolean isTask(final String tableName) {
        return "TASK".equalsIgnoreCase(tableName);
    }

    @SuppressWarnings("unchecked")
    protected void exportTable(
            final TransformerHandler handler,
            final Connection conn,
            final String tableName,
            final String whereClause,
            final BidiMap<String, EntityType<?>> entities,
            final Set<EntityType<?>> taskEntities,
            final Map<String, Pair<String, String>> relationTables) throws SQLException, SAXException {

        LOG.debug("Export table {}", tableName);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            StringBuilder orderBy = new StringBuilder();

            DatabaseMetaData meta = conn.getMetaData();

            // retrieve primary keys to perform an ordered select
            ResultSet pkeyRS = null;
            try {
                pkeyRS = meta.getPrimaryKeys(null, null, tableName);
                while (pkeyRS.next()) {
                    String columnName = pkeyRS.getString("COLUMN_NAME");
                    if (columnName != null) {
                        if (orderBy.length() > 0) {
                            orderBy.append(',');
                        }

                        orderBy.append(columnName);
                    }
                }
            } finally {
                if (pkeyRS != null) {
                    try {
                        pkeyRS.close();
                    } catch (SQLException e) {
                        LOG.error("While closing result set", e);
                    }
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

            List<Map<String, String>> rows = new ArrayList<>();

            Optional<EntityType<?>> entity = entities.entrySet().stream().
                    filter(entry -> entry.getKey().equalsIgnoreCase(tableName)).
                    findFirst().
                    map(Map.Entry::getValue);

            String outputTableName = entity.isPresent()
                    ? entities.getKey(entity.get())
                    : relationTables.keySet().stream().filter(tableName::equalsIgnoreCase).findFirst().
                            orElse(tableName);
            if (isTask(tableName)) {
                outputTableName = "Task";
            }

            rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                rows.add(row);

                ResultSetMetaData rsMeta = rs.getMetaData();
                for (int i = 0; i < rsMeta.getColumnCount(); i++) {
                    String columnName = rsMeta.getColumnName(i + 1);
                    Integer columnType = rsMeta.getColumnType(i + 1);

                    // Retrieve value taking care of binary values.
                    String value = getValues(rs, columnName, columnType);
                    if (value != null && (!COLUMNS_TO_BE_NULLIFIED.containsKey(tableName)
                            || !COLUMNS_TO_BE_NULLIFIED.get(tableName).contains(columnName))) {

                        String name = columnName;
                        if (entity.isPresent()) {
                            name = columnName(
                                    () -> (Stream<Attribute<?, ?>>) entity.get().getAttributes().stream(), columnName);
                        }

                        if (isTask(tableName)) {
                            name = columnName(
                                    () -> taskEntities.stream().flatMap(e -> e.getAttributes().stream()), columnName);
                        }

                        if (relationTables.containsKey(outputTableName)) {
                            Pair<String, String> relationColumns = relationTables.get(outputTableName);
                            if (name.equalsIgnoreCase(relationColumns.getLeft())) {
                                name = relationColumns.getLeft();
                            } else if (name.equalsIgnoreCase(relationColumns.getRight())) {
                                name = relationColumns.getRight();
                            }
                        }

                        row.put(name, value);
                        LOG.debug("Add for table {}: {}=\"{}\"", outputTableName, name, value);
                    }
                }
            }

            if (tableName.equalsIgnoreCase(JPARealm.TABLE)) {
                List<Map<String, String>> realmRows = new ArrayList<>(rows);
                rows.clear();
                realmDAO.findAll().forEach(realm -> realmRows.stream().filter(row -> {
                    String id = row.get("ID");
                    if (id == null) {
                        id = row.get("id");
                    }
                    return realm.getKey().equals(id);
                }).findFirst().ifPresent(rows::add));
            }

            for (Map<String, String> row : rows) {
                AttributesImpl attrs = new AttributesImpl();
                row.forEach((key, value) -> attrs.addAttribute("", "", key, "CDATA", value));

                handler.startElement("", "", outputTableName, attrs);
                handler.endElement("", "", outputTableName);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("While closing result set", e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LOG.error("While closing statement", e);
                }
            }
        }
    }

    protected Set<EntityType<?>> taskEntities(final Set<EntityType<?>> entityTypes) {
        return entityTypes.stream().filter(e -> e.getName().endsWith("Task")).collect(Collectors.toSet());
    }

    protected BidiMap<String, EntityType<?>> entities(final Set<EntityType<?>> entityTypes) {
        BidiMap<String, EntityType<?>> entities = new DualHashBidiMap<>();
        entityTypes.forEach(entity -> {
            Table table = entity.getBindableJavaType().getAnnotation(Table.class);
            if (table != null) {
                entities.put(table.name(), entity);
            }
        });

        return entities;
    }

    protected Map<String, Pair<String, String>> relationTables(final BidiMap<String, EntityType<?>> entities) {
        Map<String, Pair<String, String>> relationTables = new HashMap<>();
        entities.values().stream().forEach(e -> e.getAttributes().stream().
                filter(a -> a.getPersistentAttributeType() != Attribute.PersistentAttributeType.BASIC).
                forEach(a -> {
                    String attrName = a.getName();

                    Field field = (Field) a.getJavaMember();
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        attrName = column.name();
                    }

                    CollectionTable collectionTable = field.getAnnotation(CollectionTable.class);
                    if (collectionTable != null) {
                        relationTables.put(
                                collectionTable.name(),
                                Pair.of(attrName, collectionTable.joinColumns()[0].name()));
                    }

                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    if (joinTable != null) {
                        String tableName = joinTable.name();
                        if (StringUtils.isBlank(tableName)) {
                            tableName = entities.getKey(e) + "_"
                                    + entities.getKey(((PluralAttribute) a).getElementType());
                        }

                        relationTables.put(
                                tableName,
                                Pair.of(joinTable.joinColumns()[0].name(),
                                        joinTable.inverseJoinColumns()[0].name()));
                    }
                }));

        return relationTables;
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
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        TransformerHandler handler = transformerFactory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(streamResult);
        handler.startDocument();
        handler.startElement("", "", ROOT_ELEMENT, new AttributesImpl());

        DataSource dataSource = domainHolder.getDomains().get(domain);
        if (dataSource == null) {
            throw new IllegalArgumentException("Could not find DataSource for domain " + domain);
        }

        String schema = null;
        if (ApplicationContextProvider.getBeanFactory().containsBean(domain + "DatabaseSchema")) {
            Object schemaBean = ApplicationContextProvider.getBeanFactory().getBean(domain + "DatabaseSchema");
            if (schemaBean instanceof String) {
                schema = (String) schemaBean;
            }
        }

        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            final DatabaseMetaData meta = conn.getMetaData();

            rs = meta.getTables(null, StringUtils.isBlank(schema) ? null : schema, null, new String[] { "TABLE" });

            Set<String> tableNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                LOG.debug("Found table {}", tableName);
                if (isTableAllowed(tableName)) {
                    tableNames.add(tableName);
                }
            }

            LOG.debug("Tables to be exported {}", tableNames);

            EntityManagerFactory emf = EntityManagerFactoryUtils.findEntityManagerFactory(
                    ApplicationContextProvider.getBeanFactory(), domain);
            Set<EntityType<?>> entityTypes = emf == null ? Set.of() : emf.getMetamodel().getEntities();
            BidiMap<String, EntityType<?>> entities = entities(entityTypes);

            // then sort tables based on foreign keys and dump
            for (String tableName : sortByForeignKeys(schema, conn, tableNames)) {
                try {
                    exportTable(
                            handler, conn, tableName, TABLES_TO_BE_FILTERED.get(tableName.toUpperCase()),
                            entities, taskEntities(entityTypes), relationTables(entities));
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
