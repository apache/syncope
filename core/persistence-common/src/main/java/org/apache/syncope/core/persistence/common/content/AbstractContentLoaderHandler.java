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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractContentLoaderHandler extends DefaultHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractContentLoaderHandler.class);

    private static final String CONF_DIR = "syncope.conf.dir";

    private final String rootElement;

    protected final boolean continueOnError;

    protected final Map<String, String> fetches = new HashMap<>();

    protected final StringSubstitutor paramSubstitutor;

    protected AbstractContentLoaderHandler(
            final String rootElement,
            final boolean continueOnError,
            final Environment env) {

        this.rootElement = rootElement;
        this.continueOnError = continueOnError;
        this.paramSubstitutor = new StringSubstitutor(key -> {
            String value = env.getProperty(key, fetches.get(key));
            if (value != null && CONF_DIR.equals(key)) {
                value = value.replace('\\', '/');
            }
            return StringUtils.isBlank(value) ? null : value;
        });
    }

    protected abstract void fetch(Attributes atts);

    protected abstract void create(String qName, Attributes atts);

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts) {

        // skip root element
        if (rootElement.equals(qName)) {
            return;
        }

        if ("fetch".equalsIgnoreCase(qName)) {
            fetch(atts);
        } else {
            create(qName, atts);
        }
    }
}
