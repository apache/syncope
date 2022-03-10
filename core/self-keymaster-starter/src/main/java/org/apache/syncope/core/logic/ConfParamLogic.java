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
package org.apache.syncope.core.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.persistence.api.dao.ConfParamDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConfParam;
import org.apache.syncope.core.persistence.api.entity.SelfKeymasterEntityFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ConfParamLogic extends AbstractTransactionalLogic<EntityTO> {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected final ConfParamDAO confParamDAO;

    protected final SelfKeymasterEntityFactory entityFactory;

    public ConfParamLogic(final ConfParamDAO confParamDAO, final SelfKeymasterEntityFactory entityFactory) {
        this.confParamDAO = confParamDAO;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
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

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    @Transactional(readOnly = true)
    public JsonNode get(final String key) {
        ConfParam param = confParamDAO.find(key);

        return Optional.ofNullable(param).map(ConfParam::getValue).orElse(null);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    public void set(final String key, final JsonNode value) {
        ConfParam param = confParamDAO.find(key);
        if (param == null) {
            param = entityFactory.newConfParam();
            param.setKey(key);
        }

        if (value == null) {
            throw new NotFoundException("No value provided for " + key);
        }

        param.setValue(value);
        confParamDAO.save(param);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name and not(isAnonymous())")
    public void remove(final String key) {
        confParamDAO.delete(key);
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        throw new UnsupportedOperationException();
    }
}
