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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.TreeMap;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.ConfParamDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.ConfParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class InternalConfParamHelper {

    protected static final Logger LOG = LoggerFactory.getLogger(InternalConfParamHelper.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ConfParamDAO confParamDAO;

    protected final EntityFactory entityFactory;

    public InternalConfParamHelper(final ConfParamDAO confParamDAO, final EntityFactory entityFactory) {
        this.confParamDAO = confParamDAO;
        this.entityFactory = entityFactory;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        Map<String, Object> params = new TreeMap<>();
        confParamDAO.findAll().forEach(param -> {
            try {
                params.put(param.getKey(), MAPPER.treeToValue(param.getValue(), Object.class));
            } catch (JsonProcessingException e) {
                LOG.error("While processing {}'s value", param.getKey(), e);
            }
        });
        return params;
    }

    @Transactional(readOnly = true)
    public JsonNode get(final String key) {
        return confParamDAO.findById(key).map(ConfParam::getValue).orElse(null);
    }

    @Transactional
    public void set(final String key, final JsonNode value) {
        if (value == null) {
            throw new NotFoundException("No value provided for " + key);
        }

        ConfParam param = confParamDAO.findById(key).orElse(null);
        if (param == null) {
            param = entityFactory.newEntity(ConfParam.class);
            param.setKey(key);
        }
        param.setValue(value);
        confParamDAO.save(param);
    }

    @Transactional
    public void remove(final String key) {
        confParamDAO.deleteById(key);
    }
}
