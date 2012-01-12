/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.syncope.core.util.multiparent.CycleInMultiParentTreeException;
import org.syncope.core.util.multiparent.MultiParentNode;
import org.syncope.core.util.multiparent.MultiParentNodeOp;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

@Component
public class ImportExport extends DefaultHandler {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ImportExport.class);

    private final static String ROOT_ELEMENT = "dataset";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @Override
    public void startElement(final String uri, final String localName,
            final String qName, final Attributes atts)
            throws SAXException {

        // skip root element
        if (ROOT_ELEMENT.equals(qName)) {
            return;
        }

        StringBuilder queryString =
                new StringBuilder("INSERT INTO ").append(qName).append('(');

        StringBuilder values = new StringBuilder();

        for (int i = 0; i < atts.getLength(); i++) {
            queryString.append(atts.getQName(i));
            values.append('?');
            if (i < atts.getLength() - 1) {
                queryString.append(',');
                values.append(',');
            }
        }
        queryString.append(") VALUES (").append(values).append(')');

        Query query = entityManager.createNativeQuery(queryString.toString());
        for (int i = 0; i < atts.getLength(); i++) {
            query.setParameter(i + 1, atts.getValue(i));
        }
        query.executeUpdate();
    }

    private void doExportTable(final TransformerHandler handler,
            final Connection conn, final String tableName)
            throws SQLException, SAXException {

        AttributesImpl atts = new AttributesImpl();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM " + tableName + " a");
            rs = stmt.executeQuery();
            for (int rowNo = 0; rs.next(); rowNo++) {
                atts.clear();

                ResultSetMetaData metaData = rs.getMetaData();
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i + 1);
                    String value = rs.getString(columnName);
                    if (value != null) {
                        atts.addAttribute("", "", columnName, "CDATA", value);
                    }
                }

                handler.startElement("", "", tableName, atts);
                handler.endElement("", "", tableName);
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
                    LOG.error("While closing result set", e);
                }
            }
        }
    }

    private List<String> sortByForeignKeys(final Connection conn,
            final String schema, final Set<String> tableNames)
            throws SQLException, CycleInMultiParentTreeException {

        MultiParentNode<String> root =
                new MultiParentNode<String>(ROOT_ELEMENT);

        for (String tableName : tableNames) {
            MultiParentNode<String> node =
                    MultiParentNodeOp.findInTree(root, tableName);
            if (node == null) {
                node = new MultiParentNode<String>(tableName);
                root.addChild(node);
            }

            // manca lo schema - per Oracle
            ResultSet rs = null;
            try {
                rs = conn.getMetaData().
                        getExportedKeys(conn.getCatalog(), schema, tableName);
                while (rs.next()) {
                    String fkTableName = rs.getString("FKTABLE_NAME");
                    if (!tableName.equals(fkTableName)) {
                        MultiParentNode<String> fkNode =
                                MultiParentNodeOp.findInTree(root, fkTableName);
                        if (fkNode == null) {
                            fkNode = new MultiParentNode<String>(fkTableName);
                            root.addChild(fkNode);
                        }
                        fkNode.addChild(node);
                        if (root.isParent(node)) {
                            root.removeChild(node);
                        }
                    }
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
        }

        List<String> sortedTableNames = new ArrayList<String>(tableNames.size());
        MultiParentNodeOp.traverseTree(root, sortedTableNames);
        return sortedTableNames.subList(0, sortedTableNames.size() - 1);
    }

    public void export(final OutputStream os)
            throws SAXException, TransformerConfigurationException,
            CycleInMultiParentTreeException {

        // 0. read persistence.properties (for database schema)
        InputStream dbPropsStream = null;
        String dbSchema = null;
        try {
            dbPropsStream = getClass().getResourceAsStream(
                    "/persistence.properties");
            Properties dbProps = new Properties();
            dbProps.load(dbPropsStream);
            dbSchema = dbProps.getProperty("database.schema");
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not find persistence.properties", t);
            } else {
                LOG.error("Could not find persistence.properties");
            }
        } finally {
            if (dbPropsStream != null) {
                try {
                    dbPropsStream.close();
                } catch (IOException e) {
                    LOG.error("While trying to read persistence.properties", e);
                }
            }
        }

        StreamResult streamResult = new StreamResult(os);
        SAXTransformerFactory transformerFactory =
                (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        TransformerHandler handler = transformerFactory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        handler.setResult(streamResult);
        handler.startDocument();
        handler.startElement("", "", ROOT_ELEMENT, new AttributesImpl());

        Connection conn = DataSourceUtils.getConnection(dataSource);
        ResultSet rs = null;
        try {
            // first read all tables...
            rs = conn.getMetaData().getTables(
                    null, null, null, new String[]{"TABLE"});
            Set<String> tableNames = new HashSet<String>();
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // these tables must be ignored
                if (!tableName.toUpperCase().startsWith("QRTZ_")
                        && !tableName.toUpperCase().equals("ACT_GE_PROPERTY")) {

                    tableNames.add(tableName);
                }
            }
            // then sort tables based on foreign keys and dump
            for (String tableName :
                    sortByForeignKeys(conn, dbSchema, tableNames)) {

                doExportTable(handler, conn, tableName);
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
        }

        handler.endElement("", "", ROOT_ELEMENT);
        handler.endDocument();
    }
}
