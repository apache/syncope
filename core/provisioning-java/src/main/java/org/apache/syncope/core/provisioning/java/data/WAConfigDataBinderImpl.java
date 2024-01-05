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

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;

public class WAConfigDataBinderImpl implements WAConfigDataBinder {

    protected final WAConfigDAO waConfigDAO;

    protected final EntityFactory entityFactory;

    public WAConfigDataBinderImpl(final WAConfigDAO waConfigDAO, final EntityFactory entityFactory) {
        this.waConfigDAO = waConfigDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public Attr get(final WAConfigEntry waConfigEntry) {
        return new Attr.Builder(waConfigEntry.getKey()).values(waConfigEntry.getValues()).build();
    }

    @Override
    public WAConfigEntry set(final Attr attr) {
        WAConfigEntry entry = waConfigDAO.findById(attr.getSchema()).orElse(null);
        if (entry == null) {
            entry = entityFactory.newEntity(WAConfigEntry.class);
            entry.setKey(attr.getSchema());
        }
        entry.setValues(attr.getValues());
        return entry;
    }
}
