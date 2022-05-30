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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.persistence.api.content.ConfParamLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initialize Keymaster with default content if no data is present already.
 */
public class KeymasterConfParamLoader implements ConfParamLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(KeymasterConfParamLoader.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ConfParamOps confParamOps;

    public KeymasterConfParamLoader(final ConfParamOps confParamOps) {
        this.confParamOps = confParamOps;
    }

    @Override
    public int getOrder() {
        return 450;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        boolean existingData;
        try {
            existingData = !confParamOps.list(domain).isEmpty();
        } catch (Exception e) {
            LOG.error("[{}] Could not access Keymaster", domain, e);
            existingData = true;
        }

        if (existingData) {
            LOG.info("[{}] Data found in Keymaster, leaving untouched", domain);
        } else {
            LOG.info("[{}] Empty Keymaster found, loading default content", domain);

            try {
                InputStream contentJSON = ApplicationContextProvider.getBeanFactory().
                        getBean(domain + "KeymasterConfParamsJSON", InputStream.class);
                loadDefaultContent(domain, contentJSON);
            } catch (Exception e) {
                LOG.error("[{}] While loading default Keymaster content", domain, e);
            }
        }
    }

    protected void loadDefaultContent(final String domain, final InputStream contentJSON)
            throws IOException {

        try (contentJSON) {
            JsonNode content = MAPPER.readTree(contentJSON);
            for (Iterator<Map.Entry<String, JsonNode>> itor = content.fields(); itor.hasNext();) {
                Map.Entry<String, JsonNode> param = itor.next();
                Object value = MAPPER.treeToValue(param.getValue(), Object.class);
                if (value != null) {
                    confParamOps.set(domain, param.getKey(), value);
                }
            }
        }
    }
}
