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
package org.apache.syncope.core.keymaster.rest.cxf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.syncope.common.keymaster.rest.api.service.ConfParamService;
import org.apache.syncope.core.logic.ConfParamLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfParamServiceImpl implements ConfParamService {

    private static final long serialVersionUID = 3954522705963997651L;

    protected static final Logger LOG = LoggerFactory.getLogger(ConfParamService.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ConfParamLogic logic;

    public ConfParamServiceImpl(final ConfParamLogic logic) {
        this.logic = logic;
    }

    @Override
    public Map<String, Object> list() {
        return logic.list();
    }

    @Override
    public Response get(final String key) {
        return Response.ok(logic.get(key)).build();
    }

    @Override
    public void set(final String key, final InputStream value) {
        JsonNode valueNode = null;
        try {
            valueNode = MAPPER.readTree(value);
        } catch (IOException e) {
            LOG.error("Could not deserialize body as valid JSON", e);
        }

        logic.set(key, valueNode);
    }

    @Override
    public void remove(final String key) {
        logic.remove(key);
    }
}
