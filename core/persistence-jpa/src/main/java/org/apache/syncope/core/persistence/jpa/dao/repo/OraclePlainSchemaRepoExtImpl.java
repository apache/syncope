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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.common.dao.AbstractSearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.SearchSupport;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;

public class OraclePlainSchemaRepoExtImpl extends AbstractPlainSchemaRepoExt {

    protected static final String HAS_ATTRS_QUERY = "SELECT COUNT(id) AS counts FROM %TABLE% "
            + "WHERE JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"%SCHEMA%\")')";

    protected static String hasAttrQueryWithValue(final String table, final PlainSchema schema) {
        return new StringBuilder("SELECT COUNT(id) FROM ").
            append(table).
            append(" WHERE JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"").
            append(schema.getKey()).append("\" && @.uniqueValue.").append(AbstractSearchDAO.key(schema.getType())).
            append(" == $value)' PASSING ?1 AS \"value\") AND id <> ?2").
            toString();
    }

    protected final PlainSchemaDAO plainSchemaDAO;

    public OraclePlainSchemaRepoExtImpl(
            final AnyUtilsFactory anyUtilsFactory,
            final ExternalResourceDAO resourceDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityManager entityManager) {

        super(anyUtilsFactory, resourceDAO, entityManager);
        this.plainSchemaDAO = plainSchemaDAO;
    }

    @Override
    public boolean hasAttrs(final PlainSchema schema) {
        return hasAttrs(schema, HAS_ATTRS_QUERY, StringUtils.EMPTY);
    }

    @Override
    public boolean existsPlainAttrUniqueValue(
            final String realmKey,
            final PlainSchema schema,
            final PlainAttrValue attrValue) {

        return doExistsPlainAttrUniqueValue(JPARealm.TABLE, schema,
            attrValue.getValue(), realmKey);
    }

    @Override
    public boolean existsPlainAttrUniqueValue(
            final AnyUtils anyUtils,
            final String anyKey,
            final PlainSchema schema,
            final PlainAttrValue attrValue) {

        return doExistsPlainAttrUniqueValue(new SearchSupport(anyUtils.anyTypeKind()).table().name(), schema,
            attrValue.getValue(), anyKey);
    }

    protected boolean doExistsPlainAttrUniqueValue(final String table, final PlainSchema schema,
        final String attrValue, final String attrKey) {

        Query query = entityManager.createNativeQuery(hasAttrQueryWithValue(table, schema));

        query.setParameter(1, attrValue);
        query.setParameter(2, attrKey);

        return ((Number) query.getSingleResult()).longValue() > 0;
    }
}
