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
import java.util.Properties;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.xml.sax.SAXException;

/**
 * Initialize Database with default content if no data is present already.
 */
public class XMLContentLoader implements ContentLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(XMLContentLoader.class);

    protected final Resource viewsXML;

    protected final Resource indexesXML;

    protected final Environment env;

    public XMLContentLoader(
            final Resource viewsXML,
            final Resource indexesXML,
            final Environment env) {

        this.viewsXML = viewsXML;
        this.indexesXML = indexesXML;
        this.env = env;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        LOG.debug("Loading data for domain [{}]", domain);

        // create EntityManager so OpenJPA will build the SQL schema
        EntityManagerFactoryUtils.findEntityManagerFactory(
                ApplicationContextProvider.getBeanFactory(), domain).createEntityManager();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
        boolean existingData;
        try {
            existingData = jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + JPARealm.TABLE, Integer.class) > 0;
        } catch (DataAccessException e) {
            LOG.error("[{}] Could not access table " + JPARealm.TABLE, domain, e);
            existingData = true;
        }

        if (existingData) {
            LOG.info("[{}] Data found in the database, leaving untouched", domain);
        } else {
            LOG.info("[{}] Empty database found, loading default content", domain);

            try {
                createViews(domain, datasource);
            } catch (IOException e) {
                LOG.error("[{}] While creating views", domain, e);
            }
            try {
                createIndexes(domain, datasource);
            } catch (IOException e) {
                LOG.error("[{}] While creating indexes", domain, e);
            }
            try {
                InputStream contentXML = ApplicationContextProvider.getBeanFactory().
                        getBean(domain + "ContentXML", InputStream.class);
                loadDefaultContent(domain, contentXML, datasource);
            } catch (Exception e) {
                LOG.error("[{}] While loading default content", domain, e);
            }
        }
    }

    protected void loadDefaultContent(
            final String domain, final InputStream contentXML, final DataSource dataSource)
            throws IOException, ParserConfigurationException, SAXException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (contentXML) {
            SAXParser parser = factory.newSAXParser();
            parser.parse(contentXML, new ContentLoaderHandler(dataSource, ROOT_ELEMENT, true, env));
            LOG.debug("[{}] Default content successfully loaded", domain);
        }
    }

    protected void createViews(final String domain, final DataSource dataSource) throws IOException {
        LOG.debug("[{}] Creating views", domain);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Properties views = PropertiesLoaderUtils.loadProperties(viewsXML);
        views.stringPropertyNames().stream().sorted().forEach(idx -> {
            LOG.debug("[{}] Creating view {}", domain, views.get(idx).toString());
            try {
                jdbcTemplate.execute(views.getProperty(idx).replaceAll("\\n", " "));
            } catch (DataAccessException e) {
                LOG.error("[{}] Could not create view", domain, e);
            }
        });

        LOG.debug("Views created");
    }

    protected void createIndexes(final String domain, final DataSource dataSource) throws IOException {
        LOG.debug("[{}] Creating indexes", domain);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Properties indexes = PropertiesLoaderUtils.loadProperties(indexesXML);
        indexes.stringPropertyNames().stream().sorted().forEach(idx -> {
            LOG.debug("[{}] Creating index {}", domain, indexes.get(idx).toString());
            try {
                jdbcTemplate.execute(indexes.getProperty(idx));
            } catch (DataAccessException e) {
                LOG.error("[{}] Could not create index", domain, e);
            }
        });

        LOG.debug("Indexes created");
    }
}
