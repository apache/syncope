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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.dao.SearchSupport;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class MySQLPlainSchemaRepoExtImpl extends AbstractPlainSchemaRepoExt {

    public MySQLPlainSchemaRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        super(anyUtilsFactory, resourceDAO, entityManager);
    }

    @Override
    public <T extends PlainAttr<?>> boolean hasAttrs(final PlainSchema schema, final Class<T> reference) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM " + new SearchSupport(getAnyTypeKind(reference)).field().name()
                + " WHERE JSON_CONTAINS(plainAttrs, '[{\"schema\":\"" + schema.getKey() + "\"}]')");

        return ((Number) query.getSingleResult()).intValue() > 0;
    }

    @Override
    public boolean existsPlainAttrUniqueValue(
            final AnyTypeKind anyTypeKind,
            final String anyKey,
            final PlainAttr<?> attr) {

        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM " + new SearchSupport(anyTypeKind).field().name()
                + " WHERE JSON_CONTAINS(plainAttrs, '" + POJOHelper.serialize(List.of(attr)).replace("'", "''") + "')"
                + " AND id <> ?1");
        query.setParameter(1, anyKey);

        return ((Number) query.getSingleResult()).intValue() > 0;
    }
}
