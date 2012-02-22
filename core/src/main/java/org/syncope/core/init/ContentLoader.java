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
package org.syncope.core.init;

import org.syncope.core.util.ImportExport;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SyncopeConf;

/**
 * If empty, load default content to Syncope database by reading from
 * <code>content.xml</code>.
 */
@Component
public class ContentLoader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ContentLoader.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ImportExport importExport;

    @Transactional
    public void load() {
        // 0. DB connection, to be used below
        Connection conn = DataSourceUtils.getConnection(dataSource);

        // 1. Check wether we are allowed to load default content into the DB
        Statement statement = null;
        ResultSet resultSet = null;
        boolean existingData = false;
        try {
            statement = conn.createStatement(
                    ResultSet.TYPE_SCROLL_SENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            resultSet = statement.executeQuery("SELECT * FROM "
                    + SyncopeConf.class.getSimpleName());
            resultSet.last();

            existingData = resultSet.getRow() > 0;
        } catch (SQLException e) {
            LOG.error("Could not access to table "
                    + SyncopeConf.class.getSimpleName(), e);

            // Setting this to true make nothing to be done below
            existingData = true;
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                LOG.error("While closing SQL result set", e);
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                LOG.error("While closing SQL statement", e);
            }
        }

        if (existingData) {
            LOG.info("Data found in the database, leaving untouched");
            return;
        }

        LOG.info("Empty database found, loading default content");

        // 2. Create views
        LOG.debug("Creating views");
        try {
            InputStream viewsStream = getClass().getResourceAsStream(
                    "/views.xml");
            Properties views = new Properties();
            views.loadFromXML(viewsStream);

            for (String idx : views.stringPropertyNames()) {
                LOG.debug("Creating view {}", views.get(idx).toString());

                try {
                    statement = conn.createStatement();
                    statement.executeUpdate(views.get(idx).toString().
                            replaceAll("\\n", " "));
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Could not create view ", e);
                }
            }

            LOG.debug("Views created, go for indexes");
        } catch (Throwable t) {
            LOG.error("While creating views", t);
        }

        // 3. Create indexes
        LOG.debug("Creating indexes");
        try {
            InputStream indexesStream = getClass().getResourceAsStream(
                    "/indexes.xml");
            Properties indexes = new Properties();
            indexes.loadFromXML(indexesStream);

            for (String idx : indexes.stringPropertyNames()) {
                LOG.debug("Creating index {}", indexes.get(idx).toString());

                try {
                    statement = conn.createStatement();
                    statement.executeUpdate(indexes.get(idx).toString());
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Could not create index ", e);
                }
            }

            LOG.debug("Indexes created, go for default content");
        } catch (Throwable t) {
            LOG.error("While creating indexes", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        try {
            conn.close();
        } catch (SQLException e) {
            LOG.error("While closing SQL connection", e);
        }

        // 4. Load default content
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(getClass().getResourceAsStream("/content.xml"),
                    importExport);
            LOG.debug("Default content successfully loaded");
        } catch (Throwable t) {
            LOG.error("While loading default content", t);
        }
    }
}
