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
package org.apache.syncope.core.provisioning.java;

import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;

public class DummyConfParamOps implements ConfParamOps {

    @Override
    public Map<String, Object> list(final String domain) {
        return Map.of();
    }

    @Override
    public <T> T get(final String domain, final String key, final T defaultValue, final Class<T> reference) {
        return defaultValue;
    }

    @Override
    public <T> void set(final String domain, final String key, final T value) {
    }

    @Override
    public void remove(final String domain, final String key) {
    }
}
