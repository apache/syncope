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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.content.ConfParamLoader;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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
    public void load(final String domain) {
        AuthContextUtils.runAsAdmin(domain, new Runnable() {

            @Transactional
            @Override
            public void run() {
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

                    try (InputStream contentJSON = ApplicationContextProvider.getBeanFactory().
                            getBean(domain + "KeymasterConfParamsJSON", InputStream.class)) {

                        JsonNode content = MAPPER.readTree(contentJSON);
                        for (Iterator<Map.Entry<String, JsonNode>> itor = content.fields(); itor.hasNext();) {
                            Map.Entry<String, JsonNode> param = itor.next();
                            Optional.ofNullable(MAPPER.treeToValue(param.getValue(), Object.class)).
                                    ifPresent(value -> confParamOps.set(domain, param.getKey(), value));
                        }
                    } catch (Exception e) {
                        LOG.error("[{}] While loading default Keymaster content", domain, e);
                    }
                }
            }
        });
    }
}
