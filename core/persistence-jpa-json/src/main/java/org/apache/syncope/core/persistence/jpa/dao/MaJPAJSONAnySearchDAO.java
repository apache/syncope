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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class MaJPAJSONAnySearchDAO extends JPAAnySearchDAO {

    public MaJPAJSONAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                entityManagerFactory,
                entityManager);
    }

    @Override
    protected String getQuery(
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked = check(cond, svs.anyTypeKind);

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        StringBuilder query =
                new StringBuilder("SELECT DISTINCT any_id FROM ").append(svs.field().name()).append(" WHERE ");
        switch (cond.getType()) {
            case ISNOTNULL:
                query.append("JSON_SEARCH(plainAttrs, 'one', '").
                        append(checked.getLeft().getKey()).
                        append("', NULL, '$[*].schema') IS NOT NULL");
                break;

            case ISNULL:
                query.append("JSON_SEARCH(plainAttrs, 'one', '").
                        append(checked.getLeft().getKey()).
                        append("', NULL, '$[*].schema') IS NULL");
                break;

            default:
                if (!not && cond.getType() == AttrCond.Type.EQ) {
                    PlainAttr<?> container = anyUtilsFactory.getInstance(svs.anyTypeKind).newPlainAttr();
                    container.setSchema(checked.getLeft());
                    if (checked.getRight() instanceof PlainAttrUniqueValue plainAttrUniqueValue) {
                        container.setUniqueValue(plainAttrUniqueValue);
                    } else {
                        ((JSONPlainAttr) container).add(checked.getRight());
                    }

                    query.append("JSON_CONTAINS(plainAttrs, '").
                            append(POJOHelper.serialize(List.of(container)).replace("'", "''")).
                            append("')");
                } else {
                    query = new StringBuilder("SELECT DISTINCT any_id FROM ");
                    if (not && !(cond instanceof AnyCond) && checked.getLeft().isMultivalue()) {
                        query.append(svs.field().name()).append(" WHERE ");
                    } else {
                        query.append((checked.getLeft().isUniqueConstraint()
                                ? svs.asSearchViewSupport().uniqueAttr().name()
                                : svs.asSearchViewSupport().attr().name())).
                                append(" WHERE schema_id='").append(checked.getLeft().getKey());
                    }

                    Optional.ofNullable(checked.getRight().getDateValue()).
                            map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                            ifPresent(formatted -> {
                                checked.getRight().setDateValue(null);
                                checked.getRight().setStringValue(formatted);
                            });

                    fillAttrQuery(query, checked.getRight(), checked.getLeft(), cond, not, parameters, svs);
                }
        }

        return query.toString();
    }
}
