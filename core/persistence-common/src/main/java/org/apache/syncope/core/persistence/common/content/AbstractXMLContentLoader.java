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
package org.apache.syncope.core.persistence.common.content;

import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.syncope.core.persistence.api.content.ContentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.xml.sax.SAXException;

public abstract class AbstractXMLContentLoader implements ContentLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(ContentLoader.class);

    protected final Environment env;

    protected AbstractXMLContentLoader(final Environment env) {
        this.env = env;
    }

    @Override
    public int getOrder() {
        return 400;
    }

    protected abstract boolean existingData(String domain);

    protected abstract void createViews(String domain) throws IOException;

    protected abstract void createIndexes(String domain) throws IOException;

    protected SAXParser saxParser()
            throws ParserConfigurationException, SAXException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newSAXParser();
    }

    protected abstract void loadDefaultContent(String domain, String contentXML) throws Exception;

    @Override
    public void load(final String domain) {
        LOG.debug("Loading data for domain [{}]", domain);

        if (existingData(domain)) {
            LOG.info("[{}] Data found in the database, leaving untouched", domain);
        } else {
            LOG.info("[{}] Empty database found, loading default content", domain);

            try {
                createViews(domain);
            } catch (IOException e) {
                LOG.error("[{}] While creating views", domain, e);
            }

            try {
                createIndexes(domain);
            } catch (IOException e) {
                LOG.error("[{}] While creating indexes", domain, e);
            }

            try {
                loadDefaultContent(domain, domain + "ContentXML");
            } catch (Exception e) {
                LOG.error("[{}] While loading default content", domain, e);
            }
        }
    }

}
