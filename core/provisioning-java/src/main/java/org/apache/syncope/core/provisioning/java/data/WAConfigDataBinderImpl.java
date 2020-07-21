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

package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.WAConfigTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WAConfigDataBinderImpl implements WAConfigDataBinder {
    @Autowired
    private EntityFactory entityFactory;

    @Override
    public WAConfigTO getConfigTO(final WAConfigEntry waConfigEntry) {
        WAConfigTO configTO = new WAConfigTO();

        configTO.setKey(waConfigEntry.getKey());
        configTO.setValue(waConfigEntry.getValue());

        return configTO;
    }

    @Override
    public WAConfigEntry create(final WAConfigTO configTO) {
        return update(entityFactory.newEntity(WAConfigEntry.class), configTO);
    }

    @Override
    public WAConfigEntry update(final WAConfigEntry entry, final WAConfigTO configTO) {
         return getConfigEntry(entry, configTO);
    }

    private WAConfigEntry getConfigEntry(
        final WAConfigEntry configEntry,
        final WAConfigTO configTO) {

        WAConfigEntry result = configEntry;
        if (result == null) {
            result = entityFactory.newEntity(WAConfigEntry.class);
        }
        result.setValue(configTO.getValue());
        result.setKey(configTO.getKey());
        return result;
    }
}
