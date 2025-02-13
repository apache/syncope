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
package org.apache.syncope.core.persistence.neo4j.content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.common.content.AbstractXMLContentLoader;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

public class XMLContentLoader extends AbstractXMLContentLoader {

    protected final DomainHolder<Driver> domainHolder;

    protected final Neo4jMappingContext mappingContext;

    protected final Resource indexesXML;

    public XMLContentLoader(
            final DomainHolder<Driver> domainHolder,
            final Neo4jMappingContext mappingContext,
            final Resource indexesXML,
            final Environment env) {

        super(env);
        this.domainHolder = domainHolder;
        this.mappingContext = mappingContext;
        this.indexesXML = indexesXML;
    }

    @Override
    protected boolean existingData(final String domain) {
        boolean existingData;
        try (Session session = domainHolder.getDomains().get(domain).session()) {
            existingData = session.run("MATCH (n:Realm) WITH COUNT(n) > 0 AS node_exists RETURN node_exists").
                    single().get(0).asBoolean();
        } catch (Exception e) {
            LOG.error("[{}] Could not access node Realm", domain, e);
            existingData = true;
        }
        return existingData;
    }

    @Override
    protected void createViews(final String domain) {
        // nothing to do
    }

    @Override
    protected void createIndexes(final String domain) throws IOException {
        LOG.debug("[{}] Creating indexes", domain);

        try (Session session = domainHolder.getDomains().get(domain).session()) {
            Properties indexes = PropertiesLoaderUtils.loadProperties(indexesXML);
            indexes.stringPropertyNames().stream().sorted().forEach(idx -> {
                LOG.debug("[{}] Creating index {}", domain, indexes.get(idx).toString());
                try {
                    session.run(indexes.getProperty(idx));
                } catch (Exception e) {
                    LOG.error("[{}] Could not create index", domain, e);
                }
            });
        }

        LOG.debug("Indexes created");
    }

    @Override
    protected void loadDefaultContent(final String domain, final String contentXML) throws Exception {
        InputStream in = ApplicationContextProvider.getBeanFactory().getBean(contentXML, InputStream.class);
        try (in) {
            saxParser().parse(in, new ContentLoaderHandler(
                    domainHolder.getDomains().get(domain), mappingContext, ROOT_ELEMENT, true, env));
            LOG.debug("[{}] Default content successfully loaded", domain);
        }
    }
}
