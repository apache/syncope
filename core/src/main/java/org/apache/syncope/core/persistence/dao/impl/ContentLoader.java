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
package org.apache.syncope.core.persistence.dao.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.util.ImportExport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialize Database with default content if no data is present already.
 */
@Component
public class ContentLoader extends AbstractContentDealer {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ImportExport importExport;

    @Transactional
    public void load() {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);

            boolean existingData = isDataPresent(conn);
            if (existingData) {
                LOG.info("Data found in the database, leaving untouched");
            } else {
                LOG.info("Empty database found, loading default content");

                loadDefaultContent();
                createIndexes(conn);
                createViews(conn);
            }
        } finally {
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
    }

    private boolean isDataPresent(final Connection conn) {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            final String queryContent = "SELECT * FROM " + SyncopeConf.class.getSimpleName();
            statement = conn.prepareStatement(
                    queryContent, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = statement.executeQuery();
            rs.last();
            return rs.getRow() > 0;
        } catch (SQLException e) {
            LOG.error("Could not access to table " + SyncopeConf.class.getSimpleName(), e);
            return true;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.error("While closing tables result set", e);
                }
            }

            closeStatement(statement);
        }
    }

    private void loadDefaultContent() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/" + ImportExport.CONTENT_FILE);

            SAXParser parser = factory.newSAXParser();
            parser.parse(in, importExport);
            LOG.debug("Default content successfully loaded");
        } catch (Exception e) {
            LOG.error("While loading default content", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
