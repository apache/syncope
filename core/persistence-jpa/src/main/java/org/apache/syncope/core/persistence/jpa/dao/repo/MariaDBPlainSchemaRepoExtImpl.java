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
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.dao.SearchSupport;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class MariaDBPlainSchemaRepoExtImpl extends AbstractPlainSchemaRepoExt {

    protected static final String HAS_ATTRS_QUERY = "SELECT id FROM %TABLE% "
            + "WHERE JSON_CONTAINS(plainAttrs, '[{\"schema\":\"%SCHEMA%\"}]') ";

    public MariaDBPlainSchemaRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        super(anyUtilsFactory, resourceDAO, entityManager);
    }

    @Override
    public boolean hasAttrs(final PlainSchema schema) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(id) FROM ( "
                + TABLES.stream().
                        map(t -> HAS_ATTRS_QUERY.replace("%TABLE%", t).replace("%SCHEMA%", schema.getKey())).
                        collect(Collectors.joining(" UNION "))
                + ") AS count");

        return ((Number) query.getSingleResult()).intValue() > 0;
    }

    @Override
    public boolean existsPlainAttrUniqueValue(
            final AnyUtils anyUtils,
            final String anyKey,
            final PlainSchema schema,
            final PlainAttrValue attrValue) {

        PlainAttr attr = new PlainAttr();
        attr.setSchema(schema.getKey());
        attr.setUniqueValue(attrValue);

        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM "
                + new SearchSupport(anyUtils.anyTypeKind()).field().name()
                + " WHERE JSON_CONTAINS(plainAttrs, '" + POJOHelper.serialize(List.of(attr)).replace("'", "''") + "')"
                + " AND id <> ?1");
        query.setParameter(1, anyKey);

        return ((Number) query.getSingleResult()).intValue() > 0;
    }
}
