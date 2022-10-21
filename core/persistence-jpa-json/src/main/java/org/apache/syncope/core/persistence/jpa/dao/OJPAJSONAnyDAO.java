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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class OJPAJSONAnyDAO extends AbstractJPAJSONAnyDAO {

    public OJPAJSONAnyDAO(final PlainSchemaDAO plainSchemaDAO) {
        super(plainSchemaDAO);
    }

    @Override
    protected String queryBegin(final String table) {
        return "SELECT DISTINCT id FROM " + view(table) + ' ';
    }

    @Override
    protected Object getAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return schema.getType() == AttrSchemaType.Boolean
                ? BooleanUtils.toStringTrueFalse(attrValue.getBooleanValue())
                : schema.getType() == AttrSchemaType.String && ignoreCaseMatch
                ? StringUtils.lowerCase(attrValue.getStringValue())
                : attrValue.getValue();
    }

    @Override
    protected String attrValueMatch(
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        StringBuilder query = new StringBuilder("plainSchema = ? AND ");

        Pair<String, Boolean> schemaInfo = schemaInfo(schema.getType(), ignoreCaseMatch);
        query.append(schemaInfo.getRight() ? "LOWER(" : "");

        if (schema.isUniqueConstraint()) {
            query.append("u").append(schemaInfo.getLeft());
        } else {
            query.append("JSON_VALUE(").append(schemaInfo.getLeft()).append(", '$[*]')");
        }

        query.append(schemaInfo.getRight() ? ")" : "").
                append(" = ").
                append(schemaInfo.getRight() ? "LOWER(" : "").
                append('?').append(schemaInfo.getRight() ? ")" : "");

        return query.toString();
    }
}
