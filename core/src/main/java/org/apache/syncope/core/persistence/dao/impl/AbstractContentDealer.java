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
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractContentDealer {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractContentDealer.class);

    private static final String VIEWS_FILE = "/views.xml";

    private static final String INDEXES_FILE = "/indexes.xml";

    protected void createIndexes(final Connection conn) {
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

    protected void createViews(final Connection conn) {
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
            IOUtils.closeQuietly(viewsStream);
        }
    }

    protected void closeStatement(final PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOG.error("Error closing SQL statement", e);
            }
        }
    }
}
