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
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.dao.OracleJPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.SearchSupport;

public class OraclePlainSchemaRepoExtImpl extends AbstractPlainSchemaRepoExt {

    protected static final String HAS_ATTRS_QUERY = "SELECT id FROM %TABLE%, %JSON_TABLE% ";

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
        Query query = entityManager.createNativeQuery("SELECT COUNT(id) FROM ( "
                + TABLES.stream().
                        map(t -> HAS_ATTRS_QUERY.replace("%TABLE%", t).
                        replace("%JSON_TABLE%", OracleJPAAnySearchDAO.from(schema))).
                        collect(Collectors.joining(" UNION "))
                + ")");

        return ((Number) query.getSingleResult()).intValue() > 0;
    }

    @Override
    public boolean existsPlainAttrUniqueValue(
            final AnyUtils anyUtils,
            final String anyKey,
            final PlainSchema schema,
            final PlainAttrValue attrValue) {

        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM "
                + new SearchSupport(anyUtils.anyTypeKind()).table().name() + ","
                + OracleJPAAnySearchDAO.from(plainSchemaDAO.findById(schema.getKey()).
                        orElseThrow(() -> new NotFoundException("PlainSchema " + schema.getKey())))
                + " WHERE " + schema.getKey() + ".uniqueValue=?1 AND id <> ?2");
        query.setParameter(1, attrValue.getValue());
        query.setParameter(2, anyKey);

        return ((Number) query.getSingleResult()).intValue() > 0;
    }
}
