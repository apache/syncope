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
package org.apache.syncope.core.persistence.dao.impl;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.beans.conf.CSchema;
import org.apache.syncope.core.persistence.beans.conf.SyncopeConf;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConfDAOImpl extends AbstractDAOImpl implements ConfDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Transactional(readOnly = true)
    @Override
    public CAttr find(final String key) {
        return get().getAttr(key);
    }

    @Transactional(readOnly = true)
    @Override
    public CAttr find(final String key, final String defaultValue) {
        CAttr result = get().getAttr(key);
        if (result == null) {
            result = new CAttr();
            result.setSchema(schemaDAO.find(key, CSchema.class));

            result.addValue(defaultValue, AttributableUtil.getInstance(AttributableType.CONFIGURATION));
        }

        return result;
    }

    @Override
    public SyncopeConf get() {
        SyncopeConf instance = entityManager.find(SyncopeConf.class, 1L);
        if (instance == null) {
            instance = new SyncopeConf();
            instance.setId(1L);

            instance = entityManager.merge(instance);
        }

        return instance;
    }

    @Override
    public SyncopeConf save(final CAttr attr) {
        delete(attr.getSchema().getName());

        SyncopeConf instance = get();
        instance.addAttr(attr);
        return entityManager.merge(instance);
    }

    @Override
    public SyncopeConf delete(final String key) {
        SyncopeConf instance = get();
        CAttr attr = instance.getAttr(key);
        if (attr != null) {
            instance.removeAttr(attr);
            instance = entityManager.merge(instance);
        }

        return instance;
    }
}
