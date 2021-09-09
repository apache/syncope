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
package org.apache.syncope.core.persistence.jpa.entity;

import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIEntityFactory;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProviderItem;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;

public class JPAOIDCC4UIEntityFactory implements OIDCC4UIEntityFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(OIDCC4UIProvider.class)) {
            result = (E) new JPAOIDCC4UIProvider();
        } else if (reference.equals(OIDCC4UIProviderItem.class)) {
            result = (E) new JPAOIDCC4UIProviderItem();
        } else if (reference.equals(OIDCC4UIUserTemplate.class)) {
            result = (E) new JPAOIDCC4UIUserTemplate();
        } else {
            throw new IllegalArgumentException("Could not find a JPA implementation of " + reference.getName());
        }

        if (result instanceof AbstractGeneratedKeyEntity) {
            ((AbstractGeneratedKeyEntity) result).setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        return result;
    }
}
