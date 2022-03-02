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
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.core.logic.ConfParamLogic;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfKeymasterInternalConfParamOps implements ConfParamOps {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfParamOps.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ConfParamLogic logic;

    protected final KeymasterProperties props;

    public SelfKeymasterInternalConfParamOps(final ConfParamLogic logic, final KeymasterProperties props) {
        this.logic = logic;
        this.props = props;
    }

    @Override
    public Map<String, Object> list(final String domain) {
        return AuthContextUtils.callAs(
                domain,
                props.getUsername(),
                List.of(),
                () -> logic.list());
    }

    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        JsonNode valueNode = AuthContextUtils.callAs(
                domain,
                props.getUsername(),
                List.of(),
                () -> logic.get(key));
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

    @Override
    public <T> void set(final String domain, final String key, final T value) {
        if (value == null) {
            remove(domain, key);
        } else {
            JsonNode valueNode = MAPPER.valueToTree(value);

            AuthContextUtils.callAs(domain, props.getUsername(), List.of(), () -> {
                logic.set(key, valueNode);
                return null;
            });
        }
    }

    @Override
    public void remove(final String domain, final String key) {
        AuthContextUtils.callAs(domain, props.getUsername(), List.of(), () -> {
            logic.remove(key);
            return null;
        });
    }
}
