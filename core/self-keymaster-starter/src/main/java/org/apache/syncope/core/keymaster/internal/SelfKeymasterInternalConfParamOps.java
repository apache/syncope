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
package org.apache.syncope.core.keymaster.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class SelfKeymasterInternalConfParamOps implements ConfParamOps {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfParamOps.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final InternalConfParamHelper helper;

    public SelfKeymasterInternalConfParamOps(final InternalConfParamHelper helper) {
        this.helper = helper;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> list(final String domain) {
        return helper.list();
    }

    @Transactional(readOnly = true)
    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        JsonNode valueNode = helper.get(key);
        if (valueNode == null) {
            return defaultValue;
        }

        try {
            return MAPPER.treeToValue(valueNode, reference);
        } catch (IOException e) {
            LOG.error("Could not deserialize {}", valueNode, e);
            return defaultValue;
        }
    }

    @Transactional
    @Override
    public <T> void set(final String domain, final String key, final T value) {
        if (value == null) {
            remove(domain, key);
        } else {
            helper.set(key, MAPPER.valueToTree(value));
        }
    }

    @Transactional
    @Override
    public void remove(final String domain, final String key) {
        helper.remove(key);
    }
}
