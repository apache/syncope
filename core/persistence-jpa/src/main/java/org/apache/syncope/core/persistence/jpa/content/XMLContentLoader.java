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
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.common.content.AbstractXMLContentLoader;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Initialize Database with default content if no data is present already.
 */
public class XMLContentLoader extends AbstractXMLContentLoader {

    protected final DomainHolder<DataSource> domainHolder;

    protected final Resource viewsXML;

    protected final Resource indexesXML;

    public XMLContentLoader(
            final DomainHolder<DataSource> domainHolder,
            final Resource viewsXML,
            final Resource indexesXML,
            final Environment env) {

        super(env);
        this.domainHolder = domainHolder;
        this.viewsXML = viewsXML;
        this.indexesXML = indexesXML;
    }

    @Override
    protected boolean existingData(final String domain) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(domainHolder.getDomains().get(domain));
        boolean existingData;
        try {
            existingData = jdbcTemplate.queryForObject("SELECT COUNT(0) FROM " + JPARealm.TABLE, Integer.class) > 0;
        } catch (DataAccessException e) {
            LOG.error("[{}] Could not access table {}", domain, JPARealm.TABLE, e);
            existingData = true;
        }
        return existingData;
    }

    @Override
    protected void createViews(final String domain) throws IOException {
        LOG.debug("[{}] Creating views", domain);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(domainHolder.getDomains().get(domain));

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

    @Override
    protected void createIndexes(final String domain) throws IOException {
        LOG.debug("[{}] Creating indexes", domain);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(domainHolder.getDomains().get(domain));

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

    @Override
    protected void loadDefaultContent(final String domain, final String contentXML) throws Exception {
        InputStream in = ApplicationContextProvider.getBeanFactory().getBean(contentXML, InputStream.class);
        try (in) {
            saxParser().parse(in, new ContentLoaderHandler(
                    domainHolder.getDomains().get(domain), ROOT_ELEMENT, true, env));
            LOG.debug("[{}] Default content successfully loaded", domain);
        }
    }
}
