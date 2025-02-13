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
package org.apache.syncope.core.spring.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.Encryptor;
import org.apache.syncope.core.persistence.api.EncryptorManager;

public class DefaultEncryptorManager implements EncryptorManager {

    protected final Map<String, DefaultEncryptor> instances = new ConcurrentHashMap<>();

    @Override
    public Encryptor getInstance() {
        return getInstance(null);
    }

    @Override
    public Encryptor getInstance(final String secretKey) {
        String actualKey = StringUtils.isBlank(secretKey) ? DefaultEncryptor.DEFAULT_SECRET_KEY : secretKey;
        return instances.computeIfAbsent(actualKey, DefaultEncryptor::new);
    }
}
