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

import java.io.IOException;
import java.util.Properties;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractContentDealer {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractContentDealer.class);

    protected static final String ROOT_ELEMENT = "dataset";

    private static final String PERSISTENCE_PROPERTIES = "/persistence.properties";

    private static final String VIEWS_XML = "/views.xml";

    private static final String INDEXES_XML = "/indexes.xml";

    protected static String dbSchema;

    protected static Properties views;

    protected static Properties indexes;

    @Autowired
    protected DataSource dataSource;

    static {
        try {
            Properties persistence = PropertiesLoaderUtils.loadProperties(
                    new ClassPathResource(PERSISTENCE_PROPERTIES));
            dbSchema = persistence.getProperty("database.schema");

            views = PropertiesLoaderUtils.loadProperties(new ClassPathResource(VIEWS_XML));

            indexes = PropertiesLoaderUtils.loadProperties(new ClassPathResource(INDEXES_XML));
        } catch (IOException e) {
            LOG.error("Could not read one or more properties files", e);
        }
    }

    protected void createIndexes() {
        LOG.debug("Creating indexes");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (String idx : indexes.stringPropertyNames()) {
            LOG.debug("Creating index {}", indexes.get(idx).toString());

            try {
                jdbcTemplate.execute(indexes.get(idx).toString());
            } catch (DataAccessException e) {
                LOG.error("Could not create index ", e);
            }
        }

        LOG.debug("Indexes created");
    }

    protected void createViews() {
        LOG.debug("Creating views");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        for (String idx : views.stringPropertyNames()) {
            LOG.debug("Creating view {}", views.get(idx).toString());

            try {
                jdbcTemplate.execute(views.get(idx).toString().replaceAll("\\n", " "));
            } catch (DataAccessException e) {
                LOG.error("Could not create view ", e);
            }
        }

        LOG.debug("Ciews created");
    }
}
