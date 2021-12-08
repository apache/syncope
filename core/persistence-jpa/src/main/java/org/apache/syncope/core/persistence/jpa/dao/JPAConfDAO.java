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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.Optional;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class JPAConfDAO extends AbstractDAO<Conf> implements ConfDAO {

    private static final String KEY = "cd64d66f-6fff-4008-b966-a06b1cc1436d";

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Conf get() {
        Conf instance = entityManager().find(entityFactory.confClass(), KEY);
        if (instance == null) {
            instance = entityFactory.newEntity(Conf.class);
            instance.setKey(KEY);

            instance = entityManager().merge(instance);
        }

        return instance;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<? extends CPlainAttr> find(final String key) {
        return get().getPlainAttr(key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> getValuesAsStrings(final String key) {
        return find(key).map(CPlainAttr::getValuesAsStrings).orElse(Collections.emptyList());
    }

    @Transactional(readOnly = true)
    @Override
    public <T> T find(final String key, final T defaultValue) {
        return find(key).
                map(attr -> attr.getUniqueValue() == null
                ? attr.getValues().isEmpty()
                ? null
                : attr.getValues().get(0).<T>getValue()
                : attr.getUniqueValue().<T>getValue()).
                orElse(defaultValue);
    }

    @Override
    public Conf save(final CPlainAttr attr) {
        Conf instance = get();

        Optional<? extends CPlainAttr> old = instance.getPlainAttr(attr.getSchema().getKey());
        if (old.isPresent() && (!attr.getSchema().isUniqueConstraint()
                || (!attr.getUniqueValue().getStringValue().equals(old.get().getUniqueValue().getStringValue())))) {

            old.get().setOwner(null);
            instance.remove(old.get());
        }

        instance.add(attr);
        attr.setOwner(instance);

        return entityManager().merge(instance);
    }

    @Override
    public Conf delete(final String key) {
        Conf instance = get();
        Optional<? extends CPlainAttr> attr = instance.getPlainAttr(key);
        if (attr.isPresent()) {
            attr.get().setOwner(null);
            instance.remove(attr.get());

            instance = entityManager().merge(instance);
        }

        return instance;
    }
}
