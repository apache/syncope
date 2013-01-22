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
import java.util.Properties;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.util.ImportExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialize Database with default content if no data is present already.
 */
@Component
public class ContentLoader {

    private static final String VIEWS_FILE = "/views.xml";

    private static final String INDEXES_FILE = "/indexes.xml";

    private static final String CONTENT_FILE = "/content.xml";

    private static final String ACTIVITY_CONTENT_FILE = "/activiticontent.xml";

    private static final Logger LOG = LoggerFactory.getLogger(ContentLoader.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ImportExport importExport;

    @Transactional
    public void load(final boolean activitiEnabledForUsers) {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);

            boolean existingData = isDataPresent(conn);
            if (existingData) {
                LOG.info("Data found in the database, leaving untouched");
            } else {
                LOG.info("Empty database found, loading default content");

                createViews(conn);
                createIndexes(conn);
                if (activitiEnabledForUsers) {
                    deleteActivitiProperties(conn);
                }
                loadDefaultContent(CONTENT_FILE);
                if (activitiEnabledForUsers) {
                    loadDefaultContent(ACTIVITY_CONTENT_FILE);
                }
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

    private void createViews(final Connection conn) {
        LOG.debug("Creating views");
        InputStream viewsStream = null;
        try {
            viewsStream = getClass().getResourceAsStream(VIEWS_FILE);
            Properties views = new Properties();
            views.loadFromXML(viewsStream);

            for (String idx : views.stringPropertyNames()) {
                LOG.debug("Creating view {}", views.get(idx).toString());
                PreparedStatement statement = null;
                try {
                    final String updateViews = views.get(idx).toString().replaceAll("\\n", " ");
                    statement = conn.prepareStatement(updateViews);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    LOG.error("Could not create view ", e);
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }

            LOG.debug("Views created, go for indexes");
        } catch (Exception e) {
            LOG.error("While creating views", e);
        } finally {
            if (viewsStream != null) {
                IOUtils.closeQuietly(viewsStream);
            }
        }
    }

    private void createIndexes(final Connection conn) {
        LOG.debug("Creating indexes");

        InputStream indexesStream = null;
        Properties indexes = new Properties();
        try {
            indexesStream = getClass().getResourceAsStream(INDEXES_FILE);
            indexes.loadFromXML(indexesStream);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from stream", e);
        } finally {
            IOUtils.closeQuietly(indexesStream);
        }

        for (String idx : indexes.stringPropertyNames()) {
            LOG.debug("Creating index {}", indexes.get(idx).toString());
            PreparedStatement statement = null;
            try {
                final String updateIndexed = indexes.get(idx).toString();
                statement = conn.prepareStatement(updateIndexed);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOG.error("Could not create index ", e);
            } finally {
                closeStatement(statement);
            }
        }
    }

    private void deleteActivitiProperties(final Connection conn) {
        PreparedStatement statement = null;
        try {
            statement = conn.prepareStatement("DELETE FROM ACT_GE_PROPERTY");
            statement.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error during ACT_GE_PROPERTY delete rows", e);
        } finally {
            closeStatement(statement);
        }
    }

    private void loadDefaultContent(final String contentPath) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream(contentPath);

            SAXParser parser = factory.newSAXParser();
            parser.parse(in, importExport);
            LOG.debug("Default content successfully loaded");
        } catch (Exception e) {
            LOG.error("While loading default content", e);
        } finally {
            if (in != null) {
                IOUtils.closeQuietly(in);
            }
        }
    }

    private void closeStatement(final PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOG.error("Error closing SQL statement", e);
            }
        }
    }
}
