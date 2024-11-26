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

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.util.Map;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.keymaster.internal.InternalConfParamHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ConfParamLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final InternalConfParamHelper helper;

    public ConfParamLogic(final InternalConfParamHelper helper) {
        this.helper = helper;
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        return helper.list();
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    @Transactional(readOnly = true)
    public JsonNode get(final String key) {
        return helper.get(key);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void set(final String key, final JsonNode value) {
        helper.set(key, value);
    }

    @PreAuthorize("@environment.getProperty('keymaster.username') == authentication.name")
    public void remove(final String key) {
        helper.remove(key);
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args) {
        throw new UnsupportedOperationException();
    }
}
